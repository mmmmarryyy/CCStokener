package spidr.export;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.log4j.*;
import spidr.datamodel.*;

/**
 * Proxy embedded content such as images for portal sessions.
 * When portal is running over ssl, HttpProxyServlet can be used
 * to deliver insecure content such as images over ssl to avoid
 * mixed content in the browser window.
 */
public class HttpProxyServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(HttpProxyServlet.class);

    /**
   * Returns content retreived from location following context (Path Info)
   * If no content found returns 404
   */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("HttpProxyServlet: no session");
            response.setStatus(404);
            return;
        }
        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("HttpProxyServlet: user not logged in");
            response.setStatus(404);
            return;
        }
        String target = null;
        if (request.getPathInfo() != null && !request.getPathInfo().equals("")) {
            target = "http:/" + request.getPathInfo() + "?" + request.getQueryString();
            log.info("HttpProxyServlet: target=" + target);
        } else {
            log.warn("HttpProxyServlet: missing pathInfo");
            response.setStatus(404);
            return;
        }
        InputStream is = null;
        ServletOutputStream out = null;
        try {
            URL url = new URL(target);
            URLConnection uc = url.openConnection();
            response.setContentType(uc.getContentType());
            is = uc.getInputStream();
            out = response.getOutputStream();
            byte[] buf = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }
        } catch (MalformedURLException e) {
            log.warn("HttpProxyServlet: malformed URL");
            response.setStatus(404);
        } catch (IOException e) {
            log.warn("HttpProxyServlet: I/O exception");
            response.setStatus(404);
        } finally {
            if (is != null) {
                is.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
