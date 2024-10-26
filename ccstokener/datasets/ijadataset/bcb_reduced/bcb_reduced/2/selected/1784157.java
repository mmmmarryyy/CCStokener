package org.openliberty.wsc.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author tguion
 *
 */
public class SSLUtilities {

    public static X509TrustManager xtm = new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    public static HostnameVerifier hnv = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static String postSOAPFileViaHttpURLConnection(String urlString, String inputFileName) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = createConnection(urlString);
        } catch (IOException e) {
            throw (e);
        }
        InputStream is = SSLUtilities.class.getResourceAsStream(inputFileName);
        String fileContents = IOUtils.toString(is);
        IOUtils.closeQuietly(is);
        OutputStream os = conn.getOutputStream();
        IOUtils.write(fileContents, os);
        os.flush();
        IOUtils.closeQuietly(os);
        is = conn.getInputStream();
        String response = IOUtils.toString(is);
        IOUtils.closeQuietly(is);
        return response;
    }

    /**
	 * Uility that takes a Document and posts it via HTTP
	 * 
	 * @param urlString
	 * @param message
	 * @return
	 * @throws IOException
	 */
    public static String postSOAPMessageViaHttpURLConnection(String urlString, Element message) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = createConnection(urlString);
        } catch (IOException e) {
            throw (e);
        }
        OutputStream os = conn.getOutputStream();
        IOUtils.write(XMLHelper.nodeToString(message), os);
        os.flush();
        IOUtils.closeQuietly(os);
        InputStream is;
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }
        if (null != is) {
            String response = IOUtils.toString(is);
            IOUtils.closeQuietly(is);
            return response;
        }
        return "";
    }

    public static String postSOAPMessageViaHttpURLConnection(String urlString, String message) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = createConnection(urlString);
        } catch (IOException e) {
            throw (e);
        }
        OutputStream os = conn.getOutputStream();
        IOUtils.write(message, os);
        os.flush();
        IOUtils.closeQuietly(os);
        InputStream is = conn.getInputStream();
        String response = IOUtils.toString(is);
        IOUtils.closeQuietly(is);
        return response;
    }

    public static InputStream postSOAPMessageViaHttpURLConnectionIS(String urlString, String message) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = createConnection(urlString);
        } catch (IOException e) {
            throw (e);
        }
        OutputStream os = conn.getOutputStream();
        IOUtils.write(message, os);
        os.flush();
        IOUtils.closeQuietly(os);
        InputStream is = conn.getInputStream();
        return is;
    }

    private static HttpURLConnection createConnection(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("SOAPAction", "");
        conn.setInstanceFollowRedirects(false);
        conn.setUseCaches(false);
        return conn;
    }

    static {
    }
}
