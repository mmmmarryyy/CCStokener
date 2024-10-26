package jlib.Helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jlib.exception.ProgrammingException;

/**
 * Decides which X509 certificates may be used to authenticate the remote side of a secure socket. 
 * This manager trusts all certificates without any checking. Not very secure, but practical
 * for accessing servers inside the same private network. 
 * 
 * @author U930GN
 */
public class TrustAnyoneManager implements X509TrustManager {

    /**
 * Returns an initialized <code>InputStream</code> with the specified <code>url</code>
 * @param url A valid link to the remote resource. 
 * @return An initialized <code>InputStream</code> ready to provide data.
 * @throws IOException If the specified <code>url</code> cannot be resolved (i.e. the server
 * doesn't exist, or the server doesn't contain the specified resource). 
 */
    public static InputStream openStreamFromUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            SSLContext context;
            try {
                context = SSLContext.getInstance("TLS");
            } catch (NoSuchAlgorithmException e) {
                throw new ProgrammingException(ProgrammingException.MISSING_SSL_ALGORITHM, "Could not initialize an SSL context with 'TLS' algorithm.", e);
            }
            TrustAnyoneManager taom = new TrustAnyoneManager();
            try {
                context.init(null, new TrustManager[] { taom }, null);
            } catch (KeyManagementException e) {
                throw new ProgrammingException(ProgrammingException.UNKNOWN, "Could not initialize the SSL context.", e);
            }
            SSLSocketFactory sf = context.getSocketFactory();
            HttpsURLConnection ssl = (HttpsURLConnection) connection;
            HttpsURLConnection.setDefaultSSLSocketFactory(sf);
            ssl.setHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        return connection.getInputStream();
    }

    public static InputStream openStreamFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        return openStreamFromUrl(url);
    }

    public TrustAnyoneManager() {
        super();
    }

    public X509Certificate[] getAcceptedIssuers() {
        throw new UnsupportedOperationException();
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new UnsupportedOperationException();
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        return;
    }
}
