package com.mlib.unittest.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

public class URLRequest {

    private String url = null;

    private String cookie;

    private String text;

    public void dorequest(Map<String, String> ps, String method) throws IOException {
        StringBuffer httpResponse = new StringBuffer();
        URL ourl = new URL(url);
        HttpURLConnection httpConnection = (HttpURLConnection) ourl.openConnection();
        httpConnection.setRequestMethod(method);
        httpConnection.setDoOutput(true);
        this.setCookie(httpConnection);
        OutputStream httpOutputStream = httpConnection.getOutputStream();
        StringBuffer postParams = new StringBuffer("");
        for (Entry<String, String> entry : ps.entrySet()) {
            postParams.append(entry.getKey());
            postParams.append("=");
            postParams.append(entry.getValue());
            postParams.append("&");
        }
        httpOutputStream.write(postParams.toString().getBytes());
        BufferedReader httpBufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        httpResponse.append(this.readBufferedContent(httpBufferedReader));
        text = httpResponse.toString();
        this.readCookie(httpConnection);
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    private String readBufferedContent(BufferedReader bufferedReader) {
        if (bufferedReader == null) return null;
        StringBuffer result = new StringBuffer();
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    public String getText() {
        return text;
    }

    private void setCookie(HttpURLConnection httpConnection) {
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
    }

    private void readCookie(HttpURLConnection httpConnection) {
        if (cookie == null || cookie.length() == 0) {
            String setCookie = httpConnection.getHeaderField("Set-Cookie");
            if (setCookie != null) cookie = setCookie.substring(0, setCookie.indexOf(";"));
        }
    }

    public String getCookie() {
        return this.cookie;
    }
}
