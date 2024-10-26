package org.granite.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.jar.JarEntry;

/**
 * @author Franck WOLFF
 */
public class URIUtil {

    public static final String CLASSPATH_SCHEME = "class";

    public static String normalize(String uri) {
        if (uri == null) return null;
        if (File.separatorChar == '\\' && uri.startsWith("file://")) uri = uri.replace('\\', '/');
        uri = uri.replace(" ", "%20");
        return uri;
    }

    public static InputStream getInputStream(URI uri, ClassLoader loader) throws IOException {
        InputStream is = null;
        String scheme = uri.getScheme();
        if (CLASSPATH_SCHEME.equals(scheme)) {
            if (loader != null) is = loader.getResourceAsStream(uri.getSchemeSpecificPart());
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(uri.getSchemeSpecificPart());
                if (is == null) throw new IOException("Resource not found exception: " + uri);
            }
        } else if (scheme == null || scheme.length() <= 1) is = new FileInputStream(uri.toString()); else is = uri.toURL().openStream();
        return is;
    }

    public static InputStream getInputStream(URI uri) throws IOException {
        return getInputStream(uri, null);
    }

    public static String getContentAsString(URI uri) throws IOException {
        return getContentAsString(uri, Charset.defaultCharset());
    }

    public static String getContentAsString(URI uri, Charset charset) throws IOException {
        InputStream is = null;
        try {
            is = getInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
            StringBuilder sb = new StringBuilder(1024);
            char[] chars = new char[256];
            int count = -1;
            while ((count = reader.read(chars)) != -1) sb.append(chars, 0, count);
            return sb.toString();
        } finally {
            if (is != null) is.close();
        }
    }

    public static byte[] getContentAsBytes(URI uri) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(getInputStream(uri));
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            int b = 0;
            while ((b = is.read()) != -1) baos.write(b);
            return baos.toByteArray();
        } finally {
            if (is != null) is.close();
        }
    }

    public static long lastModified(URI uri) throws IOException {
        if (uri == null) return -1L;
        String scheme = uri.getScheme();
        if (scheme == null || scheme.length() <= 1) return new File(uri).lastModified();
        return lastModified(uri.toURL());
    }

    public static long lastModified(URL url) throws IOException {
        long lastModified = -1L;
        if (url != null) {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                JarEntry entry = ((JarURLConnection) connection).getJarEntry();
                if (entry != null) lastModified = entry.getTime();
            }
            if (lastModified == -1L) lastModified = connection.getLastModified();
        }
        return (lastModified == 0L ? -1L : lastModified);
    }
}
