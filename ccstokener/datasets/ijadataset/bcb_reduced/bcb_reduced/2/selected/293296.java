package org.jmx4odp.junitDiagnosticWorkers;

import java.lang.StringBuffer;
import java.io.*;
import java.net.*;
import junit.framework.*;

/**
 * Extend or implement this test class for tests of HTTP services
 *
 * @author  Lucas McGregor
 */
public class HttpClientTest extends TestCase {

    /** Creates a new instance of HttpClient */
    public HttpClientTest() {
    }

    /**
     * Get a HttpConnection object based on the URL
     * from the string. It needs to be an HTTP URL.
     **/
    public HttpURLConnection getUrlConnection(String s) throws Exception {
        URL url = new URL(s);
        return getUrlConnection(url);
    }

    /**
     * Get a HttpConnection object based on the URL.
     * I tneeds to be an HTTP URL.
     **/
    public HttpURLConnection getUrlConnection(URL url) throws Exception {
        if (!url.getProtocol().equalsIgnoreCase("http")) throw new Exception("URL does not use the HTTP protocol");
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * returns a true if the HttpUrlConnection status is the same
     * as the status passed in.
     **/
    public static boolean isHttpStatus(HttpURLConnection con, int status) {
        try {
            return (con.getResponseCode() == status);
        } catch (Exception e) {
            fail(e.toString());
        }
        return false;
    }

    /**
     * assertTrue for isHttpStatus(con, java.net.HttpURLConnection.HTTP_OK)
     **/
    public static void assertHttpOk(HttpURLConnection con) {
        assertTrue(isHttpStatus(con, java.net.HttpURLConnection.HTTP_OK));
    }

    /**
     * assertTrue for isHttpStatus(con, java.net.HttpURLConnection.HTTP_ACCEPTED)
     **/
    public static void assertHttpAccepted(HttpURLConnection con) {
        assertTrue(isHttpStatus(con, java.net.HttpURLConnection.HTTP_ACCEPTED));
    }

    /**
     * assertTrue for isHttpStatus(con, java.net.HttpURLConnection.HTTP_FORBIDDEN)
     **/
    public static void assertHttpForbidden(HttpURLConnection con) {
        assertTrue(isHttpStatus(con, java.net.HttpURLConnection.HTTP_FORBIDDEN));
    }

    /**
     * assertTrue for isHttpStatus(con, java.net.HttpURLConnection.HTTP_MOVED_PERM)
     **/
    public static void assertHttpMovedPerm(HttpURLConnection con) {
        assertTrue(isHttpStatus(con, java.net.HttpURLConnection.HTTP_MOVED_PERM));
    }

    /**
     * assertTrue for isHttpStatus(con, java.net.HttpURLConnection.HTTP_MOVED_TEMP)
     **/
    public static void assertHttpMovedTemp(HttpURLConnection con) {
        assertTrue(isHttpStatus(con, java.net.HttpURLConnection.HTTP_MOVED_TEMP));
    }

    /**
     * assertTrue for isHttpStatus(con, java.net.HttpURLConnection.HTTP_NOT_FOUND)
     **/
    public static void assertHttpNotFound(HttpURLConnection con) {
        assertTrue(isHttpStatus(con, java.net.HttpURLConnection.HTTP_NOT_FOUND));
    }

    /**
     * asserts true if the content returned from the HttpURLConnection contains
     * the string anywhere in it.
     **/
    public static void assertHttpString(HttpURLConnection con, String pattern) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            char[] buffer = new char[10240];
            int index = 0;
            StringBuffer sb = new StringBuffer();
            index = reader.read(buffer);
            while (index != -1) {
                sb.append(buffer, 0, index);
                index = reader.read(buffer);
            }
            String dataString = sb.toString();
            assertTrue((dataString.indexOf(pattern) != -1));
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    /**
     * asserts true if the content returned from the HttpURLConnection does
     * not contain the string anywhere in it.
     **/
    public static void assertHttpNoString(HttpURLConnection con, String pattern) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            char[] buffer = new char[10240];
            int index = 0;
            StringBuffer sb = new StringBuffer();
            index = reader.read(buffer);
            while (index != -1) {
                sb.append(buffer, 0, index);
                index = reader.read(buffer);
            }
            String dataString = sb.toString();
            assertTrue((dataString.indexOf(pattern) == -1));
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
