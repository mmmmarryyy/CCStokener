package jacky.lanlan.song.resource;

import static org.junit.Assert.*;
import jacky.lanlan.song.resource.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.junit.*;

public class ResourceUtilsTest {

    @Before
    public void inti() {
    }

    @Test
    public void testGetFile_String() throws Exception {
        File log4j = ResourceUtils.getFile("classpath:log4j.properties");
        assertNotNull(log4j);
        assertTrue(log4j.exists());
        assertTrue(log4j.canRead());
        assertEquals("log4j.properties", log4j.getName());
    }

    @Test
    public void testIsJarURL() throws Exception {
        assertTrue(ResourceUtils.isJarURL(new URL("jar:file:myjar.jar!/mypath")));
        assertTrue(ResourceUtils.isJarURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
        assertTrue(ResourceUtils.isJarURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
        assertFalse(ResourceUtils.isJarURL(new URL("file:myjar.jar")));
        assertFalse(ResourceUtils.isJarURL(new URL("http:myserver/myjar.jar")));
    }

    @Test
    public void testExtractJarFileURL() throws Exception {
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/mypath")));
        assertEquals(new URL("file:/myjar.jar"), ResourceUtils.extractJarFileURL(new URL(null, "jar:myjar.jar!/mypath", new DummyURLStreamHandler())));
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/mypath", new DummyURLStreamHandler())));
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL("jar:file:myjar.jar!/")));
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL(null, "zip:file:myjar.jar!/", new DummyURLStreamHandler())));
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL(null, "wsjar:file:myjar.jar!/", new DummyURLStreamHandler())));
        assertEquals(new URL("file:myjar.jar"), ResourceUtils.extractJarFileURL(new URL("file:myjar.jar")));
    }

    /**
	 * Dummy URLStreamHandler that's just specified to suppress the standard
	 * <code>java.net.URL</code> URLStreamHandler lookup, to be able to
	 * use the standard URL class for parsing "rmi:..." URLs.
	 */
    private static class DummyURLStreamHandler extends URLStreamHandler {

        @SuppressWarnings("unused")
        protected URLConnection openConnection(URL url) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    @After
    public void destory() {
    }
}
