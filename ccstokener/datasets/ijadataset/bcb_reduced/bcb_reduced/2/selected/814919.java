package ise.checkeredflag;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import javax.net.ssl.*;
import ise.checkeredflag.parser.HtmlDocument;
import ise.checkeredflag.parser.HtmlDocument.Attribute;

public class Utilities {

    private static boolean sslInitialized = false;

    public static URLConnection createConnection(URL url) throws IOException {
        if (url == null) {
            throw new IOException("Invalid URL: url is null.");
        }
        URLConnection connection = url.openConnection();
        connection.setAllowUserInteraction(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", PropertyManager.getUserAgent());
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setFollowRedirects(true);
        }
        return connection;
    }

    public static void closeConnection(URLConnection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (connection.getDoInput() == true) {
                connection.getInputStream().close();
            }
        } catch (Exception e) {
        }
        try {
            if (connection.getDoOutput() == true) {
                connection.getOutputStream().flush();
                connection.getOutputStream().close();
            }
        } catch (Exception e) {
        }
        try {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        } catch (Exception e) {
        }
    }

    public static void initializeSSL() {
        if (!sslInitialized) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                } };
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                sslInitialized = true;
            } catch (Exception e) {
                e.printStackTrace();
                sslInitialized = false;
            }
        }
    }

    public static void copyToWriter(Reader from, Writer to) throws IOException {
        char[] buffer = new char[1024 * 32];
        int chars_read;
        while (true) {
            chars_read = from.read(buffer);
            if (chars_read == -1) {
                break;
            }
            to.write(buffer, 0, chars_read);
        }
        to.flush();
        from.close();
    }

    public static String getAttribute(HtmlDocument.Tag t, String name) {
        for (Iterator i = t.attributeList.attributes.iterator(); i.hasNext(); ) {
            Attribute attribute = (Attribute) i.next();
            if (attribute.name.equalsIgnoreCase(name)) {
                return Utilities.deQuote(attribute.value);
            }
        }
        return null;
    }

    public static String deQuote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }
}
