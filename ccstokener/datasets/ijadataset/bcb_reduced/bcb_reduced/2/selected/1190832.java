package juploader.httpclient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** @author Adam Pawelec */
public class GetPageRequestTest extends AbstractHttpRequestTest {

    private GetPageRequest request;

    @Before
    public void setUp() throws Exception {
        request = httpClient.createGetPageRequest();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetPage() throws Exception {
        request.setUrl("http://www.google.pl");
        HttpResponse response = httpClient.execute(request);
        assertTrue(response.is2xxSuccess());
        assertTrue(response.getResponseHeaders().size() > 0);
        response.close();
    }
}
