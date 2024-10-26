package org.jsslutils.sslcontext.test;

import static org.junit.Assert.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import org.jsslutils.sslcontext.DefaultSSLContextFactory;
import org.jsslutils.sslcontext.PKIXSSLContextFactory;
import org.jsslutils.sslcontext.SSLContextFactory;
import org.jsslutils.sslcontext.X509SSLContextFactory;
import org.junit.Test;

/**
 * 
 * @author Bruno Harbulot
 * 
 */
public class DefaultStoreTest {

    public static final String KNOWN_CA_URL = "https://jsslutils.googlecode.com/";

    public static final String UNKNOWN_CA_URL = "https://ca.grid-support.ac.uk/";

    public void connect(SSLContext sslContext, String address) throws Exception {
        URL url = new URL(address);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        if (sslContext != null) {
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
        connection.connect();
        connection.disconnect();
    }

    @Test
    public void testKnownCA() throws Exception {
        connect(null, KNOWN_CA_URL);
    }

    @Test
    public void testUnknownCA() throws Exception {
        try {
            connect(null, UNKNOWN_CA_URL);
            fail();
        } catch (SSLHandshakeException e) {
        }
    }

    @Test
    public void testDefaultFactoryKnownCA() throws Exception {
        SSLContextFactory sslContextFactory = new DefaultSSLContextFactory();
        SSLContext sslContext = sslContextFactory.buildSSLContext();
        connect(sslContext, KNOWN_CA_URL);
    }

    @Test
    public void testDefaultFactoryUnKnownCA() throws Exception {
        SSLContextFactory sslContextFactory = new DefaultSSLContextFactory();
        SSLContext sslContext = sslContextFactory.buildSSLContext();
        try {
            connect(sslContext, UNKNOWN_CA_URL);
            fail();
        } catch (SSLHandshakeException e) {
        }
    }

    @Test
    public void testX509FactoryKnownCA() throws Exception {
        SSLContextFactory sslContextFactory = new X509SSLContextFactory();
        SSLContext sslContext = sslContextFactory.buildSSLContext();
        connect(sslContext, KNOWN_CA_URL);
    }

    @Test
    public void testX509FactoryUnKnownCA() throws Exception {
        SSLContextFactory sslContextFactory = new X509SSLContextFactory();
        SSLContext sslContext = sslContextFactory.buildSSLContext();
        try {
            connect(sslContext, UNKNOWN_CA_URL);
            fail();
        } catch (SSLHandshakeException e) {
        }
    }

    @Test
    public void testPKIXFactoryKnownCA() throws Exception {
        SSLContextFactory sslContextFactory = new PKIXSSLContextFactory();
        SSLContext sslContext = sslContextFactory.buildSSLContext();
        connect(sslContext, KNOWN_CA_URL);
    }

    @Test
    public void testPKIXFactoryUnKnownCA() throws Exception {
        SSLContextFactory sslContextFactory = new PKIXSSLContextFactory();
        SSLContext sslContext = sslContextFactory.buildSSLContext();
        try {
            connect(sslContext, UNKNOWN_CA_URL);
            fail();
        } catch (SSLHandshakeException e) {
        }
    }
}
