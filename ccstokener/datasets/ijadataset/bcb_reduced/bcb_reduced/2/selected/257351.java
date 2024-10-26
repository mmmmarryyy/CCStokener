package org.garret.ptl.startup.tomcat;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import org.garret.ptl.startup.Configuration;
import org.junit.Ignore;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Tests the logic of lconfig detection and codepack config/resource merging in Configuration class.
 *
 * @author Andrey Subbotin
 */
@Ignore
public class TestCatalinaCluster {

    public void ztest_cluster() throws Exception {
        Configuration.init();
        TomcatServer ts1 = new TomcatServer();
        ts1.registerServlet("/*", TestServlet.class.getName());
        ts1.registerCluster(5554);
        ts1.start(5555);
        TomcatServer ts2 = new TomcatServer();
        ts2.registerServlet("/*", TestServlet.class.getName());
        ts2.registerCluster(5554);
        ts2.start(5556);
        URL url1 = new URL("http://127.0.0.1:5555/a");
        HttpURLConnection c1 = (HttpURLConnection) url1.openConnection();
        assert getData(c1).equals("a null");
        String cookie = c1.getHeaderField("Set-Cookie");
        Thread.sleep(5000);
        URL url2 = new URL("http://127.0.0.1:5556/a");
        HttpURLConnection c2 = (HttpURLConnection) url2.openConnection();
        c2.setRequestProperty("Cookie", cookie);
        assert getData(c2).equals("a a");
    }

    public void test_filecluster() throws Exception {
        Configuration.init();
        LruPersistentManager sessionManager2 = new LruPersistentManager(new File("d:/temp/test"));
        TomcatServer ts2 = new TomcatServer("hf1", sessionManager2);
        ts2.registerServlet("/*", TestServlet.class.getName());
        ts2.start(5556);
        LruPersistentManager sessionManager1 = new LruPersistentManager(new File("d:/temp/test"));
        TomcatServer ts1 = new TomcatServer("hf2", sessionManager1);
        ts1.registerServlet("/*", TestServlet.class.getName());
        ts1.start(5555);
        URL url1 = new URL("http://127.0.0.1:5555/a");
        HttpURLConnection c1 = (HttpURLConnection) url1.openConnection();
        assert getData(c1).equals("a null");
        String cookie = c1.getHeaderField("Set-Cookie");
        Thread.sleep(10000);
        URL url2 = new URL("http://127.0.0.1:5556/a");
        HttpURLConnection c2 = (HttpURLConnection) url2.openConnection();
        c2.setRequestProperty("Cookie", cookie);
        assert getData(c2).equals("a a");
        Thread.sleep(15000);
    }

    private static String getData(HttpURLConnection c) throws Exception {
        StringBuffer sb = new StringBuffer();
        InputStream is = c.getInputStream();
        int chr;
        while ((chr = is.read()) >= 0) sb.append((char) chr);
        is.close();
        return sb.toString();
    }

    public static class TestServlet extends HttpServlet {

        protected synchronized void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
            HttpSession session = httpServletRequest.getSession(true);
            session.setAttribute("test" + httpServletRequest.getServerPort(), "a");
            System.err.println("SERVLET CALLED=" + httpServletRequest.getServerPort() + " " + session.getAttribute("test" + 5555) + " " + session.getAttribute("test" + 5556) + " " + session.getId());
            String body = "" + session.getAttribute("test" + 5555) + " " + session.getAttribute("test" + 5556);
            OutputStream os = httpServletResponse.getOutputStream();
            os.write(body.getBytes());
            os.close();
        }
    }
}
