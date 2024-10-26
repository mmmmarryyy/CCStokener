package test.net.sf.karatasi.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.japi.net.rest.server.RestServer;
import net.sf.japi.net.rest.server.RestServerFactory;
import net.sf.karatasi.User;
import net.sf.karatasi.UserAuthenticator;
import net.sf.karatasi.librarian.DatabaseLibrarian;
import net.sf.karatasi.server.SyncServerSessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import test.net.sf.karatasi.TestHelpers;

@SuppressWarnings({ "HardcodedFileSeparator", "HardcodedLineSeparator" })
public class SyncServerURLTest {

    /** The default test port for the SyncServer. */
    private static final int DEFAULT_TEST_PORT = 12347;

    /** The testling, our SyncServer with which we will communicate. */
    private RestServer syncServer;

    /** The port on which the server is running. */
    private static int testPort;

    /** The UserAuthenticator. */
    private static UserAuthenticator deviceAuthenticator;

    /** The test directory. */
    private File testDir;

    /** The DatabaseLibrarian. */
    private DatabaseLibrarian databaseLibrarian;

    /** Creates the UserAuthenticator. */
    @BeforeClass
    public static void createUserAuthenticator() {
        deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User("Aladdin", "open sesame"));
    }

    /** Initializes {@link #testPort}.
     * The test port is intentionally only set once.
     * After a test, the port must be free again and reusable for the next test.
     * Reusing the test port makes sure that this theory is verified.
     * @throws IOException (unexpected)
     */
    @BeforeClass
    public static void initTestPort() throws IOException {
        testPort = TestHelpers.findUnboundSocket(DEFAULT_TEST_PORT);
    }

    /** Creates the test directory, a Database Librarian and starts the server.
     * @throws InterruptedException (unexpected)
     * @throws IOException (unexpected)
     */
    @Before
    public void startServer() throws InterruptedException, IOException {
        testDir = TestHelpers.prepareDataDirectory();
        testDir.deleteOnExit();
        databaseLibrarian = new DatabaseLibrarian(testDir);
        syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(databaseLibrarian, deviceAuthenticator));
        syncServer.start();
        assertNotNull("Server runnable has to be allocated.", syncServer);
        assertTrue("Server should be running now.", syncServer.isRunning());
    }

    /** Stops the server.
     * @throws InterruptedException (unexpected)
     */
    @After
    public void stopServer() throws InterruptedException {
        testDir.delete();
        syncServer.stop();
        assertFalse("Server must not be running now.", syncServer.isRunning());
    }

    /** Tests that server can be started and stopped.
     */
    @SuppressWarnings({ "JUnitTestMethodWithNoAssertions" })
    @Test
    public void testServerStartStop() {
    }

    /** Tests that connecting to the server works.
     * @throws IOException (unexpected)
     */
    @SuppressWarnings({ "JUnitTestMethodWithNoAssertions" })
    @Test
    public void testSocket() throws IOException {
        @SuppressWarnings({ "SocketOpenedButNotSafelyClosed" }) final Socket socket = new Socket("127.0.0.1", testPort);
        socket.close();
    }

    /** Tests that retrieving a non-existent resource without authorization properly returns 401.
     * @throws IOException (unexpected)
     */
    @Test
    public void testUnauthorizedFileNotFound() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/thisdoesnotexist");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        assertEquals("Expecting request to be unauthorized.", HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
        final InputStream err = con.getErrorStream();
        assertNotNull("Expecting an error stream.", err);
    }

    /** Tests that retrieving a non-existent resource with authorization properly returns 404.
     * @throws IOException (unexpected)
     */
    @Test
    public void testAuthorizedFileNotFound() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/thisdoesnotexist");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty("WWW-Authenticate", "Basic realm=\"karatasi\"");
        assertEquals("Expecting resource to not exist.", HttpURLConnection.HTTP_NOT_FOUND, con.getResponseCode());
        final InputStream err = con.getErrorStream();
        assertNotNull("Expecting an error stream.", err);
    }

    /** Tests that retrieving the mirror without authorization properly returns 401.
     * @throws IOException (unexpected)
     */
    @Test
    public void testUnauthorizedMirror() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/mirror?version=5&direction=just+right");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("testline1", "1234567890");
        assertEquals("Expecting request to be unauthorized.", HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
        final InputStream err = con.getErrorStream();
        assertNotNull("Expecting an error stream.", err);
    }

    /** Tests that retrieving the mirror with authorization properly returns 200 and the mirror.
     * @throws IOException (unexpected)
     */
    @Test
    public void testAuthorizedMirror() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/mirror?version=5&direction=just+right");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty("WWW-Authenticate", "Basic realm=\"karatasi\"");
        con.setRequestProperty("testline1", "1234567890");
        assertEquals("Expecting resource to exist.", HttpURLConnection.HTTP_OK, con.getResponseCode());
        assertTrue("mirror responds with Content-Type text/plain.", con.getContentType().startsWith("text/plain"));
        assertNull("The server does not use any special encoding.", con.getContentEncoding());
        int bytesRemaining = con.getContentLength();
        final InputStream err = con.getErrorStream();
        assertNull("Expecting no error stream.", err);
        final InputStream in = con.getInputStream();
        final byte[] buf = new byte[bytesRemaining];
        for (int bytesRead; bytesRemaining > 0 && (bytesRead = in.read(buf, buf.length - bytesRemaining, bytesRemaining)) != -1; bytesRemaining -= bytesRead) {
        }
        assertEquals("Expecting server to send not fewer bytes as indicated.", 0, bytesRemaining);
        final String testResult = new String(buf, "ASCII");
        assertContains("Response has to contain this line.", testResult, "url direction::just right\r\n");
        assertContains("Response has to contain this line.", testResult, "url version::5\r\n");
        assertContains("Response has to contain this line.", testResult, "body Authorization::Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n");
        assertContains("Response has to contain this line.", testResult, "body WWW-Authenticate::Basic realm=\"karatasi\"\r\n");
        assertContains("Response has to contain this line.", testResult, "body testline1::1234567890\r\n");
    }

    /** Tests that retrieving the mirror with authorization and HEAD properly returns 200 and no mirror.
     * @throws IOException (unexpected)
     */
    @Test
    public void testAuthorizedMirrorHEAD() throws IOException {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/mirror?version=5&direction=just+right");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("HEAD");
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty("WWW-Authenticate", "Basic realm=\"karatasi\"");
        con.setRequestProperty("testline1", "1234567890");
        assertEquals("Expecting resource to exist.", HttpURLConnection.HTTP_OK, con.getResponseCode());
        assertTrue("mirror responds with Content-Type text/plain.", con.getContentType().startsWith("text/plain"));
        assertNull("The server does not use any special encoding.", con.getContentEncoding());
        int bytesRemaining = con.getContentLength();
        final int originalBytesRemaining = bytesRemaining;
        final InputStream err = con.getErrorStream();
        assertNull("Expecting no error stream.", err);
        final InputStream in = con.getInputStream();
        final byte[] buf = new byte[bytesRemaining];
        for (int bytesRead; bytesRemaining > 0 && (bytesRead = in.read(buf, buf.length - bytesRemaining, bytesRemaining)) != -1; bytesRemaining -= bytesRead) {
            fail("Expecting server to send no data.");
        }
        assertEquals("Expecting server to send not fewer bytes as indicated.", originalBytesRemaining, bytesRemaining);
    }

    /** Tests that sending a request with an unsupported method is answered with 405 Method Not Allowed.
     * @throws Exception (unexpected)
     */
    @Test
    public void testUnsupportedMethod() throws Exception {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/list");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestMethod("PUT");
        assertEquals("Expecting method to be unsupported.", HttpURLConnection.HTTP_BAD_METHOD, con.getResponseCode());
        assertEquals("Expecting supported method GET to be listed in Allow header.", "GET", con.getHeaderField("Allow"));
    }

    /** Tests that sending an upload request (PUT request to /databases/test1) without a Content-Length header is answered with 411 Length Required.
     * @throws Exception (unexpected)
     */
    @Test
    public void testUploadContentLengthRequired() throws Exception {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/databases/test1");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        assertEquals("Expecting missing Content-Length to be replied with 411 Length Required.", HttpURLConnection.HTTP_LENGTH_REQUIRED, con.getResponseCode());
    }

    /** Tests that sending a request with an unsupported Content-* header is answered with 501 Not Implemented.
     * @throws Exception (unexpected)
     */
    @Test
    public void testUploadContentNotImplemented() throws Exception {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/databases/test1");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty("Content-Length", "12");
        con.setRequestProperty("Content-Foo", "foobar");
        assertEquals("Expecting Content-Foo header to be not implemenented.", HttpURLConnection.HTTP_NOT_IMPLEMENTED, con.getResponseCode());
    }

    /** Tests that retrieving the list without authorization properly returns 401.
     * @throws IOException (unexpected)
     */
    @Test
    public void testUnauthorizedList() throws Exception {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/list?version=5");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        assertEquals("Expecting request to be unauthorized.", HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
        final InputStream err = con.getErrorStream();
        assertNotNull("Expecting an error stream.", err);
    }

    /** Tests that retrieving the list with authorization properly returns the expected list.
     * @throws IOException (unexpected)
     */
    @Test
    public void testAuthorizedList() throws Exception {
        final URL url = new URL("http://127.0.0.1:" + testPort + "/list?version=5");
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        con.setRequestProperty("WWW-Authenticate", "Basic realm=\"karatasi\"");
        assertEquals("Expecting resource to exist.", HttpURLConnection.HTTP_OK, con.getResponseCode());
        assertTrue("mirror responds with Content-Type text/plain.", con.getContentType().matches("^(text|application)/xml.*"));
        assertNull("The server does not use any special encoding.", con.getContentEncoding());
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);
        dbf.setExpandEntityReferences(true);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(false);
        dbf.setValidating(true);
        dbf.setXIncludeAware(true);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver() {

            @Nullable
            public InputSource resolveEntity(@Nullable final String publicId, final String systemId) throws SAXException, IOException {
                if ("-//Karatasi//DTD Karatasi DB List 1.0//EN".equals(publicId) || "http://www.karatasi.org/DTD/karatasiDbList1.0.dtd".equals(systemId)) {
                    return new InputSource(new FileInputStream("src/prj/resources/karatasiDbList1.0.dtd"));
                }
                return null;
            }
        });
        db.parse(con.getInputStream(), url.toString());
    }

    /** Asserts that a String contains another String.
     * @param msg Failure message.
     * @param text String to test.
     * @param substring Substring to test.
     */
    public static void assertContains(final String msg, @NotNull final String text, final String substring) {
        if (text.indexOf(substring) < 0) {
            fail(msg + " (" + substring + ')');
        }
    }
}
