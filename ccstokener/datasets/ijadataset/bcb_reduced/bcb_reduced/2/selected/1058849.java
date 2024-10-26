package com.rpc.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author ted stockwell
 */
public class IOUtils {

    public static byte[] readURLByteArray(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.connect();
            return IOUtils.readByteArray(new BufferedInputStream(connection.getInputStream()));
        } finally {
            connection.disconnect();
        }
    }

    public static byte[] readByteArray(InputStream inputStream, int count) throws IOException {
        byte[] bs = new byte[count];
        int off = 0;
        int read = 0;
        while (0 < count && ((read = inputStream.read(bs, off, count)) != -1)) {
            off += read;
            count -= read;
        }
        return bs;
    }

    /**
     * Reads all input from the given stream until the end of file is reached
     * and returns all the bytes in a single array.
     */
    public static byte[] readByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bs = new byte[1024 * 64];
        int read = 0;
        while ((read = inputStream.read(bs)) != -1) {
            baos.write(bs, 0, read);
        }
        return baos.toByteArray();
    }

    public static byte[] readFile(File file) throws IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            return readByteArray(inputStream);
        } finally {
            try {
                inputStream.close();
            } catch (IOException x) {
            }
        }
    }

    public static void copyFile(File destination, File source) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        try {
            byte[] buffer = new byte[32 * 1024];
            in = new BufferedInputStream(new FileInputStream(source));
            out = new BufferedOutputStream(new FileOutputStream(destination));
            while (in.read(buffer) != -1) {
                out.write(buffer);
            }
        } finally {
            try {
                in.close();
            } catch (Throwable t) {
            }
            try {
                out.close();
            } catch (Throwable t) {
            }
        }
    }

    public static boolean canWriteFile(File file) {
        FileOutputStream testOut = null;
        boolean result = false;
        try {
            file.getCanonicalPath();
            testOut = new FileOutputStream(file);
            result = true;
        } catch (Exception e) {
        } finally {
            if (testOut != null) {
                try {
                    testOut.close();
                } catch (IOException ioe) {
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean canWriteDir(String dirName) {
        File dir = new File(dirName);
        boolean result = false;
        try {
            dir.getCanonicalPath();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir.canWrite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
