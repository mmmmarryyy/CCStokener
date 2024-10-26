package it.rm.bracco.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebPageFetcher {

    public static String fetch(HttpURLConnection urlConnection) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line;
        StringBuffer webPage = new StringBuffer();
        while ((line = bufferedReader.readLine()) != null) {
            webPage.append(line);
        }
        bufferedReader.close();
        return webPage.toString();
    }

    public static String fetch(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
        String result = fetch(urlConnection);
        urlConnection.disconnect();
        return result;
    }
}
