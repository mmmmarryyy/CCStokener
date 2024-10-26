package net.sipvip.server.services.cookies.impl;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.AbstractHandler;
import com.sun.xml.internal.ws.transport.http.client.HttpCookie;

public class InCookiesServImplTest {

    private Server server = new Server(8080);

    private Object cookie;

    @Before
    public void setUp() throws Exception {
        server.setHandler((Handler) new JettyCookiesHandle());
        server.setStopAtShutdown(true);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testCheckCookies() {
        URL url = null;
        try {
            url = new URL("http://localhost:8080");
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        StringBuffer content = new StringBuffer();
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");
            cookie = connection.getHeaderField("Set-Cookie");
            if (cookie != null) System.out.println("cookie: " + cookie.toString());
            connection.setDoInput(true);
            InputStream is = connection.getInputStream();
            byte[] buffer = new byte[2048];
            int count;
            while (-1 != (count = is.read(buffer))) {
                content.append(new String(buffer, 0, count));
            }
        } catch (IOException e) {
            System.out.print(e.getMessage());
            return;
        }
    }

    private class JettyCookiesHandle extends AbstractHandler {

        @Override
        public void handle(String terget, Request baseRequest, HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Set-Cookie", "test=test1; expires=Fri, 29-Jul-2011 23:59:59 GMT; path=/; domain=.aikuis.com; HttpOnly");
            baseRequest.setHandled(true);
            response.getWriter().print("jsonStr");
        }
    }
}
