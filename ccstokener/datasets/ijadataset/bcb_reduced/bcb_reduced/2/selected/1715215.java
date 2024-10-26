package com.prem.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class UrlRequester {

    public static void request() {
        try {
            URL url = new URL("http://www.nseindia.com/marketinfo/companyinfo/companysearch.jsp?cons=ghcl&section=7");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String argc[]) {
        UrlRequester.request();
    }
}
