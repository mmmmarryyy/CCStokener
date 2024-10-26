package org.bdgp.io;

import java.io.*;
import java.net.*;

public final class IOUtil {

    public static ProgressableInputStream getProgressableStream(String uri) throws IOException {
        File file = new File(uri);
        String pathStr = null;
        InputStream stream;
        if (file.exists()) {
            return new ProgressableFileInputStream(file);
        } else {
            URL url = null;
            try {
                url = new URL(uri);
                return new ProgressableURLInputStream(url);
            } catch (MalformedURLException ex) {
                throw new IOException("Invalid path " + uri);
            }
        }
    }

    public static InputStream getStream(String uri) throws IOException {
        File file = new File(uri);
        String pathStr = null;
        InputStream stream;
        if (file.exists()) {
            stream = new FileInputStream(file);
            pathStr = file.toString();
        } else {
            URL url = null;
            try {
                url = new URL(uri);
            } catch (MalformedURLException ex) {
                throw new IOException("Invalid path " + uri);
            }
            pathStr = url.toString();
            stream = url.openStream();
        }
        return new BufferedInputStream(stream);
    }

    public static URL getURL(String path) {
        try {
            URL url = new URL(path);
            File file = new File(path);
            if (file.exists()) return new URL("file:" + file.getCanonicalPath());
            return url;
        } catch (MalformedURLException e) {
            try {
                File file = new File(path);
                return new URL("file:" + file.getCanonicalPath());
            } catch (MalformedURLException ex) {
                return null;
            } catch (IOException ex) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isURL(String path) {
        try {
            URL url = new URL(path);
            return true;
        } catch (MalformedURLException ex) {
            return false;
        }
    }
}
