package org.t2framework.vili.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.jar.JarFile;
import werkzeugkasten.common.util.Streams;

public class StreamUtils {

    private static final int BUF_SIZE = 65536;

    private StreamUtils() {
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        in = new BufferedInputStream(in, BUF_SIZE);
        out = new BufferedOutputStream(out, BUF_SIZE);
        byte[] buf = new byte[BUF_SIZE];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void close(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static void close(JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static byte[] read(URL url) throws IOException {
        if (url == null) {
            return null;
        }
        InputStream is = url.openStream();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Streams.copy(is, os);
            return os.toByteArray();
        } finally {
            StreamUtils.close(is);
        }
    }
}
