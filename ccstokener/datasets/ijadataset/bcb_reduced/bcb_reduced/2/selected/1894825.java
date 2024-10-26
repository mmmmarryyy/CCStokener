package calendar.remote;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedInputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Summary;
import calendar.DefaultAppointment;
import calendar.DefaultCalendarModel;
import calendar.Appointment;

/**
 * Example Calendar Model for the iCal Format (using ical4j) - it's incomplete, as it's only a proof-of-concept 
 * @author Florian Roks
 *
 */
public class ICalCalendarModel extends DefaultCalendarModel {

    private static final Log LOG = LogFactory.getLog(ICalCalendarModel.class);

    private String uri;

    private Calendar iCalendar;

    public ICalCalendarModel(String uri, String proxy, int proxyport) {
        super();
        setUri(uri, proxy, proxyport);
    }

    public ICalCalendarModel(String uri) {
        this(uri, null, 0);
    }

    private void setUri(String uri, String proxy, int proxyport) {
        try {
            String oldUri = this.uri;
            this.uri = uri;
            URL url = new URL(uri);
            URLConnection urlCon;
            if (proxy != null) urlCon = url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy, proxyport))); else urlCon = url.openConnection();
            BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
            CalendarBuilder builder = new CalendarBuilder();
            iCalendar = builder.build(in);
            updateAppointments();
            propertyChangeSupport.firePropertyChange("uri", oldUri, this.uri);
        } catch (Throwable t) {
            t.printStackTrace();
            LOG.fatal("FATAL Error: " + t.getMessage());
        }
    }

    private void updateAppointments() {
        Collection<Appointment> appointments = new ArrayList<Appointment>();
        for (Iterator<Component> i = iCalendar.getComponents().iterator(); i.hasNext(); ) {
            Component component = i.next();
            if (component.getName() != "VEVENT") continue;
            DefaultAppointment appointment = new DefaultAppointment();
            if (component.getProperty("DTSTART") == null) {
                LOG.info("no Start date");
                continue;
            }
            if (component.getProperty("DTEND") == null) {
                LOG.info("no end date");
                continue;
            }
            appointment.setAppointmentStartDate(new Date(((DtStart) component.getProperty("DTSTART")).getDate().getTime()));
            appointment.setAppointmentEndDate(new Date(((DtEnd) component.getProperty("DTEND")).getDate().getTime()));
            if (component.getProperty("SUMMARY") != null) {
                String summary = ((Summary) component.getProperty("SUMMARY")).getValue();
                appointment.setAppointmentName(summary);
            }
            if (component.getProperty("DESCRIPTION") != null) {
                String description = ((Description) component.getProperty("DESCRIPTION")).getValue();
                appointment.setAppointmentDescription(description);
            }
            int sequenceNr = ((Sequence) component.getProperty("SEQUENCE")).getSequenceNo();
            if (sequenceNr == 1) {
                appointment.setAppointmentType(Appointment.AppointmentType.ALLDAY);
            }
            appointments.add(appointment);
        }
        this.setAppointments(appointments);
    }
}
