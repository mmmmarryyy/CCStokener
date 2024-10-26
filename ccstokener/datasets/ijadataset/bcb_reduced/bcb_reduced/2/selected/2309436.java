package com.dzelenetskiy.www.server.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HTTPClient {

    private static final Set<String> ProtocolSet = new HashSet<String>();

    static {
        ProtocolSet.add("http");
        ProtocolSet.add("https");
    }

    private static Object content = null;

    private static Map<String, List<String>> responseHeader = null;

    private static URL responseURL = null;

    private static int responseCode = -1;

    private static String MIMEtype = null;

    private static String charset = null;

    public static String getContent(URL url) throws IOException {
        if (!ProtocolSet.contains(url.getProtocol())) throw new java.lang.IllegalArgumentException("Non supported protocol");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-agent", "Opera/10.50 (Windows NT 6.1; U; en-GB) Presto/2.2.2");
        conn.connect();
        responseHeader = conn.getHeaderFields();
        responseCode = conn.getResponseCode();
        responseURL = conn.getURL();
        final int length = conn.getContentLength();
        final String type = conn.getContentType();
        if (type != null) {
            final String[] parts = type.split(";");
            MIMEtype = parts[0].trim();
            for (int i = 1; i < parts.length && charset == null; i++) {
                final String t = parts[i].trim();
                final int index = t.toLowerCase().indexOf("charset=");
                if (index != -1) charset = t.substring(index + 8);
            }
        }
        final InputStream stream = conn.getErrorStream();
        if (stream != null) content = readStream(length, stream); else if ((content = conn.getContent()) != null && content instanceof java.io.InputStream) content = readStream(length, (java.io.InputStream) content);
        conn.disconnect();
        return String.valueOf(content);
    }

    /** Read stream bytes and transcode. */
    private static Object readStream(int length, InputStream stream) throws IOException {
        final int buflen = Math.max(1024, Math.max(length, stream.available()));
        byte[] buf = new byte[buflen];
        ;
        byte[] bytes = null;
        for (int nRead = stream.read(buf); nRead != -1; nRead = stream.read(buf)) {
            if (bytes == null) {
                bytes = buf;
                buf = new byte[buflen];
                continue;
            }
            final byte[] newBytes = new byte[bytes.length + nRead];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
            System.arraycopy(buf, 0, newBytes, bytes.length, nRead);
            bytes = newBytes;
        }
        if (charset == null) return bytes;
        try {
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
        }
        return bytes;
    }

    public static Object GET(String url, String[][] props) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        for (int i = 0; i < props.length; ++i) {
            conn.addRequestProperty(props[i][0], URLEncoder.encode(props[i][1], "UTF-8"));
        }
        conn.connect();
        try {
            return conn.getContent();
        } finally {
            conn.disconnect();
        }
    }

    public static String POST(String url, String[][] props) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        for (int i = 0; i < props.length; ++i) {
            conn.addRequestProperty(props[i][0], props[i][1]);
        }
        conn.connect();
        try {
            return new String((byte[]) conn.getContent());
        } finally {
            conn.disconnect();
        }
    }
}
