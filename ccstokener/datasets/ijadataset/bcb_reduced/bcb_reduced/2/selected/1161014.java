package test.openjmx.adaptor.http;

import junit.framework.TestCase;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.Attribute;
import openjmx.adaptor.http.HttpAdaptor;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.InputStream;

/**
 * Class HttpAdaptorTest, tests the basics of the HttpAdaptor class
 *
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.6 $
 */
public class HttpAdaptorTest extends TestCase {

    protected MBeanServer server;

    protected ObjectName name;

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
            server.createMBean("openjmx.adaptor.http.HttpAdaptor", name, null);
            server.invoke(name, "start", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tearDown() {
        try {
            server.unregisterMBean(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testBasics() throws Exception {
        assertEquals(new Integer(8080), server.getAttribute(name, "Port"));
        assertEquals("localhost", server.getAttribute(name, "Host"));
        assertEquals("none", server.getAttribute(name, "AuthenticationMethod"));
        server.invoke(name, "stop", null, null);
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
        server.invoke(name, "addAuthorization", new Object[] { "openjmx", "openjmx" }, new String[] { "java.lang.String", "java.lang.String" });
        server.invoke(name, "stop", null, null);
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
        int port = 8080;
        URL url = new URL("http://" + host + ":" + port + "/");
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        in.close();
        server.invoke(name, "stop", null, null);
        server.setAttribute(name, new Attribute("AuthenticationMethod", "basic"));
        server.invoke(name, "addAuthorization", new Object[] { "openjmx", "openjmx" }, new String[] { "java.lang.String", "java.lang.String" });
        server.invoke(name, "start", null, null);
        url = new URL("http://" + host + ":" + port + "/");
        connection = url.openConnection();
        in = connection.getInputStream();
        in.close();
        assertEquals(((HttpURLConnection) connection).getResponseCode(), 401);
        url = new URL("http://" + host + ":" + port + "/");
        connection = url.openConnection();
        connection.setRequestProperty("Authorization", "basic b3BlbmpteDpvcGVuam14");
        in = connection.getInputStream();
        in.close();
        server.invoke(name, "stop", null, null);
        server.setAttribute(name, new Attribute("AuthenticationMethod", "none"));
    }
}
