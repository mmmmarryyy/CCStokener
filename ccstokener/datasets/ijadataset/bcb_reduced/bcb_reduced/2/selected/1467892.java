package net.sf.istcontract.aws.utils;

import java.io.*;
import java.net.*;

/**
 *  Here is  an object for InputStream creation
 *  It lets you access the URL that corresponds to the created InputStream
 *
 */
public class URLInputStream {

    private InputStream inputStream;

    private URL url;

    public URLInputStream(String fileLocation) throws Exception {
        try {
            inputStream = new FileInputStream(fileLocation);
            url = new URL("file:" + fileLocation);
        } catch (FileNotFoundException e) {
            try {
                url = getClass().getClassLoader().getResource(fileLocation);
                inputStream = url.openStream();
            } catch (Exception e2) {
                url = new URL(fileLocation);
                inputStream = url.openStream();
            }
        }
    }

    public URL getURL() {
        return url;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: java InputStreamTest <file_path>");
        } else {
            for (int i = 0; i < args.length; i++) {
                try {
                    URLInputStream urlInputStream = new URLInputStream(args[i]);
                    System.out.println(urlInputStream.getURL().toExternalForm());
                    urlInputStream.getInputStream().close();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                System.out.println();
            }
        }
        System.exit(0);
    }
}
