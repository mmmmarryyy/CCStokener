package org.rpcwc.jobs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CacheRefreshServlet extends HttpServlet {

    private static final String BASE_URL = "http://rpcwc.org/maintenance/RefreshCache.aspx";

    private static final long serialVersionUID = -1897245329933688005L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String cacheName = req.getParameter("cacheName");
        if (cacheName == null || cacheName.equals("")) {
            resp.getWriter().println("parameter cacheName required");
            return;
        } else {
            StringBuffer urlStr = new StringBuffer();
            urlStr.append(BASE_URL);
            urlStr.append("?");
            urlStr.append("cacheName=");
            urlStr.append("rpcwc.bo.cache.");
            urlStr.append(cacheName);
            URL url = new URL(urlStr.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            StringBuffer output = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                output.append(line);
                output.append(System.getProperty("line.separator"));
            }
            reader.close();
            resp.getWriter().println(output.toString());
        }
    }
}
