package org.javacraft.qa.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.javacraft.qa.model.DailyTrafficJamState;
import org.javacraft.qa.model.NationalTrafficState;
import org.javacraft.qa.model.TrafficJamState;

/**
 *
 * @author jan
 */
public class TrafficJamServiceImpl implements TrafficJamService {

    private static final Logger LOGGER = Logger.getLogger(TrafficJamServiceImpl.class.getName());

    private static final String SERVICE_URL = "http://trafficinfoservice.be-mobile.be/ContentService.asmx";

    private final EntityManager em = Persistence.createEntityManagerFactory("transactions-optional").createEntityManager();

    static {
        try {
            Velocity.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new NullLogChute());
            Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            Velocity.init();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private final Template trafficLength2Template;

    private final Template trafficLengthReportPast24H;

    public TrafficJamServiceImpl() {
        try {
            trafficLength2Template = Velocity.getTemplate("/TrafficLength2.vm");
            trafficLengthReportPast24H = Velocity.getTemplate("/TrafficLengthReportPast24H.vm");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public NationalTrafficState getCurrentState() {
        final String response = getServiceResponse("http://www.be-mobile.be/TrafficLength2", trafficLength2Template);
        System.out.println(response);
        final Date date = XMLUtils.getElementDate(response, "DateRecorded");
        final int jamLength = XMLUtils.getElementInteger(response, "TotalTrafficJamLength");
        final int lengthEver = XMLUtils.getElementInteger(response, "MaxFileLengthEver");
        return new NationalTrafficState(date, jamLength, lengthEver);
    }

    @Override
    public List<TrafficJamState> getLast24HStates() {
        final String response = getServiceResponse("http://www.be-mobile.be/TrafficLengthReportPast24H", trafficLengthReportPast24H);
        System.out.println(response);
        final String text = XMLUtils.getElementString(response, "TrafficLengthReportPast24HResult");
        if (text != null) {
            final List<TrafficJamState> states = new ArrayList<TrafficJamState>();
            for (final String state : text.trim().split("\\s")) {
                if (state != null && state.trim().length() > 0) {
                    states.add(TrafficJamState.fromReportEntry(state));
                }
            }
            return states;
        }
        return null;
    }

    public DailyTrafficJamState getTrafficState(final Date date) {
        final Date midnight = new Date(date.getYear(), date.getMonth(), date.getDate(), 0, 0, 0);
        try {
            return (DailyTrafficJamState) em.createQuery("SELECT ts FROM DailyTrafficJamState ts WHERE ts.timeQueue = :date").setParameter("date", midnight).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    private static String getServiceResponse(final String requestName, final Template template) {
        return getServiceResponse(requestName, template, new HashMap());
    }

    private static String getServiceResponse(final String requestName, final Template template, final Map variables) {
        OutputStreamWriter outputWriter = null;
        try {
            final StringWriter writer = new StringWriter();
            final VelocityContext context = new VelocityContext(variables);
            template.merge(context, writer);
            final String request = writer.toString();
            final URLConnection urlConnection = new URL(SERVICE_URL).openConnection();
            urlConnection.setUseCaches(false);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2b4) Gecko/20091124 Firefox/3.6b4");
            urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            urlConnection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
            urlConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            urlConnection.setRequestProperty("Accept-Encoding", "gzip,deflate");
            urlConnection.setRequestProperty("Keep-Alive", "115");
            urlConnection.setRequestProperty("Connection", "keep-alive");
            urlConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            urlConnection.setRequestProperty("Content-Length", "" + request.length());
            urlConnection.setRequestProperty("SOAPAction", requestName);
            outputWriter = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
            outputWriter.write(request);
            outputWriter.flush();
            final InputStream result = urlConnection.getInputStream();
            return IOUtils.toString(result);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (outputWriter != null) {
                try {
                    outputWriter.close();
                } catch (IOException logOrIgnore) {
                }
            }
        }
    }

    @Override
    public void RegisterLast12HStates() {
        List<TrafficJamState> jams = getLast24HStates();
        if (jams.size() != 288) {
            LOGGER.warning(String.format("Jams collections is %d instead of 288 !", jams.size()));
        }
        try {
            em.getTransaction().begin();
            DailyTrafficJamState currentJam = null;
            for (final TrafficJamState jam : jams) {
                if (currentJam == null || !DateUtils.isSameDay(currentJam.getDate(), jam.getDate())) {
                    if (currentJam != null) {
                        currentJam.setLengths(currentJam.getLengths());
                        System.out.println(Arrays.toString(currentJam.getLengths()));
                    }
                    em.getTransaction().commit();
                    em.getTransaction().begin();
                    currentJam = getTrafficState(jam.getDate());
                    if (currentJam == null) {
                        currentJam = em.merge(new DailyTrafficJamState());
                        currentJam.setDate(new Date(jam.getDate().getYear(), jam.getDate().getMonth(), jam.getDate().getDate(), 0, 0, 0));
                    }
                }
                final int minutes = (jam.getDate().getHours() * 60) + jam.getDate().getMinutes();
                final int index = minutes / 5;
                final int oldLength = currentJam.getLength(index);
                final int newLength = jam.getCurrentJamLength();
                if (oldLength > 0 && oldLength != newLength) {
                    LOGGER.warning(String.format("Overwriting different lengths ! %s by %s meter.", oldLength, newLength));
                }
                currentJam.setLength(index, jam.getCurrentJamLength());
            }
            currentJam.setLengths(currentJam.getLengths());
            System.out.println(Arrays.toString(currentJam.getLengths()));
            em.getTransaction().commit();
        } catch (Exception exception) {
            em.getTransaction().rollback();
            throw new RuntimeException(exception);
        }
    }
}
