package com.googlecode.mycontainer.commons.servlet;

import static org.junit.Assert.assertEquals;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.junit.Test;
import com.googlecode.mycontainer.commons.httpclient.RequestMethod;
import com.googlecode.mycontainer.commons.httpclient.WebClient;
import com.googlecode.mycontainer.commons.httpclient.WebRequest;
import com.googlecode.mycontainer.commons.httpclient.WebResponse;
import com.googlecode.mycontainer.commons.io.IOUtil;
import com.googlecode.mycontainer.commons.rhino.DefaultRhinoBoxBuilder;

public class JSEngineFilterTest extends AbstractTestCase {

    private String setValue(String url, String value) {
        WebClient client = createClient();
        WebRequest request = client.createRequest(RequestMethod.GET);
        request.setUri(url);
        request.addParameter("value", value);
        WebResponse response = request.invoke();
        try {
            assertEquals(200, response.getCode());
            return response.getContentAsString();
        } finally {
            response.close();
        }
    }

    private String getValue(String url) {
        WebClient client = createClient();
        WebRequest request = client.createRequest(RequestMethod.GET);
        request.setUri(url);
        WebResponse response = request.invoke();
        try {
            assertEquals(200, response.getCode());
            return response.getContentAsString();
        } finally {
            response.close();
        }
    }

    @Test
    public void testContext() {
        assertEquals("null", getValue("test1/getValue.txt"));
        assertEquals("2", setValue("test1/setValue.txt", "2"));
        assertEquals("2", getValue("test1/getValue.txt"));
        assertEquals("nulla", getValue("test2/getValue.txt"));
        assertEquals("3a", setValue("test2/setValue.txt", "3"));
        assertEquals("3a", getValue("test2/getValue.txt"));
        assertEquals("2", getValue("test1/getValue.txt"));
        assertEquals("3a", getValue("test2/getValue.txt"));
    }

    @Test
    public void testContextTwice() {
        DefaultRhinoBoxBuilder.instance().reset();
        testContext();
        assertEquals("2", getValue("test1/getValue.txt"));
        DefaultRhinoBoxBuilder.instance().reset();
        assertEquals("null", getValue("test1/getValue.txt"));
        testContext();
    }

    @Test
    public void testPut() throws Exception {
        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager();
        DefaultHttpClient client = new DefaultHttpClient(manager);
        HttpPut put = new HttpPut("http://localhost:8380/jseng/test3");
        put.setEntity(new StringEntity("mytest"));
        HttpResponse resp = client.execute(put);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("just ok", resp.getStatusLine().getReasonPhrase());
        assertEquals("xxx", resp.getHeaders("X-Test")[0].getValue());
        assertEquals("text/plain", resp.getHeaders("Content-Type")[0].getValue());
        assertEquals("mytest", download(resp));
    }

    public String download(HttpResponse resp) throws Exception {
        InputStreamReader c = new InputStreamReader(resp.getEntity().getContent());
        try {
            StringBuilder ret = new StringBuilder();
            IOUtil.copy(c, ret);
            return ret.toString();
        } finally {
            c.close();
        }
    }
}
