package com.googlecode.junit.ext.checkers;

import static java.lang.Integer.parseInt;
import java.net.URL;
import java.net.URLConnection;

public class URLIsReachable implements Checker {

    private String urlString;

    private int milli = 10 * 1000;

    public URLIsReachable(String url) {
        this.urlString = url;
    }

    public URLIsReachable(String[] args) {
        this.urlString = args[0];
        this.milli = parseInt(args[1]);
    }

    public boolean satisfy() {
        try {
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(milli);
            urlConnection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
