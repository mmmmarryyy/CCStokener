package org.openliberty.wsc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.ssl.KeyMaterial;
import org.apache.commons.ssl.SSL;
import org.apache.commons.ssl.SSLClient;
import org.apache.commons.ssl.TrustMaterial;
import org.apache.log4j.Logger;
import org.openliberty.LibConstants;
import org.openliberty.LibUtils;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

/**
 * This class contains a group of utilities used by the ClientLib
 * to make SSl/TLS and Mutual SSL/TLS requests.
 * 
 * @author tguion
 * @author asa
 */
public class SSLUtilities {

    private static final Logger log = Logger.getLogger(SSLUtilities.class);

    /**
     * This is a widely supported cypher that is not included by the 
     * SSLClient default.  We add it manually.
     */
    private static final String SSL_RSA_WITH_RC4_128_MD5 = "SSL_RSA_WITH_RC4_128_MD5";

    /**
     * This boolean allows the developer to specify using the old style
     * SSL which is set up to be quite relaxed, not paying attention to 
     * expiration dates, or host names, and not capable of MutualTLS
     */
    @Deprecated
    public static boolean useOldStyleSSL = false;

    /**
     * Load an X509Certificate from a file, placing it in the OpenLibertyBoostrap Trusted Certificates.
     * 
     * @author asa
     * 
     * @param pathToX509Certificate
     * @return
     */
    public static X509Certificate loadX509Certificate(String pathToX509Certificate) {
        X509Certificate cert = null;
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
            File certFile = new File(pathToX509Certificate);
            if (certFile.exists()) {
                try {
                    cert = (X509Certificate) cf.generateCertificate(new FileInputStream(certFile));
                } catch (FileNotFoundException e) {
                    log.error("Failed to load certificate from file " + certFile.getPath());
                    e.printStackTrace();
                }
            } else {
                cert = (X509Certificate) cf.generateCertificate(SSLUtilities.class.getResourceAsStream(pathToX509Certificate));
            }
        } catch (CertificateException e) {
            log.error("Failed to load certificate: " + pathToX509Certificate);
            e.printStackTrace();
        }
        if (null != cert) OpenLibertyBootstrap.getTrustedCertificates().add(cert);
        return cert;
    }

    /**
     * This is an exceptionally simple SOAP postSOAPMessageNOTLPOST using URLConnection with no TLS
     * 
     * @param postUrlString
     * @param message
     * @return
     */
    public static String postSOAPMessageNOTLS(String postUrlString, Element message) {
        String messageContent = XMLHelper.nodeToString(message);
        try {
            URL u = new URL(postUrlString);
            URLConnection uc = u.openConnection();
            HttpURLConnection connection = (HttpURLConnection) uc;
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("SOAPAction", "");
            OutputStream out = connection.getOutputStream();
            Writer wout = new OutputStreamWriter(out);
            wout.write(messageContent);
            wout.flush();
            wout.close();
            InputStream in = connection.getInputStream();
            String response = IOUtils.toString(in);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This is the method that WSFMessage uses to POST ID-* messages.
     * <p>
     * At the suggestion of the OpenSAML java developers, we are using SSLClient from 
     * the not-yet-commons-ssl library ( http://juliusdavies.ca/commons-ssl/ ).  It
     * simplifies the loading of certificates, SSL, and ClientTLS greatly, and has been 
     * said to be more stable than {@link HttpURLConnection}.
     * <p>
     * 
     * 
     * 
     * @author asa
     * 
     * @param postUrlString is the complete URL to the service being invoked.  e.g. https://mysvc.com:4343/INVOKE-DISCO
     * @param message the ID-* message as a DOM element
     * @param isClientTLS indicates whether mutual TLS should be used for this transaction
     * @return the response from the server as a string.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static String postSOAPMessage(String postUrlString, Element message, boolean isClientTLS) throws GeneralSecurityException, IOException {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (useOldStyleSSL) {
            initializeSSLProtocol();
            return postSOAPMessageViaHttpURLConnection(postUrlString, message);
        }
        SSLClient client = new SSLClient();
        for (Object cypher : SSL.SUPPORTED_CIPHERS_SET) {
            if (SSL_RSA_WITH_RC4_128_MD5.equals(cypher)) {
                String[] enabledCyphers = client.getEnabledCiphers();
                String[] newlyEnabledCyphers = new String[enabledCyphers.length + 1];
                for (int i = 0; i < enabledCyphers.length; i++) {
                    newlyEnabledCyphers[i] = enabledCyphers[i];
                }
                newlyEnabledCyphers[newlyEnabledCyphers.length - 1] = SSL_RSA_WITH_RC4_128_MD5;
                client.setEnabledCiphers(newlyEnabledCyphers);
                if (isDebugEnabled) {
                    log.debug("Cyphers Enabled:");
                    String[] strings = client.getEnabledCiphers();
                    for (String string : strings) {
                        log.debug("  > " + string);
                    }
                }
                break;
            }
        }
        client.addTrustMaterial(TrustMaterial.DEFAULT);
        for (X509Certificate certificate : OpenLibertyBootstrap.getTrustedCertificates()) {
            client.addTrustMaterial(new TrustMaterial(certificate));
        }
        client.setCheckHostname(OpenLibertyBootstrap.isCheckHostName());
        client.setCheckExpiry(OpenLibertyBootstrap.isCheckExpiry());
        if (isClientTLS) {
            client.setKeyMaterial(new KeyMaterial(OpenLibertyBootstrap.getDefaultClientTLSPKSPath(), OpenLibertyBootstrap.getDefaultClientTLSPKSPassword().toCharArray()));
        }
        URL postUrl = new URL(postUrlString);
        if (isDebugEnabled) {
            log.debug("ClientTLS: " + isClientTLS + " host: " + postUrl.getHost() + " port: " + postUrl.getPort() + " query: " + postUrl.getPath());
        }
        int port = postUrl.getPort();
        SSLSocket s = (SSLSocket) client.createSocket(postUrl.getHost(), port);
        if (isDebugEnabled) {
            log.debug("Socket created");
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())));
        String messageContent = XMLHelper.nodeToString(message);
        if (LibConstants.USE_AXIS_1_3_GREGORIAN_CALENDAR_SERIALIZATION_BUG_WORKAROUND) {
            messageContent = LibUtils.fixAxisGCSBug(messageContent);
        }
        out.println("POST " + postUrl.getPath() + " HTTP/1.1");
        out.println("Host: " + postUrl.getHost());
        out.println("Content-Type: text/soap+xml; charset=utf-8");
        out.println("Content-Length: " + messageContent.length());
        out.println("Connection: close");
        out.println("SOAPAction: \"\"" + "\n");
        out.println(messageContent);
        out.println();
        out.flush();
        if (isDebugEnabled) {
            log.debug("SOAP Message Sent");
        }
        InputStream is = s.getInputStream();
        if (isDebugEnabled) {
            log.debug("input stream created");
        }
        if (null != is) {
            String response = IOUtils.toString(is);
            if (LibConstants.USE_AXIS_1_3_GREGORIAN_CALENDAR_SERIALIZATION_BUG_WORKAROUND) {
                response = LibUtils.fixAxisGCSBug(response);
            }
            s.close();
            IOUtils.closeQuietly(is);
            if (isDebugEnabled) {
                log.debug("input stream closed");
            }
            int first_lt = response.indexOf("<");
            if (first_lt != -1) {
                int last_gt = response.lastIndexOf('>');
                if (last_gt < response.length() - 1) {
                    response = response.substring(first_lt, last_gt + 1);
                } else {
                    response = response.substring(first_lt);
                }
            } else {
                log.error("BAD RESPONSE FROM SERVER, EXPECTED SOAP");
                log.error(response);
                response = "";
            }
            if (isDebugEnabled) {
                log.debug("RESPONSE:");
                log.debug(response);
            }
            return response;
        }
        s.close();
        return "";
    }

    /**
     * Relax the SSL protocol in order to allow out-of-date certificates.
     * 
     * @author asa
     */
    @Deprecated
    public static void initializeSSLProtocol() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            X509TrustManager[] xtmArray = new X509TrustManager[] { SSLUtilities.xtm };
            sslContext.init(null, xtmArray, new java.security.SecureRandom());
        } catch (GeneralSecurityException gse) {
            gse.printStackTrace();
        }
        if (sslContext != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }
        HttpsURLConnection.setDefaultHostnameVerifier(SSLUtilities.hnv);
    }

    /**
     * Create an anonymous class to trust all certificates.
     * This is bad style, you should create a separate class.
     * 
     * @author asa
     */
    @Deprecated
    public static X509TrustManager xtm = new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    /**
     * Create a class to trust all hosts
     */
    @Deprecated
    public static HostnameVerifier hnv = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Uility that takes a Document and posts it via HTTP
     * 
     * @author tguion
     * 
     * @param urlString
     * @param message
     * @return
     * @throws IOException
     */
    @Deprecated
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

    /**
     * 
     * @author tguion
     * 
     * @param urlString
     * @param inputFileName
     * @return
     * @throws IOException
     */
    @Deprecated
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
     * 
     * @author tguion
     * 
     * @param urlString
     * @param message
     * @return
     * @throws IOException
     */
    @Deprecated
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

    /**
     * 
     * @author tguion
     * 
     * @param urlString
     * @param message
     * @return
     * @throws IOException
     */
    @Deprecated
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

    /**
     * 
     * @author tguion
     * 
     * @param urlString
     * @return
     * @throws IOException
     */
    @Deprecated
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
}
