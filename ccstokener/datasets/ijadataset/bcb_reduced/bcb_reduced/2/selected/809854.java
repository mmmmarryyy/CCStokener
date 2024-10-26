package org.archive.crawler.webui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.archive.crawler.WebUI;
import org.archive.crawler.WebUIConfig;
import org.archive.crawler.framework.EngineConfig;
import org.archive.crawler.framework.EngineImpl;
import org.archive.util.FileUtils;
import org.archive.util.IoUtils;
import org.archive.util.TmpDirTestCase;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Unit test for the web ui. Starts a crawler, starts the webui via Jetty, and
 * uses {@link java.net.URL} to connect to various pages in the webui.
 * 
 * <p>
 * The test doesn't really examine the output much. The test extracts the URL
 * for the next page from the previously fetched page, and in some cases will
 * perform a regex on a fetched page to see if desired content is present, but
 * this is not comprehensive. This unit test is mostly for testing the action
 * code, not the presentation.
 * 
 * <p>
 * By specifying any value for the org.archive.crawler.webui.wait system
 * property, the unit test will never complete: This can be useful if you want
 * to drive the UI to a certain state and then manually click around in your
 * browser.
 * 
 * <p>
 * This is named WebUIJunit and not WebUITest because, alas, it will not run
 * under maven2 due to classpath issues. (Jetty requires the system class loader
 * to be able to compile JSPs; but if we use the system class loader, then we're
 * stuck with maven2's version of commons.lang, and Heritrix requires a later
 * version.)
 * 
 * @author pjack
 */
public class WebUIJUnit extends TmpDirTestCase {

    WebUI webui;

    /** Crawler instance. */
    private EngineImpl manager;

    /** Result of System.identityHashCode(manager), used all over place. */
    private int managerId;

    /** The full text of the most recently fetched page. */
    private String lastFetched;

    private URL lastUrl;

    private String host;

    private String urlSheets;

    private String jSessionId;

    /**
     * Starts Jetty, starts Heritrix.
     */
    public void setUp() throws Exception {
        WebUIConfig webConf = new WebUIConfig();
        webConf.setPort(7777);
        webConf.setPathToWAR(WebUITestMain.getWebAppDir().getAbsolutePath());
        webConf.setUiPassword("x");
        webui = new WebUI(webConf);
        webui.start();
        setupDirs();
        EngineConfig config = new EngineConfig();
        config.setJobsDirectory(getJobsDir().getAbsolutePath());
        this.manager = new EngineImpl(config);
        this.managerId = System.identityHashCode(manager);
        doGet("/");
        doGet("/auth.jsp?enteredPassword=x");
        doGet("/home/do_show_home.jsp");
    }

    /**
     * Stops Jetty, stops Heritrix.
     */
    public void tearDown() {
        webui.stop();
        manager.close();
    }

    private static String extract(String url, String key) {
        int p = url.indexOf(key);
        if (p < 0) {
            throw new IllegalStateException();
        }
        int p2 = url.indexOf('&', p);
        if (p2 < 0) {
            p2 = url.length();
        }
        return url.substring(p + key.length() + 1, p2);
    }

    public void testNothing() {
    }

    /**
     * Tests the webui. Renamed to prevent from running 
     * because classpath issues are currently causing 
     * failure in Eclipse JUnit runs. 
     */
    public void xestWebui() throws Exception {
        String url = findHref("do_show_crawler.jsp", managerId);
        this.host = extract(url, "host");
        doGet(url);
        this.urlSheets = findHref("do_show_sheets.jsp", managerId, "basic");
        doGet(urlSheets);
        String editorUrl = findHref("do_show_sheet_editor.jsp", managerId, "global");
        overrideGlobal(editorUrl, "root:metadata:operator-contact-url", "string", "http://crawler.archive.org");
        overrideGlobal(editorUrl, "root:metadata:operator-from", "string", "info@archive.org");
        url = findHref("do_commit_sheet.jsp", managerId, "global");
        doGet(url);
        url = findHref("do_show_seeds.jsp", managerId, "basic");
        doGet(url);
        doAutoPost("do_save_seeds.jsp", "seeds", "http://crawler.archive.org");
        url = findHref("do_show_crawler.jsp", managerId);
        doGet(url);
        url = findHref("do_show_copy.jsp", managerId, "basic");
        doGet(url);
        doAutoPost("do_copy.jsp", "newStage", "READY", "newName", "job1");
        url = findHref("do_launch.jsp", managerId, "job1");
        doGet(url);
        final URL consoleUrl = toURL(findHref("do_show_job_console.jsp", managerId, "job1"));
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {

                public void run() {
                    while (true) try {
                        HttpURLConnection conn = (HttpURLConnection) (consoleUrl.openConnection());
                        setCookie(conn);
                        InputStream input = null;
                        try {
                            input = conn.getInputStream();
                            parseCookie(conn);
                        } finally {
                            IoUtils.close(input);
                        }
                    } catch (Exception e) {
                        return;
                    }
                }
            };
            threads[i].start();
        }
        if (System.getProperty("org.archive.crawler.webui.wait") != null) {
            Object eternity = new Object();
            synchronized (eternity) {
                eternity.wait();
            }
        }
    }

    private void overrideGlobal(String editorUrl, String path, String type, String newValue) throws Exception {
        doGet(editorUrl);
        String add = find("value=\\\"(" + path + "\\`.*?)\\\"");
        doAutoPost("do_save_single_sheet.jsp", "add", add);
        doGet(editorUrl);
        doAutoPost("do_save_single_sheet.jsp", "1", path, "type-" + path, type, "value-" + path, newValue);
        doGet(editorUrl);
    }

    /**
     * Creates a new single sheet, overrides a setting on that sheet, commits
     * the sheet, and associates an URL with the sheet.
     * 
     * The new sheet will be created in the default profile, and the setting
     * to override must be of type int.
     * 
     * @param sheet   the name of the new sheet
     * @param path    the path to the setting to override
     * @param oldValue   the old (default) value of that setting
     * @param newValue   the new value for that setting
     * @param assoc      the URL to associate with the new sheet
     * @throws Exception
     */
    private void addSingleAndOverride(String sheet, String path, String oldValue, String newValue, String assoc) throws Exception {
        doGet(urlSheets);
        String stage = "PROFILE";
        String job = "basic";
        String url = findHref("do_show_add_single_sheet.jsp", managerId, "basic");
        doGet(url);
        doPost("do_add_single_sheet.jsp", "host", host, "port", "-1", "id", Integer.toString(managerId), "stage", stage, "job", job, "sheet", sheet);
        url = findHref("do_override_path.jsp", path, oldValue);
        doGet(url);
        url = findHref("do_show_path_detail.jsp", path, oldValue);
        doGet(url);
        doPost("do_save_path.jsp", "host", host, "port", "-1", "id", Integer.toString(managerId), "stage", stage, "job", job, "sheet", sheet, "path", path, "type-" + path, "int", "value-" + path, newValue);
        doGet(urlSheets);
        url = findHref("do_commit_sheet.jsp", managerId, sheet);
        doGet(url);
        url = findHref("do_show_associate.jsp", managerId, sheet);
        doGet(url);
        doPost("do_associate.jsp", "host", host, "port", "-1", "id", Integer.toString(managerId), "stage", stage, "job", job, "sheet", sheet, "add", "Y", "surts", "# Comment line to ignore\n\n" + assoc);
        doPost("do_show_config.jsp", "host", host, "port", "-1", "id", Integer.toString(managerId), "job", job, "stage", stage, "button", "Settings", "url", assoc + "/a/b/c/d");
        int p = this.lastFetched.indexOf(newValue);
        assertTrue("Configuration for " + assoc + " did not contain override value of " + newValue, p > 0);
    }

    private URL toURL(String urlString) throws Exception {
        if (urlString.startsWith("http://")) {
            return new URL(urlString);
        }
        if (urlString.startsWith("/")) {
            return new URL("http://localhost:7777" + urlString);
        }
        if (lastUrl == null) {
            return new URL("http://localhost:7777/heritrix/" + urlString);
        }
        String last = lastUrl.toString();
        int p = last.indexOf('&');
        if (p >= 0) {
            last = last.substring(0, p);
        }
        p = last.lastIndexOf('/');
        if (p >= 0) {
            last = last.substring(0, p + 1);
        }
        return new URL(last + urlString);
    }

    private void setCookie(HttpURLConnection conn) {
        System.out.println("Setting request cookie to " + jSessionId);
        if (jSessionId != null) {
            conn.setRequestProperty("Cookie", jSessionId);
        }
    }

    private void doGet(String urlString) throws Exception {
        System.out.println("-----");
        System.out.println("doGet: " + urlString);
        URL url = toURL(urlString);
        System.out.println("full url: " + url);
        HttpURLConnection conn = (HttpURLConnection) (url.openConnection());
        setCookie(conn);
        conn.setFollowRedirects(false);
        InputStream input = null;
        try {
            input = conn.getInputStream();
            parseCookie(conn);
            lastUrl = url;
            lastFetched = IoUtils.readFullyAsString(conn.getInputStream());
            System.out.println("New request cookie to : " + jSessionId);
        } finally {
            IoUtils.close(input);
        }
    }

    private void doAutoPost(String urlString, String... pairs) throws Exception {
        List<String> pairList = new ArrayList<String>();
        pairList.addAll(Arrays.asList(pairs));
        for (String s : new String[] { "host", "port", "id", "stage", "job", "sheet" }) {
            String regex = "<input type=\"hidden\" name=\"" + s + "\" value=\"(.*?)\"";
            try {
                String value = find(regex);
                pairList.add(s);
                pairList.add(value);
            } catch (IllegalStateException e) {
            }
        }
        doPost(urlString, pairList.toArray(new String[pairList.size()]));
    }

    private void doPost(String urlString, String... pairs) throws Exception {
        System.out.println("-----");
        System.out.println("doPost: " + urlString);
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Pairs must come in pairs.");
        }
        URL url = toURL(urlString);
        System.out.println("full URL: " + url);
        for (int i = 0; i < pairs.length; i += 2) {
            System.out.println(pairs[i] + "=" + pairs[i + 1]);
        }
        HttpURLConnection conn = (HttpURLConnection) (url.openConnection());
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        setCookie(conn);
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
            if (pairs.length > 0) {
                wr.write(URLEncoder.encode(pairs[0], "UTF-8"));
                wr.write('=');
                wr.write(URLEncoder.encode(pairs[1], "UTF-8"));
                for (int i = 2; i < pairs.length; i += 2) {
                    wr.write('&');
                    wr.write(URLEncoder.encode(pairs[i], "UTF-8"));
                    wr.write('=');
                    wr.write(URLEncoder.encode(pairs[i + 1], "UTF-8"));
                }
            }
            wr.flush();
        } finally {
            IoUtils.close(wr);
        }
        InputStream input = null;
        try {
            input = conn.getInputStream();
            lastUrl = url;
            lastFetched = IoUtils.readFullyAsString(conn.getInputStream());
            parseCookie(conn);
            System.out.println("Response: " + conn.getResponseCode());
            System.out.println("Request Cookie is now: " + jSessionId);
            System.out.println(lastFetched);
        } finally {
            IoUtils.close(input);
        }
    }

    private void parseCookie(HttpURLConnection conn) {
        String cookie = conn.getHeaderField("Set-Cookie");
        System.out.println("Parse cookie: " + cookie);
        if (cookie == null) {
            return;
        }
        int p1 = cookie.indexOf("JSESSIONID=");
        if (p1 < 0) {
            return;
        }
        int p2 = cookie.indexOf(";", p1);
        if (p2 < 0) {
            jSessionId = cookie.substring(p1);
        } else {
            jSessionId = cookie.substring(p1, p2);
        }
    }

    private String findHref(Object... tokens) {
        StringBuilder regex = new StringBuilder();
        regex.append("href=\"(.*?");
        for (Object t : tokens) {
            regex.append(t).append(".*?");
        }
        regex.append(")\"");
        return find(regex.toString());
    }

    private String find(String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this.lastFetched);
        boolean found = matcher.find();
        assertTrue("Didn't find expected pattern: " + regex, found);
        return matcher.group(1);
    }

    public static File getCrawlerDir() {
        try {
            File tmp = TmpDirTestCase.tmpDir();
            return new File(tmp, "webuitest");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File getJobsDir() {
        return new File(getCrawlerDir(), "jobs");
    }

    public void setupDirs() throws Exception {
        File crawler = getCrawlerDir();
        FileUtils.deleteDir(crawler);
        crawler.mkdirs();
        File jobs = getJobsDir();
        jobs.mkdirs();
        File defProf = new File(jobs, "profile-basic");
        defProf.mkdirs();
        File defProfSheets = new File(defProf, "sheets");
        defProfSheets.mkdirs();
        new FileOutputStream(new File(defProf, "config.txt")).close();
        new FileOutputStream(new File(defProf, "seeds.txt")).close();
        File defProfGlobal = new File(defProfSheets, "global.sheet");
        copyResource("/org/archive/crawler/webui/global.sheet", defProfGlobal);
    }

    private void copyResource(String resource, File file) throws IOException {
        InputStream input = null;
        FileOutputStream output = null;
        try {
            input = getClass().getResourceAsStream(resource);
            if (input == null) {
                throw new IllegalArgumentException("Not found: " + resource);
            }
            output = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            for (int l = input.read(buf); l > 0; l = input.read(buf)) {
                output.write(buf, 0, l);
            }
        } finally {
            IoUtils.close(input);
            IoUtils.close(output);
        }
    }
}
