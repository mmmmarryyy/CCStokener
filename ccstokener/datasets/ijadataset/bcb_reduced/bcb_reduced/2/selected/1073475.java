package com.rendion.ajl;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;

public class HTTP {

    private static HttpClient http = new HttpClient(new MultiThreadedHttpConnectionManager());

    public static boolean acceptCookies = false;

    public static boolean throwExceptionWhenNot200OK = false;

    public String response;

    static {
        http.getParams().setParameter("http.socket.linger", 0);
    }

    public static void setConnectTimeout(int timeout) {
        http.getParams().setParameter("http.connection.timeout", timeout);
    }

    public static void setReadTimeout(int timeout) {
        http.getParams().setParameter("http.socket.timeout", timeout);
    }

    public static int getConnectTimeout() {
        return http.getParams().getIntParameter("http.connection.timeout", 0);
    }

    public static int getReadTimeout() {
        return http.getParams().getIntParameter("http.socket.timeout", 0);
    }

    private static HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {

        public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
            return false;
        }
    };

    public int result;

    private String getOld(String aUrl) {
        String response = "";
        int bytes;
        char[] buf = new char[10000];
        try {
            URL url = new URL(aUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            result = con.getResponseCode();
            if (result == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while (true) {
                    bytes = in.read(buf);
                    if (bytes == -1) {
                        break;
                    } else {
                        response += new String(buf, 0, bytes);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    public String get(String aUrl) throws Exception {
        return get(aUrl, (String) null);
    }

    public String get(String aUrl, String pUserAgent) {
        String response = "";
        HttpMethod get = null;
        try {
            get = new GetMethod(new URL(aUrl).toURI().toString());
            get.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
            get.getParams().setCookiePolicy(acceptCookies ? CookiePolicy.BROWSER_COMPATIBILITY : CookiePolicy.IGNORE_COOKIES);
            if (pUserAgent != null) {
                get.getParams().setParameter(HttpMethodParams.USER_AGENT, pUserAgent);
            }
            result = http.executeMethod(get);
            byte[] responseBody = get.getResponseBody();
            response = new String(responseBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            get.releaseConnection();
        }
        return response;
    }

    public String get(String aUrl, HashObject params) {
        this.response = "";
        HttpMethod get = null;
        try {
            get = new GetMethod(new URL(aUrl).toURI().toString());
            if (params != null && !params.isEmpty()) get.setQueryString(encodeQueryString(params));
            get.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
            get.getParams().setCookiePolicy(acceptCookies ? CookiePolicy.BROWSER_COMPATIBILITY : CookiePolicy.IGNORE_COOKIES);
            result = http.executeMethod(get);
            if (throwExceptionWhenNot200OK) {
                throw new RuntimeException("Not 200 OK:" + result);
            }
            byte[] responseBody = get.getResponseBody();
            response = new String(responseBody);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        } finally {
            try {
                get.releaseConnection();
            } catch (Exception e) {
            }
        }
        return response;
    }

    public static String encodeQueryString(HashObject params) {
        StringBuilder buffer = new StringBuilder(255);
        String sep = "";
        try {
            for (String s : (Set<String>) params.keySet()) {
                String value = params.get(s);
                if (value == null) value = "";
                buffer.append(sep).append(s).append("=").append(URLEncoder.encode(value, "UTF-8"));
                sep = "&";
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return buffer.toString();
    }

    public String escape(String url) {
        String escaped = null;
        try {
            escaped = new URI(url, false).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return escaped;
    }

    public String get(String aUrl, int maxLength) throws Exception {
        return get(aUrl, null, maxLength);
    }

    public String get(String aUrl, String pUserAgent, int maxLength) throws Exception {
        String response = "";
        HttpMethod get = new GetMethod(aUrl);
        maxLength = 1000;
        char[] buffer = new char[maxLength];
        try {
            get.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, myretryhandler);
            get.getParams().setCookiePolicy(acceptCookies ? CookiePolicy.BROWSER_COMPATIBILITY : CookiePolicy.IGNORE_COOKIES);
            get.getParams().setContentCharset("utf-8");
            if (pUserAgent != null) {
                get.getParams().setParameter(HttpMethodParams.USER_AGENT, pUserAgent);
            }
            long startms = new Date().getTime();
            result = http.executeMethod(get);
            System.out.println("http get in ms:" + (new Date().getTime() - startms));
            BufferedReader in = new BufferedReader(new InputStreamReader(get.getResponseBodyAsStream()), maxLength);
            int read = 0;
            int pos = 0;
            while ((maxLength - pos > 0) && (read = in.read(buffer, pos, maxLength - pos)) != -1) {
                pos += read;
            }
            if (pos > 0) {
                response = new String(buffer, 0, pos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            get.releaseConnection();
        }
        System.out.println(response);
        return response;
    }
}
