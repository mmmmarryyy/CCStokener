package org.jsemantic.services.httpclient;

import org.apache.http.HttpResponse;
import org.jsemantic.services.httpservice.client.HttpTestClient;
import org.jsemantic.services.httpservice.client.HttpTestClientImpl;
import junit.framework.TestCase;

public class TestHttpClient extends TestCase {

    private HttpTestClient client = null;

    protected void setUp() throws Exception {
        super.setUp();
        client = new HttpTestClientImpl();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        client.dispose();
    }

    public void test() {
        HttpResponse response = client.executeHttpGetRequest("http://www.google.com");
        super.assertNotNull(response);
        super.assertNotNull(response.getEntity());
    }

    public void testError() {
        try {
            HttpResponse response = client.executeHttpGetRequest("http://www.");
        } catch (Throwable e) {
            return;
        }
        super.fail();
    }
}
