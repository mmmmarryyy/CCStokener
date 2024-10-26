package com.google.code.restui.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rohit
 */
public class HttpHandler {

    private String urlLocation;

    private HttpURLConnection con;

    private String method;

    private String contentType;

    private String acceptType;

    private String sendData;

    private String recData;

    private Authenticator authenticator;

    public HttpHandler() {
    }

    public HttpHandler(HttpURLConnection con) {
        this.con = con;
    }

    public HttpHandler(String urlLocation, String method, String contentType, String acceptType, String data) {
        this.urlLocation = urlLocation;
        this.method = method;
        this.contentType = contentType;
        this.acceptType = acceptType;
        this.sendData = data;
        this.authenticator = null;
    }

    private HttpResponse connect() throws IOException {
        URL url = new URL(this.urlLocation);
        con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setDefaultUseCaches(false);
        con.setAllowUserInteraction(true);
        if (authenticator != null) {
            authenticator.authenticate(con);
        }
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String date = format.format(new Date());
        con.setRequestProperty("Date", date);
        con.setRequestMethod(method);
        if (this.contentType != null) {
            con.setRequestProperty("content-type", contentType);
        }
        if (this.acceptType != null) {
            con.setRequestProperty("accept", acceptType);
        }
        con.connect();
        if (con.getResponseCode() == 200) {
            return null;
        } else {
            if (con.getResponseCode() == 401) {
                String authType = con.getHeaderField("WWW-Authenticate");
                if (authType.contains("Basic")) {
                    return new HttpResponse(true, "Basic", "Test");
                }
            }
            return null;
        }
    }

    public HttpResponse processRequest() {
        try {
            HttpResponse hr = connect();
            if (hr != null && hr.isRequireLogin()) {
                return hr;
            }
            if (method.equals("PUT") || method.equals("POST")) {
                if (sendData != null) {
                    InputStream br = new BufferedInputStream(new ByteArrayInputStream(sendData.getBytes()));
                    con.setDoOutput(true);
                    OutputStream os = con.getOutputStream();
                    int count;
                    byte[] buffer = null;
                    while ((count = br.read(buffer)) != -1) {
                        os.write(buffer, 0, count);
                    }
                    os.flush();
                    br.close();
                }
            }
            int responseCode = con.getResponseCode();
            String responseMsg = con.getResponseMessage();
            String responseContentType = con.getContentType();
            String responseContentEncoding = con.getContentEncoding();
            long responseLastModified = con.getLastModified();
            Map<String, List<String>> responseHeaders = con.getHeaderFields();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            char[] inputBuf = new char[1024];
            StringBuffer sb = new StringBuffer();
            while (in.read(inputBuf) != -1) {
                sb.append(inputBuf);
            }
            recData = sb.toString();
            in.close();
            return new HttpResponse(responseCode, responseMsg, responseContentType, responseContentEncoding, responseLastModified, responseHeaders, recData);
        } catch (IOException ex) {
            String errMsg = "Cannot connect to :" + con.getURL();
            Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, errMsg, ex);
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                StringBuffer buf = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    buf.append(line);
                    System.out.print(line);
                }
                errMsg = buf.toString();
                return new HttpResponse(true, errMsg);
            } catch (Exception ex1) {
                Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Error getting the error msg", ex1);
                return new HttpResponse(true, errMsg);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }
}
