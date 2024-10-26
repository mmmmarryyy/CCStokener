package com.ssg.tools.jsonxml.common.tools;

import com.ssg.tools.jsonxml.common.tools.URLResolver.CacheingURLResolver;
import com.ssg.tools.jsonxml.json.schema.JSONSchemaException;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;

/**
 *
 * @author ssg
 */
public class URLResolverTest extends TestCase {

    public URLResolverTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of resolveURL method, of class JSONSchemaURIResolver.
     */
    public void testResolveURL() throws Exception {
        System.out.println("resolveURL");
        File bigFile = new File("./src/test/java/big.json");
        File smallFile = new File("./src/test/java/sample1.json");
        Object[] urls = new Object[] { "http://json-schema.org/schema", "http://json-schema.org/hyper-schema", "http://json-schema.org/json-ref", "http://json-schema.org/interfaces", "http://json-schema.org/geo", "http://json-schema.org/card", "http://json-schema.org/calendar", "http://json-schema.org/address", bigFile };
        JSONSchemaURIResolverImpl uriResolver = new JSONSchemaURIResolverImpl();
        JSONSchemaURIResolverImpl uriResolver2 = new JSONSchemaURIResolverImpl();
        try {
            InputStream is = new URL((String) urls[0]).openStream();
            is.close();
        } catch (ConnectException cex) {
            for (int i = 2; i < urls.length; i++) {
                if (urls[i] instanceof String) {
                    String url = (String) urls[i];
                    uriResolver.register(new URL(url), new File("./src/test/java/" + url.replace(":", "_").replace("/", "_") + ".schema.json"));
                } else if (urls[i] instanceof File) {
                    uriResolver.register(((File) urls[i]).toURI().toURL(), urls[i]);
                }
            }
        } catch (Exception ex) {
        }
        for (int i = 2; i < urls.length; i++) {
            if (urls[i] instanceof String) {
                String url = (String) urls[i];
                uriResolver2.register(new URL(url), new File("./src/test/java/" + url.replace(":", "_").replace("/", "_") + ".schema.json"));
            } else if (urls[i] instanceof File) {
                uriResolver2.register(((File) urls[i]).toURI().toURL(), urls[i]);
            }
        }
        for (Object source : urls) {
            try {
                if (source instanceof String) {
                    StreamSource ss = uriResolver.resolveURI(new URI((String) source), null);
                    assertNotNull(ss.getReader());
                    assertNull(ss.getInputStream());
                    ss.getReader().close();
                }
            } catch (Throwable th) {
                fail("Unexpected problem: " + source + ". Error: " + th);
            }
        }
        for (Object source : urls) {
            try {
                if (source instanceof String) {
                    StreamSource ss = uriResolver.resolveURL(new URL((String) source), null);
                    assertNotNull(ss.getReader());
                    assertNull(ss.getInputStream());
                    ss.getReader().close();
                }
            } catch (Throwable th) {
                fail("Unexpected problem: " + source + ". Error: " + th);
            }
        }
        for (Object source : urls) {
            try {
                if (source instanceof String) {
                    StreamSource ss = uriResolver2.resolveURI(new URI((String) source), null);
                    assertNotNull(ss.getReader());
                    assertNull(ss.getInputStream());
                    ss.getReader().close();
                    assertTrue((new URL((String) source)).equals(uriResolver2.lastURL));
                    assertFalse((new URL((String) source)).equals(uriResolver2.lastMapped));
                }
            } catch (Throwable th) {
                fail("Unexpected problem: " + source + ". Error: " + th);
            }
        }
        for (Object source : urls) {
            try {
                if (source instanceof String) {
                    StreamSource ss = uriResolver2.resolveURL(new URL((String) source), null);
                    assertNotNull(ss.getReader());
                    assertNull(ss.getInputStream());
                    ss.getReader().close();
                    assertTrue((new URL((String) source)).equals(uriResolver2.lastURL));
                    assertFalse((new URL((String) source)).equals(uriResolver2.lastMapped));
                }
            } catch (Throwable th) {
                fail("Unexpected problem: " + source + ". Error: " + th);
            }
        }
        uriResolver2.register(new URL("ftp://localhost/1"), bigFile);
        uriResolver2.register(new URL("ftp://localhost/2"), smallFile);
        uriResolver2.register(new URL("ftp://localhost/2#2"), smallFile);
        try {
            Reader r1 = uriResolver2.resolveURL(new URL("ftp://localhost/2"), null).getReader();
            Reader r2 = uriResolver2.resolveURL(new URL("ftp://localhost/2#2"), null).getReader();
            int ch = 0;
            while ((ch = r1.read()) != -1) {
                assertEquals(ch, r2.read());
            }
            assertEquals(-1, r2.read());
        } catch (Throwable th) {
            fail("Failed while testing identity of same mapped files. Error: " + th);
        }
        uriResolver2.register(new URL("ftp://localhost/1"), null);
        uriResolver2.register(new URL("ftp://localhost/2"), null);
        uriResolver2.register(new URL("ftp://localhost/2#2"), null);
        uriResolver2.register(new URL("ftp://localhost/1"), bigFile, true);
        uriResolver2.register(new URL("ftp://localhost/2"), smallFile, true);
        uriResolver2.register(new URL("ftp://localhost/2#2"), smallFile, true);
        uriResolver2.unregister(new URL("ftp://localhost/1"), true);
        uriResolver2.unregister(new URL("ftp://localhost/2"), true);
        uriResolver2.unregister(new URL("ftp://localhost/2#2"), true);
    }

    /**
     * Test of resolveURI method, of class JSONSchemaURIResolver.
     */
    public void testResolveURI() throws Exception {
        System.out.println("resolveURI");
        testResolveURL();
    }

    /**
     * Test of getEncoding method, of class JSONSchemaURIResolver.
     */
    public void testGetEncoding() throws Exception {
        System.out.println("getEncoding");
        URLResolver instance = new JSONSchemaURIResolverImpl();
        assertEquals("UTF-8", instance.getEncoding(null));
        assertEquals("UTF-8", instance.getEncoding("UTF-8"));
        assertEquals("AAA", instance.getEncoding("AAA"));
    }

    /**
     * Test of toStreamSource method, of class JSONSchemaURIResolver.
     */
    public void testToStreamSource_Object_String() throws Exception {
        System.out.println("toStreamSource");
        URLResolver uriResolver = new JSONSchemaURIResolverImpl();
        File bigFile = new File("./src/test/java/big.json");
        File smallFile = new File("./src/test/java/sample1.json");
        for (Object o : new Object[] { "http://json-schema.org/schema", "http://json-schema.org/draft-03/schema#", "http://json-schema.org/hyper-schema", "http://json-schema.org/draft-03/hyper-schema#", bigFile, smallFile, bigFile.toURI(), smallFile.toURI() }) {
            Reader r = null;
            InputStream is = null;
            try {
                r = uriResolver.toStreamSource(o, null).getReader();
                assertNotNull(r);
                is = uriResolver.toStreamSource(o, null, false).getInputStream();
                assertNotNull(is);
            } finally {
                if (r != null) {
                    r.close();
                }
                if (is != null) {
                    is.close();
                }
            }
        }
        for (Object o : new Object[] { "test://json-schema.org/schema", "urn:object:json-schema.org/draft-03/schema#", new File(bigFile.getAbsolutePath() + ".zzz"), new File(bigFile.getAbsolutePath() + ".zzz").toURI() }) {
            Reader r = null;
            InputStream is = null;
            try {
                r = uriResolver.toStreamSource(o, null).getReader();
                is = uriResolver.toStreamSource(o, null, false).getInputStream();
                fail("MUST throw exception for invalid source.");
            } catch (Throwable th) {
            } finally {
                if (r != null) {
                    r.close();
                }
                if (is != null) {
                    is.close();
                }
            }
        }
    }

    /**
     * Test of toStreamSource method, of class JSONSchemaURIResolver.
     */
    public void testToStreamSource_3args() throws Exception {
        System.out.println("toStreamSource");
    }

    /**
     * Based on default implementation and used to track substitution/resolution process.
     */
    public class JSONSchemaURIResolverImpl extends CacheingURLResolver {

        URL lastURL;

        String lastEncoding1;

        String lastEncoding2;

        Object lastMapped;

        Boolean lastAsReader;

        public JSONSchemaURIResolverImpl() throws JSONSchemaException {
        }

        @Override
        public StreamSource resolveURL(URL url, String encoding) throws JSONSchemaException {
            lastURL = url;
            lastEncoding1 = encoding;
            lastMapped = null;
            lastAsReader = null;
            return super.resolveURL(url, encoding);
        }

        @Override
        public StreamSource toStreamSource(Object source, String encoding, boolean asReader) throws JSONSchemaException {
            lastMapped = source;
            lastEncoding2 = encoding;
            lastAsReader = asReader;
            return super.toStreamSource(source, encoding, asReader);
        }
    }
}
