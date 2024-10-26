package org.mortbay.jetty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.BoundedThreadPool;
import org.mortbay.util.IO;

public class WebAppTest extends TestCase {

    Server server = new Server();

    BoundedThreadPool threadPool = new BoundedThreadPool();

    Connector connector = new SelectChannelConnector();

    HandlerCollection handlers = new HandlerCollection();

    ContextHandlerCollection contexts = new ContextHandlerCollection();

    HashUserRealm userRealm = new HashUserRealm();

    RequestLogHandler requestLogHandler = new RequestLogHandler();

    WebAppContext context;

    protected void setUp() throws Exception {
        File dir = new File(".").getAbsoluteFile();
        while (!new File(dir, "webapps").exists()) {
            dir = dir.getParentFile();
        }
        threadPool.setMaxThreads(100);
        server.setThreadPool(threadPool);
        server.setConnectors(new Connector[] { connector });
        handlers.setHandlers(new Handler[] { contexts, new DefaultHandler(), requestLogHandler });
        server.setHandler(handlers);
        context = new WebAppContext(contexts, dir.getAbsolutePath() + "/webapps/test", "/test");
        userRealm.setName("Test Realm");
        userRealm.setConfig(dir.getAbsolutePath() + "/etc/realm.properties");
        server.setUserRealms(new UserRealm[] { userRealm });
        File file = File.createTempFile("test", ".log");
        NCSARequestLog requestLog = new NCSARequestLog(file.getAbsolutePath());
        requestLog.setExtended(false);
        requestLogHandler.setRequestLog(requestLog);
        server.setSendServerVersion(true);
        server.start();
    }

    protected void tearDown() throws Exception {
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
    }

    public void testDoGet() throws Exception {
        URL url = null;
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dump/info?query=foo");
        assertTrue(IO.toString(url.openStream()).startsWith("<html>"));
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/");
        try {
            IO.toString(url.openStream());
            assertTrue(false);
        } catch (FileNotFoundException e) {
            assertTrue(true);
        }
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test");
        IO.toString(url.openStream());
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/");
        String s1 = IO.toString(url.openStream());
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/index.html");
        String s2 = IO.toString(url.openStream());
        assertEquals(s1, s2);
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/d.txt");
        assertTrue(IO.toString(url.openStream()).startsWith("0000"));
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/data.txt");
        String result = IO.toString(url.openStream());
        if (result.endsWith("\r\n")) {
            result = result.substring(0, result.length() - 2);
        } else if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        } else {
            assertTrue(false);
        }
        assertTrue(result.endsWith("9999 3333333333333333333333333333333333333333333333333333333"));
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dispatch/forward/dump/info?query=foo");
        assertTrue(IO.toString(url.openStream()).startsWith("<html>"));
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dispatch/includeW/dump/info?query=foo");
        assertTrue(IO.toString(url.openStream()).startsWith("<H1>"));
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dispatch/includeS/dump/info?query=foo");
        assertTrue(IO.toString(url.openStream()).startsWith("<H1>"));
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dump/info?continue=1000");
        assertTrue(IO.toString(url.openStream()).startsWith("<html>"));
    }

    public void testDoPost() throws Exception {
        URL url = null;
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dump/info?query=foo");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.addRequestProperty(HttpHeaders.CONTENT_TYPE, MimeTypes.FORM_ENCODED);
        connection.addRequestProperty(HttpHeaders.CONTENT_LENGTH, "10");
        connection.getOutputStream().write("abcd=1234\n".getBytes());
        connection.getOutputStream().flush();
        connection.connect();
        String s0 = IO.toString(connection.getInputStream());
        assertTrue(s0.startsWith("<html>"));
        assertTrue(s0.indexOf("<td>POST</td>") > 0);
        assertTrue(s0.indexOf("abcd:&nbsp;</th><td>1234") > 0);
    }

    public void testWebInfAccess() throws Exception {
        assertNotFound("WEB-INF/foo");
        assertNotFound("web-inf");
        assertNotFound("web-inf/");
        assertNotFound("./web-inf/");
        assertNotFound("web-inf/jetty-web.xml");
        assertNotFound("Web-Inf/web.xml");
        assertNotFound("./WEB-INF/web.xml");
        assertNotFound("META-INF");
        assertNotFound("meta-inf/manifest.mf");
        assertNotFound("Meta-Inf/foo");
        assertFound("index.html");
    }

    public void testUnavailable() throws Exception {
        URL url = null;
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dump/info?query=foo");
        assertTrue(IO.toString(url.openStream()).startsWith("<html>"));
        assertTrue(context.getServletHandler().isAvailable());
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/dump/ex2/2");
        try {
            IO.toString(url.openStream());
        } catch (IOException e) {
        }
        assertFalse(context.getServletHandler().isAvailable());
        Thread.sleep(4000);
        assertTrue(context.getServletHandler().isAvailable());
    }

    private void assertNotFound(String resource) throws MalformedURLException, IOException {
        try {
            getResource(resource);
        } catch (FileNotFoundException e) {
            return;
        }
        fail("Expected 404 for resource: " + resource);
    }

    private void assertFound(String resource) throws MalformedURLException, IOException {
        try {
            getResource(resource);
        } catch (FileNotFoundException e) {
            fail("Expected 200 for resource: " + resource);
        }
        return;
    }

    private void getResource(String resource) throws MalformedURLException, IOException {
        URL url;
        url = new URL("http://127.0.0.1:" + connector.getLocalPort() + "/test/" + resource);
        url.openStream();
    }
}
