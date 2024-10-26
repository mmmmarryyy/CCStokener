package com.mycila.testing.ea;

import com.mycila.testing.core.util.SoftHashMap;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class ExtendedAssert {

    private static final String DEFAULT_ENCODING = System.getProperty("file.encoding");

    private static final SoftHashMap<URL, byte[]> cache = new SoftHashMap<URL, byte[]>();

    private ExtendedAssert() {
    }

    public void assertSameXml(String actual, String expected) {
        assertSameXml(null, actual, expected);
    }

    public void assertSameXml(String message, String actual, String expected) {
        if (actual == null && expected == null) {
            return;
        }
        if (expected == null || actual == null) {
            fail(message, actual, expected);
        }
        if (!actual.replaceAll("\\r|\\n", "").replaceAll(">\\s*<", "><").equals(expected.replaceAll("\\r|\\n", "").replaceAll(">\\s*<", "><"))) {
            fail(message, actual, expected);
        }
    }

    public static void assertNotEquals(Object actual, Object expected) {
        assertNotEquals(null, actual, expected);
    }

    public static void assertNotEquals(String message, Object actual, Object expected) {
        if (expected == null && actual == null || expected != null && expected.equals(actual)) {
            fail(message, actual, "Objects not equals");
        }
    }

    public static void assertEmpty(String actual) {
        assertEmpty(null, actual);
    }

    public static void assertEmpty(String message, String actual) {
        if (actual != null && actual.length() > 0) {
            fail(message, actual, "Empty string");
        }
    }

    public static void assertEmpty(Collection<?> actual) {
        assertEmpty(null, actual);
    }

    public static void assertEmpty(String message, Collection<?> actual) {
        if (actual != null && !actual.isEmpty()) {
            fail(message, actual, "Empty collection");
        }
    }

    public static void assertEmpty(Object[] actual) {
        assertEmpty(null, actual);
    }

    public static void assertEmpty(String message, Object[] actual) {
        if (actual != null && actual.length > 0) {
            fail(message, actual, "Empty array");
        }
    }

    public static void assertBlank(String actual) {
        assertBlank(null, actual);
    }

    public static void assertBlank(String message, String actual) {
        if (actual != null && actual.trim().length() > 0) {
            fail(message, actual, "Blank string");
        }
    }

    public static URL resource(String classPath) {
        URL u = Thread.currentThread().getContextClassLoader().getResource(classPath.startsWith("/") ? classPath.substring(1) : classPath);
        if (u == null) {
            throw new IllegalArgumentException("Resource not found in classpath: " + classPath);
        }
        return u;
    }

    public static String asString(File file) {
        return asString(file, DEFAULT_ENCODING);
    }

    public static String asString(URL url) {
        return asString(url, DEFAULT_ENCODING);
    }

    public static String asString(String classPath) {
        return asString(classPath, DEFAULT_ENCODING);
    }

    public static String asString(File file, String encoding) {
        try {
            return asString(file.toURI().toURL(), encoding);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String asString(String classPath, String encoding) {
        try {
            return new String(asBytes(classPath), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String asString(URL url, String encoding) {
        try {
            return new String(asBytes(url), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] asBytes(File file) {
        try {
            return asBytes(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static byte[] asBytes(String classPath) {
        return asBytes(resource(classPath));
    }

    public static byte[] asBytes(URL url) {
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
        return data;
    }

    public static AssertException assertThrow(final Class<? extends Throwable> exceptionClass) {
        return new AssertionExceptionImpl(exceptionClass);
    }

    static String format(String message, Object actual, Object expected) {
        String formatted = "";
        if (message != null && !message.equals("")) formatted = message + " ";
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (expectedString.equals(actualString)) return formatted + "expected: " + formatClassAndValue(expected, expectedString) + " but was: " + formatClassAndValue(actual, actualString); else return formatted + "expected:<" + expectedString + "> but was:<" + actualString + ">";
    }

    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }

    static void fail(String message, Object actual, Object expected) {
        fail(format(message, actual, expected));
    }

    static void fail(String message) {
        throw new AssertionError(message == null ? "" : message);
    }

    public static interface AssertException {

        AssertException withMessage(String message);

        AssertException containingMessage(String message);

        void whenRunning(Code code);
    }
}
