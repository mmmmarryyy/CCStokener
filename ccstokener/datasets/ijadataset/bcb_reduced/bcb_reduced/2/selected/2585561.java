package it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AjaxTest extends AbstractItTestCase {

    public void test1_withSimplyGET() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/ajax";
        assertEquals("ajax", getPostRequestContent(baseUrl + "/execute", "GET", "X-Requested-With", "xmlhttprequest"));
    }

    public void test2_withActionPath() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/ajax";
        assertEquals("ajax2", getPostRequestContent(baseUrl + "/hoge", "GET", "X-Requested-With", "xmlhttprequest"));
    }

    public void test3_withSimplyGETWithoutXRequestedWith() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/ajax";
        try {
            getPostRequestContent(baseUrl + "/execute", "GET", null, null);
            fail();
        } catch (IOException expected) {
        }
    }

    public void test4_withActionPathWithoutXRequestedWith() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/ajax";
        try {
            getPostRequestContent(baseUrl + "/hoge", "GET", null, null);
            fail();
        } catch (IOException expected) {
        }
    }

    public void test5_withSimplyGET2() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/ajax";
        assertEquals("ajax", getPostRequestContent(baseUrl + "/execute", "GET", "x-requested-with", "XmlHttpRequest"));
    }

    protected String getPostRequestContent(String urlText, String method, String headerKey, String headerValue) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestMethod(method);
        urlcon.setUseCaches(false);
        urlcon.setDoOutput(true);
        if (headerKey != null && headerValue != null) {
            urlcon.addRequestProperty(headerKey, headerValue);
        }
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
