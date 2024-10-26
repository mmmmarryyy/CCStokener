package org.garret.ptl.startup;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import org.garret.ptl.startup.tomcat.TomcatServer;
import org.junit.Ignore;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Tests the logic of lconfig detection and codepack config/resource merging in Configuration class.
 *
 * @author Andrey Subbotin
 */
@Ignore
public class TestConfig {

    public void test_config() throws Exception {
        System.clearProperty("config-test-property");
        Configuration.init();
        File startFolder = Configuration.codepackRoot("portal");
        XmlConfiguration config = Configuration.buildConfiguration(new File(startFolder, "test/resources/sample-config/packet2"));
        assert "set".equals(System.getProperty("config-test-property"));
        assert "0".equals(config.getAttribute("lconfig-attribute"));
        assert "2".equals(config.getAttribute("packet2-attribute"));
        assert null == config.getAttribute("unknown-attribute");
        assert "zz".equals(config.getAttribute("unknown-attribute", "zz"));
        assert "0_2".equals(config.getAttribute("composite-reference"));
        assert 0 == config.getAttribute("lconfig-attribute", 1);
        assert 1 == config.getAttribute("unknown-attribute", 1);
        List<String> correctlist = Arrays.asList(new String[] { "value-lconfig", "value-packet1", "value-packet2", "value-included", "2" });
        List<String> configlist = Arrays.asList(config.getMultiAttribute("test_multiattribute"));
        assert new HashSet<String>(configlist).equals(new HashSet<String>(correctlist));
        Map<String, String> correctmap = new HashMap<String, String>();
        correctmap.put("lconfig", "lconfig_value");
        correctmap.put("packet1", "packet1_value");
        correctmap.put("packet2", "packet2_value");
        correctmap.put("included", "included_value");
        correctmap.put("lconfig_ref", "0");
        assert correctmap.equals(config.getMapAttribute("test_map"));
        assert config.codepackRoot("packet1").equals(new File(startFolder, "test/resources/sample-config/packet1.jar"));
        assert config.codepackRoot("packet2").equals(new File(startFolder, "test/resources/sample-config/packet2"));
        XmlConfiguration old = Configuration.replaceConfigurationWithTest(config);
        try {
            assert (Object) Configuration.getSpringBean("sample") instanceof ArrayList;
            ValueHolder holder = Configuration.getSpringBean("holder");
            assert holder.value.equals("2");
        } finally {
            Configuration.replaceConfigurationWithTest(old);
        }
    }

    public void testTomcat() throws Exception {
        Configuration.init();
        TomcatServer ts = new TomcatServer();
        ts.registerServlet("/*", TestServlet.class.getName());
        ts.registerFilter("/*", StaticContentServletFilter.class.getName());
        ts.registerFilter("/*", TestFilter.class.getName());
        ts.start(5555);
        File startFolder = Configuration.codepackRoot("portal");
        XmlConfiguration config = Configuration.buildConfiguration(new File(startFolder, "test/resources/sample-config/packet1"));
        XmlConfiguration old = Configuration.replaceConfigurationWithTest(config);
        try {
            TestServlet.clearCalls();
            TestFilter.clearCalls();
            URL url = new URL("http://127.0.0.1:5555/a");
            URLConnection c = url.openConnection();
            InputStream is = c.getInputStream();
            is.close();
            assert TestServlet.calls() == 1;
            assert TestFilter.calls() == 1;
            url = new URL("http://127.0.0.1:5555/file1.txt");
            c = url.openConnection();
            assert c.getContentLength() == 3;
            url = new URL("http://127.0.0.1:5555/file2.txt");
            c = url.openConnection();
            assert c.getContentLength() == 6;
        } finally {
            Configuration.replaceConfigurationWithTest(old);
        }
    }

    public static class TestServlet extends HttpServlet {

        private static int o_calls = 0;

        static synchronized void clearCalls() {
            TestServlet.o_calls = 0;
        }

        static synchronized int calls() {
            return TestServlet.o_calls;
        }

        protected synchronized void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
            TestServlet.o_calls++;
        }
    }

    public static class TestFilter implements Filter {

        private static int o_calls = 0;

        static synchronized void clearCalls() {
            TestFilter.o_calls = 0;
        }

        static synchronized int calls() {
            return TestFilter.o_calls;
        }

        public void init(FilterConfig filterConfig) throws ServletException {
        }

        public void destroy() {
        }

        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            TestFilter.o_calls++;
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public static class ValueHolder {

        private String value = null;

        public ValueHolder() {
        }

        public void setValue(String param) {
            value = param;
        }
    }
}
