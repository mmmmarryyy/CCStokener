package com.mycila.xmltool;

import org.testng.Assert;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public abstract class AbstractTest {

    private static final SoftHashMap<URL, byte[]> cache = new SoftHashMap<URL, byte[]>();

    private static final byte[] NULL = new byte[0];

    protected void assertSameDoc(String actual, String expected) {
        Assert.assertEquals(actual.replaceAll("\\r|\\n", "").replaceAll(">\\s*<", "><"), expected.replaceAll("\\r|\\n", "").replaceAll(">\\s*<", "><"));
    }

    public static URL resource(String classPath) {
        URL u = Thread.currentThread().getContextClassLoader().getResource(classPath.startsWith("/") ? classPath.substring(1) : classPath);
        if (u == null) {
            throw new IllegalArgumentException("Resource not found in classpath: " + classPath);
        }
        return u;
    }

    public static String readString(String classPath) {
        return readString(classPath, "UTF-8");
    }

    public static String readString(String classPath, String encoding) {
        try {
            return new String(readByte(classPath), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] readByte(String classPath) {
        return readByte(resource(classPath));
    }

    public static byte[] readByte(URL url) {
        byte[] data = cache.get(url);
        if (data == null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedInputStream bis = new BufferedInputStream(url.openStream());
                data = new byte[8192];
                int count;
                while ((count = bis.read(data)) != -1) {
                    baos.write(data, 0, count);
                }
                bis.close();
                data = baos.toByteArray();
                cache.put(url, data);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return data == NULL ? null : data;
    }
}
