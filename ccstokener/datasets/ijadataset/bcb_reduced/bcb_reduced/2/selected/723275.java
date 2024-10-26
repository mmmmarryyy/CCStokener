package net.sf.openrds;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * junit tests for SimpleRmiHttpServer
 * @author Rodrigo
 */
public class SimpleRmiHttpServerTest extends OpenRDSTestCase {

    private static final int TEST_PORT = 45489;

    private static final String URL_BASE = "http://localhost:" + TEST_PORT + "/";

    /**
	 * junit
	 * @throws Exception on any error
	 */
    public void testGetClass() throws Exception {
        final SimpleRmiHttpServer server = new SimpleRmiHttpServer(TEST_PORT);
        try {
            final URL url = new URL(URL_BASE + "net/sf/openrds/IMainNode.class");
            assertInputStreamEquals(this.getClass().getResourceAsStream("IMainNode.class"), new BufferedInputStream(url.openStream()));
        } finally {
            server.stopRunning(true);
        }
    }

    /**
	 * junit
	 * @throws Exception on any error
	 */
    public void testGetMissingClass() throws Exception {
        final SimpleRmiHttpServer server = new SimpleRmiHttpServer(TEST_PORT);
        try {
            final URL url = new URL(URL_BASE + "net/sf/openrds/SomeInexistent.class");
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.connect();
            assertEquals("Wrong HTTP response code.", 404, con.getResponseCode());
        } finally {
            server.stopRunning(true);
        }
    }

    /**
	 * Asserts that two input streams are equal
	 * @param expected expected input stream
	 * @param result result input stream
	 * @throws IOException on any error
	 */
    private static void assertInputStreamEquals(InputStream expected, InputStream result) throws IOException {
        try {
            int b1, b2;
            for (int i = 1; ; i++) {
                b1 = expected.read();
                b2 = result.read();
                assertEquals("Wrong byte #" + i, b1, b2);
                if (b1 == -1) {
                    return;
                }
            }
        } finally {
            expected.close();
            result.close();
        }
    }
}
