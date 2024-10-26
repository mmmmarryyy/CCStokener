package ru.aslanov.schedule.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import ru.aslanov.schedule.client.ScheduleServices;
import ru.aslanov.schedule.client.UserData;
import ru.aslanov.schedule.model.*;
import ru.aslanov.schedule.server.gcalendar.GCalendarSyncService;
import ru.aslanov.schedule.utils.MemCacheUtil;
import ru.aslanov.schedule.utils.XMLUtil;
import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * Created: Feb 11, 2010 6:03:47 PM
 *
 * @author Sergey Aslanov
 */
public class ScheduleServicesImpl extends RemoteServiceServlet implements ScheduleServices {

    private static final Logger log = Logger.getLogger(ScheduleServicesImpl.class.getName());

    private static final String PUBLISH_ENC = "Cp1251";

    @Override
    public void publishSchedule(String scheduleKey) throws Exception {
        publish(scheduleKey, true);
    }

    @Override
    public void unpublishSchedule(String scheduleKey) throws Exception {
        publish(scheduleKey, false);
    }

    private void publish(String scheduleKey, boolean publish) throws Exception {
        final PersistenceManager pm = PMF.getThreadLocalPersistenceManager();
        final Transaction tran = pm.currentTransaction();
        AccessManager.getInstance().checkScheduleAdmin(scheduleKey);
        try {
            final Schedule persistentSchedule = pm.getObjectById(Schedule.class, scheduleKey);
            Document newDocument = null;
            if (publish) {
                String xmlData = scheduleToXml(persistentSchedule, persistentSchedule.getDefaultInputLanguage());
                newDocument = new Document(xmlData.getBytes("UTF-8"), "text/xml");
            }
            tran.begin();
            Document document = persistentSchedule.getPublishedDocument();
            if (document != null) {
                pm.deletePersistent(document);
            }
            persistentSchedule.setPublishedDocument(newDocument);
            tran.commit();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Publish error", e);
            throw e;
        } finally {
            if (tran.isActive()) tran.rollback();
        }
    }

    public void postXmlToUrl(String scheduleKey) throws Exception {
        AccessManager.getInstance().checkScheduleAdmin(scheduleKey);
        final PersistenceManager pm = PMF.getThreadLocalPersistenceManager();
        try {
            final Schedule persistentSchedule = pm.getObjectById(Schedule.class, scheduleKey);
            GCalendarSyncService.enqueEventsSync(scheduleKey);
            final I18nString publishUrlI18n = persistentSchedule.getPublishUrlI18n();
            boolean wasPublished = false;
            for (InputLang inputLang : persistentSchedule.getInputLangs()) {
                String publishUrl = publishUrlI18n.getValue(inputLang.getId());
                if (publishUrl != null) {
                    publishLang(persistentSchedule, publishUrl, inputLang.getId());
                    wasPublished = true;
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error publishing", e);
            throw e;
        }
    }

    private void publishLang(Schedule persistentSchedule, String publishUrl, String lang) throws Exception {
        final String xmlData = scheduleToXml(persistentSchedule, lang);
        log.info("Publishing schedule lang=" + lang + " to: " + publishUrl);
        URL url = new URL(publishUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        String authStr = "YWRtaW46YWE0c2RXZWI=";
        connection.setRequestProperty("Authorization", "Basic " + authStr);
        final OutputStream os = connection.getOutputStream();
        addParameter(os, "block_template_id", "0");
        addParameter(os, "description", "");
        addParameter(os, "data_process_id", "0");
        addParameter(os, "data_type_id", "5");
        addParameter(os, "is_published", "1");
        addParameter(os, "btnUpdate", "true");
        addParameter(os, "data", xmlData);
        os.close();
        final InputStream is = connection.getInputStream();
        while (is.read() >= 0) {
        }
        is.close();
        connection.disconnect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Server returned error code: " + connection.getResponseCode());
        }
        log.info("Published successfully");
    }

    private static void addParameter(OutputStream os, String name, String value) throws IOException {
        String str = name + "=" + URLEncoder.encode(value, PUBLISH_ENC) + "&";
        os.write(str.getBytes());
    }

    @Override
    public String getXml(String scheduleKey, String lang) throws Exception {
        AccessManager.getInstance().checkScheduleAdmin(scheduleKey);
        final PersistenceManager pm = PMF.getThreadLocalPersistenceManager();
        final Schedule schedule = pm.getObjectById(Schedule.class, scheduleKey);
        final String xmlData = scheduleToXml(schedule, lang);
        return xmlData;
    }

    @Override
    public void syncGroupDays() throws Exception {
        AccessManager.getInstance().checkAdmin();
        final PersistenceManager pm = PMF.getThreadLocalPersistenceManager();
        final Transaction tran = pm.currentTransaction();
        final Extent<Group> groupExtent = pm.getExtent(Group.class);
        try {
            for (Group group : groupExtent) {
                tran.begin();
                group.syncDays(null);
                tran.commit();
            }
        } finally {
            if (tran.isActive()) tran.rollback();
        }
    }

    @Override
    public void clearMemCache() throws Exception {
        AccessManager.getInstance().checkAdmin();
        MemCacheUtil.clearCache();
    }

    private String scheduleToXml(Schedule persistentSchedule, String lang) throws JAXBException {
        log.fine("scheduleToXml(lang=" + lang + ")");
        InputLangUtil.setThreadLocalInputLang(lang, persistentSchedule.getDefaultInputLanguage());
        try {
            List<Group> groups = new ArrayList<Group>();
            for (Group group : persistentSchedule.getGroups()) {
                if (!group.isHidden()) groups.add(group);
            }
            Schedule detachedSchedule = new Schedule();
            detachedSchedule.setCity(persistentSchedule.getCity());
            detachedSchedule.setGroups(groups);
            detachedSchedule.setDances(persistentSchedule.getDances());
            detachedSchedule.setLevels(persistentSchedule.getLevels());
            detachedSchedule.setTeachers(persistentSchedule.getTeachers());
            detachedSchedule.setLocations(persistentSchedule.getLocations());
            detachedSchedule.setCalendars(persistentSchedule.getCalendars());
            StringWriter stringWriter = new StringWriter();
            final Marshaller marshaller = XMLUtil.JAXB_CONTEXT.createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
            marshaller.setProperty("jaxb.fragment", Boolean.TRUE);
            marshaller.marshal(detachedSchedule, stringWriter);
            return stringWriter.toString();
        } finally {
            InputLangUtil.clearThreadLocalInputLang();
        }
    }

    @Override
    public UserData getUserData() {
        final AccessManager am = AccessManager.getInstance();
        return new UserData(am.getEmail(), am.isAdmin());
    }

    @Override
    public void loadFromGCalendar(String scheduleKey) throws Exception {
        AccessManager.getInstance().checkAdmin();
        final GCalendarSyncService syncService = new GCalendarSyncService();
        syncService.loadFromGCalendar(scheduleKey);
    }

    @Override
    public void publishToGCalendar(String scheduleKey) throws Exception {
        AccessManager.getInstance().checkScheduleAdmin(scheduleKey);
        GCalendarSyncService gCalendarSyncService = new GCalendarSyncService();
        gCalendarSyncService.publishToGCalendar(scheduleKey);
    }

    @Override
    public void removeAuthorization(String scheduleKey) throws Exception {
        AccessManager.getInstance().checkScheduleAdmin(scheduleKey);
        final PersistenceManager pm = PMF.getThreadLocalPersistenceManager();
        final Transaction tran = pm.currentTransaction();
        try {
            final Schedule schedule = pm.getObjectById(Schedule.class, scheduleKey);
            if (schedule.getGoogleCalendarSync() != null) {
                tran.begin();
                schedule.getGoogleCalendarSync().setSessionToken(null);
                schedule.getGoogleCalendarSync().setAuthorizedByUser(null);
                tran.commit();
            }
        } finally {
            if (tran.isActive()) tran.rollback();
        }
    }
}
