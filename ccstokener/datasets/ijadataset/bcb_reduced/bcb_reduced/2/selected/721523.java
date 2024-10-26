package com.untilov.gb.se.http;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import com.untilov.gb.http.DataPumpIF;

public class DataPumpSEImpl implements DataPumpIF {

    private Vector cookies;

    private String url;

    public DataPumpSEImpl(Vector cookies, String url) {
        this.cookies = cookies;
        this.url = url;
    }

    public InputStream getData() throws Exception {
        URL url = new URL(this.url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        prepareConnection(connection);
        addCookies(connection);
        System.out.println("Encoding: " + connection.getContentEncoding());
        return connection.getInputStream();
    }

    private void addCookies(HttpURLConnection connection) {
        int cookiesSize = cookies.size();
        for (int i = 0; i < cookiesSize; i++) {
            connection.addRequestProperty("Cookie", this.cookies.get(i).toString());
        }
    }

    private void prepareConnection(HttpURLConnection connection) throws Exception {
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setAllowUserInteraction(false);
    }
}
