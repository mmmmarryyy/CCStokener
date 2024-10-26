import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This simply loads the file at a location. It can be used with
 * an applet to load a file given by an URL into the applet.
 * It is used with my LatDraw applet.
 *
 * parameter:
 *
 * location             the URL
 *
 * @author      Ralph Freese
 */
public class LoadURL extends HttpServlet {

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        ServletOutputStream out = res.getOutputStream();
        InputStream in = null;
        try {
            URL url = new URL(req.getParameter("location"));
            in = url.openStream();
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = in.read(buffer)) != -1) out.write(buffer, 0, bytes_read);
        } catch (MalformedURLException e) {
            System.err.println(e.toString());
        } finally {
            try {
                in.close();
                out.close();
            } catch (Exception e) {
            }
        }
    }
}
