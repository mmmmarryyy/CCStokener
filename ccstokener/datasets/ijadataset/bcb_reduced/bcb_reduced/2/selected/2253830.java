package juploader.httpclient;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

/**
 * Testy dla żądania wysyłania plików. Ponieważ wymagają one pobrania pliku
 * testowego oraz wysłania go na wybrane serwery należy je odpalać tylko wtedy
 * gdy faktycznie jest taka potrzeba.
 *
 * @author Adam Pawelec
 */
@Ignore
public class FileUploadRequestTest extends AbstractHttpRequestTest {

    /** Plik, który będzie wysyłany na serwery. */
    private static File file = new File("file.png");

    public static final String TEST_FILE_URL = "http://www.google.com/intl/en_com/images/srpr/logo1w.png";

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractHttpRequestTest.setUpClass();
        FileDownloadRequest request = httpClient.createFileDownloadRequest();
        request.setUrl(TEST_FILE_URL);
        request.setDestFile(file);
        httpClient.execute(request);
        file.deleteOnExit();
        assertTrue(file.exists());
    }

    private FileUploadRequest request;

    @Before
    public void setUp() throws Exception {
        request = httpClient.createFileUploadRequest();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSpeedyShareUpload() throws Exception {
        request.setUrl("http://www.speedyshare.com/upload.php");
        request.setFile("fileup0", file);
        HttpResponse response = httpClient.execute(request);
        assertTrue(response.is2xxSuccess());
        assertTrue(response.getResponseHeaders().size() > 0);
        String body = IOUtils.toString(response.getResponseBody());
        assertTrue(body.contains("Download link"));
        assertTrue(body.contains("Delete password"));
        response.close();
    }

    @Test
    public void testImageshackUpload() throws Exception {
        request.setUrl("http://www.imageshack.us/index.php");
        request.addParameter("xml", "yes");
        request.setFile("fileupload", file);
        HttpResponse response = httpClient.execute(request);
        assertTrue(response.is2xxSuccess());
        assertTrue(response.getResponseHeaders().size() > 0);
        String body = IOUtils.toString(response.getResponseBody());
        assertTrue(body.contains("<image_link>"));
        assertTrue(body.contains("<thumb_link>"));
        assertTrue(body.contains("<image_location>"));
        assertTrue(body.contains("<image_name>"));
        response.close();
    }
}
