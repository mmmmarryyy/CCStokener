package org.apache.myfaces.trinidad.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.myfaces.trinidad.resource.ResourceLoader;
import junit.framework.TestCase;

public abstract class ResourceLoaderTestCase extends TestCase {

    public ResourceLoaderTestCase(String testName) {
        super(testName);
    }

    protected void doTestUnknownContentLength(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        long actualContentLength = conn.getContentLength();
        assertEquals("Invalid explicit content length", -1L, actualContentLength);
    }

    protected void doTestContentLength(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        long expectedContentLength = conn.getContentLength();
        if (expectedContentLength != -1) {
            byte[] buffer = new byte[2048];
            InputStream in = conn.getInputStream();
            try {
                long actualContentLength = 0;
                int length;
                while ((length = (in.read(buffer))) >= 0) {
                    actualContentLength += length;
                }
                assertEquals("Inaccurate explicit content length", expectedContentLength, actualContentLength);
            } finally {
                in.close();
            }
        }
    }

    public class LocalResourceLoader extends ResourceLoader {

        @Override
        protected URL findResource(String name) throws IOException {
            return getClass().getResource(name);
        }
    }
}
