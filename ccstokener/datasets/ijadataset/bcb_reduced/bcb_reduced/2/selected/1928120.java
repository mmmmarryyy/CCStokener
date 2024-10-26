package net.jwde.extractor;

import java.net.*;
import java.io.*;

public class URLReader {

    public static String read(URL url) {
        String outContent = null;
        try {
            URLConnection inConnection = url.openConnection();
            InputStream is = inConnection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int totalBytes = 0;
            byte[] buffer = new byte[65536];
            while (true) {
                int bytesRead = is.read(buffer, 0, buffer.length);
                if (bytesRead < 0) break;
                for (int i = 0; i < bytesRead; i++) {
                    byte b = buffer[i];
                    if (b < 32 && b != 10 && b != 13 && b != 9) b = 32;
                    buffer[i] = b;
                }
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            is.close();
            out.close();
            outContent = out.toString();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
        return outContent;
    }

    public static String read(String urlName) {
        String inputLine = null;
        try {
            URL url = new URL(urlName);
            inputLine = read(url);
            return inputLine;
        } catch (MalformedURLException malEx) {
            malEx.printStackTrace();
        }
        return inputLine;
    }

    public static String getInputType(String urlName) {
        String inputType = null;
        try {
            URL url = new URL(urlName);
            inputType = getInputType(url);
            return inputType;
        } catch (MalformedURLException malURLEx) {
            malURLEx.printStackTrace();
        }
        return inputType;
    }

    public static String getInputType(URL url) {
        String inputType = null;
        try {
            URLConnection inConnection = url.openConnection();
            inputType = inConnection.getContentType();
            return inputType.trim();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
        return inputType;
    }
}
