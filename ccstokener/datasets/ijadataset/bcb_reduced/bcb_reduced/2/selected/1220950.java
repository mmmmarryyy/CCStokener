package org.vesuf.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *  This servlet acts as a proxy.
 *  Can be used by applets to request urls from other servers.
 *
 *  @property proxyurl	This request parameter specifies the url to retrieve.
 */
public class ProxyServlet extends HttpServlet {

    /**
	 *  Retrieve an url.
	 *  @param request	The http request.
	 *  @param respone	The http response.
	 *  @throws ServletException, IOException.
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getParameter("proxyurl");
        URLConnection conn = new URL(url).openConnection();
        Reader in = new InputStreamReader(conn.getInputStream(), response.getCharacterEncoding());
        response.setContentType(conn.getContentType());
        response.setContentLength(conn.getContentLength());
        Writer out = response.getWriter();
        char[] buf = new char[256];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        String log = request.getParameter("logging");
        if (log != null && log.toLowerCase().equals("true")) logRequest(request);
    }

    /**
	 *  Retrieve an url.
	 *  @param request	The http request.
	 *  @param respone	The http response.
	 *  @throws ServletException, IOException.
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getParameter("proxyurl");
        URLConnection conn = new URL(url).openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        Enumeration params = request.getParameterNames();
        boolean first = true;
        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();
            if (!param.equals("proxyurl")) {
                if (first) {
                    first = false;
                } else {
                    dos.writeBytes("&");
                }
                dos.writeBytes(URLEncoder.encode(param));
                dos.writeBytes("=");
                dos.writeBytes(URLEncoder.encode(request.getParameter(param)));
            }
        }
        dos.close();
        Reader in = new InputStreamReader(conn.getInputStream(), response.getCharacterEncoding());
        response.setContentType(conn.getContentType());
        response.setContentLength(conn.getContentLength());
        Writer out = response.getWriter();
        char[] buf = new char[256];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        String log = request.getParameter("logging");
        if (log != null && log.toLowerCase().equals("true")) logRequest(request);
    }

    /** 
	 *  Synchronized method for securing stream access.
	 *  @param symbol	The requested symbol.
	 */
    protected synchronized void logRequest(HttpServletRequest request) throws IOException {
        String url = request.getParameter("proxyurl");
        FileInputStream is = null;
        try {
            is = new FileInputStream("c:\\temp\\proxy_log.txt");
        } catch (Exception e) {
        }
        Properties props = new Properties();
        if (is != null) {
            props.load(is);
            is.close();
        }
        int cnt = 1;
        String str = props.getProperty(url);
        if (str != null) cnt = Integer.parseInt(str) + 1;
        props.put(url, new Integer(cnt).toString());
        FileOutputStream os = new FileOutputStream("c:\\temp\\proxy_log.txt");
        props.save(os, "Proxy redirected urls.");
        os.close();
    }
}
