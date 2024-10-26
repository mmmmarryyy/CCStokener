package com.byjyate.rssdreamwork;

import java.io.*;
import java.net.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class PubSubHubBubSubscribeServlet extends HttpServlet {

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        URL url = new URL("http://pubsubhubbub.appspot.com/subscribe");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write("hub.mode=subscribe&hub.verify=sync&hub.topic=" + req.getParameter("url") + "&hub.callback=" + req.getRequestURL().toString().replace("tasks/subscribe", "callback"));
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
