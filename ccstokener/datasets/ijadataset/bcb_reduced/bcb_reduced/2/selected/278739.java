package com.vmware.vcloud.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Steve Jin (sjin@vmware.com)
 */
public class RestClient {

    private String baseUrl = null;

    private String cookie = null;

    static {
        try {
            trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
        }
    }

    public RestClient(String serverUrl, String username, String password) {
        this.baseUrl = serverUrl;
    }

    private String getUrl(String urlStr) {
        return urlStr.startsWith("http") ? urlStr : baseUrl + urlStr;
    }

    public String get(String urlStr) throws MalformedURLException, IOException {
        HttpURLConnection getCon = (HttpURLConnection) new URL(getUrl(urlStr)).openConnection();
        if (cookie != null) {
            getCon.setRequestProperty("Cookie", cookie);
        }
        getCon.connect();
        return StreamUtil.readStream(getCon.getInputStream()).toString();
    }

    public String post(String urlStr) throws MalformedURLException, IOException {
        return post(urlStr, null);
    }

    public String post(String urlStr, String body) throws MalformedURLException, IOException {
        return send("POST", null, urlStr, body);
    }

    public String post(String urlStr, String body, String contentType) throws MalformedURLException, IOException {
        return send("POST", contentType, urlStr, body);
    }

    public String delete(String urlStr) throws MalformedURLException, IOException {
        return send("DELETE", null, urlStr, null);
    }

    public String put(String urlStr, String body) throws MalformedURLException, IOException {
        return send("PUT", null, urlStr, body);
    }

    public String put(String urlStr, String body, String contentType) throws MalformedURLException, IOException {
        return send("PUT", contentType, urlStr, body);
    }

    private String send(String method, String contentType, String urlStr, String body) throws MalformedURLException, IOException {
        HttpURLConnection postCon = (HttpURLConnection) new URL(getUrl(urlStr)).openConnection();
        postCon.setRequestMethod(method);
        postCon.setDoOutput(true);
        postCon.setDoInput(true);
        if (cookie != null) {
            postCon.setRequestProperty("Cookie", cookie);
            if (contentType != null) {
                postCon.setRequestProperty("Content-type", contentType);
            }
            postCon.setRequestProperty("Content-Length", body == null ? "0" : Integer.toString(body.length()));
        }
        if (body != null) {
            OutputStream os = postCon.getOutputStream();
            OutputStreamWriter out = new OutputStreamWriter(os);
            out.write(body);
            out.close();
        }
        InputStream is = null;
        try {
            is = postCon.getInputStream();
        } catch (IOException ioe) {
            is = postCon.getErrorStream();
        }
        int resCode = postCon.getResponseCode();
        if (resCode == 201 || resCode == 202) {
            String loc = postCon.getHeaderField("Location");
            System.out.println("loc:" + loc);
            return loc;
        }
        StringBuffer sb = StreamUtil.readStream(is);
        return sb.toString();
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    private static void trustAllHttpsCertificates() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[1];
        trustAllCerts[0] = new TrustAllManager();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private static class TrustAllManager implements X509TrustManager {

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }
    }
}
