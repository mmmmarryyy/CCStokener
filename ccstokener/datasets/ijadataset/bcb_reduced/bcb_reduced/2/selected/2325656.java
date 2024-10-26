package com.android.unit_tests;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.Suppress;
import com.google.android.net.GoogleHttpClient;
import com.android.internal.net.DbSSLSessionCache;
import com.android.internal.net.DbSSLSessionCache.DatabaseHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

/** Unit test for SSL session caching with {@link GoogleHttpClient}.
 *  Uses network resources. 
 */
@Suppress
public class DbSSLSessionCacheTest extends AndroidTestCase {

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /** 
     * We want to test the actual database write - the actual hooking into 
     * low-level SSL is tested. 
     */
    @LargeTest
    public void testSslCacheAdd() throws Exception {
        DbSSLSessionCache cache = DbSSLSessionCache.getInstanceForPackage(getContext());
        cache.clear();
        makeRequestInNewContext("https://www.google.com");
        SQLiteOpenHelper helper = new DatabaseHelper(getContext());
        Cursor query = null;
        try {
            query = helper.getReadableDatabase().query(DbSSLSessionCache.SSL_CACHE_TABLE, new String[] { "hostport" }, null, null, null, null, null);
            assertTrue(query.moveToFirst());
            String hostPort = query.getString(0);
            assertEquals(hostPort, "www.google.com:443");
        } finally {
            query.close();
        }
    }

    @LargeTest
    public void testExpire() throws Exception {
        DatabaseHelper helper = new DatabaseHelper(getContext());
        DbSSLSessionCache cache = new DbSSLSessionCache(helper);
        cache.clear();
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < DbSSLSessionCache.MAX_CACHE_SIZE + 2; i++) {
            final int port = i;
            cache.putSessionData(new MockSession() {

                public String getPeerHost() {
                    return "test.host.com";
                }

                public int getPeerPort() {
                    return port;
                }
            }, new byte[256]);
        }
        long t1 = System.currentTimeMillis();
        System.err.println("Time to insert " + (DbSSLSessionCache.MAX_CACHE_SIZE + 2) + " " + (t1 - t0));
        Cursor query = helper.getReadableDatabase().query(DbSSLSessionCache.SSL_CACHE_TABLE, new String[] { "hostport", "session" }, null, null, null, null, null);
        int cnt = query.getCount();
        assertTrue(query.moveToFirst());
        String hostPort = query.getString(0);
        assertEquals("test.host.com:2", hostPort);
        while (query.moveToNext()) {
            hostPort = query.getString(0);
            String session = query.getString(1);
        }
        long t2 = System.currentTimeMillis();
        System.err.println("Time to load " + cnt + " " + (t2 - t1));
        query.close();
    }

    private void makeRequestInNewContext(String url) throws IOException {
        GoogleHttpClient client = new GoogleHttpClient(getContext(), "Test", false);
        try {
            HttpGet method = new HttpGet(url);
            HttpResponse response = client.execute(method);
        } finally {
            client.close();
        }
    }

    private static class MockSession implements SSLSession {

        public String getPeerHost() {
            throw new UnsupportedOperationException();
        }

        public int getPeerPort() {
            throw new UnsupportedOperationException();
        }

        public int getApplicationBufferSize() {
            throw new UnsupportedOperationException();
        }

        public String getCipherSuite() {
            throw new UnsupportedOperationException();
        }

        public long getCreationTime() {
            throw new UnsupportedOperationException();
        }

        public byte[] getId() {
            throw new UnsupportedOperationException();
        }

        public long getLastAccessedTime() {
            throw new UnsupportedOperationException();
        }

        public Certificate[] getLocalCertificates() {
            throw new UnsupportedOperationException();
        }

        public Principal getLocalPrincipal() {
            throw new UnsupportedOperationException();
        }

        public int getPacketBufferSize() {
            throw new UnsupportedOperationException();
        }

        public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
            throw new UnsupportedOperationException();
        }

        public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
            throw new UnsupportedOperationException();
        }

        public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
            throw new UnsupportedOperationException();
        }

        public String getProtocol() {
            throw new UnsupportedOperationException();
        }

        public SSLSessionContext getSessionContext() {
            throw new UnsupportedOperationException();
        }

        public Object getValue(String name) {
            throw new UnsupportedOperationException();
        }

        public String[] getValueNames() {
            throw new UnsupportedOperationException();
        }

        public void invalidate() {
            throw new UnsupportedOperationException();
        }

        public boolean isValid() {
            throw new UnsupportedOperationException();
        }

        public void putValue(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        public void removeValue(String name) {
            throw new UnsupportedOperationException();
        }
    }
}
