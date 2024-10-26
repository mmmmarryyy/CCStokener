package com.afaker.rss.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author new
 */
public class HttpConnector {

    private URL url;

    private InputStream input;

    private String lastModified;

    private int responseCode = -1;

    private long lastModify = 0;

    private long previousModify = 0;

    private String eTag;

    private String previousETag = null;

    private String previousLastModified = null;

    private boolean dead;

    /** Creates a new instance of HttpConnector */
    public HttpConnector(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    public HttpConnector(URL url) {
        this.url = url;
    }

    public boolean load() {
        HttpURLConnection connection = null;
        responseCode = -1;
        try {
            connection = connect();
        } catch (IOException e) {
            dead = true;
            return false;
        }
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e1) {
            dead = true;
            return false;
        }
        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            connection.disconnect();
            dead = false;
            return false;
        }
        InputStream inputStream = null;
        try {
            inputStream = getInputStream(connection);
        } catch (IOException e2) {
            dead = true;
            e2.printStackTrace();
            return false;
        }
        dead = false;
        return true;
    }

    public HttpURLConnection connect() throws IOException {
        if (url == null) {
            return null;
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (previousETag != null) {
            connection.addRequestProperty("If-None-Match", previousETag);
        }
        if (previousLastModified != null) {
            connection.addRequestProperty("If-Modified-Since", previousLastModified);
        }
        return connection;
    }

    public InputStream getInputStream(HttpURLConnection connection) throws IOException {
        lastModified = connection.getHeaderField("Last-Modified");
        previousLastModified = lastModified;
        lastModify = connection.getLastModified();
        eTag = connection.getHeaderField("ETag");
        if (connection == null) {
            return null;
        }
        input = connection.getInputStream();
        return input;
    }

    public String getETag() {
        return eTag;
    }

    public String getLastModified() {
        return lastModified;
    }

    public InputStream getInputStream() {
        return input;
    }

    public boolean isDead() {
        return dead;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getStatus() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("\n\nFeed:" + url);
        buffer.append("\nresponse code:" + getResponseCode());
        buffer.append("\nlast-modified:" + getLastModified());
        buffer.append("\nisDead:" + isDead());
        return buffer.toString();
    }
}
