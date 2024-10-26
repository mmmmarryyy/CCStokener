package org.happycomp.radio.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Umoznuje se dotazovat na fedoru, ktera potrebuje autentizaci
 * @author pavels
 */
public class RESTHelper {

    public static InputStream inputStream(String urlString) throws IOException {
        URLConnection uc = openConnection(urlString);
        HttpURLConnection httpUrlConnection = (HttpURLConnection) uc;
        if (httpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return uc.getInputStream();
        } else {
            throw new IOException("bad status code :" + httpUrlConnection.getResponseCode());
        }
    }

    public static InputStream inputStream(URL url) throws IOException {
        URLConnection uc = openConnection(url);
        return uc.getInputStream();
    }

    public static URLConnection openConnection(String urlString) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        return openConnection(url);
    }

    public static URLConnection openConnection(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        return uc;
    }
}
