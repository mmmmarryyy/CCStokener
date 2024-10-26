package org.hourglassstudios.tempuspre.library.net;

import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnectionFactory {

    public static HttpURLConnection createConnection(URL url) {
        try {
            HttpURLConnection conn = null;
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(7000);
            return conn;
        } catch (Exception e) {
            return null;
        }
    }
}
