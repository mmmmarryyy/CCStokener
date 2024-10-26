package photospace.web;

import java.io.*;
import java.net.*;
import javax.servlet.http.*;
import org.apache.commons.logging.*;
import junit.framework.*;
import photospace.vfs.FileSystem;
import photospace.vfs.*;

public class ImageServletTest extends TestCase {

    private static final Log log = LogFactory.getLog(ImageServletTest.class);

    File file;

    String url;

    public void setUp() throws Exception {
        FileSystem filesystem = new FileSystemImpl();
        filesystem.setRoot(new File("images/"));
        file = filesystem.getFile("/2003/gps.jpg");
        url = "http://localhost:8080/photospace/image/2003/gps.jpg";
        assertTrue(file.exists());
    }

    public void tearDown() throws Exception {
    }

    public void testDoGet() throws Exception {
        HttpURLConnection connection = getConnection(url);
        connection.connect();
        assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
        assertEquals("image/jpeg", connection.getContentType());
        assertEquals(file.lastModified(), connection.getLastModified(), 999);
        assertTrue(connection.getExpiration() > file.lastModified());
        assertEquals(connection.getContentLength(), getBytes(connection.getInputStream()).length);
    }

    public void testDoGetThumbnail() throws Exception {
        HttpURLConnection connection = getConnection(url + "?w=100&h=100");
        connection.connect();
        assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
        assertEquals("image/jpeg", connection.getContentType());
        assertEquals(file.lastModified(), connection.getLastModified(), 999);
        assertEquals(connection.getContentLength(), getBytes(connection.getInputStream()).length);
        assertTrue(file.length() > connection.getContentLength());
    }

    public void testIfModifiedSince() throws Exception {
        HttpURLConnection connection = getConnection(url);
        connection.setIfModifiedSince(file.lastModified());
        connection.connect();
        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, connection.getResponseCode());
    }

    private HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setUseCaches(false);
        return connection;
    }

    private byte[] getBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageServlet.write(in, out);
        return out.toByteArray();
    }
}
