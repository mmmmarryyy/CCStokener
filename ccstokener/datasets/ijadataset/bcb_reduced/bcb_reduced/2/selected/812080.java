package vobs.webapp;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import wdc.settings.*;
import java.util.*;

public class HttpProxyServlet extends HttpServlet {

    private static Vector allowedUrls = new Vector();

    public void init() {
        int serverCount = 0;
        while (null != Settings.get("proxy.serverurl" + serverCount)) {
            try {
                allowedUrls.addElement(new URL("http:/" + Settings.get("proxy.serverurl" + serverCount++)));
            } catch (MalformedURLException ex) {
                System.err.println("Error in url: " + "http:/" + Settings.get("proxy.serverurl" + (serverCount - 1)));
            }
        }
    }

    /**
   * Returns content retreived from location following context (Path Info)
   * If no content found returns 404
   */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String target = null;
        boolean allowedToAccess = false;
        try {
            URL requestUrl = new URL("http:/" + request.getPathInfo());
            for (Enumeration en = allowedUrls.elements(); en.hasMoreElements(); ) {
                URL nextUrl = (URL) en.nextElement();
                if ((nextUrl).getHost().equalsIgnoreCase(requestUrl.getHost())) {
                    allowedToAccess = true;
                }
            }
        } catch (MalformedURLException ex) {
            System.err.println("Error in url: " + "http:/" + request.getPathInfo());
            return;
        }
        if (!allowedToAccess) {
            response.setStatus(407);
            return;
        }
        if (request.getPathInfo() != null && !request.getPathInfo().equals("")) {
            target = "http:/" + request.getPathInfo() + "?" + request.getQueryString();
        } else {
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
            response.setStatus(404);
        } catch (IOException e) {
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
