package studentcalendar;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ListIterator;
import org.json.simple.*;

/**
 *
 * @author Samu
 */
public class Kalenteri {

    private ArrayList<Event> events;

    private ArrayList<Course> courses;

    private String dataFileName = "events.json";

    public static void main(String[] args) throws Exception {
        Kalenteri c = new Kalenteri("events.json");
        c.fetchCourses("http://www.cs.helsinki.fi/u/tkairi/rajapinta/courses.json");
        c.addEvent("kurssi 4", "A111", new Date(), new Date());
        SimpleDateFormat startdf = new SimpleDateFormat("dd.MM.yyyy   HH:mm");
        SimpleDateFormat enddf = new SimpleDateFormat("HH:mm");
        SimpleDateFormat koe = new SimpleDateFormat("dd.MM.yyyy.HH.mm");
        Date date = koe.parse("13.09.1989.13.23");
        System.out.println(date);
        Event[] evnts = c.findAllEvents();
        for (Event e : evnts) {
            System.out.println(startdf.format(e.getStartDate()) + "-" + enddf.format(e.getEndDate()) + "   " + e.getName() + "   " + e.getLocation());
        }
    }

    /**
     * Constructor
     * @param dataFileName Relative path to the JSON file where events are loaded from. Defaults to "events.json"
     * @throws IOException
     */
    public Kalenteri(String dataFileName) throws IOException {
        if (dataFileName != null) {
            this.dataFileName = dataFileName;
        }
        courses = new ArrayList<Course>();
        events = new ArrayList<Event>();
        FileReader in = null;
        try {
            in = new FileReader(dataFileName);
            JSONObject root = (JSONObject) JSONValue.parse(in);
            JSONArray eventAr = (JSONArray) root.get("events");
            ListIterator<JSONObject> it = eventAr.listIterator();
            while (it.hasNext()) {
                JSONObject obj = it.next();
                Date startDate = new Date(((Number) obj.get("start_date")).longValue());
                Date endDate = new Date(((Number) obj.get("end_date")).longValue());
                events.add(new Event((String) obj.get("name"), (String) obj.get("location"), startDate, endDate));
            }
        } catch (Exception e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Fetches an up-to-date list of courses from a remote JSON file.
     * @param jsonurl URL to the remote JSON file.
     */
    public void fetchCourses(String jsonurl) {
        if (jsonurl == null) {
            throw new NullPointerException("jsonurl");
        }
        InputStreamReader in = null;
        try {
            URL url = new URL(jsonurl);
            in = new InputStreamReader(url.openConnection().getInputStream());
            JSONObject root = (JSONObject) JSONValue.parse(in);
            JSONArray courseAr = (JSONArray) root.get("courses");
            ListIterator<JSONObject> it = courseAr.listIterator();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            while (it.hasNext()) {
                JSONObject obj = it.next();
                Course c;
                try {
                    c = new Course((String) obj.get("course"), df.parse((String) obj.get("start_date")), df.parse((String) obj.get("end_date")));
                    courses.add(c);
                } catch (ParseException pe) {
                }
            }
            in.close();
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Converts strings to GregorianCalendar objects. The string must be in the following format: yyyy-mm-dd
     * @param date The date string
     * @return The GregorianCalendar object
     */
    @Deprecated
    public static GregorianCalendar strToCalendar(String date) {
        String[] tmp = date.split("-");
        return new GregorianCalendar(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]));
    }

    /**
     * Converts a GregorianCalendar object to a date string in the following format: dd.mm.yyyy
     * @param cal the GregorianCalendar to convert
     * @return the resulting string
     */
    @Deprecated
    public static String calendarToDateStr(GregorianCalendar cal) {
        String s = "";
        s += cal.get(GregorianCalendar.DAY_OF_MONTH);
        s += '.';
        s += cal.get(GregorianCalendar.MONTH) + 1;
        s += '.';
        s += cal.get(GregorianCalendar.YEAR);
        return s;
    }

    /**
     * Converts a GregorianCalendar object to a time string in the following format: hh:mm
     * @param cal the GregorianCalendar to convert
     * @return the resulting string
     */
    @Deprecated
    public static String calendarToTimeStr(GregorianCalendar cal) {
        String s = "";
        s += cal.get(GregorianCalendar.HOUR);
        s += ':';
        s += cal.get(GregorianCalendar.MINUTE);
        return s;
    }

    /**
     * Converts a GregorianCalendar object to a string in the following format: dd.mm.yyyy hh:mm
     * @param cal the GregorianCalendar to convert
     * @return the resulting string
     */
    @Deprecated
    public static String calendarToDateTimeStr(GregorianCalendar cal) {
        return calendarToDateStr(cal) + " " + calendarToTimeStr(cal);
    }

    /**
     * Finds events from the event list by their name.
     * @param name Search string.
     * @return A list of matching events.
     */
    public Event[] findEventsByName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (name.equals("")) {
            throw new IllegalArgumentException("name");
        }
        ArrayList<Event> matches = new ArrayList<Event>();
        ListIterator<Event> it = events.listIterator();
        name = name.toLowerCase();
        while (it.hasNext()) {
            Event e = it.next();
            if (e.getName().toLowerCase().contains(name)) {
                matches.add(e);
            }
        }
        Event[] ar = new Event[matches.size()];
        matches.toArray(ar);
        Arrays.sort(ar);
        return ar;
    }

    /**
     * Finds events from the event list by their starting date. Includes all
     * events that start within the given range (between start and end).
     * @param start Start of the range.
     * @param end End of the range.
     * @return A list of matching events.
     */
    public Event[] findEventsByDate(Date start, Date end) {
        if (start == null) {
            throw new NullPointerException("start");
        }
        if (end == null) {
            throw new NullPointerException("end");
        }
        ArrayList<Event> matches = new ArrayList<Event>();
        ListIterator<Event> it = events.listIterator();
        while (it.hasNext()) {
            Event t = it.next();
            if (t.getStartDate().after(start) && t.getStartDate().before(end)) {
                matches.add(t);
            }
        }
        Event[] ar = new Event[matches.size()];
        matches.toArray(ar);
        Arrays.sort(ar);
        return ar;
    }

    public Event[] findAllEvents() {
        Event[] ar = new Event[events.size()];
        events.toArray(ar);
        return ar;
    }

    public int getEventCount() {
        return events.size();
    }

    public void addEvent(Event event) {
        events.add(event);
        saveData();
    }

    public void addEvent(String name, String location, Date startDate, Date endDate) {
        addEvent(new Event(name, location, startDate, endDate));
    }

    public void removeEvents(Event event) {
        events.remove(event);
        saveData();
    }

    public void removeEvents(Event[] eventAr) {
        for (Event e : eventAr) {
            events.remove(e);
        }
        saveData();
    }

    private void saveData() {
        FileWriter out = null;
        try {
            out = new FileWriter(dataFileName);
            out.write("{\"events\":" + JSONValue.toJSONString(events) + "}");
        } catch (IOException e) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                }
            }
        }
    }
}
