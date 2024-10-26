package com.rcreations.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.IOUtils;

/**
 * Utility class with helper functions for http calls (works with HttpURLConnection).
 */
public class HttpUtils {

    /**
    * During a HTTP GET/POST, the browser sends
    *	cookie name and values in the headers.
    *
    *	An example cookie header looks like:
    *		Cookie: manualCookie=manualCookieValue,c2=abc
    *
    *	Given an array of name/value pairs,
    *	this function returns the complete Cookie value,
    * e.g. "manualCookie=manualCookieValue,c2=abc"
    *
    * @return cookie value like "manualCookie=manualCookieValue,c2=abc"
    */
    public static String createCookieHeaderValue(HashMap hashTable) {
        StringBuffer buf = new StringBuffer();
        int i = 0;
        Iterator it = hashTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (i != 0) {
                buf.append(",");
            }
            i++;
            buf.append(key).append("=").append(value);
        }
        return buf.toString();
    }

    /**
    * Starts a HTTP GET operation by sending headers.
    * Caller only has to get the response code and read the output (via conn.getInputStream()).
    */
    public static void httpStartGet(HttpURLConnection conn, HashMap hashHeaders) throws IOException {
        if (hashHeaders != null) {
            Iterator it = hashHeaders.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
                String key = entry.getKey();
                String value = entry.getValue();
                conn.setRequestProperty(key, value);
            }
        }
    }

    /**
    * Calls conn.getInputStream() and write the data into the given ByteArrayOutputStream.
    */
    public static void httpReadOutput(HttpURLConnection conn, ByteArrayOutputStream bufOutput) throws IOException {
        InputStream in = conn.getInputStream();
        try {
            byte buffer[] = new byte[4096];
            int iRead = 0;
            while (iRead > -1) {
                iRead = in.read(buffer, 0, 4096);
                if (iRead > 0) {
                    bufOutput.write(buffer, 0, iRead);
                }
            }
        } finally {
            in.close();
        }
    }

    /**
    * Performs a HTTP GET operation.
    */
    public static void httpDoGet(HttpURLConnection conn, HashMap hashHeaders, StringBuffer bufOutput) throws IOException {
        bufOutput.setLength(0);
        httpStartGet(conn, hashHeaders);
        InputStreamReader inReader = null;
        try {
            inReader = new InputStreamReader(conn.getInputStream());
            char buffer[] = new char[4096];
            int iRead = 0;
            while (iRead > -1) {
                iRead = inReader.read(buffer, 0, 4096);
                if (iRead > 0) {
                    bufOutput.append(buffer, 0, iRead);
                }
            }
        } finally {
            IOUtils.closeQuietly(inReader);
        }
    }

    /**
    * Performs a HTTP GET operation.
    */
    public static HttpURLConnection httpDoGet(String strAbsUrlWithParams, HashMap hashHeaders, StringBuffer bufOutput) throws MalformedURLException, IOException {
        URL url = new URL(strAbsUrlWithParams);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        httpDoGet(conn, hashHeaders, bufOutput);
        return conn;
    }

    /**
    * Performs a HTTP GET operation.
    */
    public static HttpURLConnection httpDoGet(String strAbsUrl, HashMap hashParams, HashMap hashHeaders, StringBuffer bufOutput) throws MalformedURLException, IOException {
        String strAbsUrlWithParams = strAbsUrl;
        if (hashParams != null) {
            strAbsUrlWithParams = UrlUtils.combineUrlAndQueryParams(strAbsUrl, hashParams);
        }
        return httpDoGet(strAbsUrlWithParams, hashHeaders, bufOutput);
    }

    /**
    * Starts a HTTP POST operation by sending parameters and headers.
    * Caller only has to get the response code and read the output (via conn.getInputStream()).
    */
    public static void httpStartPost(HttpURLConnection conn, HashMap hashParams, HashMap hashHeaders) throws IOException {
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (hashHeaders != null) {
            Iterator it = hashHeaders.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) it.next();
                String key = entry.getKey();
                String value = entry.getValue();
                conn.setRequestProperty(key, value);
            }
        }
        String queryString = UrlUtils.toQueryString(hashParams);
        PrintWriter outStream = new PrintWriter(conn.getOutputStream());
        try {
            outStream.print(queryString);
        } finally {
            outStream.close();
        }
    }

    /**
    * Performs a HTTP POST operation.
    */
    public static void httpDoPost(HttpURLConnection conn, HashMap hashParams, HashMap hashHeaders, StringBuffer bufOutput) throws IOException {
        bufOutput.setLength(0);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpStartPost(conn, hashParams, hashHeaders);
        InputStreamReader inReader = null;
        try {
            inReader = new InputStreamReader(conn.getInputStream());
            char buffer[] = new char[4096];
            int iRead = 0;
            while (iRead > -1) {
                iRead = inReader.read(buffer, 0, 4096);
                if (iRead > 0) {
                    bufOutput.append(buffer, 0, iRead);
                }
            }
        } finally {
            IOUtils.closeQuietly(inReader);
        }
    }

    /**
    * Performs a HTTP POST operation.
    */
    public static HttpURLConnection httpDoPost(String strAbsUrl, HashMap hashParams, HashMap hashHeaders, StringBuffer bufOutput) throws MalformedURLException, IOException {
        URL url = new URL(strAbsUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        httpDoPost(conn, hashParams, hashHeaders, bufOutput);
        return conn;
    }
}
