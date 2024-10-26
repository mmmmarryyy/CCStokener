package vi.crawl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import vi.Doc;
import vi.Link;
import vi.Repository;

/**
 * @author Michal Laclavik
 *
 */
public class SimpleCrawler {

    private Repository repository = null;

    private Set<URL> visitedLinks = new HashSet<URL>();

    private String urlRegexFilter = null;

    private int sleepTime = 200;

    public SimpleCrawler(URL url, Repository _rep) throws Exception {
        repository = _rep;
        crawlURL(url);
    }

    public SimpleCrawler(URL url, String _urlRegexFiler, Repository _rep) throws Exception {
        repository = _rep;
        urlRegexFilter = _urlRegexFiler;
        crawlURL(url);
    }

    private void crawlURL(URL url) throws Exception {
        if (visitedLinks.contains(url)) return;
        if (urlRegexFilter != null && !url.toString().matches(urlRegexFilter)) return;
        visitedLinks.add(url);
        String s = readURL(url);
        String title = getXMLvalue(s, "title", false);
        String body = getXMLvalue(s, "body", false);
        Link[] links = Link.extractLinks(body, url);
        repository.processDoc(new Doc(url, title, body, links));
        Thread.sleep(sleepTime);
        for (int i = 0; i < links.length; i++) {
            crawlURL(links[i].getUrl());
        }
    }

    private static String[] splitXML(String xml, String tag) {
        Pattern p = Pattern.compile("(?i)</" + tag + ">");
        String[] items = p.split(xml);
        return items;
    }

    private static String getXMLvalue(String xml, String tag, boolean strict) {
        String value = "";
        String vP = ".+";
        if (strict) vP = "[^<]+";
        Pattern p = Pattern.compile("(?i)<" + tag + "[^>]*>(" + vP + ")</" + tag + ">");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            value = m.group(1).trim();
        }
        value = StringEscapeUtils.unescapeXml(value);
        return value;
    }

    private static String readURL(URL url) {
        String s = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            while ((str = in.readLine()) != null) {
                s += str;
            }
            in.close();
        } catch (Exception e) {
            s = null;
        }
        return s;
    }

    /**
   * @param args
 * @throws Exception 
   */
    public static void main(String[] args) throws Exception {
        try {
            String urlRegexFilter = ".*(1|3|5).*";
            SimpleCrawler crawler = new SimpleCrawler(new URL("http://irlesons.sourceforge.net/data/1.html"), urlRegexFilter, new Repository());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
