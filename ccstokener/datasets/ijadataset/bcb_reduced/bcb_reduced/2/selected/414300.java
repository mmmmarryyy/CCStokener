package org.opencube.oms.test;

import java.io.*;
import java.net.*;
import java.security.*;

public class BasicBrowser {

    String urlString;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java Browser url");
            System.exit(1);
        }
        BasicBrowser browser = new BasicBrowser(args[0]);
        browser.run();
    }

    public BasicBrowser(String urlString) {
        this.urlString = urlString;
    }

    public void run() throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        System.out.println("THE HEADERS");
        System.out.println("-----------");
        for (int i = 1; ; ++i) {
            String key;
            String value;
            if ((key = urlc.getHeaderFieldKey(i)) == null) break;
            if ((value = urlc.getHeaderField(i)) == null) break;
            System.out.println("KEY: " + key);
            System.out.println("VALUE: " + value);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
        String line;
        System.out.println("THE CONTENT");
        System.out.println("-----------");
        while ((line = reader.readLine()) != null) System.out.println(line);
    }
}
