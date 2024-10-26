package spidr.services;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import wdc.settings.*;

/**
 * This class exists to ensure that any REST messaging queues that need to ensure they exist
 * have an outlet to issue the add command
 */
public class QueueServletInitializer extends HttpServlet {

    private static final long serialVersionUID = -8974253368370003286L;

    private String[] _initQueues = new String[] { "add persistent drap", "add persistent ustec" };

    public void init() throws ServletException {
        String uri = Settings.get("locations.modelingQueue");
        String agent = "Mozilla/4.0";
        String type = "text/plain; charset=UTF-8";
        try {
            URL url = new URL(uri);
            for (int i = 0; i < _initQueues.length; i++) {
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setDoInput(true);
                urlConn.setUseCaches(false);
                urlConn.setAllowUserInteraction(false);
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("User-Agent", agent);
                urlConn.setRequestProperty("Content-Type", type);
                OutputStreamWriter urlOut = new OutputStreamWriter(urlConn.getOutputStream(), "UTF-8");
                urlOut.write(_initQueues[i]);
                urlOut.flush();
                urlOut.close();
            }
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }
}
