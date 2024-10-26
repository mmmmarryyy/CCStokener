package org.freebxml.omar.client.ui.web.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.freebxml.omar.common.CredentialInfo;

public class HttpsURLStreamHandler extends sun.net.www.protocol.https.Handler {

    /**
     * Custom trust manager implementation that trusts every server certificate.
     */
    private static class TrustManagerImpl implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Custom hostname verifier that accepts any hostname.
     */
    private static class HostnameVerifierImpl implements HostnameVerifier {

        public boolean verify(String string, SSLSession sslSession) {
            return true;
        }
    }

    private final SSLSocketFactory sslSocketFactory;

    /**
     * Create a stream handler that uses a given client certificate.
     *
     * @param keyStore the keystore to extract the client certificate from
     * @param alias the alias of the key in the keystore
     */
    public HttpsURLStreamHandler(CredentialInfo credentialInfo) throws GeneralSecurityException, IOException {
        KeyManager[] keyManagers;
        KeyStore keyStore2 = KeyStore.getInstance("JKS");
        keyStore2.load(null, null);
        keyStore2.setKeyEntry("root", credentialInfo.privateKey, new char[0], credentialInfo.certChain);
        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
        factory.init(keyStore2, new char[0]);
        keyManagers = factory.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, new TrustManager[] { new TrustManagerImpl() }, new SecureRandom());
        sslSocketFactory = sslContext.getSocketFactory();
    }

    /**
     * Create a new URL that uses this stream handler.
     */
    public URL createURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile(), this);
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) super.openConnection(url);
        connection.setSSLSocketFactory(sslSocketFactory);
        connection.setHostnameVerifier(new HostnameVerifierImpl());
        return connection;
    }
}
