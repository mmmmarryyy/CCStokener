package web;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletOutputStream;

public class Download {

    public static String download(String address, String outputFolder) {
        URL url = null;
        String fileName = "";
        try {
            url = new URL(address);
            System.err.println("Indirizzo valido!");
        } catch (MalformedURLException ex) {
            System.err.println("Indirizzo non valido!");
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range", "bytes=0-");
            connection.connect();
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                System.err.println("Errore, c'e' qualcosa che non va!");
                return "";
            }
            fileName = url.getFile();
            fileName = fileName.substring(url.getFile().lastIndexOf('/') + 1);
            RandomAccessFile file = new RandomAccessFile(outputFolder + fileName, "rw");
            file.seek(0);
            InputStream stream = connection.getInputStream();
            byte[] buffer = new byte[1024];
            while (true) {
                int read = stream.read(buffer);
                if (read == -1) {
                    break;
                }
                file.write(buffer, 0, read);
            }
            file.close();
        } catch (IOException ioe) {
            System.err.println("I/O error!");
        }
        return outputFolder + fileName;
    }

    public static String alternativeDownload(String address, String outputFolder) {
        {
            try {
                String fileName = address.substring(address.lastIndexOf('/') + 1);
                java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.net.URL(address).openStream());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFolder + fileName);
                java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                byte data[] = new byte[1024];
                while (in.read(data, 0, 1024) >= 0) {
                    bout.write(data);
                }
                bout.close();
                in.close();
                return fileName;
            } catch (Exception e) {
            }
        }
        return "";
    }

    public static long getFileSize(String address) {
        URL url = null;
        try {
            url = new URL(address);
            System.err.println("Indirizzo valido - " + url.toString().substring(0, 10) + "...");
        } catch (MalformedURLException ex) {
            System.err.println("Indirizzo non valido!");
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range", "bytes=0-");
            connection.connect();
            return connection.getContentLength();
        } catch (IOException ioe) {
            System.err.println("I/O error!");
            return 0;
        }
    }
}
