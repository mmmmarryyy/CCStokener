package org.apache.http.conn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.localserver.ServerTestBase;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

public class TestConnectionAutoRelease extends ServerTestBase {

    public TestConnectionAutoRelease(String testName) {
        super(testName);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestConnectionAutoRelease.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestConnectionAutoRelease.class);
    }

    public ThreadSafeClientConnManager createTSCCM(HttpParams params, SchemeRegistry schreg) {
        if (params == null) params = defaultParams;
        if (schreg == null) schreg = supportedSchemes;
        return new ThreadSafeClientConnManager(params, schreg);
    }

    public void testReleaseOnEntityConsumeContent() throws Exception {
        HttpParams params = defaultParams.copy();
        ConnManagerParams.setMaxTotalConnections(params, 1);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(1));
        ThreadSafeClientConnManager mgr = createTSCCM(params, null);
        assertEquals(0, mgr.getConnectionsInPool());
        DefaultHttpClient client = new DefaultHttpClient(mgr, params);
        HttpGet httpget = new HttpGet("/random/20000");
        HttpHost target = getServerHttp();
        HttpResponse response = client.execute(target, httpget);
        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }
        HttpEntity e = response.getEntity();
        assertNotNull(e);
        e.consumeContent();
        assertEquals(1, mgr.getConnectionsInPool());
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);
        mgr.releaseConnection(conn, -1, null);
        mgr.shutdown();
    }

    public void testReleaseOnEntityWriteTo() throws Exception {
        HttpParams params = defaultParams.copy();
        ConnManagerParams.setMaxTotalConnections(params, 1);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(1));
        ThreadSafeClientConnManager mgr = createTSCCM(params, null);
        assertEquals(0, mgr.getConnectionsInPool());
        DefaultHttpClient client = new DefaultHttpClient(mgr, params);
        HttpGet httpget = new HttpGet("/random/20000");
        HttpHost target = getServerHttp();
        HttpResponse response = client.execute(target, httpget);
        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }
        HttpEntity e = response.getEntity();
        assertNotNull(e);
        ByteArrayOutputStream outsteam = new ByteArrayOutputStream();
        e.writeTo(outsteam);
        assertEquals(1, mgr.getConnectionsInPool());
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);
        mgr.releaseConnection(conn, -1, null);
        mgr.shutdown();
    }

    public void testReleaseOnAbort() throws Exception {
        HttpParams params = defaultParams.copy();
        ConnManagerParams.setMaxTotalConnections(params, 1);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(1));
        ThreadSafeClientConnManager mgr = createTSCCM(params, null);
        assertEquals(0, mgr.getConnectionsInPool());
        DefaultHttpClient client = new DefaultHttpClient(mgr, params);
        HttpGet httpget = new HttpGet("/random/20000");
        HttpHost target = getServerHttp();
        HttpResponse response = client.execute(target, httpget);
        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }
        HttpEntity e = response.getEntity();
        assertNotNull(e);
        httpget.abort();
        assertEquals(0, mgr.getConnectionsInPool());
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);
        mgr.releaseConnection(conn, -1, null);
        mgr.shutdown();
    }

    public void testReleaseOnIOException() throws Exception {
        localServer.register("/dropdead", new HttpRequestHandler() {

            public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {
                BasicHttpEntity entity = new BasicHttpEntity() {

                    @Override
                    public void writeTo(final OutputStream outstream) throws IOException {
                        byte[] tmp = new byte[5];
                        outstream.write(tmp);
                        outstream.flush();
                        DefaultHttpServerConnection conn = (DefaultHttpServerConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
                        try {
                            conn.sendResponseHeader(response);
                        } catch (HttpException ignore) {
                        }
                    }
                };
                entity.setChunked(true);
                response.setEntity(entity);
            }
        });
        HttpParams params = defaultParams.copy();
        ConnManagerParams.setMaxTotalConnections(params, 1);
        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(1));
        ThreadSafeClientConnManager mgr = createTSCCM(params, null);
        assertEquals(0, mgr.getConnectionsInPool());
        DefaultHttpClient client = new DefaultHttpClient(mgr, params);
        HttpGet httpget = new HttpGet("/dropdead");
        HttpHost target = getServerHttp();
        HttpResponse response = client.execute(target, httpget);
        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }
        HttpEntity e = response.getEntity();
        assertNotNull(e);
        try {
            EntityUtils.toByteArray(e);
            fail("MalformedChunkCodingException should have been thrown");
        } catch (MalformedChunkCodingException expected) {
        }
        assertEquals(0, mgr.getConnectionsInPool());
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);
        mgr.releaseConnection(conn, -1, null);
        mgr.shutdown();
    }
}
