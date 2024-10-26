package jp.go.aist.sot.client.net;

import com.sun.net.ssl.internal.ssl.Provider;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.Security;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import jp.go.aist.sot.client.net.SSLTunnelSocketFactory;

public class SSLConnectionUtil {

    static {
        System.setProperty("java.protocol.handler.pkgs", "sun.net.www.protocol");
        java.security.Provider prov = new com.sun.net.ssl.internal.ssl.Provider();
        Security.addProvider(prov);
    }

    public static String getProxyHost() {
        String host = null;
        if (((host = System.getProperty("proxyHost")) != null) || ((host = System.getProperty("http.proxyHost")) != null) || ((host = System.getProperty("https.proxyHost")) != null)) {
            return host;
        }
        return host;
    }

    public static int getProxyPort() throws NumberFormatException {
        String port = null;
        int portNum = 0;
        if (((port = System.getProperty("proxyPort")) != null) || ((port = System.getProperty("http.proxyPort")) != null) || ((port = System.getProperty("https.proxyPort")) != null)) {
            portNum = Integer.parseInt(port);
        }
        return portNum;
    }

    public static URLConnection getConnection(String urlStr, SSLContext ctx) throws Exception {
        URL url = new URL(urlStr);
        URLConnection con = url.openConnection();
        if (con instanceof HttpsURLConnection) {
            HttpsURLConnection connection = (HttpsURLConnection) con;
            SSLSocketFactory factory = null;
            String proxyHost = null;
            int proxyPort = 0;
            proxyHost = getProxyHost();
            proxyPort = getProxyPort();
            factory = new SSLTunnelSocketFactory(ctx, proxyHost, proxyPort);
            connection.setSSLSocketFactory(factory);
        }
        con.setDoInput(true);
        con.setDoOutput(true);
        return con;
    }
}
