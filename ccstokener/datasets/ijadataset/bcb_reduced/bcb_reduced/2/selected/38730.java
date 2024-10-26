package org.cakethursday.modules.javautils;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLUtils {

    public static String getURLData(String stringUrl, boolean secure) throws Exception {
        URL url = new URL(stringUrl);
        HttpURLConnection httpURLConnection;
        if (secure) {
            httpURLConnection = (HttpsURLConnection) url.openConnection();
        } else {
            httpURLConnection = (HttpURLConnection) url.openConnection();
        }
        return getDataFromURL(httpURLConnection);
    }

    private static String getDataFromURL(URLConnection urlConnection) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String currentLine;
        StringBuilder builder = new StringBuilder();
        while ((currentLine = br.readLine()) != null) {
            builder.append(currentLine);
        }
        return builder.toString();
    }
}
