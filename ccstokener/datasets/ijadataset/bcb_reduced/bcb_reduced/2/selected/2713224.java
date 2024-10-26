package test.mx4j.tools.adaptor.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

/**
 * Test of HttpAdapter XML results
 *
 * @version $Revision: 1.3 $
 */
public class HttpAdaptorXMLTest extends XMLTestCase {

    protected MBeanServer server;

    protected ObjectName httpName, test1Name, test2Name, test3Name;

    protected String host = "localhost";

    protected int port = 8080;

    TestClass test1 = new TestClass("t1");

    TestClass test2 = new TestClass("t1");

    TestClass2 test3 = new TestClass2("t3");

    /**
    * Construct the test case
    */
    public HttpAdaptorXMLTest(String name) {
        super(name);
    }

    public static interface TestClassMBean {

        public String getStr();

        public Double getDouble();

        public boolean isTrue();

        public void setStr(String str);

        public Boolean aMethod(String string);

        public void anotherMethod(String string, int test);
    }

    public static class TestClass extends NotificationBroadcasterSupport implements TestClassMBean {

        private String str;

        public TestClass(String str) {
            this.str = str;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public Double getDouble() {
            return new Double(0);
        }

        public boolean isTrue() {
            return true;
        }

        public Boolean aMethod(String string) {
            return new Boolean(string.equals("true"));
        }

        public void anotherMethod(String string, int test) {
            this.str = string;
        }

        public MBeanNotificationInfo[] getNotificationInfo() {
            MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[1];
            notifications[0] = new MBeanNotificationInfo(new String[] { "test1", "test2" }, "name", "test");
            return notifications;
        }
    }

    public static interface TestClass2MBean {

        public String getStr();

        public String[] getStrArray();

        public void setStrArray(String[] str);

        public Double getDouble();

        public void setDouble(Double doubleValue);

        public void setStr(String str);

        public Map getMap();

        public List getList();
    }

    public static class TestClass2 extends NotificationBroadcasterSupport implements TestClass2MBean {

        private String str;

        private String[] strArray = new String[] { "a", "b", "c" };

        private Map map = new HashMap();

        private List list = new ArrayList();

        private Double doubleValue = new Double(0);

        public TestClass2(String str) {
            this.str = str;
            list.add("1");
            list.add("2");
            map.put("1", new Integer(1));
            map.put("2", new Integer(2));
        }

        public String getStr() {
            return str;
        }

        public String[] getStrArray() {
            return strArray;
        }

        public void setStrArray(String[] str) {
            this.strArray = strArray;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public void setDouble(Double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public Double getDouble() {
            return doubleValue;
        }

        public Map getMap() {
            return map;
        }

        public List getList() {
            return list;
        }
    }

    public void setUp() {
        try {
            server = MBeanServerFactory.createMBeanServer("Http");
            httpName = new ObjectName("Http:name=HttpAdaptor");
            test1Name = new ObjectName("Test:name=test1");
            test2Name = new ObjectName("Test:name=test2");
            test3Name = new ObjectName("Test:name=test3");
            server.createMBean("mx4j.tools.adaptor.http.HttpAdaptor", httpName, null);
            String hostProperty = System.getProperty("test.http.host");
            if (hostProperty != null) {
                host = hostProperty;
            }
            String portProperty = System.getProperty("test.http.port");
            if (portProperty != null) {
                port = Integer.parseInt(portProperty);
            }
            server.setAttribute(httpName, new Attribute("Host", host));
            server.setAttribute(httpName, new Attribute("Port", new Integer(port)));
            server.registerMBean(test1, test1Name);
            server.registerMBean(test2, test2Name);
            server.registerMBean(test3, test3Name);
            server.invoke(httpName, "start", null, null);
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setControlParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
            XMLUnit.setTestParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tearDown() {
        try {
            while (((Boolean) server.getAttribute(httpName, "Active")).booleanValue()) {
                try {
                    server.invoke(httpName, "stop", null, null);
                    synchronized (this) {
                        wait(1000);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            server.unregisterMBean(httpName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    * Test the mbeans request
    */
    public void testServer() throws Exception {
        Reader stream = getReader(host, port, "server");
        StringBuffer result = new StringBuffer();
        int i = 0;
        while ((i = stream.read()) >= 0) {
            result.append((char) i);
        }
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<MBean classname=\"mx4j.tools.adaptor.http.HttpAdaptor\" description=\"HttpAdaptor MBean\" objectname=\"Http:name=HttpAdaptor\"/>" + "<MBean classname=\"mx4j.server.interceptor.ContextClassLoaderMBeanServerInterceptor\" description=\"MBeanServer interceptor\" objectname=\"JMImplementation:interceptor=contextclassloader\"/>" + "<MBean classname=\"mx4j.server.interceptor.InvokerMBeanServerInterceptor\" description=\"The interceptor that invokes on the MBean instance\" objectname=\"JMImplementation:interceptor=invoker\"/>" + "<MBean classname=\"mx4j.server.interceptor.NotificationListenerMBeanServerInterceptor\" description=\"MBeanServer interceptor\" objectname=\"JMImplementation:interceptor=notificationwrapper\"/>" + "<MBean classname=\"mx4j.server.interceptor.SecurityMBeanServerInterceptor\" description=\"The interceptor that performs security checks for MBeanServer to MBean calls\" objectname=\"JMImplementation:interceptor=security\"/>" + "<MBean classname=\"mx4j.server.MX4JMBeanServerDelegate\" description=\"Manageable Bean\" objectname=\"JMImplementation:type=MBeanServerDelegate\"/>" + "<MBean classname=\"mx4j.server.interceptor.MBeanServerInterceptorConfigurator\" description=\"Configurator for MBeanServer to MBean interceptors\" objectname=\"JMImplementation:type=MBeanServerInterceptorConfigurator\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test2\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\"/>" + "</Server>";
        assertXMLEqual(controlMBean, result.toString());
        stream.close();
    }

    /**
    * Test the mbeans request
    */
    public void testServerAndFilters() throws Exception {
        Reader stream = getReader(host, port, "server?instanceof=test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test2\"/>" + "</Server>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
    }

    public void testServerByDomain() throws Exception {
        Reader stream = getReader(host, port, "serverbydomain");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<Domain name=\"Http\">" + "<MBean classname=\"mx4j.tools.adaptor.http.HttpAdaptor\" description=\"HttpAdaptor MBean\" objectname=\"Http:name=HttpAdaptor\"/>" + "</Domain>" + "<Domain name=\"JMImplementation\">" + "<MBean classname=\"mx4j.server.interceptor.ContextClassLoaderMBeanServerInterceptor\" description=\"MBeanServer interceptor\" objectname=\"JMImplementation:interceptor=contextclassloader\"/>" + "<MBean classname=\"mx4j.server.interceptor.InvokerMBeanServerInterceptor\" description=\"The interceptor that invokes on the MBean instance\" objectname=\"JMImplementation:interceptor=invoker\"/>" + "<MBean classname=\"mx4j.server.interceptor.NotificationListenerMBeanServerInterceptor\" description=\"MBeanServer interceptor\" objectname=\"JMImplementation:interceptor=notificationwrapper\"/>" + "<MBean classname=\"mx4j.server.interceptor.SecurityMBeanServerInterceptor\" description=\"The interceptor that performs security checks for MBeanServer to MBean calls\" objectname=\"JMImplementation:interceptor=security\"/>" + "<MBean classname=\"mx4j.server.MX4JMBeanServerDelegate\" description=\"Manageable Bean\" objectname=\"JMImplementation:type=MBeanServerDelegate\"/>" + "<MBean classname=\"mx4j.server.interceptor.MBeanServerInterceptorConfigurator\" description=\"Configurator for MBeanServer to MBean interceptors\" objectname=\"JMImplementation:type=MBeanServerInterceptorConfigurator\"/>" + "</Domain>" + "<Domain name=\"Test\">" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test2\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\"/>" + "</Domain>" + "</Server>";
        assertXMLEqual(new StringReader(controlMBean), stream);
    }

    public void testServerByDomainAndFilters() throws Exception {
        Reader stream = getReader(host, port, "serverbydomain?instanceof=test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<Domain name=\"Http\"/>" + "<Domain name=\"JMImplementation\"/>" + "<Domain name=\"Test\">" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test2\"/>" + "</Domain>" + "</Server>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream = getReader(host, port, "serverbydomain?querynames=*:*");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<Domain name=\"Http\">" + "<MBean classname=\"mx4j.tools.adaptor.http.HttpAdaptor\" description=\"HttpAdaptor MBean\" objectname=\"Http:name=HttpAdaptor\"/>" + "</Domain>" + "<Domain name=\"JMImplementation\">" + "<MBean classname=\"mx4j.server.interceptor.ContextClassLoaderMBeanServerInterceptor\" description=\"MBeanServer interceptor\" objectname=\"JMImplementation:interceptor=contextclassloader\"/>" + "<MBean classname=\"mx4j.server.interceptor.InvokerMBeanServerInterceptor\" description=\"The interceptor that invokes on the MBean instance\" objectname=\"JMImplementation:interceptor=invoker\"/>" + "<MBean classname=\"mx4j.server.interceptor.NotificationListenerMBeanServerInterceptor\" description=\"MBeanServer interceptor\" objectname=\"JMImplementation:interceptor=notificationwrapper\"/>" + "<MBean classname=\"mx4j.server.interceptor.SecurityMBeanServerInterceptor\" description=\"The interceptor that performs security checks for MBeanServer to MBean calls\" objectname=\"JMImplementation:interceptor=security\"/>" + "<MBean classname=\"mx4j.server.MX4JMBeanServerDelegate\" description=\"Manageable Bean\" objectname=\"JMImplementation:type=MBeanServerDelegate\"/>" + "<MBean classname=\"mx4j.server.interceptor.MBeanServerInterceptorConfigurator\" description=\"Configurator for MBeanServer to MBean interceptors\" objectname=\"JMImplementation:type=MBeanServerInterceptorConfigurator\"/>" + "</Domain>" + "<Domain name=\"Test\">" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test2\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\"/>" + "</Domain>" + "</Server>";
        stream = getReader(host, port, "serverbydomain?querynames=Test:*");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<Domain name=\"Test\">" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test2\"/>" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\"/>" + "</Domain>" + "</Server>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream = getReader(host, port, "serverbydomain?querynames=something");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Server>" + "<Exception errorMsg=\"\"/>" + "</Server>";
    }

    /**
    * Test the mbeans delete
    */
    public void testDelete() throws Exception {
        try {
            Reader stream = getReader(host, port, "delete?objectname=Test:name=test1");
            String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test1\" operation=\"delete\" result=\"success\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertTrue(!server.isRegistered(test1Name));
            stream.close();
            stream = getReader(host, port, "delete?objectname=Test:name=test5");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation errorMsg=\"MBean Test:name=test5 not registered\" objectname=\"Test:name=test5\" operation=\"delete\" result=\"error\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            stream.close();
        } finally {
            server.registerMBean(test1, test1Name);
        }
    }

    /**
    * Test the operations invoke
    */
    public void testInvoke() throws Exception {
        Reader stream = getReader(host, port, "invoke?objectname=Test:name=test1&operation=aMethod&type0=java.lang.String&value0=true");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test1\" operation=\"invoke\" result=\"success\" return=\"true\" returnclass=\"java.lang.Boolean\"/>" + "</MBeanOperation>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "invoke?objectname=Test:name=test1&operation=aMethod&type0=java.lang.String&value0=test");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test1\" operation=\"invoke\" result=\"success\" return=\"false\"  returnclass=\"java.lang.Boolean\"/>" + "</MBeanOperation>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
    }

    /**
    * Test the set attribute request
    */
    public void testSetAttribute() throws Exception {
        try {
            Reader stream = getReader(host, port, "setattribute?objectname=Test:name=test1&attribute=Str&value=t2");
            String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test1\" operation=\"setattribute\" result=\"success\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals("t2", server.getAttribute(test1Name, "Str"));
            server.setAttribute(test1Name, new Attribute("Str", "t1"));
            stream.close();
            stream = getReader(host, port, "setattribute?attribute=Str&value=t2");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation errorMsg=\"Incorrect parameters in the request\" operation=\"setattribute\" result=\"error\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals("t1", server.getAttribute(test1Name, "Str"));
            stream = getReader(host, port, "setattribute?objectname=3&attribute=Str&value=t2");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation errorMsg=\"Malformed object name\" objectname=\"3\" operation=\"setattribute\" result=\"error\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals("t1", server.getAttribute(test1Name, "Str"));
            stream.close();
            stream = getReader(host, port, "setattribute?objectname=Test:name=test1&attribute=Number&value=t2");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation errorMsg=\"Attribute Number not found\" objectname=\"Test:name=test1\" operation=\"setattribute\" result=\"error\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals("t1", server.getAttribute(test1Name, "Str"));
            stream.close();
        } finally {
            server.setAttribute(test1Name, new Attribute("Str", "t1"));
        }
    }

    /**
    * Test the set attributes request
    */
    public void testSetAttributes() throws Exception {
        try {
            Reader stream = getReader(host, port, "setattributes?objectname=Test:name=test3&value_Str=t2&set_Str=Set");
            String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test3\" operation=\"setattributes\">" + "<Attribute attribute=\"Str\" value=\"t2\"  result=\"success\"/>" + "</Operation>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals("t2", server.getAttribute(test3Name, "Str"));
            server.setAttribute(test3Name, new Attribute("Str", "t1"));
            stream.close();
            stream = getReader(host, port, "setattributes?objectname=Test:name=test3&value_Str=t2&value_Double=3&setall=Set");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test3\" operation=\"setattributes\">" + "<Attribute attribute=\"Double\" value=\"3\" result=\"success\"/>" + "<Attribute attribute=\"Str\" value=\"t2\" result=\"success\"/>" + "</Operation>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals(new Double(3), server.getAttribute(test3Name, "Double"));
            stream.close();
            stream = getReader(host, port, "setattributes?objectname=Test:name=test3&value_Str=t3&value_Double=4&set_Str=Set&set_Double=Set");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test3\" operation=\"setattributes\">" + "<Attribute attribute=\"Double\" value=\"4\" result=\"success\"/>" + "<Attribute attribute=\"Str\" value=\"t3\" result=\"success\"/>" + "</Operation>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals(new Double(4), server.getAttribute(test3Name, "Double"));
            assertEquals("t3", server.getAttribute(test3Name, "Str"));
            stream = getReader(host, port, "setattributes?objectname=Test:name=test3&value_Str=t3&value_Double=c4&set_Str=Set&set_Double=Set");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Test:name=test3\" operation=\"setattributes\">" + "<Attribute attribute=\"Double\" errorMsg=\"Value: c4 could not be converted to java.lang.Double\" result=\"error\"/>" + "<Attribute attribute=\"Str\" value=\"t3\" result=\"success\"/>" + "</Operation>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
            assertEquals(new Double(4), server.getAttribute(test3Name, "Double"));
            assertEquals("t3", server.getAttribute(test3Name, "Str"));
        } finally {
            server.setAttribute(test3Name, new Attribute("Str", "t1"));
            server.setAttribute(test3Name, new Attribute("Double", new Double(0)));
        }
    }

    public void testCreate() throws Exception {
        ObjectName name = new ObjectName("Http:name=create");
        try {
            Reader stream = getReader(host, port, "create?class=mx4j.tools.adaptor.http.HttpAdaptor&objectname=" + name.toString());
            String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBeanOperation>" + "<Operation objectname=\"Http:name=create\" name=\"create\" result=\"success\"/>" + "</MBeanOperation>";
            assertXMLEqual(new StringReader(controlMBean), stream);
        } finally {
            server.unregisterMBean(name);
        }
    }

    public void testEmpty() throws Exception {
        Reader stream = getReader(host, port, "empty");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><empty/>\n";
        assertXMLEqual(new StringReader(controlMBean), stream);
    }

    /**
    * Test the mbeans request
    */
    public void testSingleMBean() throws Exception {
        Reader stream = getReader(host, port, "mbean?objectname=Test:name=test1");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Double\" strinit=\"true\" type=\"java.lang.Double\" value=\"0.0\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Str\" strinit=\"true\" type=\"java.lang.String\" value=\"t1\"/>" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"True\" strinit=\"true\" type=\"boolean\"  value=\"true\"/>" + "<Constructor description=\"Constructor exposed for management\" name=\"HttpAdaptorXMLTest$TestClass\">" + "<Parameter description=\"Constructor's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"aMethod\" return=\"java.lang.Boolean\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\">" + "</Parameter>" + "</Operation>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"anotherMethod\" return=\"void\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"></Parameter>" + "<Parameter description=\"Operation's parameter n. 2\" id=\"1\" name=\"param2\" strinit=\"true\" type=\"int\"></Parameter>" + "</Operation>" + "<Notification description=\"test\" name=\"name\">" + "<Type name=\"test1\"></Type>" + "<Type name=\"test2\"></Type>" + "</Notification>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream = getReader(host, port, "mbean?objectname=Test:name=test3");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Double\" strinit=\"true\" type=\"java.lang.Double\" value=\"0.0\"/>" + "<Attribute aggregation=\"collection\" availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"List\" strinit=\"false\" type=\"java.util.List\" value=\"[1, 2]\"/>" + "<Attribute aggregation=\"map\" availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Map\" strinit=\"false\" type=\"java.util.Map\" value=\"{2=2, 1=1}\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Str\" strinit=\"true\" type=\"java.lang.String\" value=\"t3\"/>" + "<Attribute aggregation=\"array\" availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"StrArray\" strinit=\"false\" type=\"[Ljava.lang.String;\" value=\"" + test3.getStrArray().toString() + "\"/>" + "<Constructor description=\"Constructor exposed for management\" name=\"HttpAdaptorXMLTest$TestClass2\">" + "<Parameter description=\"Constructor&apos;s parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        Object theMap = server.getAttribute(new ObjectName("Test:name=test3"), "StrArray");
        try {
            server.setAttribute(new ObjectName("Test:name=test3"), new Attribute("StrArray", null));
            stream = getReader(host, port, "getattribute?objectname=Test:name=test3&attribute=StrArray&format=map");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute aggregation=\"array\" availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"true\" name=\"StrArray\" strinit=\"false\" type=\"[Ljava.lang.String;\" value=\"null\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Double\" strinit=\"true\" type=\"java.lang.Double\" value=\"0.0\"/>" + "<Attribute aggregation=\"collection\" availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"List\" strinit=\"false\" type=\"java.util.List\" value=\"[1, 2]\"/>" + "<Attribute aggregation=\"map\" availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Map\" strinit=\"false\" type=\"java.util.Map\" value=\"{2=2, 1=1}\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Str\" strinit=\"true\" type=\"java.lang.String\" value=\"t3\"/>" + "<Constructor description=\"Constructor exposed for management\" name=\"HttpAdaptorXMLTest$TestClass2\">" + "<Parameter description=\"Constructor&apos;s parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "</MBean>";
        } finally {
            server.setAttribute(new ObjectName("Test:name=test3"), new Attribute("StrArray", theMap));
        }
    }

    public void testSingleMBeanAndFilters() throws Exception {
        Reader stream = getReader(host, port, "mbean?objectname=Test:name=test1&attributes=false");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "<Constructor description=\"Constructor exposed for management\" name=\"HttpAdaptorXMLTest$TestClass\">" + "<Parameter description=\"Constructor's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"aMethod\" return=\"java.lang.Boolean\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\">" + "</Parameter>" + "</Operation>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"anotherMethod\" return=\"void\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"></Parameter>" + "<Parameter description=\"Operation's parameter n. 2\" id=\"1\" name=\"param2\" strinit=\"true\" type=\"int\"></Parameter>" + "</Operation>" + "<Notification description=\"test\" name=\"name\">" + "<Type name=\"test1\"></Type>" + "<Type name=\"test2\"></Type>" + "</Notification>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "mbean?objectname=Test:name=test1&constructors=false");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Double\" strinit=\"true\" type=\"java.lang.Double\" value=\"0.0\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Str\" strinit=\"true\" type=\"java.lang.String\" value=\"t1\"/>" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"True\" strinit=\"true\" type=\"boolean\"  value=\"true\"/>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"aMethod\" return=\"java.lang.Boolean\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\">" + "</Parameter>" + "</Operation>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"anotherMethod\" return=\"void\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"></Parameter>" + "<Parameter description=\"Operation's parameter n. 2\" id=\"1\" name=\"param2\" strinit=\"true\" type=\"int\"></Parameter>" + "</Operation>" + "<Notification description=\"test\" name=\"name\">" + "<Type name=\"test1\"></Type>" + "<Type name=\"test2\"></Type>" + "</Notification>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "mbean?objectname=Test:name=test1&operations=false");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Double\" strinit=\"true\" type=\"java.lang.Double\" value=\"0.0\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Str\" strinit=\"true\" type=\"java.lang.String\" value=\"t1\"/>" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"True\" strinit=\"true\" type=\"boolean\"  value=\"true\"/>" + "<Constructor description=\"Constructor exposed for management\" name=\"HttpAdaptorXMLTest$TestClass\">" + "<Parameter description=\"Constructor's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "<Notification description=\"test\" name=\"name\">" + "<Type name=\"test1\"></Type>" + "<Type name=\"test2\"></Type>" + "</Notification>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "mbean?objectname=Test:name=test1&notifications=false");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Double\" strinit=\"true\" type=\"java.lang.Double\" value=\"0.0\"/>" + "<Attribute availability=\"RW\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"Str\" strinit=\"true\" type=\"java.lang.String\" value=\"t1\"/>" + "<Attribute availability=\"RO\" description=\"Attribute exposed for management\" isnull=\"false\" name=\"True\" strinit=\"true\" type=\"boolean\"  value=\"true\"/>" + "<Constructor description=\"Constructor exposed for management\" name=\"HttpAdaptorXMLTest$TestClass\">" + "<Parameter description=\"Constructor's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"aMethod\" return=\"java.lang.Boolean\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\">" + "</Parameter>" + "</Operation>" + "<Operation description=\"Operation exposed for management\" impact=\"unknown\" name=\"anotherMethod\" return=\"void\">" + "<Parameter description=\"Operation's parameter n. 1\" id=\"0\" name=\"param1\" strinit=\"true\" type=\"java.lang.String\"></Parameter>" + "<Parameter description=\"Operation's parameter n. 2\" id=\"1\" name=\"param2\" strinit=\"true\" type=\"int\"></Parameter>" + "</Operation>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "mbean?objectname=Test:name=test1&notifications=false&attributes=false&operations=false&constructors=false");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
    }

    public void testConstructors() throws Exception {
        Reader stream = getReader(host, port, "constructors?classname=mx4j.tools.adaptor.http.HttpAdaptor");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Class classname=\"mx4j.tools.adaptor.http.HttpAdaptor\">" + "<Constructor name=\"mx4j.tools.adaptor.http.HttpAdaptor\"/>" + "<Constructor name=\"mx4j.tools.adaptor.http.HttpAdaptor\">" + "<Parameter id=\"0\" strinit=\"true\" type=\"int\"/>" + "</Constructor>" + "<Constructor name=\"mx4j.tools.adaptor.http.HttpAdaptor\">" + "<Parameter id=\"0\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "<Constructor name=\"mx4j.tools.adaptor.http.HttpAdaptor\">" + "<Parameter id=\"0\" strinit=\"true\" type=\"int\"/>" + "<Parameter id=\"1\" strinit=\"true\" type=\"java.lang.String\"/>" + "</Constructor>" + "</Class>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream = getReader(host, port, "constructors?classname=mx4j.Something");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<Exception classname=\"mx4j.Something\" errorMsg=\"class mx4j.Something not found\"/>";
        assertXMLEqual(new StringReader(controlMBean), stream);
    }

    /**
    * Test the get attribute request for arrays
    */
    public void testGetAttribute() throws Exception {
        Reader stream = getReader(host, port, "getattribute?objectname=Test:name=test1&attribute=Str");
        String controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass\" description=\"Manageable Bean\" objectname=\"Test:name=test1\">" + "<Attribute classname=\"java.lang.String\" isnull=\"false\" name=\"Str\" value=\"t1\"/>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "getattribute?objectname=Test:name=test3&attribute=StrArray&format=array");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute classname=\"[Ljava.lang.String;\" isnull=\"false\" name=\"StrArray\">" + "<Array componentclass=\"java.lang.String\" length=\"3\">" + "<Element element=\"a\" isnull=\"false\" index=\"0\"/>" + "<Element element=\"b\" isnull=\"false\" index=\"1\"/>" + "<Element element=\"c\" isnull=\"false\" index=\"2\"/>" + "</Array>" + "</Attribute>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "getattribute?objectname=Test:name=test3&attribute=List&format=collection");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute classname=\"java.util.List\" isnull=\"false\" name=\"List\">" + "<Collection length=\"2\">" + "<Element element=\"1\" elementclass=\"java.lang.String\" index=\"0\"/>" + "<Element element=\"2\" elementclass=\"java.lang.String\" index=\"1\"/>" + "</Collection>" + "</Attribute>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        stream.close();
        stream = getReader(host, port, "getattribute?objectname=Test:name=test3&attribute=Map&format=map");
        controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute classname=\"java.util.Map\" isnull=\"false\" name=\"Map\">" + "<Map length=\"2\">" + "<Element index=\"0\" key=\"2\" keyclass=\"java.lang.String\" element=\"2\" elementclass=\"java.lang.Integer\"/>" + "<Element index=\"1\" key=\"1\" keyclass=\"java.lang.String\" element=\"1\" elementclass=\"java.lang.Integer\"/>" + "</Map>" + "</Attribute>" + "</MBean>";
        assertXMLEqual(new StringReader(controlMBean), stream);
        Object theArray = server.getAttribute(new ObjectName("Test:name=test3"), "StrArray");
        try {
            server.setAttribute(new ObjectName("Test:name=test3"), new Attribute("StrArray", null));
            stream = getReader(host, port, "getattribute?objectname=Test:name=test3&attribute=StrArray&format=map");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute classname=\"[Ljava.lang.String;\" isnull=\"true\" name=\"StrArray\"/>" + "</MBean>";
        } finally {
            server.setAttribute(new ObjectName("Test:name=test3"), new Attribute("StrArray", theArray));
        }
        String strArray[] = (String[]) server.getAttribute(new ObjectName("Test:name=test3"), "StrArray");
        String valueAt1 = strArray[1];
        strArray[1] = null;
        try {
            server.setAttribute(new ObjectName("Test:name=test3"), new Attribute("StrArray", strArray));
            stream = getReader(host, port, "getattribute?objectname=Test:name=test3&attribute=StrArray&format=map");
            controlMBean = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<MBean classname=\"test.mx4j.tools.adaptor.http.HttpAdaptorXMLTest$TestClass2\" description=\"Manageable Bean\" objectname=\"Test:name=test3\">" + "<Attribute classname=\"[Ljava.lang.String;\" isnull=\"false\" name=\"StrArray\">" + "<Array componentclass=\"java.lang.String\" length=\"3\">" + "<Element element=\"a\" isnull=\"false\" index=\"0\"/>" + "<Element element=\"null\" isnull=\"true\" index=\"1\"/>" + "<Element element=\"c\" isnull=\"false\" index=\"2\"/>" + "</Array>" + "</Attribute>" + "</MBean>";
        } finally {
            strArray[1] = valueAt1;
            server.setAttribute(new ObjectName("Test:name=test3"), new Attribute("StrArray", strArray));
        }
    }

    public Reader getReader(String host, int port, String path) throws IOException, MalformedURLException {
        URL url = new URL("http://" + host + ":" + port + "/" + path);
        URLConnection connection = url.openConnection();
        return new InputStreamReader(connection.getInputStream());
    }
}
