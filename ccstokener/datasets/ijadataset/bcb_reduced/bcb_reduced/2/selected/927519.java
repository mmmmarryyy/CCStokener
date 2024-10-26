package org.eiichiro.jazzmaster.examples.petstore.ui.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Minimum set of HTTPclient supporting both http and https.
 * It's aslo capable of POST, but it doesn't provide doGet because
 * the caller can just read the inputstream.
 */
public class HttpClient {

    private String proxyHost = null;

    private String proxyPort = null;

    private boolean isHttps = false;

    private boolean isProxy = false;

    private URLConnection urlConnection = null;

    /**
     * @param url URL string
     */
    public HttpClient(String url) {
        this.urlConnection = getURLConnection(url);
    }

    /**
     * @param phost PROXY host name
     * @param pport PROXY port string
     * @param url URL string
     */
    public HttpClient(String phost, String pport, String url) {
        if (phost != null && pport != null) {
            this.isProxy = true;
        }
        this.proxyHost = phost;
        this.proxyPort = pport;
        if (url.indexOf("https") >= 0) {
            isHttps = true;
        }
        this.urlConnection = getURLConnection(url);
    }

    /**
     * private method to get the URLConnection
     * @param str URL string
     */
    private URLConnection getURLConnection(String str) {
        try {
            if (isHttps) {
                System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
                if (isProxy) {
                    System.setProperty("https.proxyHost", proxyHost);
                    System.setProperty("https.proxyPort", proxyPort);
                }
            } else {
                if (isProxy) {
                    System.setProperty("http.proxyHost", proxyHost);
                    System.setProperty("http.proxyPort", proxyPort);
                }
            }
            URL url = new URL(str);
            return (url.openConnection());
        } catch (MalformedURLException me) {
            System.out.println("Malformed URL");
            me.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * returns the inputstream from URLConnection
     * @return InputStream
     */
    public InputStream getInputStream() {
        try {
            return (this.urlConnection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * return the OutputStream from URLConnection
     * @return OutputStream
     */
    public OutputStream getOutputStream() {
        try {
            return (this.urlConnection.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * posts data to the inputstream and returns the InputStream.
     * @param postData data to be posted. must be url-encoded already.
     * @return InputStream input stream from URLConnection
     */
    public InputStream doPost(String postData) {
        this.urlConnection.setDoOutput(true);
        OutputStream os = this.getOutputStream();
        PrintStream ps = new PrintStream(os);
        ps.print(postData);
        ps.close();
        return (this.getInputStream());
    }

    public String getContentEncoding() {
        return (this.urlConnection.getContentEncoding());
    }

    public int getContentLength() {
        return (this.urlConnection.getContentLength());
    }

    public String getContentType() {
        return (this.urlConnection.getContentType());
    }

    public long getDate() {
        return (this.urlConnection.getDate());
    }

    public String getHeader(String name) {
        return (this.urlConnection.getHeaderField(name));
    }

    public long getIfModifiedSince() {
        return (this.urlConnection.getIfModifiedSince());
    }
}
