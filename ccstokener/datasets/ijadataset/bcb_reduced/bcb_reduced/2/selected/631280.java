package com.volantis.testtools.testurl;

import java.io.*;
import java.net.*;
import junit.framework.*;

public class TestURLConnectionTestCase extends TestCase {

    static {
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

            public URLStreamHandler createURLStreamHandler(String protocol) {
                if ("testurl".equals(protocol)) {
                    return new Handler();
                }
                return null;
            }
        });
    }

    public TestURLConnectionTestCase(String name) {
        super(name);
    }

    /**
     * Test acquiring an input stream from a valid URL 
     * @throws Exception
     */
    public void testGetInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream("xyzzy".getBytes());
        TestURLRegistry.register("xyzzy", is);
        URLConnection uc = new TestURLConnection(new URL("testurl:xyzzy"));
        assertNull(uc.getInputStream());
        uc.connect();
        assertSame(is, uc.getInputStream());
    }

    /**
     * Test that an error on connect URL can be created but does not error
     * if it is not accessed.
     * @throws Exception
     */
    public void testConstructor() throws Exception {
        TestURLRegistry.register();
        URL url = new URL("testurl:errorOnConnect");
        new TestURLConnection(url);
    }

    /**
     * Test that an exception is thrown if an error URL is created and 
     * we try to access it.
     * @throws Exception
     */
    public void testErrorOnConnect() throws Exception {
        TestURLRegistry.register();
        URL url = new URL("testurl:errorOnConnect");
        try {
            url.openStream();
            fail("Should have thrown exception");
        } catch (IOException ioex) {
            assertTrue(ioex.getMessage().indexOf("Simulated") > -1);
        }
    }

    /**
     * Test that exceptions are thrown if we try to access a URL with no content.
     * @throws Exception
     */
    public void testUnsupportedFeatures() throws Exception {
        TestURLRegistry.register("unsupp", new ByteArrayInputStream("".getBytes()));
        URL url = new URL("testurl:unsupp");
        URLConnection uc = url.openConnection();
        try {
            uc.getOutputStream();
            fail("Should have thrown exception");
        } catch (UnknownServiceException use) {
        }
        try {
            uc.getContent();
            fail("Should have thrown exception");
        } catch (Exception e) {
        }
        assertNull(uc.getHeaderField(0));
    }
}
