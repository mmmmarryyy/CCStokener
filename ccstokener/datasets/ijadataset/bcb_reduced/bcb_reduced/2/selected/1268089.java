package org.projectnotes.web;

import org.junit.*;
import java.net.URL;
import java.net.HttpURLConnection;

public class WebappIT {

    private String baseUrl;

    @Before
    public void setUp() throws Exception {
        String port = System.getProperty("servlet.port");
        this.baseUrl = "http://localhost:" + port + "/projectnotes/test.html";
    }

    @Test
    public void indexPagTest() throws Exception {
        URL url = new URL(this.baseUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        Assert.assertEquals(200, connection.getResponseCode());
    }
}
