package org.apache.jetspeed.util;

import java.io.*;
import java.net.URL;

public class FileCopy {

    public static final int BUFFER_SIZE = 4096;

    public static final void copy(String source, String destination) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream input;
        BufferedOutputStream output;
        input = new BufferedInputStream(new FileInputStream(source));
        output = new BufferedOutputStream(new FileOutputStream(destination));
        copyStream(input, output, buffer);
        input.close();
        output.close();
    }

    public static final void copyFromURL(String source, String destination) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        URL url = new URL(source);
        BufferedInputStream input;
        BufferedOutputStream output;
        input = new BufferedInputStream(new DataInputStream(url.openStream()));
        output = new BufferedOutputStream(new FileOutputStream(destination));
        copyStream(input, output, buffer);
        input.close();
        output.close();
    }

    public static final void copyStream(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) output.write(buffer, 0, bytesRead);
    }
}
