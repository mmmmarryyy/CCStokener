package ru.adv.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.util.Assert;
import ru.adv.cache.Include;
import ru.adv.io.InputOutputException;
import ru.adv.io.atomic.FileTransaction;

/**
 * @version $Revision: 1.3 $
 */
public class HTTPInputOutput extends InputOutput {

    private static final String HTTPS = "https";

    private HTTPConnectionParameters params;

    private DefaultHttpClient httpClient;

    public HTTPInputOutput() {
    }

    public void setConnectionParameters(HTTPConnectionParameters params) {
        this.params = params;
    }

    public InputStream getInputStream(FileTransaction txt) throws InputOutputException {
        Assert.isNull(httpClient, getClass().toString() + " is not support multithreading");
        httpClient = new DefaultHttpClient();
        HttpRequestBase httpRequest;
        if (params != null) {
            httpRequest = params.createMethod(getSystemId());
            String contentType = params.createContentTypeString();
            if (contentType != null) {
                httpRequest.addHeader("Content-Type", contentType);
            }
            if (params.isAuthRequired()) {
                httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, params.createCredentials());
            }
        } else {
            httpRequest = new HttpGet(getSystemId());
        }
        try {
            if (params != null && params.isIgnoreSslChecks() && HTTPS.equals(httpRequest.getURI().getScheme())) {
                doNotValidateSslSertificate(httpRequest);
            }
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return httpResponse.getEntity().getContent();
            }
            InputOutputException e = new InputOutputException("Error on fetch " + getSystemId(), httpResponse.getStatusLine().getReasonPhrase());
            e.setAttr("status line", httpResponse.getStatusLine().toString());
            e.setAttr("statusCode", statusCode);
            throw e;
        } catch (Exception e) {
            throw new InputOutputException(e, getSystemId());
        }
    }

    private void doNotValidateSslSertificate(HttpRequestBase httpRequest) throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { new EasyTrustManager() }, null);
        SSLSocketFactory sf = new SSLSocketFactory(sslContext);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme httpsScheme = new Scheme(HTTPS, sf, httpRequest.getURI().getPort());
        httpClient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
    }

    @Override
    public void destroy() {
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        super.destroy();
    }

    public OutputStream getOutputStream(FileTransaction txt) throws InputOutputException {
        throw new InputOutputException("Cannot get output stream for ", getSystemId());
    }

    public long getSize() {
        return 0;
    }

    public Include createInclude(boolean checkCreationTime) {
        return new HTTPInclude(getSystemId());
    }

    public boolean isLocalFile() {
        return false;
    }

    public long lastModified() {
        return -1;
    }

    class EasyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    ;
}
