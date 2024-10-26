package net.sf.hdkp.util;

import java.io.*;
import java.net.*;
import java.util.Properties;

public final class StreamUtils {

    private StreamUtils() {
    }

    public static void closeIgnoringExceptions(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
        }
    }

    public static void downloadFile(URL url, File file) throws IOException {
        final InputStream is = StreamUtils.getInputStream(url);
        try {
            final OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            copyFromToStream(is, os);
            os.flush();
            os.close();
        } finally {
            closeIgnoringExceptions(is);
        }
    }

    private static void copyFromToStream(InputStream is, OutputStream os) throws IOException {
        final byte[] buffer = new byte[8192];
        int len = is.read(buffer);
        while (len > 0) {
            os.write(buffer, 0, len);
            len = is.read(buffer);
        }
    }

    public static void loadProperties(Properties properties, InputStream is) throws IOException {
        try {
            properties.load(is);
        } finally {
            closeIgnoringExceptions(is);
        }
    }

    public static void storeProperties(String comments, Properties properties, OutputStream os) throws IOException {
        try {
            properties.store(os, comments);
            os.flush();
        } finally {
            closeIgnoringExceptions(os);
        }
    }

    public static Properties createProperties(URL url) throws IOException {
        return createProperties(getInputStream(url));
    }

    public static Properties createProperties(InputStream is) throws IOException {
        final Properties properties = new Properties();
        loadProperties(properties, is);
        return properties;
    }

    public static InputStream getInputStream(URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/4.0");
        return new BufferedInputStream(connection.getInputStream());
    }
}
