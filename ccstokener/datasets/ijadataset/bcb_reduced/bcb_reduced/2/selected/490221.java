package com.widen.prima.util.test;

import java.net.*;
import java.io.*;

public class GetHTML {

    public static String urlString = "http://localhost:8080/birt-viewer/run?__report=report/income_statement.rptdesign&queryTime=20051031235959999";

    public static void main(String args[]) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            System.err.println(e.toString());
            System.exit(1);
        }
        try {
            InputStream ins = url.openStream();
            BufferedReader breader = new BufferedReader(new InputStreamReader(ins));
            String info = breader.readLine();
            while (info != null) {
                System.out.println(info);
                info = breader.readLine();
            }
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}
