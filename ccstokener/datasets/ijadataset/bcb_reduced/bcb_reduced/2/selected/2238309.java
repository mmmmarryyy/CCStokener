package com.techfort.tfal.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class WebUtil {

    public WebUtil() {
    }

    public static String fetch(String str_url) throws IOException {
        URL url;
        URLConnection connection;
        String jsonText = "";
        url = new URL(str_url);
        connection = url.openConnection();
        InputStream is = connection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null) {
            jsonText += line;
        }
        return jsonText;
    }
}
