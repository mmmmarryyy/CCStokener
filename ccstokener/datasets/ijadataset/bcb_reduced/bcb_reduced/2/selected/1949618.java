package de.jrummler.xtm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import de.jrummler.xtm.helpclasses.InitDatabase;
import java.net.*;

/**
 * Servlet implementation class for Servlet: AutoComplete
 * 
 * @author - Jens Rummler
 * @version $Rev: 4 $ - $Date: 2009-02-24 07:41:39 -0500 (Tue, 24 Feb 2009) $
 */
public class AutoComplete extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static final long serialVersionUID = 1L;

    public AutoComplete() {
        super();
    }

    private Connection conn = null;

    private HttpSession session;

    public void destroy() {
        conn = InitDatabase.stopConnection(conn);
        super.destroy();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        session = request.getSession(true);
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String searchTerm = new String();
            if (request.getParameter("searchdb") != null) {
                searchTerm = request.getParameter("searchdb");
                out.write("<ul>");
                PreparedStatement sqlGetLikeBaseString = conn.prepareStatement("SELECT * FROM ENTRIES WHERE XTM_SESSION_ID = ? AND XTM_TEXT LIKE ?");
                sqlGetLikeBaseString.setString(1, session.getId());
                sqlGetLikeBaseString.setString(2, new String("%" + searchTerm + "%"));
                ResultSet res = sqlGetLikeBaseString.executeQuery();
                while (res.next()) {
                    out.write("<li>");
                    out.write(res.getString("XTM_TEXT"));
                    out.write("</li>");
                }
                out.write("</ul>");
                res.close();
            }
            if (request.getParameter("searchwiki") != null) {
                searchTerm = request.getParameter("searchwiki");
                out.write("<ul>");
                try {
                    searchTerm = URLEncoder.encode(searchTerm, "UTF-8");
                    URL url = new URL("http://www.wikipedia.de/suggest.php?lang=de&search=" + searchTerm);
                    URLConnection con = url.openConnection();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        out.write("<li>");
                        String[] split = line.split("\t");
                        out.write(split[0]);
                        out.write("</li>");
                    }
                    rd.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                out.write("</ul>");
            } else {
                return;
            }
        } catch (SQLException e) {
            out.println("Caught SQLException:" + e.getMessage());
        }
        ;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public void init() throws ServletException {
        conn = InitDatabase.startConnection();
        super.init();
    }
}
