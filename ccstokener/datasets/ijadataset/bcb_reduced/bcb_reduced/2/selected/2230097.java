package com.google.code.sagetvaddons.sre.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public final class OverrideProxy extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 8L;

    private static final String targetServer = "http://sre-maps.appspot.com";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp, false);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp, true);
    }

    @SuppressWarnings("unchecked")
    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, boolean isPost) throws ServletException, IOException {
        HttpClient httpclient = WebReader.getHttpClient();
        try {
            StringBuffer sb = new StringBuffer();
            sb.append(targetServer);
            sb.append(req.getRequestURI());
            if (req.getQueryString() != null) {
                sb.append("?" + req.getQueryString());
            }
            HttpRequestBase targetRequest = null;
            if (isPost) {
                HttpPost post = new HttpPost(sb.toString());
                Enumeration<String> paramNames = req.getParameterNames();
                String paramName = null;
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                while (paramNames.hasMoreElements()) {
                    paramName = paramNames.nextElement();
                    params.add(new BasicNameValuePair(paramName, req.getParameterValues(paramName)[0]));
                }
                post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                targetRequest = post;
            } else {
                System.out.println("GET");
                HttpGet get = new HttpGet(sb.toString());
                targetRequest = get;
            }
            HttpResponse targetResponse = httpclient.execute(targetRequest);
            HttpEntity entity = targetResponse.getEntity();
            InputStream input = entity.getContent();
            OutputStream output = resp.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
            String line = reader.readLine();
            while (line != null) {
                writer.write(line + "\n");
                line = reader.readLine();
            }
            reader.close();
            writer.close();
        } finally {
            WebReader.returnHttpClient(httpclient);
        }
    }
}
