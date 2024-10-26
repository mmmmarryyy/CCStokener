package com.bol.service;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Map;
import com.bol.log.BLogger;
import com.bol.log.BolLogger;

public class WGet {

    private static BLogger log = BolLogger.getLogger(WGet.class);

    public static void main(String[] args) throws Exception {
        try {
            writeFromURL("http://www.indiabolbol.com/bol/home.bol", "c:/john2/home.html");
        } catch (Exception e) {
            System.out.println("Error occured while writing to file" + e.getMessage());
        }
    }

    public static void writeFromURL(String urlstr, String filename) throws Exception {
        URL url = new URL(urlstr);
        InputStream in = url.openStream();
        BufferedReader bf = null;
        StringBuffer sb = new StringBuffer();
        try {
            bf = new BufferedReader(new InputStreamReader(in, "latin1"));
            String s;
            while (true) {
                s = bf.readLine();
                if (s != null) {
                    sb.append(s);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            bf.close();
        }
        writeRawBytes(sb.toString(), filename);
    }

    private static void writeRawBytes(String file, String filename) throws IOException {
        final FileOutputStream fos;
        if (filename != null && filename.trim().length() > 0) {
            fos = new FileOutputStream(filename);
            log.debug("ELSE GET FILENAME FROM JETTY Filename is;:" + filename, "writeRawBytes");
        } else {
            fos = new FileOutputStream(getHomeFilename());
            log.debug("ELSE GET FILENAME FROM JETTY Filename to write is::" + getHomeFilename(), "writeRawBytes");
        }
        FileChannel fc = fos.getChannel();
        ByteBuffer buffer = ByteBuffer.wrap(file.getBytes());
        fc.write(buffer);
        fc.close();
    }

    private static String getHomeFilename() {
        Map env = System.getProperties();
        String path = (String) env.get("jetty.home") + "/webapps/bol";
        String home = "/home.html";
        String filename = path + home;
        return filename;
    }
}
