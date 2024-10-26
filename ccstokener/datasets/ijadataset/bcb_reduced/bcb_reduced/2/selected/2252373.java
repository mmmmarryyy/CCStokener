package org.melati.util;

import java.io.Writer;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * IO voodoo.
 *
 * @todo Javadoc
 */
public class IoUtils {

    public static byte[] slurp(InputStream i, int estimate, int limit) throws IOException {
        try {
            byte[] b = new byte[estimate];
            int p = 0;
            for (; ; ) {
                int g = i.read(b, p, Math.min(b.length, limit) - p);
                if (g == -1) break;
                p += g;
                if (p >= limit) break;
                if (p >= b.length) {
                    byte[] c = new byte[2 * b.length];
                    System.arraycopy(b, 0, c, 0, p);
                    b = c;
                }
            }
            if (p == b.length) return b; else {
                byte[] c = new byte[p];
                System.arraycopy(b, 0, c, 0, p);
                return c;
            }
        } finally {
            try {
                i.close();
            } catch (Exception e) {
                ;
            }
        }
    }

    public static byte[] slurp(InputStream i, int estimate) throws IOException {
        return slurp(i, estimate, Integer.MAX_VALUE);
    }

    public static byte[] slurp(URL url, int estimate) throws IOException {
        return slurp(url.openStream(), estimate);
    }

    public static byte[] slurp(URL url, int estimate, int max) throws IOException {
        return slurp(url.openStream(), estimate, max);
    }

    public static byte[] slurp(File f, int estimate) throws IOException {
        return slurp(new FileInputStream(f), estimate);
    }

    public static char[] slurp(Reader i, int estimate, int limit) throws IOException {
        try {
            char[] b = new char[estimate];
            int p = 0;
            for (; ; ) {
                int g = i.read(b, p, Math.min(b.length, limit) - p);
                if (g == -1) break;
                p += g;
                if (p >= limit) break;
                if (p >= b.length) {
                    char[] c = new char[2 * b.length];
                    System.arraycopy(b, 0, c, 0, p);
                    b = c;
                }
            }
            if (p == b.length) return b; else {
                char[] c = new char[p];
                System.arraycopy(b, 0, c, 0, p);
                return c;
            }
        } finally {
            try {
                i.close();
            } catch (Exception e) {
                ;
            }
        }
    }

    public static char[] slurp(Reader i, int estimate) throws IOException {
        return slurp(i, estimate, Integer.MAX_VALUE);
    }

    /**
   * FIXME warn about potential inefficiency
   */
    public static byte[] slurpOutputOf_bytes(String[] command, int estimate, int limit) throws IOException {
        Process proc = Runtime.getRuntime().exec(command);
        byte[] output = IoUtils.slurp(proc.getInputStream(), estimate, limit);
        byte[] errors = IoUtils.slurp(proc.getErrorStream(), estimate, limit);
        try {
            if (proc.waitFor() != 0) throw new ProcessFailedException(command[0] + " failed", new String(errors));
            return output;
        } catch (InterruptedException e) {
            throw new IOException("interrupted while waiting for " + command[0] + " to complete");
        }
    }

    /**
   * FIXME warn about potential inefficiency
   */
    public static String slurpOutputOf(String[] command, int estimate, int limit) throws IOException {
        return new String(slurpOutputOf_bytes(command, estimate, limit));
    }

    public static void copy(InputStream i, int buf, OutputStream o) throws IOException {
        byte b[] = new byte[buf];
        for (; ; ) {
            int g = i.read(b);
            if (g == -1) break;
            o.write(b, 0, g);
        }
    }

    public static void copy(Reader i, int buf, Writer o) throws IOException {
        char b[] = new char[buf];
        for (; ; ) {
            int g = i.read(b);
            if (g == -1) break;
            o.write(b, 0, g);
        }
    }

    public static void copy(File from, int buf, File to) throws IOException {
        FileReader i = new FileReader(from);
        try {
            FileWriter o = new FileWriter(to);
            try {
                copy(i, buf, o);
            } finally {
                try {
                    o.close();
                } catch (Exception e) {
                    ;
                }
            }
        } finally {
            try {
                i.close();
            } catch (Exception e) {
                ;
            }
        }
    }
}
