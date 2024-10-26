package org.light.portal.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * 
 * @author Jianmin Liu
 **/
public class HttpUtil {

    public static boolean ping(String urlString) {
        boolean ping = false;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            if (HttpURLConnection.HTTP_OK == conn.getResponseCode()) ping = true;
        } catch (Exception e) {
        }
        return ping;
    }

    public static String getStringResponse(String urlString) throws Exception {
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String inputLine;
        StringBuilder buffer = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            buffer.append(inputLine);
        }
        in.close();
        return buffer.toString();
    }
}
