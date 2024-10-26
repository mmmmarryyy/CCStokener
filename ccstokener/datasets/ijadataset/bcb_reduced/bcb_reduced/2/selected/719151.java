package org.wizard4j.test;

import javax.xml.parsers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import java.net.*;
import java.io.*;

public class W3CValidator {

    private static Logger logger = LoggerFactory.getLogger(W3CValidator.class);

    public W3CValidator() {
    }

    public static String validateUrl(String uri) {
        return null;
    }

    public static Document validateContentViaGet(String content) {
        String url = null;
        try {
            url = "http://validator.w3.org/check?fragment=" + java.net.URLEncoder.encode(content, "UTF-8") + ";output=xml";
            Document document = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(url);
            Thread.sleep(1000);
            return document;
        } catch (Exception e) {
            logger.error("Error parsing URL " + url + ": ", e);
            return null;
        }
    }

    public static Document validateContentViaPost(String content) {
        String url = "http://validator.w3.org/check";
        try {
            String boundary = "AaB03x";
            String eol = "\r\n";
            HttpURLConnection httpC = (HttpURLConnection) new URL(url).openConnection();
            httpC.setDoOutput(true);
            httpC.setDoInput(true);
            httpC.setUseCaches(false);
            httpC.setRequestMethod("POST");
            httpC.setRequestProperty("Connection", "Keep-Alive");
            httpC.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            httpC.setRequestProperty("Content-Length", "xxx");
            DataOutputStream out = new DataOutputStream(httpC.getOutputStream());
            out.writeBytes("--");
            out.writeBytes(boundary);
            out.writeBytes(eol);
            out.writeBytes("Content-Disposition: form-data; name=\"output\"");
            out.writeBytes(eol);
            out.writeBytes("Content-Type: text/html; charset=UTF-8");
            out.writeBytes(eol);
            out.writeBytes(eol);
            out.writeBytes("xml");
            out.writeBytes(eol);
            out.writeBytes("--");
            out.writeBytes(boundary);
            out.writeBytes(eol);
            out.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"test.html\"");
            out.writeBytes(eol);
            out.writeBytes("Content-Type: text/html; charset=UTF-8");
            out.writeBytes(eol);
            out.writeBytes(eol);
            out.writeBytes(content + "\n");
            out.writeBytes(eol + "--" + boundary + "--" + eol);
            out.flush();
            out.close();
            Document document = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(httpC.getInputStream());
            Thread.sleep(1000);
            return document;
        } catch (Exception e) {
            logger.error("Error with POST validation", e);
            return null;
        }
    }
}
