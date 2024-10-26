package com.manning.junitbook.ch06.stubs;

import static org.junit.Assert.assertEquals;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test case that tests the WebClient class by stubbing the HTTP connection.
 * 
 * @version $Id$
 */
public class TestWebClient1 {

    @BeforeClass
    public static void setUp() {
        TestWebClient1 t = new TestWebClient1();
        URL.setURLStreamHandlerFactory(t.new StubStreamHandlerFactory());
    }

    private class StubStreamHandlerFactory implements URLStreamHandlerFactory {

        public URLStreamHandler createURLStreamHandler(String protocol) {
            return new StubHttpURLStreamHandler();
        }
    }

    private class StubHttpURLStreamHandler extends URLStreamHandler {

        protected URLConnection openConnection(URL url) throws IOException {
            return new StubHttpURLConnection(url);
        }
    }

    @Test
    public void testGetContentOk() throws Exception {
        WebClient client = new WebClient();
        String result = client.getContent(new URL("http://localhost/"));
        assertEquals("It works", result);
    }
}
