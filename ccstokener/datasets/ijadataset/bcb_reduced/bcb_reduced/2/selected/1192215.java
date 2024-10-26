package com.byjyate.rssdreamwork;

import java.io.*;
import java.net.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class PubSubHubBubPublishServlet extends HttpServlet {

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        URL url = new URL("http://pubsubhubbub.appspot.com");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("hub.mode=publish&hub.url=" + req.getParameter("url"));
        out.flush();
        out.close();
        conn.getResponseCode();
        try {
            resp.sendRedirect(req.getParameter("from"));
        } catch (Exception e) {
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }
}
