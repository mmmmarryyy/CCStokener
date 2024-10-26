package com.googlecode.mycontainer.test.web;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

public class MycontainerWebTest extends AbstractWebBaseTestCase {

    @Test
    public void testBasic() throws Exception {
        testURL("http://localhost:8380/test/index.html");
        testURL("http://localhost:8380/test/test.txt");
        testURL("http://localhost:8380/test/filter.txt");
        testURL("http://localhost:8380/test-other/index.html");
        testURL("http://localhost:8380/test-other/test.txt");
        testURL("http://localhost:8380/test-other/filter.txt");
    }

    private void testURL(String urlStr) throws MalformedURLException, IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            int code = conn.getResponseCode();
            assertEquals(HttpURLConnection.HTTP_OK, code);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
