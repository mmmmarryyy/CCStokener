package com.saar.chichak;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

/**
 *
 * @author Ramin
 */
public class HTMLPost {

    public static int doPost(String urlString, String username, String password, Map<String, String> parameters) throws IOException {
        PrintWriter out = null;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            if (username != null && password != null) {
                String encoding = base64Encode(username + ':' + password);
                connection.setRequestProperty("Authorization", "Basic " + encoding);
            }
            connection.setDoOutput(true);
            out = new PrintWriter(connection.getOutputStream());
            boolean first = true;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    out.print('&');
                }
                out.print(entry.getKey());
                out.print('=');
                out.print(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            out.close();
            connection.connect();
            if (!(connection instanceof HttpURLConnection)) {
                throw new IOException();
            }
            return ((HttpURLConnection) connection).getResponseCode();
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static String base64Encode(String s) {
        String temp = new String();
        byte sBytes[] = s.getBytes();
        final String key = new String("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
        for (int i = 0; i < ((int) (sBytes.length / 3)) * 3; i += 3) {
            temp += key.charAt(sBytes[i] >> 2);
            temp += key.charAt(((sBytes[i] & 3) << 4) + (sBytes[i + 1] >> 4));
            temp += key.charAt(((sBytes[i + 1] & 0x0F) << 2) + (sBytes[i + 2] >> 6));
            temp += key.charAt(sBytes[i + 2] & 0x3F);
        }
        if (sBytes.length % 3 == 1) {
            temp += key.charAt(sBytes[sBytes.length - 1] >> 2);
            temp += key.charAt((sBytes[sBytes.length - 1] & 3) << 4);
            temp += "==";
        } else if (sBytes.length % 3 == 2) {
            temp += key.charAt(sBytes[sBytes.length - 2] >> 2);
            temp += key.charAt(((sBytes[sBytes.length - 2] & 3) << 4) + (sBytes[sBytes.length - 1] >> 4));
            temp += key.charAt((sBytes[sBytes.length - 1] & 0x0F) << 2);
            temp += "=";
        }
        return temp;
    }
}
