package soapdust.urlhandler.dust;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import javax.net.ssl.HttpsURLConnection;
import junit.framework.TestCase;
import soapdust.Client;
import soapdust.urlhandler.test.Handler;

public class HandlerTest extends TestCase {

    private static final byte[] TEST_DATA = new byte[] { 0, 1, 2, 3 };

    private static final String TEST_FILE = System.getProperty("java.io.tmpdir") + File.separator + "test_file";

    private static final String TEST_FILE_2 = System.getProperty("java.io.tmpdir") + File.separator + "test_file_2";

    private static final byte[] TEST_DATA_2 = new byte[] { 4, 5, 6, 7 };

    @Override
    protected void setUp() throws Exception {
        new Client();
        Handler.clear();
        writeFile(TEST_FILE, TEST_DATA);
        writeFile(TEST_FILE_2, TEST_DATA_2);
    }

    public void testDustProtocolIsSUpportedByURL() throws MalformedURLException {
        new URL("test:");
    }

    public void testOpeningADustURLReturnsAnHttpConnection() throws IOException {
        URLConnection connection = new URL("test:").openConnection();
        assertTrue(connection instanceof HttpURLConnection);
    }

    public void testOpeningADustURLReturnsAnHttpsConnection() throws IOException {
        URLConnection connection = new URL("test:").openConnection();
        assertTrue(connection instanceof HttpsURLConnection);
    }

    public void testDustUrlRequestStatusCodeDefaultsTo200() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("test:").openConnection();
        assertEquals(200, connection.getResponseCode());
    }

    public void testOneCanOverrideDefaultResponseCode() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("test:status:500").openConnection();
        assertEquals(500, connection.getResponseCode());
    }

    public void testDustServerResponseIsEmptyWhenNoFileSpecified() throws IOException {
        assertUrlContent(new URL("test:"), new byte[0]);
    }

    public void testDustServerResponseContentIsExtractedFromFile() throws IOException {
        assertUrlContent(new URL("test:file:" + TEST_FILE), TEST_DATA);
    }

    public void testDustServerCanSendSeveralResponsesFromSeveralFiles() throws IOException {
        String testUrl = "test:file:" + TEST_FILE + ";file:" + TEST_FILE_2;
        assertUrlContent(new URL(testUrl), TEST_DATA);
        assertUrlContent(new URL(testUrl), TEST_DATA_2);
    }

    public void testDustServerResponseContentIsExtractedFromFileWithExplicitStatus200() throws IOException {
        assertUrlContent(new URL("test:status:200;file:" + TEST_FILE), TEST_DATA);
    }

    public void test5xxStatusThrowsIOExceptionwhenTryingToRead() throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("test:status:500;file:test/soapdust/urlhandler/dust/hello.txt").openConnection();
        try {
            connection.getInputStream();
            fail();
        } catch (IOException e) {
        }
    }

    public void testCanReadResponseFromErrorStreamWhen5xxStatus() throws MalformedURLException, IOException {
        assertUrlErrorStreamContent(new URL("test:status:500;file:" + TEST_FILE), TEST_DATA);
    }

    public void testCanNotReadResponseFromErrorStreamWhenNot5xxStatus() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("test:file:" + TEST_FILE).openConnection();
        assertNull(connection.getErrorStream());
    }

    public void testOneCanWriteInADustUrl() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("test:").openConnection();
        connection.getOutputStream();
    }

    public void testWrittenDataAreStoredByUrlKeys() throws MalformedURLException, IOException {
        byte[] written = new byte[] { 1, 2, 3, 4 };
        writeToUrl(written, "test:");
        assertTrue(Arrays.equals(written, Handler.lastSaved("test:").toByteArray()));
    }

    public void testWrittenDataAreStoredByUrlKeysInList() throws MalformedURLException, IOException {
        byte[] writtenFirst = new byte[] { 1, 2, 3, 4 };
        writeToUrl(writtenFirst, "test:");
        byte[] writtenSecond = new byte[] { 5, 6, 7, 8 };
        writeToUrl(writtenSecond, "test:");
        assertTrue(Arrays.equals(writtenFirst, Handler.saved.get("test:").get(0).toByteArray()));
        assertTrue(Arrays.equals(writtenSecond, Handler.saved.get("test:").get(1).toByteArray()));
    }

    private void assertUrlContent(URL url, byte[] expectedContent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream = connection.getInputStream();
        assertStreamContent(expectedContent, stream);
    }

    private void assertUrlErrorStreamContent(URL url, byte[] expectedContent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream stream = connection.getErrorStream();
        assertStreamContent(expectedContent, stream);
    }

    private void assertStreamContent(byte[] expectedContent, InputStream stream) throws IOException {
        assertTrue(Arrays.equals(expectedContent, readFully(stream)));
    }

    private byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int read = in.read(buffer, 0, buffer.length); read != -1; read = in.read(buffer, 0, buffer.length)) {
            content.write(buffer, 0, read);
        }
        return content.toByteArray();
    }

    private void writeFile(String file, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        out.write(data);
        out.flush();
    }

    private void writeToUrl(byte[] data, String url) throws IOException, MalformedURLException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        OutputStream out = connection.getOutputStream();
        out.write(data);
        out.flush();
        out.close();
    }
}
