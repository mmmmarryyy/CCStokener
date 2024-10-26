package org.mediavirus.graphl.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Flo Ledermann <ledermann@ims.tuwien.ac.at>
 *
 */
public class ProxyServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getParameter("url");
        URL url = null;
        try {
            url = new URL(path);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            response.setContentType(conn.getContentType());
            PrintWriter w = response.getWriter();
            String line;
            while ((line = in.readLine()) != null) {
                w.println(line);
            }
            w.close();
        } catch (Exception ex) {
            PrintWriter w = response.getWriter();
            w.println("<html><head><link href=\"graphl.css\" rel=\"stylesheet\"></head>");
            w.println("<body><h2>Error</h2>");
            w.println("Error opening " + path + ": " + ex.getMessage());
            w.println("return to <a href=\"http://www.mediavirus.org/graphl\">main page</a>");
            w.println("</body>");
            w.close();
        }
    }
}
