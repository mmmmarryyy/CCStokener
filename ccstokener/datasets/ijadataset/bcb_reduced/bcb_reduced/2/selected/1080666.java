package com.googlecode.yami;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class GetVersion {

    public static void initBars(String ver) {
        if (ver.equals(MainWindow.VERSION)) {
            MainWindow.STATUS_BAR.setText("Up to date!");
            try {
                java.lang.Thread.sleep(900);
                MainWindow.STATUS_BAR.setText("Version " + MainWindow.VERSION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (ver.equals("0")) {
            MainWindow.STATUS_BAR.setText("Cannot connect.");
            try {
                java.lang.Thread.sleep(1800);
                MainWindow.STATUS_BAR.setText("Version " + MainWindow.VERSION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            MainWindow.STATUS_BAR.setText("Latest available version is: " + ver + "! ");
            try {
                java.lang.Thread.sleep(1800);
                MainWindow.STATUS_BAR.setText("Version " + MainWindow.VERSION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProgramVersion() {
        String s = "0";
        try {
            URL url;
            URLConnection urlConn;
            DataInputStream dis;
            url = new URL("http://www.dombosfest.org.yu/log/yamiversion.dat");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            dis = new DataInputStream(urlConn.getInputStream());
            while ((dis.readUTF()) != null) {
                s = dis.readUTF();
            }
            dis.close();
        } catch (MalformedURLException mue) {
            System.out.println("mue:" + mue.getMessage());
        } catch (IOException ioe) {
            System.out.println("ioe:" + ioe.getMessage());
        }
        return s;
    }
}
