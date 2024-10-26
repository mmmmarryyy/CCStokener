package com.jadcon.demos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlFetcher {

    public static void main(String args[]) {
        if (args.length < 1) {
            printUsage();
        }
        URL url;
        BufferedReader in = null;
        try {
            url = new URL(args[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
            } else {
                System.out.println("Response code " + responseCode + " means there was an error reading url " + args[0]);
            }
        } catch (IOException e) {
            System.err.println("IOException attempting to read url " + args[0]);
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: UrlFetcher {url}");
        System.exit(1);
    }
}
