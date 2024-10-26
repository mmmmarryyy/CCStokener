package test.mx4j.tools.adaptor.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import junit.framework.TestCase;
import mx4j.tools.adaptor.http.HttpCommandProcessorAdaptor;
import mx4j.tools.adaptor.http.HttpInputStream;
import org.w3c.dom.Document;

/**
 * Class HttpAdaptorTest, tests the basics of the HttpAdaptor class
 *
 * @version $Revision: 1.3 $
 */
public class HttpAdaptorTest extends TestCase {

    protected MBeanServer server;

    protected ObjectName name;

    protected static final int DEFAULT_PORT = 9090;

    /**
    * Constructor requested by the JUnit framework
    */
    public HttpAdaptorTest() {
        super("HTTPAdapter Test");
    }

    /**
    * Constructor requested by the JUnit framework
    */
    public HttpAdaptorTest(String name) {
        super(name);
    }

    public void setUp() {
        try {
            server = MBeanServerFactory.createMBeanServer("Http");
            name = new ObjectName("Http:name=HttpAdaptor");
            server.createMBean("mx4j.tools.adaptor.http.HttpAdaptor", name, null);
            server.setAttribute(name, new Attribute("Port", new Integer(DEFAULT_PORT)));
            server.invoke(name, "start", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tearDown() {
        waitToStop();
        try {
            server.unregisterMBean(name);
        } catch (Exception e) {
        }
    }

    public void testBasics() throws Exception {
        assertEquals(new Integer(DEFAULT_PORT), server.getAttribute(name, "Port"));
        assertEquals("localhost", server.getAttribute(name, "Host"));
        assertEquals("none", server.getAttribute(name, "AuthenticationMethod"));
        waitToStop();
        server.setAttribute(name, new Attribute("Port", new Integer(8000)));
        assertEquals(new Integer(8000), server.getAttribute(name, "Port"));
        server.setAttribute(name, new Attribute("Host", "1.1.1.1"));
        assertEquals("1.1.1.1", server.getAttribute(name, "Host"));
        server.setAttribute(name, new Attribute("AuthenticationMethod", "basic"));
        assertEquals("basic", server.getAttribute(name, "AuthenticationMethod"));
        boolean exception = false;
        try {
            server.setAttribute(name, new Attribute("AuthenticationMethod", "something"));
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(exception);
        exception = false;
        try {
            server.invoke(name, "addAuthorization", new Object[] { null, null }, new String[] { "java.lang.String", "java.lang.String" });
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(exception);
        server.invoke(name, "addAuthorization", new Object[] { "mx4j", "mx4j" }, new String[] { "java.lang.String", "java.lang.String" });
        waitToStop();
        exception = false;
        try {
            server.setAttribute(name, new Attribute("Port", new Integer(8000)));
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(true);
        exception = false;
        try {
            server.setAttribute(name, new Attribute("Host", "localhost"));
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(true);
        exception = false;
        try {
            server.setAttribute(name, new Attribute("AuthenticationMethod", "digest"));
        } catch (Exception e) {
            exception = true;
        }
        assertTrue(true);
    }

    public void testAuthentication() throws Exception {
        String host = "localhost";
        int port = DEFAULT_PORT;
        URL url = new URL("http://" + host + ":" + port + "/");
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        in.close();
        waitToStop();
        server.setAttribute(name, new Attribute("AuthenticationMethod", "basic"));
        server.invoke(name, "addAuthorization", new Object[] { "mx4j", "mx4j" }, new String[] { "java.lang.String", "java.lang.String" });
        server.invoke(name, "start", null, null);
        url = new URL("http://" + host + ":" + port + "/");
        connection = url.openConnection();
        try {
            in = connection.getInputStream();
        } catch (Exception e) {
        } finally {
            in.close();
        }
        assertEquals(((HttpURLConnection) connection).getResponseCode(), 401);
        url = new URL("http://" + host + ":" + port + "/");
        connection = url.openConnection();
        connection.setRequestProperty("Authorization", "basic bXg0ajpteDRq");
        in = connection.getInputStream();
        in.close();
        waitToStop();
        server.setAttribute(name, new Attribute("AuthenticationMethod", "none"));
    }

    public void testAddCommandProcessor() throws Exception {
        String host = "localhost";
        int port = DEFAULT_PORT;
        URLConnection connection = null;
        URL url = new URL("http://" + host + ":" + port + "/nonexistant");
        server.invoke(name, "addCommandProcessor", new Object[] { "nonexistant", new DummyCommandProcessor() }, new String[] { "java.lang.String", "mx4j.tools.adaptor.http.HttpCommandProcessor" });
        connection = url.openConnection();
        assertEquals(200, ((HttpURLConnection) connection).getResponseCode());
        server.invoke(name, "removeCommandProcessor", new Object[] { "nonexistant" }, new String[] { "java.lang.String" });
        connection = url.openConnection();
        assertEquals(404, ((HttpURLConnection) connection).getResponseCode());
        server.invoke(name, "addCommandProcessor", new Object[] { "nonexistant", "test.mx4j.tools.adaptor.http.HttpAdaptorTest$DummyCommandProcessor" }, new String[] { "java.lang.String", "java.lang.String" });
        connection = url.openConnection();
        assertEquals(200, ((HttpURLConnection) connection).getResponseCode());
    }

    private void waitToStop() {
        try {
            while (((Boolean) server.getAttribute(name, "Active")).booleanValue()) {
                try {
                    server.invoke(name, "stop", null, null);
                    synchronized (this) {
                        wait(1000);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class DummyCommandProcessor extends HttpCommandProcessorAdaptor {

        public Document executeRequest(HttpInputStream in) throws IOException, JMException {
            return builder.newDocument();
        }
    }
}
