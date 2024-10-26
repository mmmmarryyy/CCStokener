package com.googlecode.jwsm;

import java.io.*;
import java.net.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jcommon.util.*;

/**
 * Servlet implementation class for Servlet: GenerateJUnit
 *
 * @web.servlet
 *   name="GenerateJUnit"
 *   display-name="GenerateJUnit" 
 *
 * @web.servlet-mapping
 *   url-pattern="/GenerateJUnit"
 *  
 */
public class GenerateJUnit extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getRequestURL().toString();
        url = url.replaceAll("GenerateJUnit", "ServiceRequest");
        if (request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }
        String service = request.getRequestURL().toString();
        service = service.substring(service.indexOf("GenerateJUnit/") + "GenerateJUnit/".length());
        String method = null;
        if (service.indexOf('/') != -1) {
            String[] split = service.split("/");
            service = split[0];
            method = split[1];
        }
        String serviceProper = Character.toUpperCase(service.charAt(0)) + service.substring(1);
        String methodProper = "Default";
        if (method != null) {
            methodProper = Character.toUpperCase(method.charAt(0)) + method.substring(1);
        }
        String postData = "null";
        if (request.getMethod().equalsIgnoreCase("POST")) {
            postData = "\"" + getInputStreamAsString(request.getInputStream()) + "\"";
        }
        String contentType = "null";
        if (request.getContentType() != null) {
            contentType = "\"" + request.getContentType() + "\"";
        }
        response.setContentType("text/plain");
        String template = getInputStreamAsString(GenerateJUnit.class.getClassLoader().getResourceAsStream("resource/WebServiceTestCase.template"));
        template = StringUtilities.replaceAll(template, "%%URL%%", url);
        template = StringUtilities.replaceAll(template, "%%SERVICE_NAME%%", serviceProper);
        template = StringUtilities.replaceAll(template, "%%METHOD_NAME%%", methodProper);
        template = StringUtilities.replaceAll(template, "%%POST_DATA%%", postData);
        template = StringUtilities.replaceAll(template, "%%CONTENT_TYPE%%", contentType);
        template = StringUtilities.replaceAll(template, "%%RESULT%%", getResult(url, postData));
        PrintWriter writer = response.getWriter();
        writer.print(template);
        writer.flush();
        writer.close();
    }

    private static final String getResult(String url, String postData) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        if (!postData.equals("null")) {
            postData = postData.substring(1, postData.length() - 1);
            connection.setDoOutput(true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write(postData);
            writer.flush();
        }
        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
        int i;
        StringBuffer buffer = new StringBuffer();
        while ((i = reader.read()) != -1) {
            buffer.append((char) i);
        }
        reader.close();
        String response = buffer.toString().trim();
        response = StringUtilities.replaceAll(response, "\r\n", "\\r\\n");
        response = StringUtilities.replaceAll(response, "\"", "\\\"");
        return "\"" + response + "\"";
    }

    private static final String getInputStreamAsString(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(stream);
        int i;
        StringBuffer buffer = new StringBuffer();
        while ((i = reader.read()) != -1) {
            buffer.append((char) i);
        }
        reader.close();
        return buffer.toString();
    }
}
