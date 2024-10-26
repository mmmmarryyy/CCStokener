package com.iv.flash;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 */
public final class SiteTest {

    public static void help() {
        System.err.println("Site test v1.0");
        System.err.println("Copyright (c) Dmitry Skavish, 2000. All rights reserved.");
        System.err.println("");
        System.err.println("Usage: sitetest [options] <url>");
        System.err.println("");
        System.err.println("Options:");
        System.err.println("    -help                  displays usage text");
        System.err.println("    -users <num>           number of users (20 default)");
        System.err.println("    -verbose               verbose output");
        System.err.println("");
        System.exit(1);
    }

    public static void err(String msg) {
        System.err.println(msg);
        help();
    }

    public static void main(String[] args) throws MalformedURLException {
        String url = null;
        int users = 50;
        boolean verbose = false;
        int l = args.length - 1;
        for (int i = 0; i <= l; i++) {
            if (args[i].equals("-help")) {
                help();
            } else if (args[i].equals("-users")) {
                if (i + 1 > l) err("Number of users is not specified");
                users = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-verbose")) {
                verbose = true;
            } else {
                url = args[i];
                if (i != l) err("Too many parameters");
            }
        }
        if (url == null) err("Url is not specified");
        start = System.currentTimeMillis();
        final boolean myVerbose = verbose;
        for (int i = 0; i < users; i++) {
            Thread st = new User(url);
            st.start();
        }
        new Thread() {

            public void run() {
                try {
                    for (; ; ) {
                        Thread.sleep(5000);
                        int size;
                        long time;
                        int num;
                        synchronized (SiteTest.class) {
                            size = totalSize;
                            time = totalTime;
                            num = number;
                        }
                        System.err.println("----------------------------------------------------------------------");
                        System.err.println("total size: " + size + ", total time: " + time + "ms, number: " + num);
                        System.err.println("avg size: " + (size / num) + ", avg time: " + (time / num) + "ms");
                        double nps = (num * 1000.0) / (System.currentTimeMillis() - start);
                        System.err.println("requests per second: " + nps);
                    }
                } catch (InterruptedException e) {
                }
            }
        }.start();
    }

    private static long start = 0;

    private static int totalSize = 0;

    private static long totalTime = 0;

    private static int number = 0;

    public static synchronized void addData(int size, long time) {
        totalSize += size;
        totalTime += time;
        number++;
    }

    public static class User extends Thread {

        private URL url;

        private byte[] buffer = new byte[4096 * 4];

        public User(String urlStr) throws MalformedURLException {
            url = new URL(urlStr);
        }

        public void run() {
            for (; ; ) {
                try {
                    long time = System.currentTimeMillis();
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    int thisSize = 0;
                    int size;
                    while ((size = is.read(buffer, 0, buffer.length)) > 0) {
                        thisSize += size;
                    }
                    time = System.currentTimeMillis() - time;
                    addData(thisSize, time);
                    is.close();
                } catch (IOException e) {
                    err(e.getMessage());
                }
            }
        }
    }
}
