package org.fao.fenix.web.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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
public class Proxy extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    static ApplicationContext applicationContext;

    public void init() throws ServletException {
        super.init();
        System.out.println("===Proxy initializing=========================================================");
        ServletContext servletContext = this.getServletConfig().getServletContext();
        this.applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println(FenixWebConfig.getFenixDir());
        ServletContext servletContext = this.getServletConfig().getServletContext();
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        System.out.println(request.getQueryString());
        String szUrl = request.getParameter("url");
        System.out.println(szUrl);
        URL url;
        File domains = new File("");
        try {
            url = new URL(szUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.getContent();
            ServletOutputStream sout = response.getOutputStream();
            InputStream is = con.getInputStream();
            byte buff[] = new byte[1024];
            int r;
            while ((r = is.read(buff)) != -1) {
                sout.write(buff, 0, r);
            }
            sout.flush();
            is.close();
            sout.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
