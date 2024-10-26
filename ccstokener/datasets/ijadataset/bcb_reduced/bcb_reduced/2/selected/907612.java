package yaddur.net;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * 
 * @author Viktoras Agejevas
 * @version $Id: HttpsConnection.java 10 2007-12-07 16:57:39Z inversion $
 * 
 */
public class HttpsConnection implements javax.net.ssl.X509TrustManager {

    public HttpsURLConnection getConnection(String uri) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        SSLContext sc = SSLContext.getInstance("SSLv3");
        TrustManager[] tma = { new HttpsConnection() };
        sc.init(null, tma, null);
        SSLSocketFactory ssf = sc.getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(ssf);
        HttpsURLConnection connection = null;
        uri = uri.replaceAll("^(https://|http://)", "");
        URL url = new URL("https://" + uri);
        connection = (HttpsURLConnection) url.openConnection();
        return connection;
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
