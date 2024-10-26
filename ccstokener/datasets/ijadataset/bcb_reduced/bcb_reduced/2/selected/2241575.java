package org.phill84.twitsync.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {

    public static String connect(String url_str, String oauth_header, String data) throws IOException {
        URL url = new URL(url_str);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setAllowUserInteraction(true);
        if (oauth_header != null || data != null) {
            conn.setAllowUserInteraction(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            if (oauth_header != null) {
                conn.setRequestProperty("Authorization", oauth_header);
            }
            if (data != null) {
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(data);
                out.flush();
                out.close();
            }
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String out = "";
        String temp;
        while ((temp = reader.readLine()) != null) {
            out += temp;
        }
        return out;
    }

    public static String connect(String url_str, String oauth_header) throws IOException {
        return connect(url_str, oauth_header, null);
    }

    public static String connect(String url_str) throws IOException {
        return connect(url_str, null, null);
    }
}
