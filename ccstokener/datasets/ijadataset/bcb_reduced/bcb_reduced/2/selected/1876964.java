package org.tamacat.httpd.auth;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.tamacat.httpd.config.ServerConfig;
import org.tamacat.httpd.ssl.SSLContextCreator;

public class ClientCert_test {

    static KeyStore keyStore;

    static KeyStore trustStore;

    public static void main(String[] args) throws Exception {
        HttpGet get = new HttpGet("https://localhost/docs/index.html");
        DefaultHttpClient client = new DefaultHttpClient();
        ServerConfig config = new ServerConfig(new Properties());
        config.setParam("https.keyStoreFile", "test.keystore");
        config.setParam("https.keyPassword", "nopassword");
        config.setParam("https.keyStoreType", "JKS");
        config.setParam("https.protocol", "SSLv3");
        SSLContextCreator ssl = new SSLContextCreator(config);
        SSLContext ctx = ssl.getSSLContext();
        SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
        Scheme sch = new Scheme("https", 443, socketFactory);
        client.getConnectionManager().getSchemeRegistry().register(sch);
        HttpResponse response = client.execute(get);
        System.out.println(response.getStatusLine().getStatusCode());
    }

    static void load() throws Exception {
        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream instream = ClientCert_test.class.getResourceAsStream("/test.truststore");
        try {
            trustStore.load(instream, "password".toCharArray());
        } finally {
            try {
                instream.close();
            } catch (Exception ignore) {
            }
        }
        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        instream = ClientCert_test.class.getResourceAsStream("/test.keystore");
        try {
            keyStore.load(instream, "password".toCharArray());
        } finally {
            try {
                instream.close();
            } catch (Exception ignore) {
            }
        }
    }
}
