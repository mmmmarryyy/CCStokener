package org.lindenb.tool.metaweb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.lindenb.io.IOUtils;
import org.lindenb.json.JSONParser;
import org.lindenb.json.ParseException;
import org.lindenb.lang.ResourceUtils;
import org.lindenb.sw.vocabulary.KML;
import org.lindenb.util.C;
import org.lindenb.util.Compilation;
import org.lindenb.util.TimeUtils;
import org.lindenb.xml.XMLUtilities;

/** abstract class for Date YYYY-MM-DD */
abstract class Date {

    /** day of month */
    private Short dayOfMonth = null;

    /** month 1-12 */
    private Short month = null;

    /** year */
    private int year;

    /** constructor with current date */
    protected Date() {
        GregorianCalendar cal = new GregorianCalendar();
        this.year = cal.get(GregorianCalendar.YEAR);
        this.month = (short) (cal.get(GregorianCalendar.MONTH) + 1);
        this.dayOfMonth = new Short((short) cal.get(GregorianCalendar.DAY_OF_MONTH));
    }

    /** constructor from string YYYY-MM-DD */
    protected Date(String s) {
        boolean neg = false;
        if (s.startsWith("-")) {
            s = s.substring(1);
            neg = true;
        }
        String toks[] = s.split("[\\-]");
        this.year = Integer.parseInt(toks[0]) * (neg ? -1 : 1);
        if (toks.length > 1) {
            this.month = Short.parseShort(toks[1]);
            if (toks.length > 2) {
                this.dayOfMonth = Short.parseShort(toks[2]);
            }
        }
    }

    /** @returns year */
    public int getYear() {
        return year;
    }

    /** @returns month (1-12) or null */
    public Short getMonth() {
        return month;
    }

    /** @returns day of month (1-31) or null */
    public Short getDayOfMonth() {
        return dayOfMonth;
    }

    /** base method to compare dates, used by derived classes */
    protected int compareDate(Date o, int side) {
        int i = getYear() - o.getYear();
        if (i != 0) return i;
        if (getMonth() == null && o.getMonth() == null) {
            return 0;
        } else if (getMonth() != null && o.getMonth() == null) {
            return side;
        } else if (getMonth() == null && o.getMonth() != null) {
            return -side;
        }
        i = getMonth() - o.getMonth();
        if (i != 0) return i;
        if (getDayOfMonth() == null && o.getDayOfMonth() == null) {
            return 0;
        } else if (getDayOfMonth() != null && o.getDayOfMonth() == null) {
            return side;
        } else if (getDayOfMonth() == null && o.getDayOfMonth() != null) {
            return -side;
        }
        return getDayOfMonth() - o.getDayOfMonth();
    }

    public String toISO() {
        StringBuilder b = new StringBuilder();
        b.append(getYear());
        if (getMonth() != null) {
            b.append("-" + (getMonth() < 10 ? "0" : "") + getMonth());
            if (getDayOfMonth() != null) {
                b.append("-" + (getDayOfMonth() < 10 ? "0" : "") + getDayOfMonth());
            }
        }
        return b.toString();
    }

    public String toWikipedia(String locale) {
        StringBuilder b = new StringBuilder();
        if (getMonth() != null) {
            b.append("[[");
            switch(getMonth()) {
                case 1:
                    b.append("January");
                    break;
                case 2:
                    b.append("February");
                    break;
                case 3:
                    b.append("March");
                    break;
                case 4:
                    b.append("April");
                    break;
                case 5:
                    b.append("May");
                    break;
                case 6:
                    b.append("June");
                    break;
                case 7:
                    b.append("July");
                    break;
                case 8:
                    b.append("August");
                    break;
                case 9:
                    b.append("September");
                    break;
                case 10:
                    b.append("October");
                    break;
                case 11:
                    b.append("November");
                    break;
                case 12:
                    b.append("December");
                    break;
                default:
                    b.append(getMonth());
                    break;
            }
            if (getDayOfMonth() != null) {
                b.append(" ");
                b.append(getDayOfMonth() < 10 ? "0" : "").append(String.valueOf(getDayOfMonth()));
            }
            b.append("]], ");
        }
        b.append("[[" + String.valueOf(getYear()) + "]]");
        return b.toString();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (getMonth() != null) {
            switch(getMonth()) {
                case 1:
                    b.append("Jan");
                    break;
                case 2:
                    b.append("Feb");
                    break;
                case 3:
                    b.append("Mar");
                    break;
                case 4:
                    b.append("Apr");
                    break;
                case 5:
                    b.append("May");
                    break;
                case 6:
                    b.append("Jun");
                    break;
                case 7:
                    b.append("Jul");
                    break;
                case 8:
                    b.append("Aug");
                    break;
                case 9:
                    b.append("Sep");
                    break;
                case 10:
                    b.append("Oct");
                    break;
                case 11:
                    b.append("Nov");
                    break;
                case 12:
                    b.append("Dec");
                    break;
                default:
                    b.append("???");
                    break;
            }
            b.append(".");
            if (getDayOfMonth() != null) {
                b.append(" ");
                b.append(getDayOfMonth() < 10 ? "0" : "").append(String.valueOf(getDayOfMonth()));
            }
            b.append(", ");
        }
        b.append(String.valueOf(getYear()));
        return b.toString();
    }
}

/**
 * Start Date
 * @author pierre
 *
 */
class StartDate extends Date implements Comparable<StartDate> {

    public StartDate(String s) {
        super(s);
    }

    @Override
    public int compareTo(StartDate o) {
        return compareDate(o, 1);
    }

    /** convert this date to the number of days since year 0
	 *  using the first day of the year/month */
    public double dayValue() {
        double v = getYear() * 365.25;
        if (getMonth() != null) {
            v += (365.25 / 12.0) * getMonth();
            if (getDayOfMonth() != null) {
                v += getDayOfMonth();
            }
        }
        return v;
    }
}

/**
 * End Date
 * @author pierre
 *
 */
class EndDate extends Date implements Comparable<EndDate> {

    public EndDate() {
        super();
    }

    public EndDate(String s) {
        super(s);
    }

    @Override
    public int compareTo(EndDate o) {
        return compareDate(o, -1);
    }

    /** convert this date to the number of days since year 0 
	 * using the last day of the year/month
	 */
    public double dayValue() {
        double v = 0;
        if (getMonth() != null) {
            if (getDayOfMonth() != null) {
                v += (1 + getDayOfMonth());
                v += getMonth() * (365.25 / 12.0);
            } else {
                v += (getMonth() + 1) * (365.25 / 12.0);
            }
            v += getYear() * 365.25;
        } else {
            v += (1 + getYear()) * 365.25;
        }
        return v;
    }
}

/** Metaweb01 */
public class Metaweb01 {

    private static final String COOKIE = "metaweb-user";

    private static final String BASE_URL = "http://www.freebase.com";

    private static final String MQLREADURL = BASE_URL + "/api/service/mqlread";

    /** metaweb-user cookie */
    private String metawebCookie = null;

    /** google anaytics */
    private String urchinID = null;

    /** where we save our data */
    private File tmpFolder = null;

    /** base URL */
    private String baseURL = null;

    /** default icon size */
    public static final int DEFAULT_ICON_SIZE = 64;

    /** icon size */
    private int iconSize = DEFAULT_ICON_SIZE;

    /** the smallest birth date we found */
    private StartDate minDate = null;

    /** the biggest death date we found */
    private EndDate maxDate = null;

    /** all the person we found */
    private Vector<Person> persons = new Vector<Person>();

    /** number of persons to be fetched */
    private int limitNumberOfPerson = 10000;

    /** echo request */
    private boolean echoRequest = true;

    /** for debugging: a simple class which echos the bytes read by the inputstream */
    private class EchoReader extends InputStream {

        private InputStream in;

        public EchoReader(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            byte array[] = new byte[1];
            return read(array, 0, 1);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = in.read(b, off, len);
            if (n != -1 && isDebugging()) {
                System.err.write(b, off, n);
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /**
     * Metaweb01
     */
    private Metaweb01() {
    }

    private boolean isDebugging() {
        return echoRequest;
    }

    private int getIconSize() {
        return iconSize;
    }

    /** takes as input a freebase image id and save it into this.tmpFolder */
    private File makeIcon(String id) {
        if (id.startsWith("#")) id = id.substring(1);
        File dest = new File(this.tmpFolder, id + ".png");
        if (dest.exists()) return dest;
        try {
            BufferedImage src = ImageIO.read(new URL("http://www.freebase.com/api/trans/raw/guid/" + id));
            BufferedImage img = new BufferedImage(this.iconSize, this.iconSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getIconSize(), getIconSize());
            if (src.getWidth() < src.getHeight()) {
                double ratio = src.getWidth() / (double) src.getHeight();
                int len = (int) (getIconSize() * ratio);
                int x = (getIconSize() - len) / 2;
                int y = 0;
                g.drawImage(src, x, y, len, getIconSize(), null);
            } else {
                double ratio = src.getHeight() / (double) src.getWidth();
                int len = (int) (getIconSize() * ratio);
                int y = (getIconSize() - len) / 2;
                int x = 0;
                g.drawImage(src, x, y, getIconSize(), len, null);
            }
            g.dispose();
            ImageIO.write(img, "png", dest);
            return dest;
        } catch (Exception e) {
            System.err.println("Cannot create icon for " + id + " " + e.getMessage());
            return null;
        }
    }

    /** performs a query over www.rebase.com */
    private Object query(String json) throws IOException, ParseException {
        String envelope = "{\"qname1\":{\"query\":" + json + "}}";
        String urlStr = MQLREADURL + "?queries=" + URLEncoder.encode(envelope, "UTF-8");
        if (isDebugging()) {
            if (echoRequest) System.err.println("Sending:" + envelope);
        }
        URL url = new URL(urlStr);
        URLConnection con = url.openConnection();
        con.setRequestProperty("Cookie", COOKIE + "=" + "\"" + getMetawebCookie() + "\"");
        con.connect();
        InputStream in = con.getInputStream();
        Object item = new JSONParser(echoRequest ? new EchoReader(in) : in).object();
        in.close();
        String code = getString(item, "code");
        if (!"/api/status/ok".equals(code)) {
            throw new IOException("Bad code " + item);
        }
        code = getString(item, "qname1.code");
        if (!"/api/status/ok".equals(code)) {
            throw new IOException("Bad code " + item);
        }
        return item;
    }

    private void run() throws IOException, ParseException {
        Object item = query("[{\"guid\":null,\"type\":\"/user/lindenb/default_domain/scientist\",\"limit\":" + this.limitNumberOfPerson + "}]");
        if (item == null) return;
        Object json1 = find(item, "qname1.result");
        if (!isArray(json1)) return;
        for (Object i : asArray(json1)) {
            if (!isObject(i)) continue;
            handlePerson(asObject(i));
        }
        Collections.sort(persons);
        for (Person o : persons) {
            if (this.minDate == null || o.startDate.compareTo(this.minDate) < 0) {
                this.minDate = o.startDate;
            }
            if (o.endDate != null && (this.maxDate == null || this.maxDate.compareTo(o.endDate) < 0)) {
                this.maxDate = o.endDate;
            }
        }
        Vector<Person> remains = new Vector<Person>(this.persons);
        int nLine = -1;
        while (!remains.isEmpty()) {
            ++nLine;
            Person first = remains.firstElement();
            remains.removeElementAt(0);
            first.y = nLine;
            while (true) {
                Person best = null;
                int bestIndex = -1;
                for (int i = 0; i < remains.size(); ++i) {
                    Person next = remains.elementAt(i);
                    if (next.x1() < first.x2() + 5) continue;
                    if (best == null || (next.x1() - first.x2() < best.x1() - first.x2())) {
                        best = next;
                        bestIndex = i;
                    }
                }
                if (best == null) break;
                first = best;
                first.y = nLine;
                remains.removeElementAt(bestIndex);
            }
        }
        remains = null;
        final int MARGIN = 2;
        PrintWriter out = new PrintWriter(new FileWriter(new File(this.tmpFolder, "history.kml")));
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<kml xmlns=\"" + KML.NS + "\">");
        out.println("<Document>");
        out.println("<name>History of Science</name>");
        for (Person o : persons) {
            for (int side = 0; side < 2; ++side) {
                Date date = (side == 0 ? o.startDate : o.endDate);
                Place place = (side == 0 ? o.startPlace : o.endPlace);
                if (date == null || place == null) continue;
                out.println("<Placemark>");
                out.println("<TimeStamp><when>" + date.toISO() + "</when></TimeStamp>");
                out.println("<name>" + (side == 0 ? "Birth of " : "Death of ") + XMLUtilities.escape(o.get("name")) + " at " + XMLUtilities.escape(o.get(side == 0 ? "place_of_birth" : "place_of_death")) + "</name>");
                out.print("<description>");
                out.print(XMLUtilities.escape("<div style='padding: " + MARGIN + "px; background-color:black; color:white;'>"));
                if (o.iconFile != null) {
                    out.print(XMLUtilities.escape("<img  align ='left' width='" + getIconSize() + "' height='" + getIconSize() + "'  src=\'" + this.baseURL + o.iconFile.getName() + "\'/>"));
                }
                out.print((side == 0 ? "Birth of " : "Death of ") + XMLUtilities.escape(o.getHTML()));
                out.print(XMLUtilities.escape("</div>"));
                out.println("</description>");
                out.print("<Point><coordinates>");
                out.print(place.longitude + "," + place.latitude);
                out.println("</coordinates></Point>");
                out.println("</Placemark>");
            }
        }
        out.println("</Document>");
        out.println("</kml>");
        out.flush();
        out.close();
        out = new PrintWriter(new FileWriter(new File(this.tmpFolder, "person.js")));
        out.println("var persons=[");
        for (int i = 0; i < persons.size(); ++i) {
            if (i > 0) out.println(",");
            Person p = persons.elementAt(i);
            out.println("{");
            out.println("name:" + quote(p.getName()) + ",");
            out.println("guid:" + quote(p.getID()) + ",");
            out.println("gender:" + quote(p.get("gender")) + ",");
            out.println("x1:" + p.x1() + ",");
            out.println("x2:" + p.x2() + ",");
            out.println("y:" + p.y + ",");
            out.println("node:null,");
            out.println("selected:true,");
            out.println("nationality:" + quote(p.getArray("nationality")) + ",");
            out.println("shortBio:" + quote(p.get("shortBio")) + ",");
            out.println("profession:" + quote(p.getArray("profession")) + ",");
            out.print("birthDate:");
            if (p.startDate == null) {
                out.println("null,");
            } else {
                out.println("new StartDate(" + p.startDate.getYear() + "," + (p.startDate.getMonth() == null ? "null,null" : p.startDate.getMonth() + "," + p.startDate.getDayOfMonth()) + "),");
            }
            out.print("birthPlace:" + quote(p.get("place_of_birth")) + ",");
            out.print("deathDate:");
            if (p.endDate == null) {
                out.println("null,");
            } else {
                out.println("new EndDate(" + p.endDate.getYear() + "," + (p.endDate.getMonth() == null ? "null,null" : p.endDate.getMonth() + "," + p.endDate.getDayOfMonth()) + "),");
            }
            out.println("deathPlace:" + quote(p.get("place_of_death")) + ",");
            out.println("knownFor:" + quote(p.getArray("knownFor")) + ",");
            out.println("img:" + quote(p.iconFile == null ? null : p.iconFile.getName()) + ",");
            out.print("awards:[");
            for (int k = 0; k < p.awards.size(); ++k) {
                if (k > 0) out.print(",");
                out.print(quote(p.awards.elementAt(k).name));
            }
            out.println("]");
            out.println("}");
        }
        out.println("];");
        out.flush();
        out.close();
        String xul = ResourceUtils.getContent(getClass(), "history.xul");
        out = new PrintWriter(new FileWriter(new File(this.tmpFolder, "history.xul")));
        xul = xul.replaceAll("__ABOUT__", "Updated: " + TimeUtils.toYYYYMMDD()).replaceAll(Pattern.quote("<!-- __URCHIN__ -->"), (this.urchinID == null ? "" : "<script src=\"http://www.google-analytics.com/urchin.js\" type=\"text/javascript\">" + "</script>" + "<script type=\"text/javascript\">" + "_uacct = \"" + urchinID + "\";" + "urchinTracker();" + "</script>")).replace("__KML__", "http://maps.google.com/maps?f=q&amp;hl=en&amp;geocode=&amp;q=" + XMLUtilities.escape(URLEncoder.encode(this.baseURL + "history.kml", "UTF-8")) + "&amp;ie=UTF8&amp;ll=53.956086,-13.007812&amp;spn=99.233765,284.0625&amp;t=h&amp;z=2");
        out.print(xul);
        out.flush();
        out.close();
        out = new PrintWriter(new FileWriter(new File(this.tmpFolder, "history.js")));
        out.print(ResourceUtils.getContent(getClass(), "history.js"));
        out.flush();
        out.close();
        out = new PrintWriter(new FileWriter(new File(this.tmpFolder, "infoboxes_en.txt")));
        for (Person o : this.persons) {
            String wikipediaurl = "http://en.wikipedia.org/wiki/" + URLEncoder.encode(o.getName().replace(' ', '_'), "UTF-8");
            try {
                String s = IOUtils.getURLContent(new URL("http://en.wikipedia.org/w/index.php?title=" + URLEncoder.encode(o.getName().replace(' ', '_'), "UTF-8") + "&action=edit"));
                s = s.toLowerCase().replaceAll("[ ]", "_");
                if (s.contains("infobox_scientist") || s.contains("infobox_person")) continue;
            } catch (Exception e) {
                e.printStackTrace();
            }
            out.println(wikipediaurl);
            out.println();
            out.println("{{Infobox Scientist");
            out.println("|name              = " + o.getName());
            out.println("|box_width         =");
            out.println("|image             = " + ("Female".equals(o.getGender()) ? "Replace_this_image_female.svg" : "Replace_this_image_male.svg"));
            out.println("|image_width       = 150px");
            out.println("|caption           = " + o.getName());
            out.println("|birth_date        = " + (o.startDate == null ? "" : o.startDate.toWikipedia("en")));
            out.println("|birth_place       = " + (o.getBirthPlace() == null ? "" : "[[" + o.getBirthPlace() + "]]"));
            out.println("|death_date        = " + (o.endDate == null ? "" : o.endDate.toWikipedia("en")));
            out.println("|death_place       = " + (o.getDeathPlace() == null ? "" : "[[" + o.getDeathPlace() + "]]"));
            out.println("|residence         = ");
            out.println("|citizenship       = ");
            out.print("|nationality       =");
            for (String s : o.getArray("nationality")) out.print(", [[" + s + "]]");
            out.println();
            out.println("|ethnicity         = ");
            out.print("|field             = ");
            for (String s : o.getArray("profession")) out.print(", [[" + s + "]]");
            out.println();
            out.println("|work_institutions = ");
            out.println("|alma_mater        = ");
            out.println("|doctoral_advisor  = ");
            out.println("|doctoral_students = ");
            out.print("|known_for         = ");
            for (String s : o.getArray("knownFor")) out.print(", [[" + s + "]]");
            out.println();
            out.println("|author_abbrev_bot = ");
            out.println("|author_abbrev_zoo = ");
            out.println("|influences        = ");
            out.println("|influenced        = ");
            out.print("|prizes            =");
            for (Award s : o.awards) {
                out.print(", [[" + s.name + "]]");
                if (s.year != null) out.print("([[" + s.year + "]])");
            }
            out.println();
            out.println("|footnotes         = ");
            out.println("|signature         =");
            out.println("}}");
            out.println();
            out.println("============================================================");
            out.println();
        }
        out.flush();
        out.close();
        out = new PrintWriter(new FileWriter(new File(this.tmpFolder, "history.ical")));
        out.println("BEGIN:VCALENDAR");
        out.println("CALSCALE:GREGORIAN");
        out.println("METHOD:PUBLISH");
        out.println("X-WR-CALNAME;VALUE=TEXT:History Of Sciences");
        out.println("VERSION:2.0");
        for (Person o : this.persons) {
            for (int side = 0; side < 2; ++side) {
                Date date = (side == 0 ? o.startDate : o.endDate);
                if (date == null || date.getMonth() == null || date.getDayOfMonth() == null) continue;
                String title = (side == 0 ? "Birth" : "Death") + " of " + C.escape(o.getName() + " <a href=\"http://www.google.com\">TEST URL</a>");
                Place place = (side == 0 ? o.startPlace : o.endPlace);
                out.println("BEGIN:VEVENT");
                out.println("SUMMARY:" + title);
                out.println("DTSTART;VALUE=DATE:1900" + (date.getMonth() < 10 ? "0" : "") + date.getMonth() + (date.getDayOfMonth() < 10 ? "0" : "") + date.getDayOfMonth());
                out.println("DTEND;VALUE=DATE:1900" + (date.getMonth() < 10 ? "0" : "") + date.getMonth() + (date.getDayOfMonth() < 10 ? "0" : "") + date.getDayOfMonth());
                out.println("RRULE:FREQ=YEARLY;WKST=SU");
                out.println("UID:" + o.getID() + (side == 0 ? "b" : "d") + "@freebase.com");
                out.println("DESCRIPTION:" + title);
                out.println("LOCATION:" + (place == null ? null : C.escape(side == 0 ? o.getBirthPlace() : o.getDeathPlace())));
                if (o.iconFile != null) out.println("X-GOOGLE-CALENDAR-CONTENT-ICON:" + this.baseURL + o.iconFile.getName());
                out.println("END:VEVENT");
            }
        }
        out.println("END:VCALENDAR");
        out.flush();
        out.close();
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + C.escape(s) + "\"";
    }

    private static String quote(String s[]) {
        if (s == null) return "[]";
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < s.length; ++i) {
            if (i > 0) b.append(",");
            b.append(quote(s[i]));
        }
        b.append("]");
        return b.toString();
    }

    private static class MetawebObject {

        private HashMap<String, String> properties = new HashMap<String, String>();

        private HashMap<String, HashSet<String>> name2values = new HashMap<String, HashSet<String>>();

        public MetawebObject() {
        }

        String set(String key, String value) {
            if (value == null) {
                properties.remove(key);
            } else {
                properties.put(key, value);
            }
            return value;
        }

        String get(String key) {
            return this.properties.get(key);
        }

        String[] getArray(String key) {
            HashSet<String> set = name2values.get(key);
            if (set == null || set.isEmpty()) return new String[0];
            String array[] = new String[set.size()];
            return set.toArray(array);
        }

        Collection<String> set(String key, Collection<String> values) {
            if (values == null || values.isEmpty()) return values;
            HashSet<String> set = new HashSet<String>();
            this.name2values.put(key, set);
            set.addAll(values);
            return values;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("{\n");
            for (String key : properties.keySet()) {
                b.append(key).append(":").append(properties.get(key)).append("\n");
            }
            for (String key : name2values.keySet()) {
                HashSet<String> set = this.name2values.get(key);
                b.append(key).append(":[");
                boolean found = false;
                for (String s : set) {
                    if (found) b.append(",");
                    found = true;
                    b.append(s);
                }
                b.append("]\n");
            }
            b.append("}");
            return b.toString();
        }
    }

    private static class Award {

        String name;

        Integer year;
    }

    private static class Place {

        double longitude;

        double latitude;

        @Override
        public String toString() {
            return "(" + longitude + " " + latitude + ")";
        }
    }

    private class Person extends MetawebObject implements Comparable<Person> {

        Place startPlace = null;

        Place endPlace = null;

        StartDate startDate = null;

        EndDate endDate = null;

        File iconFile = null;

        int y = 0;

        Vector<Award> awards = new Vector<Award>(1);

        @Override
        public int compareTo(Person o) {
            int i = startDate.compareTo(o.startDate);
            if (i != 0) return i;
            return endDate.compareTo(o.endDate);
        }

        public double x1() {
            return convertDate2Pixel(startDate);
        }

        public double x2() {
            return convertDate2Pixel(endDate == null ? new EndDate() : endDate);
        }

        @SuppressWarnings("unused")
        boolean intersect(Person o) {
            final int margin = 5;
            if (o == this || this.y != o.y) return false;
            return !(this.x2() + margin < o.x1() || o.x2() + margin < this.x1());
        }

        @Override
        public String toString() {
            return super.toString() + " birth " + startDate + "\ndeath " + endDate + "\n" + startPlace + "\n" + endPlace + "\n";
        }

        public String getID() {
            String gui = get("guid");
            if (gui.startsWith("#")) gui = gui.substring(1);
            return gui;
        }

        public String getName() {
            return get("name");
        }

        public String getGender() {
            return get("gender");
        }

        public String getBirthPlace() {
            return get("place_of_birth");
        }

        public String getDeathPlace() {
            return get("place_of_death");
        }

        public String getHTML() {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            String gui = getID();
            if (gui != null) {
                out.print("<a href=\"http://www.freebase.com/view/guid/" + gui + "\" title=\"");
                out.print(XMLUtilities.escape(getName()));
                out.print("\">");
            }
            out.print(XMLUtilities.escape(getName()));
            if (gui != null) {
                out.print("</a>");
            }
            out.print(" ( ");
            if (startDate != null) {
                out.print(startDate);
                String place = get("place_of_birth");
                if (place != null) {
                    out.print(" at " + XMLUtilities.escape(place));
                }
            }
            if (endDate != null) {
                out.print(", ");
                out.print(endDate);
                String place = get("place_of_death");
                if (place != null) {
                    out.print(" at " + XMLUtilities.escape(place));
                }
            }
            out.print(")");
            String s = get("shortBio");
            if (s != null) out.print("<cite>" + XMLUtilities.escape(s) + "</cite>. ");
            HashSet<String> set = super.name2values.get("knownFor");
            if (set != null && !set.isEmpty()) {
                out.print(" <b>Known for</b> : ");
                boolean found = false;
                for (String i : set) {
                    if (found) out.print(", ");
                    found = true;
                    out.print("&apos;" + XMLUtilities.escape(i) + "&apos;");
                }
            }
            return sw.toString();
        }
    }

    private int getScreenWidthInPixel() {
        return 15000;
    }

    private double convertDate2Pixel(StartDate d) {
        return getScreenWidthInPixel() * ((d.dayValue() - minDate.dayValue()) / (this.maxDate.dayValue() - this.minDate.dayValue()));
    }

    /** metaweb doesn't like escaping simple &apos; juste &quote; */
    private String escape(String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            switch(s.charAt(i)) {
                case ('\"'):
                    buffer.append("\\\"");
                    break;
                case ('\n'):
                    buffer.append("\\n");
                    break;
                case ('\t'):
                    buffer.append("\\t");
                    break;
                case ('\\'):
                    buffer.append("\\\\");
                    break;
                default:
                    buffer.append(s.charAt(i));
                    break;
            }
        }
        return buffer.toString();
    }

    private double convertDate2Pixel(EndDate d) {
        return getScreenWidthInPixel() * ((d.dayValue() - minDate.dayValue()) / (this.maxDate.dayValue() - this.minDate.dayValue()));
    }

    private void handlePerson(Map<String, ?> object) throws IOException, ParseException {
        if (!object.containsKey("guid")) return;
        Person person = new Person();
        person.set("guid", asString(object.get("guid")));
        Object q = query("{\"guid\":\"" + person.get("guid") + "\",\"type\":[]}");
        Object json = find(q, "qname1.result.type");
        for (Object i2 : asArray(json)) {
            String type = asString(i2);
            Object q3 = query("{\"guid\":\"" + person.get("guid") + "\",\"*\":null,\"type\":\"" + type + "\"}");
            if (type.equals("/user/lindenb/default_domain/scientist")) {
                person.set("shortBio", getString(q3, "qname1.result.short_bio"));
                person.set("knownFor", getArray(q3, "qname1.result.known_for"));
            } else if (type.equals("/people/deceased_person")) {
                String date = person.set("date_of_death", getString(q3, "qname1.result.date_of_death"));
                if (date != null) person.endDate = new EndDate(date);
                String place = person.set("place_of_death", getString(q3, "qname1.result.place_of_death"));
                person.set("cause_of_death", getArray(q3, "qname1.result.cause_of_death"));
                if (place != null) {
                    Place coord = handlePlace(place);
                    if (coord != null) person.endPlace = coord;
                }
            } else if (type.equals("/people/person")) {
                person.set("profession", getArray(q3, "qname1.result.profession"));
                String date = person.set("date_of_birth", getString(q3, "qname1.result.date_of_birth"));
                if (date != null) person.startDate = new StartDate(date);
                String place = person.set("place_of_birth", getString(q3, "qname1.result.place_of_birth"));
                person.set("gender", getString(q3, "qname1.result.gender"));
                person.set("nationality", getArray(q3, "qname1.result.nationality"));
                if (place != null) {
                    Place coord = handlePlace(place);
                    if (coord != null) person.startPlace = coord;
                }
            } else if (type.equals("/common/topic")) {
                String name = person.set("name", getString(q3, "qname1.result.name"));
                Collection<String> imgs = person.set("image", getArray(q3, "qname1.result.image"));
                if (imgs != null) {
                    for (String img : imgs) {
                        person.iconFile = handleImage(img, name);
                        if (person.iconFile != null) break;
                    }
                }
            } else if (type.equals("/user/mikelove/default_domain/influence_node")) {
            } else if (type.equals("/award/award_winner")) {
                Object q4 = query("{\"id\":\"" + person.get("guid") + "\",\"awards_won\":[{\"*\":null}],\"type\":\"" + type + "\"}");
                Object i5 = find(q4, "qname1.result.awards_won");
                if (i5 != null && isArray(i5)) {
                    for (Object i6 : asArray(i5)) {
                        if (i6 == null || !isObject(i6)) continue;
                        Map<String, Object> i7 = asObject(i6);
                        Object i8 = i7.get("award");
                        String awardName = (i8 != null && isString(i8) ? asString(i8) : null);
                        if (awardName == null) continue;
                        i8 = i7.get("year");
                        Integer year = (i8 != null && isConstant(i8) ? new Integer(asString(i8)) : null);
                        Award award = new Award();
                        award.name = awardName;
                        award.year = year;
                        person.awards.addElement(award);
                    }
                }
            } else {
                System.err.println("Type not handled " + type);
            }
        }
        if (person.startDate == null) return;
        persons.addElement(person);
    }

    private Place handlePlace(String place) throws IOException, ParseException {
        Object q = query("[{\"name\":\"" + escape(place) + "\",\"type\":\"/location/location\",\"geolocation\": {\"*\" : null },\"*\":null}]");
        Object i = find(q, "qname1.result[0].geolocation");
        if (i == null || !isObject(i)) {
            return null;
        }
        Map<String, Object> geoloc = asObject(i);
        if (!(geoloc.containsKey("longitude") && geoloc.containsKey("latitude"))) {
            if (isDebugging()) System.err.println("Cannot get lon/lat");
            return null;
        }
        Place coord = new Place();
        coord.longitude = asDouble(geoloc.get("longitude"));
        coord.latitude = asDouble(geoloc.get("latitude"));
        return coord;
    }

    private File handleImage(String imageName, String person) throws IOException, ParseException {
        Object q = query("[{\"name\":\"" + escape(imageName) + "\",\"type\":\"/common/image\",\"appears_in_topic_gallery\":\"" + escape(person) + "\",\"*\":null}]");
        Object result = find(q, "qname1.result");
        if (result == null || !isArray(result)) return null;
        for (Object item : asArray(result)) {
            if (item == null || !isObject(item)) continue;
            Object id = asObject(item).get("guid");
            if (id == null) continue;
            String uid = asString(id);
            if (uid.startsWith("#")) uid = uid.substring(1);
            File iconFile = makeIcon(uid);
            if (iconFile != null) return iconFile;
        }
        return null;
    }

    private String getString(Object root, String path) {
        Object item = find(root, path);
        if (item == null || !isString(item)) {
            if (isDebugging()) System.err.println("Cannot get " + path + " in " + root);
            return null;
        }
        return item.toString();
    }

    private Collection<String> getArray(Object root, String path) {
        Object item = find(root, path);
        if (item == null || !isArray(item)) {
            if (isDebugging()) System.err.println("Cannot get " + path + " in " + root);
            return null;
        }
        Vector<String> array = new Vector<String>(asArray(item).size());
        for (int i = 0; i < asArray(item).size(); ++i) {
            Object x = asArray(item).get(i);
            if (x == null || !isConstant(x)) continue;
            array.addElement(asString(x));
        }
        return array;
    }

    private String getMetawebCookie() {
        return this.metawebCookie;
    }

    public Double asDouble(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean isArray(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean isString(Object o) {
        throw new UnsupportedOperationException();
    }

    public String asString(Object o) {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> asObject(Object o) {
        throw new UnsupportedOperationException();
    }

    public List<Object> asArray(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean isObject(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean isConstant(Object o) {
        throw new UnsupportedOperationException();
    }

    public Object find(Object o, String query) {
        throw new UnsupportedOperationException();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            Metaweb01 app = new Metaweb01();
            Properties preferences = new Properties();
            File prefFile = new File(System.getProperty("user.home"), ".metaweb.xml");
            if (!prefFile.exists()) {
                System.err.println("Warning: Cannot find " + prefFile + " containing freebase cookie");
            } else {
                InputStream in = new FileInputStream(prefFile);
                preferences.loadFromXML(in);
                in.close();
            }
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println(" -h this screen");
                    System.err.println(" -d <dir> save to directory");
                    System.err.println(" -c <string> metaweb cookie");
                    System.err.println(" -u <string> base url");
                    System.err.println(" -g <string> google analytics id");
                    System.err.println(" -n <int> number of person to be fetched (optional)");
                    System.err.println(" -H hide requests messages");
                    return;
                } else if (args[optind].equals("-d")) {
                    app.tmpFolder = new File(args[++optind]);
                } else if (args[optind].equals("-c")) {
                    app.metawebCookie = args[++optind];
                } else if (args[optind].equals("-u")) {
                    app.baseURL = args[++optind];
                } else if (args[optind].equals("-g")) {
                    app.urchinID = args[++optind];
                } else if (args[optind].equals("-n")) {
                    app.limitNumberOfPerson = Integer.parseInt(args[++optind]);
                } else if (args[optind].equals("-H")) {
                    app.echoRequest = false;
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            if (optind != args.length) {
                System.err.println("Bad number of arguments.");
                return;
            }
            if (app.tmpFolder == null) {
                if (preferences.containsKey("tmp")) {
                    app.tmpFolder = new File(preferences.getProperty("tmp"));
                }
                if (app.tmpFolder == null) {
                    System.err.println("folder was not specified.");
                    return;
                }
            }
            if (app.metawebCookie == null) {
                if (preferences.containsKey(COOKIE)) {
                    app.metawebCookie = preferences.getProperty(COOKIE);
                }
                if (app.metawebCookie == null) {
                    System.err.println("Cookie" + COOKIE + " was not specified");
                    return;
                }
            }
            if (app.baseURL == null) {
                if (preferences.containsKey("url")) {
                    app.baseURL = preferences.getProperty("url");
                }
                if (app.baseURL == null) {
                    app.baseURL = "";
                }
            }
            if (app.urchinID == null) {
                app.urchinID = preferences.getProperty("urchin");
            }
            app.run();
            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
