package com.littleqworks.commons.web.http;

import java.net.*;
import java.io.*;

public class RequestSender {

    private String urlStr;

    private URL url;

    private HttpURLConnection httpURLConnection;

    private String responseContent;

    public String getUrlStr() {
        return urlStr;
    }

    public void setUrlStr(String urlStr) {
        this.urlStr = urlStr;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void sendRequest(String method) {
        try {
            url = new URL(urlStr);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.getOutputStream().flush();
            httpURLConnection.getOutputStream().close();
            System.out.println(httpURLConnection.getResponseMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }
}
