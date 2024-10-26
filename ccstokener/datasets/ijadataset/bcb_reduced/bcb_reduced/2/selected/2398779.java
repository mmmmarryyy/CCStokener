package asa.util.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Map.Entry;

public class Communicator {

    private Communicator() {
        ;
    }

    public static boolean updateUserPoints(int userid, int coins) {
        return true;
    }

    public static String sendGetData(URL url, Hashtable<String, String> data) throws IOException {
        StringBuilder outStringBuilder = new StringBuilder();
        if (data != null) {
            for (Entry<String, String> entry : data.entrySet()) {
                outStringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                outStringBuilder.append("=");
                outStringBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                outStringBuilder.append("&");
            }
        }
        URL innerURL = new URL(url.toString() + "?" + outStringBuilder.toString());
        System.out.println("URL: " + innerURL);
        URLConnection urlConnection = innerURL.openConnection();
        urlConnection.connect();
        StringBuilder inStringBuilder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        Scanner inputScanner = new Scanner(urlConnection.getInputStream());
        while (inputScanner.hasNext()) {
            inStringBuilder.append(inputScanner.next() + " ");
        }
        inputScanner.close();
        reader.close();
        return inStringBuilder.toString();
    }
}
