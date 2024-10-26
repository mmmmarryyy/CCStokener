package org.apache.http.impl.client;

import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.BasicServerTestBase;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

/**
 *  Simple tests for {@link RequestWrapper}.
 * 
 */
public class TestRequestWrapper extends BasicServerTestBase {

    public TestRequestWrapper(final String testName) {
        super(testName);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestRequestWrapper.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestRequestWrapper.class);
    }

    @Override
    protected void setUp() throws Exception {
        localServer = new LocalTestServer(null, null);
        localServer.registerDefaultHandlers();
        localServer.start();
    }

    private class SimpleService implements HttpRequestHandler {

        public SimpleService() {
            super();
        }

        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity entity = new StringEntity("Whatever");
            response.setEntity(entity);
        }
    }

    public void testRequestURIRewriting() throws Exception {
        int port = this.localServer.getServicePort();
        this.localServer.register("*", new SimpleService());
        DefaultHttpClient client = new DefaultHttpClient();
        HttpContext context = new BasicHttpContext();
        String s = "http://localhost:" + port + "/path";
        HttpGet httpget = new HttpGet(s);
        HttpResponse response = client.execute(getServerHttp(), httpget, context);
        HttpEntity e = response.getEntity();
        if (e != null) {
            e.consumeContent();
        }
        HttpRequest reqWrapper = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertTrue(reqWrapper instanceof RequestWrapper);
        assertEquals("/path", reqWrapper.getRequestLine().getUri());
    }

    public void testRequestURIRewritingEmptyPath() throws Exception {
        int port = this.localServer.getServicePort();
        this.localServer.register("*", new SimpleService());
        DefaultHttpClient client = new DefaultHttpClient();
        HttpContext context = new BasicHttpContext();
        String s = "http://localhost:" + port;
        HttpGet httpget = new HttpGet(s);
        HttpResponse response = client.execute(getServerHttp(), httpget, context);
        HttpEntity e = response.getEntity();
        if (e != null) {
            e.consumeContent();
        }
        HttpRequest reqWrapper = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertTrue(reqWrapper instanceof RequestWrapper);
        assertEquals("/", reqWrapper.getRequestLine().getUri());
    }
}
