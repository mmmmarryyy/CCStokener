package org.apache.http.impl.conn;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.localserver.ServerTestBase;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.util.EntityUtils;

/**
 * This is more a test for the {@link LocalTestServer LocalTestServer}
 * than anything else.
 */
public class TestLocalServer extends ServerTestBase {

    public TestLocalServer(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestLocalServer.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestLocalServer.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testEcho() throws Exception {
        final String message = "Hello, world!";
        final String charset = "UTF-8";
        final HttpHost target = getServerHttp();
        HttpPost request = new HttpPost("/echo/");
        request.setHeader("Host", target.getHostName());
        request.setEntity(new StringEntity(message, charset));
        HttpClientConnection conn = connectTo(target);
        httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);
        httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, request);
        request.setParams(new DefaultedHttpParams(request.getParams(), defaultParams));
        httpExecutor.preProcess(request, httpProcessor, httpContext);
        HttpResponse response = httpExecutor.execute(request, conn, httpContext);
        response.setParams(new DefaultedHttpParams(response.getParams(), defaultParams));
        httpExecutor.postProcess(response, httpProcessor, httpContext);
        assertEquals("wrong status in response", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        String received = EntityUtils.toString(response.getEntity());
        conn.close();
        assertEquals("wrong echo", message, received);
    }

    public void testRandom() throws Exception {
        final HttpHost target = getServerHttp();
        int[] sizes = new int[] { 10, 2048, 4100, 0, -1 };
        for (int i = 0; i < sizes.length; i++) {
            String uri = "/random/" + sizes[i];
            if (sizes[i] < 0) uri += "/";
            HttpGet request = new HttpGet(uri);
            HttpClientConnection conn = connectTo(target);
            httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);
            httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, request);
            request.setParams(new DefaultedHttpParams(request.getParams(), defaultParams));
            httpExecutor.preProcess(request, httpProcessor, httpContext);
            HttpResponse response = httpExecutor.execute(request, conn, httpContext);
            response.setParams(new DefaultedHttpParams(response.getParams(), defaultParams));
            httpExecutor.postProcess(response, httpProcessor, httpContext);
            assertEquals("(" + sizes[i] + ") wrong status in response", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            byte[] data = EntityUtils.toByteArray(response.getEntity());
            if (sizes[i] >= 0) assertEquals("(" + sizes[i] + ") wrong length of response", sizes[i], data.length);
            conn.close();
        }
    }
}
