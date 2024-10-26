package org.netbeans.server.uihandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.openide.util.RequestProcessor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Class that provides access to tip of the day functionality provided
 * by docs team. It is created with URL refering to the page with XML data
 * and is refereshed very hour.
 *
 * @author Jaroslav Tulach
 */
public final class TipOfTheDay implements Runnable {

    private static final Logger LOG = Logger.getLogger(TipOfTheDay.class.getName());

    private static RequestProcessor RP = new RequestProcessor("Refresh TipOfTheDay");

    private Map<String, List<Tip>> tips;

    private final URL url;

    private RequestProcessor.Task refresh;

    private TipOfTheDay(URL url) {
        this.url = url;
        if (url == null) {
            tips = Collections.emptyMap();
            return;
        }
        tips = Collections.emptyMap();
        refresh = RP.create(this);
        refresh.schedule(0);
        try {
            refresh.waitFinished(10000);
        } catch (InterruptedException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    /** Parses content provided by some URL and create the TipOfTheDay database.
     * @param url url to read the TipOfTheDay from
     */
    public static TipOfTheDay create(URL url) {
        return new TipOfTheDay(url);
    }

    private static TipOfTheDay DEFAULT;

    /** Gets default tip of the day.
     */
    public static TipOfTheDay getDefault() {
        if (DEFAULT == null) {
            String tips = Utils.getVariable("tipsOfTheDay", String.class);
            if (tips != null) {
                try {
                    DEFAULT = new TipOfTheDay(new URL(tips));
                } catch (MalformedURLException ex) {
                    LOG.log(Level.WARNING, ex.getMessage(), ex);
                }
            }
            if (DEFAULT == null) {
                return new TipOfTheDay(null);
            }
        }
        return DEFAULT;
    }

    /** Refreshes the content of the databases. Re-reads the content of 
     * the provided URL and updates internal structures.
     */
    public void run() {
        LOG.log(Level.INFO, "Refreshing content of TipOfTheDay: {0}", url);
        try {
            Parser p = new Parser();
            tips = p.parse(url);
            return;
        } catch (SAXException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        } finally {
            LOG.info("Done refreshing of TipOfTheDay");
            refresh.schedule(60 * 1000 * 60 * 24);
        }
    }

    /** Finds appropriate tip of the date for given usage of projects.
     * @param cnts collected info from ProjectTypes
     * @return randomly selected typ
     */
    public Tip find(Set<Map.Entry<String, Integer>> cnts) {
        List<? extends Tip> all = findAll(cnts);
        if (all.isEmpty()) {
            return null;
        }
        int r = new Random().nextInt(all.size());
        return all.get(r);
    }

    /** Finds all tips appropriate as tip of the date for given usage of projects.
     * @param cnts the counts
     * @return list of possible tips
     */
    public List<? extends Tip> findAll(Set<Map.Entry<String, Integer>> cnts) {
        int max = -1;
        List<Tip> found = Collections.emptyList();
        if (cnts == null) {
            return found;
        }
        for (Map.Entry<String, Integer> entry : cnts) {
            if (entry.getValue() > max) {
                List<Tip> f = tips.get(entry.getKey());
                if (f != null) {
                    max = entry.getValue();
                    found = f;
                }
            }
        }
        return found;
    }

    static String findProject(String category) throws SAXException {
        String ret = null;
        if ("Web and Enterprise Development".equals(category)) {
            ret = "Ear";
        }
        if ("NetBeans Platform Development".equals(category)) {
            ret = "NbModule";
        }
        if ("Monitoring and Profiling".equals(category)) {
            ret = "profiler";
        }
        if ("Mobile Application Development".equals(category) || "Embedded and Mobile Development".equals(category)) {
            ret = "J2ME";
        }
        if ("Basic IDE Functionality".equals(category) || "Advanced and Miscellaneous".equals(category) || "Swing GUI Development".equals(category)) {
            ret = "J2SE";
        }
        if ("PHP Development".equals(category)) {
            ret = "Php";
        }
        if ("C  and C++ Development".equals(category)) {
            ret = "Make";
        }
        if ("Dynamic Languages and JavaScript".equals(category)) {
            ret = "Web";
        }
        if ("Swing GUI & JavaFX Development".equals(category)) {
            ret = "JavaFX";
        }
        if (ret == null) {
            throw new SAXException("Unexpected category: " + category);
        }
        return ret;
    }

    /** Represents info about one tip.
     */
    public static final class Tip {

        String category;

        String description;

        String url;

        String title;

        public String getDescription() {
            return description;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }
    }

    private static final class Parser extends DefaultHandler {

        private Map<String, List<Tip>> tips = new HashMap<String, List<TipOfTheDay.Tip>>();

        private Tip current;

        private Queue<StringBuilder> values = new LinkedList<StringBuilder>();

        public Map<String, List<Tip>> parse(URL url) throws SAXException, ParserConfigurationException, IOException {
            SAXParserFactory f = SAXParserFactory.newInstance();
            SAXParser p = f.newSAXParser();
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Analytics");
            InputStream is = connection.getInputStream();
            try {
                p.parse(is, this);
            } finally {
                is.close();
            }
            return tips;
        }

        @Override
        public void startElement(String uri, String local, String qName, Attributes args) throws SAXException {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "startElement uri: {0} local: {1} qName: {2}", new Object[] { uri, local, qName });
            }
            if (qName.equals("article")) {
                assert current == null;
                current = new Tip();
            }
            values.add(new StringBuilder());
        }

        @Override
        public void characters(char[] characters, int from, int len) throws SAXException {
            assert !values.isEmpty();
            StringBuilder value = values.peek();
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "characters: {0}", new String(characters, from, len));
            }
            value.append(characters, from, len);
        }

        @Override
        public void endElement(String uri, String local, String qName) throws SAXException {
            assert !values.isEmpty();
            StringBuilder value = values.poll();
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isWhitespace(value.charAt(i))) {
                    value.delete(0, i);
                    break;
                }
            }
            for (int i = value.length() - 1; i > 0; i--) {
                if (!Character.isWhitespace(value.charAt(i))) {
                    value.delete(i + 1, value.length());
                    break;
                }
            }
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "endElement uri: {0} local: {1} qName: {2}", new Object[] { uri, local, qName });
            }
            if (qName.equals("article")) {
                assert current != null;
                String prj = findProject(current.category);
                List<Tip> arr = tips.get(prj);
                if (arr == null) {
                    arr = new ArrayList<Tip>();
                    tips.put(prj, arr);
                }
                arr.add(current);
                current = null;
                return;
            }
            if (qName.equals("description")) {
                assert current != null;
                assert value != null;
                current.description = value.toString();
            }
            if (qName.equals("title")) {
                assert current != null;
                assert value != null;
                current.title = value.toString();
            }
            if (qName.equals("url")) {
                assert current != null;
                assert value != null;
                current.url = value.toString();
            }
            if (qName.equals("category")) {
                assert current != null;
                assert value != null;
                current.category = value.toString();
            }
        }
    }
}
