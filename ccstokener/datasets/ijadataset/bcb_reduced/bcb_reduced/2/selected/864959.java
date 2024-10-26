package org.goobs.internet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.goobs.database.Database;
import org.goobs.database.ResultFactory;
import org.goobs.database.Table;
import org.goobs.io.TextConsole;
import org.goobs.threading.OperatingSystem;
import org.goobs.threading.ProcessFactory;
import org.goobs.utils.Util;

public class Browser {

    private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3";

    private static final LinkGetter linkGetter = new LinkGetter();

    private static final Pattern commentPattern = Pattern.compile("<!--.*?-->", Pattern.MULTILINE);

    private static final Pattern tagPattern = Pattern.compile("</?\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/?>");

    private static final Pattern rootURLPattern = Pattern.compile("http://[^/]*/");

    private String rawHTML;

    private String textCache;

    private URL url;

    private String addressBarText;

    private Collection<Cookie> cookies = new HashSet<Cookie>();

    public static class BrowserException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private BrowserException() {
            super();
        }

        private BrowserException(String msg) {
            super(msg);
        }
    }

    private class StringIterator implements Iterator<String> {

        private Matcher matcher;

        private String nextTerm = null;

        private String begin, end;

        private StringIterator(Matcher m, String start, String stop) {
            matcher = m;
            if (matcher.find()) {
                nextTerm = m.group();
            } else {
                nextTerm = null;
            }
            this.begin = start;
            this.end = stop;
        }

        public boolean hasNext() {
            return nextTerm != null;
        }

        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more matches found");
            }
            String rtn = nextTerm;
            if (matcher.find()) {
                nextTerm = matcher.group();
            } else {
                nextTerm = null;
            }
            return removeEnds(rtn, begin, end);
        }

        public void remove() {
            throw new UnsupportedOperationException("Cannot remove part of an html file");
        }
    }

    public static class LinkGetter {

        private Pattern htmltag;

        private Pattern link;

        private Pattern text;

        public LinkGetter() {
            htmltag = Pattern.compile("<[aA]\\b[^>]*[hH][rR][eE][fF]=\"[^>]*>(.*?)</[aA]>");
            link = Pattern.compile("href=\"[^>]*\"");
            text = Pattern.compile(">[^<]*</[aA]>");
        }

        public List<Hyperlink> getLinks(String content, String url) {
            List<Hyperlink> links = new ArrayList<Hyperlink>();
            Matcher tagmatch = htmltag.matcher(content);
            while (tagmatch.find()) {
                String raw = tagmatch.group();
                Matcher matcher = this.link.matcher(raw);
                if (!matcher.find()) {
                    continue;
                }
                String link = matcher.group();
                link = link.replaceFirst("href=\"", "");
                int end = link.indexOf('"');
                link = link.substring(0, end);
                matcher = this.text.matcher(raw);
                if (!matcher.find()) {
                    continue;
                }
                String text = matcher.group();
                text = text.substring(1, text.length() - 4);
                if (valid(link)) {
                    links.add(new Hyperlink(makeAbsolute(url, link), text));
                }
            }
            return links;
        }

        private boolean valid(String s) {
            if (s.matches("javascript:.*|mailto:.*")) {
                return false;
            }
            return true;
        }
    }

    public Browser() {
    }

    public void pointTo(String target) throws BrowserException {
        genericPointTo(target, "", WebForm.NONE);
    }

    public void submit(WebForm frm) {
        StringBuilder cb = new StringBuilder();
        for (String key : frm.getInputs()) {
            String val = frm.getValue(key);
            cb.append('&').append(key).append('=').append(val);
        }
        String content = cb.substring(1);
        String target = makeAbsolute(url.toExternalForm(), frm.getAction());
        genericPointTo(target, content, frm.getMethod());
    }

    private void genericPointTo(String target, String content, int method) {
        this.rawHTML = null;
        this.textCache = null;
        this.url = null;
        this.addressBarText = target;
        if (content != null && content.length() > 0) {
            addressBarText += ("?" + content);
        }
        HttpURLConnection urlConn;
        BufferedReader input;
        StringBuilder b = new StringBuilder();
        try {
            if (target == null) {
                throw new BrowserException("No URL given");
            }
            url = new URL(target);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("User-agent", USER_AGENT);
            String cookieStr = buildCookieStr(target);
            urlConn.setRequestProperty("Cookie", cookieStr);
            if (method == WebForm.POST) {
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConn.setRequestProperty("Content-Length", content.length() + "");
                if (!content.equals("")) {
                    DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
                    printout.writeBytes(content);
                    printout.flush();
                    printout.close();
                }
            } else if (method == WebForm.GET) {
                urlConn.setRequestMethod("GET");
            }
            input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String str;
            long timeout = 32;
            while (!input.ready()) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                }
                if (timeout > 5000) {
                    throw new BrowserException("input stream was not ready");
                }
                timeout *= 2;
            }
            while (null != ((str = input.readLine()))) {
                b.append(str).append('\n');
            }
            input.close();
            Map<String, List<String>> headers = urlConn.getHeaderFields();
            List<String> cookies = headers.get("Set-Cookie");
            if (cookies != null) {
                for (String cook : cookies) {
                    setCookie(cook, target);
                }
            }
        } catch (MalformedURLException e) {
            throw new BrowserException("Not a valid URL: " + target);
        } catch (ProtocolException e) {
            throw new BrowserException("Encountered a protocol exception: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new BrowserException("Encountered a general Input/Output exception: " + e.getMessage());
        }
        this.rawHTML = b.toString();
    }

    private String buildCookieStr(String target) {
        if (target == null) {
            throw new BrowserException("Cannot build cookie string for null target");
        }
        StringBuilder rtn = new StringBuilder();
        for (Cookie cook : cookies) {
            String host = cook.getHost();
            if (host == null) {
                throw new IllegalStateException("Cookie does not have an associated domain: " + cook);
            }
            if (target.contains(host)) {
                rtn.append("; ");
                rtn.append(cook);
            }
        }
        if (rtn.length() > 0) {
            return rtn.substring(2);
        } else {
            return "";
        }
    }

    private void setCookie(String cookie, String target) {
        String val = Util.stringBetween(cookie, null, ";");
        if (!cookie.contains(";")) {
            val = cookie;
        }
        String name = Util.stringBetween(val, null, "=");
        String value = Util.stringBetween(val, "=", null);
        HashMap<String, String> fields = new HashMap<String, String>();
        String tmp = cookie.substring(cookie.indexOf(";") + 1);
        int pos;
        while ((pos = tmp.indexOf(';')) >= 0) {
            String str = tmp.substring(0, pos).trim();
            fields.put(Util.stringBetween(str, null, "=").toLowerCase(), Util.stringBetween(str, "=", null));
            tmp = tmp.substring(pos + 1);
        }
        fields.put(Util.stringBetween(tmp, null, "=").trim().toLowerCase(), Util.stringBetween(tmp, "=", null).trim());
        String domain = fields.get("domain");
        if (domain == null) {
            domain = getHost(target);
        }
        Cookie cook = new Cookie(domain, name, value);
        cookies.add(cook);
    }

    public void addCookie(Cookie cook) {
        cookies.add(cook);
    }

    public void printCookies(String domain) {
        for (Cookie cook : cookies) {
            String host = cook.getHost();
            if (domain.contains(host)) {
                System.out.println(cook);
            }
        }
    }

    @Deprecated
    public void submitTo(String target, HashMap<String, String> values) {
        Iterator<String> iter = values.keySet().iterator();
        String content = "";
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                content = content + "&" + key + "=" + URLEncoder.encode(values.get(key), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new BrowserException("Could not encode string: " + values.get(key));
            }
        }
        if (content.length() > 0) {
            content = content.substring(1);
        }
        this.rawHTML = null;
        this.textCache = null;
        this.url = null;
        HttpURLConnection urlConn;
        BufferedReader input;
        String rtn = "";
        try {
            if (target == null) {
                throw new BrowserException("No URL entered to point to");
            }
            url = new URL(target);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("User-agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3");
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConn.setRequestProperty("Content-Length", content.length() + "");
            if (!content.equals("")) {
                DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
                printout.writeBytes(content);
                printout.flush();
                printout.close();
            }
            input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String str;
            if (!input.ready()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (!input.ready()) {
                    throw new BrowserException("input stream was not ready");
                }
            }
            while (null != ((str = input.readLine()))) {
                rtn = rtn + str + "\n";
            }
            input.close();
        } catch (MalformedURLException e) {
            throw new BrowserException("Not a valid URL: " + target);
        } catch (ProtocolException e) {
            throw new BrowserException("Encountered a protocol exception: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new BrowserException("Encountered a general Input/Output exception: " + e.getMessage());
        }
        this.rawHTML = rtn;
    }

    public String getAddressBar() {
        return addressBarText;
    }

    public String getHTML() {
        if (this.rawHTML == null) {
            return "";
        }
        return this.rawHTML;
    }

    public String extractText() {
        if (this.rawHTML == null) {
            return "";
        } else if (this.textCache != null) {
            return textCache;
        }
        return extractText(rawHTML);
    }

    public static String extractText(String html) {
        String txt = html;
        Matcher comments = commentPattern.matcher(txt);
        while (comments.find()) {
            String comment = comments.group();
            txt = txt.replace(comment, " ");
        }
        Matcher tags = tagPattern.matcher(txt);
        while (tags.find()) {
            String tag = tags.group();
            txt = txt.replace(tag, " ");
        }
        txt = txt.replaceAll("\t", " ");
        while (txt.contains("  ")) {
            txt = txt.replaceAll("  ", " ");
        }
        while (txt.contains("\n \n")) {
            txt = txt.replaceAll("\n ", "\n");
        }
        while (txt.contains("\n\n")) {
            txt = txt.replaceAll("\n\n", "\n");
        }
        txt.replaceAll("\n ", "\n");
        txt = txt.replaceAll("&nbsp;", " ");
        txt = txt.replaceAll("&quot;", "\"");
        txt = txt.replaceAll("&ndash;", "-");
        txt = txt.replaceAll("&mdash;", "--");
        txt = txt.replaceAll("&lsquo;", "`");
        txt = txt.replaceAll("&rsquo;", "'");
        txt = txt.replaceAll("&amp;", "&");
        return txt;
    }

    public String getURL() {
        if (url == null) {
            return "";
        } else {
            return url.toString();
        }
    }

    public Iterator<String> findHtmlBetween(String begin, String end, boolean greedy) {
        Matcher m = genericFindFirst(this.rawHTML, begin, end, greedy);
        return new StringIterator(m, begin, end);
    }

    public String findFirstHtmlBetween(String begin, String end, boolean greedy) {
        Matcher m = genericFindFirst(this.rawHTML, begin, end, greedy);
        if (!m.find()) {
            throw new BrowserException("No phrase found between '" + begin + "' and '" + end + "'");
        } else {
            String rtn = m.group();
            return removeEnds(rtn, begin, end);
        }
    }

    public Iterator<String> findTextBetween(String begin, String end, boolean greedy) {
        if (textCache == null) {
            this.textCache = extractText();
        }
        Matcher m = genericFindFirst(this.textCache, begin, end, greedy);
        return new StringIterator(m, begin, end);
    }

    public String findFirstTextBetween(String begin, String end, boolean greedy) {
        if (textCache == null) {
            this.textCache = extractText();
        }
        Matcher m = genericFindFirst(this.textCache, begin, end, greedy);
        String match = m.group();
        if (match != null && !match.equals("")) {
            return removeEnds(match, begin, end);
        } else {
            throw new BrowserException("No phrase found between '" + begin + "' and '" + end + "'");
        }
    }

    private Matcher genericFindFirst(String input, String begin, String end, boolean greedy) {
        String g = "";
        if (!greedy) {
            g = "?";
        }
        Pattern p = Pattern.compile(begin + ".*" + g + end, Pattern.MULTILINE | Pattern.DOTALL);
        return p.matcher(input);
    }

    private static String removeEnds(String match, String begin, String end) {
        Matcher a = Pattern.compile(begin).matcher(match);
        Matcher b = Pattern.compile(end).matcher(match);
        if (!a.find()) {
            throw new BrowserException("Could not remove ends from: " + match + " (" + begin + "\t" + end + ")");
        }
        if (!b.find()) {
            throw new BrowserException("Could not remove ends from: " + match + " (" + begin + "\t" + end + ")");
        }
        return match.substring(a.group().length(), match.length() - b.group().length());
    }

    public List<Hyperlink> getLinks() {
        return getLinks("" + rawHTML.charAt(0), "" + rawHTML.charAt(rawHTML.length() - 1));
    }

    /**
	 * 
	 * @param start
	 *            A string denoting where to start extracting links from. Will
	 *            find the first instance of that string
	 * @param stop
	 *            A string denoting where to stop extracting links. Will find
	 *            the last instance of that string.
	 * @return An array of links on the web page.
	 */
    public List<Hyperlink> getLinks(String start, String stop) {
        int startIndex = rawHTML.indexOf(start);
        int endIndex = rawHTML.lastIndexOf(stop);
        String content = rawHTML.substring(startIndex, endIndex);
        List<Hyperlink> links = linkGetter.getLinks(content, this.url.toString());
        return links;
    }

    public Set<WebForm> getForms() {
        Set<WebForm> rtn = new HashSet<WebForm>();
        Iterator<String> formIter = findHtmlBetween("< *?[Ff][Oo][Rr][Mm]", "< *?/ *?[Ff][Oo][Rr][Mm] *?>", false);
        while (formIter.hasNext()) {
            rtn.add(new WebForm("<form " + formIter.next() + " </form>"));
        }
        return rtn;
    }

    public WebForm getForm(String name) {
        Iterator<WebForm> formIter = getForms().iterator();
        while (formIter.hasNext()) {
            WebForm frm = formIter.next();
            if (frm.getName() != null && frm.getName().equalsIgnoreCase(name)) {
                return frm;
            }
        }
        return null;
    }

    private String getProfileString(String mozillaPath) {
        File profiles = new File(mozillaPath + "/profiles.ini");
        if (!profiles.exists()) {
            throw new IllegalArgumentException("Invalid mozilla path: " + mozillaPath);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(profiles));
            String line = reader.readLine();
            while (!line.contains("Path=")) {
                line = reader.readLine();
            }
            int eq = line.indexOf('=');
            String rtn = line.substring(eq + 1);
            return rtn;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public void syncFirefoxCookies() {
        this.cookies = new HashSet<Cookie>();
        OperatingSystem os = ProcessFactory.getOS();
        String home = System.getProperty("user.home");
        String mozillaPath;
        switch(os) {
            case Linux:
                mozillaPath = home + "/.mozilla/firefox";
                break;
            case Windows:
                mozillaPath = home + "/Application Data/Mozilla/firefox";
                break;
            case Mac:
                mozillaPath = home + "/Library/Mozilla/firefox";
                break;
            default:
                throw new IllegalStateException("Your operating system is not supported");
        }
        String profileStr = getProfileString(mozillaPath);
        String cookiePath = mozillaPath + "/" + profileStr + "/cookies.sqlite";
        File cookieFile = new File(cookiePath);
        if (!cookieFile.exists()) {
            throw new IllegalStateException("Could not find cookie file at path: " + cookiePath);
        }
        Database cookieDB = new Database(cookieFile);
        if (!cookieDB.connect()) {
            throw new IllegalStateException("Could not open cookie databas at path: " + cookiePath);
        }
        Table moz_cookies = cookieDB.getTable("moz_cookies");
        ResultFactory fact = moz_cookies.getAllRows();
        while (fact.hasMoreRows()) {
            Cookie cook = new Cookie();
            fact.fillObject(cook);
            cookies.add(cook);
        }
    }

    public static String makeAbsolute(String url, String link) {
        Matcher rootMatcher = rootURLPattern.matcher(url);
        String root = url;
        if (!rootMatcher.find()) {
        } else {
            String grp = rootMatcher.group();
            root = grp.substring(0, grp.length() - 1);
        }
        if (link.matches("https?://.*")) {
            return link;
        }
        if (link.matches("/.*")) {
            if (root.matches(".*$[^/]")) {
                return url + "/" + link;
            }
            if (root.matches(".*[/]")) {
                return root + link;
            }
            if (root.matches(".*[^/]")) {
                return root + link;
            }
        } else {
            if (root.matches(".*[^/]")) {
                return root + "/" + link;
            }
        }
        throw new IllegalArgumentException("Cannot make the link absolute. Url: " + url + " Link " + link);
    }

    public static String getHost(String url) {
        Matcher rootMatcher = rootURLPattern.matcher(url);
        if (!rootMatcher.find()) {
            throw new IllegalArgumentException("Malformed URL: " + url);
        }
        String base = rootMatcher.group();
        String host = base.substring(7, base.length() - 1);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return host;
    }

    public static void main(String[] args) {
        TextConsole c = new TextConsole();
        String site = "http://www.travelocity.com/";
        String from = "LAX";
        String to = "FRA";
        c.show();
        c.println("loading " + site + "...");
        Browser firefox = new Browser();
        c.println("   (cookies synced)");
        firefox.pointTo(site);
        WebForm search = firefox.getForm("form-fo");
        boolean set = true;
        set = set && search.setValue("leavingDate", "07/02/2009");
        set = set && search.setValue("returningDate", "07/09/2009");
        set = set && search.setValue("leavingFrom", "SFO");
        set = set && search.setValue("goingTo", "FRA");
        set = set && search.setValue("dateTypeSelect", "plusMinusDates");
        set = set && search.setValue("tripType", "airOnly");
        if (!set) {
            throw new BrowserException("Could not set the proper form values");
        }
        firefox.submit(search);
        String finurl = firefox.findFirstHtmlBetween("var finurl = \"", "\"", false);
        String vals = finurl.replace("ResolveAirportAction.do;", "");
        String cookieStr = Util.stringBetween(vals, null, "?");
        String seqStr = Util.stringBetween(vals, "?", null);
        Cookie cook = new Cookie("travel.travelocity.com", Util.stringBetween(cookieStr, null, "="), Util.stringBetween(cookieStr, "=", null));
        firefox.addCookie(cook);
        String action = "http://travel.travelocity.com/flights/ResolveAirportAction.do";
        HashMap<String, String> values = new HashMap<String, String>();
        values.put(Util.stringBetween(seqStr, null, "="), Util.stringBetween(seqStr, "=", null));
        WebForm resolveAirport = new WebForm("resolve-airport", action, values, WebForm.POST);
        firefox.submit(resolveAirport);
        finurl = firefox.findFirstHtmlBetween("var finurl = \"", "\"", false);
        action = "http://travel.travelocity.com/flights/" + Util.stringBetween(finurl, null, "?");
        values.clear();
        values.put(Util.stringBetween(finurl, "?", "="), Util.stringBetween(finurl, "=", null));
        WebForm airSearch = new WebForm("air-date-search", action, values, WebForm.POST);
        firefox.submit(airSearch);
        String html = firefox.getHTML();
        try {
            File f = new File("/home/gabor/junk.html");
            FileWriter writer = new FileWriter(f);
            writer.append(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
