package net.sf.alster.util;

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;

public class ClasspathURLConnectionTest extends TestCase {

    public void testRegisterFactory() throws Exception {
        try {
            new URL("classpath:/");
            fail("MalformedURLException expected");
        } catch (MalformedURLException e) {
            assertTrue(true);
        }
        ClasspathURLConnection.registerFactory();
        URL url = new URL("classpath:/dummy.txt");
        try {
            url.openStream();
            fail("IOException expected");
        } catch (IOException e) {
            assertTrue(true);
        }
        ClasspathURLConnection.registerFactory();
        url = new URL("classpath:/net/sf/alster/xsl/alster.xml");
        InputStream in = url.openStream();
        assertEquals('<', in.read());
        in.close();
    }
}
