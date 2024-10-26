package com.iv.flash;

import java.io.*;
import java.net.*;

/**
 */
public final class GetUrl {

    private static void help() {
        System.err.println("Get url v1.0");
        System.err.println("Copyright (c) Dmitry Skavish, 2000. All rights reserved.");
        System.err.println("");
        System.err.println("Usage: geturl <url> <filename>");
        System.err.println("");
        System.exit(1);
    }

    private static void err(String msg) {
        System.err.println(msg);
        help();
    }

    public static void main(String[] args) throws MalformedURLException {
        String surl = args[0];
        String filename = args[1];
        try {
            URL url = new URL(surl);
            byte[] buffer = new byte[4096 * 4];
            OutputStream out = new FileOutputStream(filename);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            int size;
            while ((size = is.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, size);
            }
            is.close();
            out.close();
            System.out.println("url.getRef():" + url.getRef());
        } catch (Exception e) {
            err(e.getMessage());
        }
    }
}
