package org.nexopenframework.context.local;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import junit.framework.TestCase;
import org.nexopenframework.context.local.ThreadLocalMap;
import org.nexopenframework.context.local.ThreadLocalMapSynchListener;
import org.nexopenframework.util.http.JettyHttpServer;

/**
 * <p>NexTReT Open Framework</p>
 * 
 * <p>Test for checking synch between {@link ThreadLocalMap} and HTTP attributes</p>
 * 
 * @author <a href="mailto:fme@nextret.net">Francesc Xavier Magdaleno</a>
 * @version 1.0
 * @since 1.0
 */
public class ThreadLocalMapSynchListenerTest extends TestCase {

    private JettyHttpServer httpServer = new JettyHttpServer();

    public void testSynchronization() throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://localhost:2580/nexopen/sync/a.do");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int code = conn.getResponseCode();
            assertEquals(200, code);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    protected void setUp() throws Exception {
        List eventListeners = new ArrayList(1);
        eventListeners.add(new ThreadLocalMapSynchListener());
        httpServer.setPort(2580);
        httpServer.setContextPath("nexopen");
        httpServer.setEventListeners(eventListeners);
        httpServer.setServlet(new ExampleServlet());
        httpServer.setServletMapping("/sync/*");
        httpServer.startServer();
    }

    protected void tearDown() throws Exception {
        httpServer.stopServer();
    }

    public static class ExampleServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            HttpSession session = req.getSession();
            session.setAttribute("other.value", "lovely value");
            String value = (String) ThreadLocalMap.get("other.value");
            assertNotNull(value);
            assertEquals("lovely value", value);
            session.setAttribute("other.value", "more lovely value");
            String other = (String) ThreadLocalMap.get("other.value");
            assertNotNull(other);
            assertEquals("more lovely value", other);
        }
    }
}
