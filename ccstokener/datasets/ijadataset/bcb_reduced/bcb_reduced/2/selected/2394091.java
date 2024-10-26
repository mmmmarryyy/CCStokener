package org.apache.ws.commons.tcpmon.core.engine;

import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.mortbay.jetty.Server;

public abstract class InterceptorTestBase extends TestCase {

    protected static final int INTERCEPTOR_PORT = 9001;

    protected static final int SERVER_PORT = 9002;

    private Server server;

    private InterceptorConfiguration config;

    private Interceptor interceptor;

    private HttpClient client;

    private String baseUri;

    @Override
    protected void setUp() throws Exception {
        server = TestUtil.createServer(SERVER_PORT);
        server.start();
        config = buildInterceptorConfiguration();
        interceptor = new Interceptor(config, new Dump(System.out));
        Thread.sleep(500);
        client = TestUtil.createClient(config);
        baseUri = TestUtil.getBaseUri(config, server);
    }

    @Override
    protected void tearDown() throws Exception {
        interceptor.halt();
        server.stop();
    }

    protected abstract InterceptorConfiguration buildInterceptorConfiguration();

    public void testGet() throws Exception {
        HttpGet request = new HttpGet(baseUri + "/test");
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("test", TestUtil.getResponseAsString(response));
    }

    public void testPost() throws Exception {
        HttpPost request = new HttpPost(baseUri + "/echo");
        request.setEntity(new StringEntity("test"));
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("test", TestUtil.getResponseAsString(response));
    }

    public void testPostWithGzip() throws Exception {
        HttpPost request = new HttpPost(baseUri + "/echo");
        request.setEntity(new GZIPCompressedEntity(new StringEntity("test")));
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("test", TestUtil.getResponseAsString(response));
    }

    public void testGetWithKeepAlive() throws Exception {
        HttpGet request = new HttpGet(baseUri + "/test");
        HttpResponse response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("test", TestUtil.getResponseAsString(response));
        response = client.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("test", TestUtil.getResponseAsString(response));
    }
}
