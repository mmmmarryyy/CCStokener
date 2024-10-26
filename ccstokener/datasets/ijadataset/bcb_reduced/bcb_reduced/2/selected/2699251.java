package org.fao.fenix.web.modules.core.server.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet implementation class for Servlet: PROXY
 * 
 * @web.servlet name="proxy"
 * 
 * @web.servlet-mapping url-pattern="/proxy"
 * 
 * @author kappu
 * 
 * It's a stupid proxy service, needed by ol to perform extrnal domain ajax
 * request
 */
public class Proxy extends HttpServlet implements Servlet {

    static ApplicationContext applicationContext;

    public void init() throws ServletException {
        super.init();
        System.out.println("===Proxy initializing=========================================================");
        ServletContext servletContext = this.getServletConfig().getServletContext();
        this.applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    protected void doTransfer(HttpServletRequest request, HttpServletResponse response, String method) throws ServletException, IOException {
        ServletContext servletContext = this.getServletConfig().getServletContext();
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        String szUrl = request.getParameter("url");
        System.out.println(szUrl);
        URL url;
        InputStream is = null;
        ServletOutputStream sout = null;
        try {
            url = new URL(szUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            Enumeration hNames = request.getHeaderNames();
            while (hNames.hasMoreElements()) {
                String txt = hNames.nextElement().toString();
                con.setRequestProperty(txt, request.getHeader(txt));
            }
            con.setRequestProperty("host", url.getHost());
            con.setRequestProperty("refer", szUrl);
            con.setRequestMethod(method);
            con.setDoOutput(true);
            con.setDoInput(true);
            InputStreamReader inBody = new InputStreamReader(request.getInputStream());
            char bufCh[] = new char[1024];
            int r;
            OutputStreamWriter outReq = new OutputStreamWriter(con.getOutputStream());
            while ((r = inBody.read(bufCh)) != -1) {
                System.out.println(bufCh);
                outReq.write(bufCh, 0, r);
            }
            outReq.flush();
            outReq.close();
            inBody.close();
            System.out.println(con.getResponseCode());
            System.out.println(con.getResponseMessage());
            if (con.getResponseCode() == con.HTTP_OK) {
                response.setContentType(con.getContentType());
                response.addHeader("Content-Encoding", con.getContentEncoding());
                sout = response.getOutputStream();
                is = con.getInputStream();
                byte buff[] = new byte[1024];
                while ((r = is.read(buff)) != -1) {
                    sout.write(buff, 0, r);
                    System.out.print(buff);
                }
                sout.flush();
                is.close();
                sout.close();
            } else {
                response.sendError(con.getResponseCode(), con.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET");
        doTransfer(request, response, "GET");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("POST");
        doTransfer(request, response, "POST");
    }

    public void run() {
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
