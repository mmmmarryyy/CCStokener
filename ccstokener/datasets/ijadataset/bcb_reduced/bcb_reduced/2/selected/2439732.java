package jp.chiheisen.httpserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URL;
import jp.chiheisen.httpserveranywhere.HttpServerAnywhere;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HandlerTest {

    private static Server server;

    @BeforeClass
    public static void beforeClass() throws IOException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method method = HttpServerAnywhere.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(null);
        server = new Server(new InetSocketAddress(Inet4Address.getByName("localhost"), 80));
        server.start();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        server.stop();
    }

    @Test
    public void testOK() throws IOException {
        assertResponseCode("http://localhost/META-INF/license.txt", 200, "text/plain");
        assertResponseCode("http://localhost/META-INF/trayicon.png", 200, "image/png");
    }

    @Test
    public void testNotFound() throws IOException {
        assertResponseCode("http://localhost/META-INF/notfound.txt", 404, "text/html");
    }

    @Test
    public void testDirectory() throws IOException {
        for (String fileName : new String[] { "index.html", "index.htm" }) {
            File file = new File(fileName);
            file.createNewFile();
            assertResponseCode("http://localhost/", 200, "text/html");
            assertTrue(file.delete());
        }
    }

    @Test
    public void testDirectoryNotFound() throws IOException {
        assertResponseCode("http://localhost/META-INF/notfound.txt", 404, "text/html");
    }

    @Test
    public void testTraversal() throws IOException {
        assertResponseCode("http://localhost/../dummy.html", 404, "text/html");
    }

    private void assertResponseCode(String url, int response, String mime) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        assertEquals(response, con.getResponseCode());
        assertEquals(mime, con.getHeaderField("Content-Type"));
    }
}
