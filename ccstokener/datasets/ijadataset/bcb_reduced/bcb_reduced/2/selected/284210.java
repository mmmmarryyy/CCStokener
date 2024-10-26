package com.googlecode.authproxy.servlets;

import org.mortbay.util.IO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import java.io.*;
import java.util.Properties;
import java.util.Enumeration;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import com.googlecode.authproxy.AuthProxySettings;

/**
 * Based on ProxyServlet.java (see license in file header)
 * @author Niels Bosma (niels.bosma@gmail.com)
 */
public class AuthProxyServlet extends HttpServlet {

    private ServletContext context;

    public void init(ServletConfig config) throws ServletException {
        this.context = config.getServletContext();
        AuthProxySettings.getInstance().updateProxy();
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String realUrl = "http:/" + request.getPathInfo();
        if (request.getQueryString() != null) {
            realUrl += "?" + request.getQueryString();
        }
        URL url = new URL(realUrl);
        URLConnection connection = url.openConnection();
        HttpURLConnection http = null;
        if (connection instanceof HttpURLConnection) {
            http = (HttpURLConnection) connection;
            http.setRequestMethod(request.getMethod());
        }
        boolean hasContent = false;
        Enumeration headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = (String) headers.nextElement();
            if ("content-type".equals(header.toLowerCase())) hasContent = true;
            Enumeration values = request.getHeaders(header);
            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                if (value != null) {
                    connection.addRequestProperty(header, value);
                }
            }
        }
        try {
            connection.setDoInput(true);
            if (hasContent) {
                InputStream proxyRequest = request.getInputStream();
                connection.setDoOutput(true);
                IO.copy(proxyRequest, connection.getOutputStream());
            }
            connection.connect();
        } catch (Exception e) {
            context.log("proxy", e);
        }
        InputStream proxyResponse = null;
        int code = 500;
        if (http != null) {
            proxyResponse = http.getErrorStream();
            code = http.getResponseCode();
            response.setStatus(code);
        }
        if (proxyResponse == null) {
            try {
                proxyResponse = connection.getInputStream();
            } catch (Exception e) {
                if (http != null) proxyResponse = http.getErrorStream();
                context.log("stream", e);
            }
        }
        int i = 0;
        String header = connection.getHeaderFieldKey(i);
        String value = connection.getHeaderField(i);
        while (header != null || value != null) {
            if (header != null && value != null) {
                response.addHeader(header, value);
            }
            ++i;
            header = connection.getHeaderFieldKey(i);
            value = connection.getHeaderField(i);
        }
        if (proxyResponse != null) {
            IO.copy(proxyResponse, response.getOutputStream());
        }
    }
}
