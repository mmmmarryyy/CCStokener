package test.net.sf.karatasi;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.jetbrains.annotations.NotNull;
import net.sf.karatasi.SyncServer;
import net.sf.karatasi.UserAuthenticator;
import net.sf.karatasi.User;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;

@SuppressWarnings({ "HardcodedFileSeparator", "HardcodedLineSeparator" })
public class SyncServerURLTest {

    /** The default test port for the SyncServer. */
    private static final int DEFAULT_TEST_PORT = 12347;

    /** The testling, our SyncServer with which we will communicate. */
    private SyncServer serverRunnable;

    /** The port on which the server is running. */
    private static int testPort;

    /** The UserAuthenticator. */
    private static UserAuthenticator deviceAuthenticator;

    /** Creates the UserAuthenticator. */
    @BeforeClass
    public static void createUserAuthenticator() {
        deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User(1, "Aladdin", "open sesame"));
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

    /** Starts the server.
     * @throws InterruptedException (unexpected)
     */
    @Before
    public void startServer() throws InterruptedException {
        serverRunnable = new SyncServer(testPort, 0, null, deviceAuthenticator);
        serverRunnable.start();
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
    }

    /** Stops the server.
     * @throws InterruptedException (unexpected)
     */
    @After
    public void stopServer() throws InterruptedException {
        serverRunnable.stop();
        Assert.assertFalse("Server must not be running now.", serverRunnable.isRunning());
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
        assertEquals("mirror responds with Content-Type text/plain.", "text/plain", con.getContentType());
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
        assertContains("Response has to contain this line.", testResult, "body authorization::Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n");
        assertContains("Response has to contain this line.", testResult, "body www-authenticate::Basic realm=\"karatasi\"\r\n");
        assertContains("Response has to contain this line.", testResult, "body testline1::1234567890\r\n");
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
