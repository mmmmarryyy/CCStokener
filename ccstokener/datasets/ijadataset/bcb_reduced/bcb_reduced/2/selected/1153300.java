package it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

public class ExcludePathTest extends TestCase {

    public void testRestLike() throws Exception {
        SDLoader loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        WebAppContext context = new WebAppContext("/it", "test/it/excludepath");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        loader.addWebAppContext(context);
        try {
            loader.start();
            String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/";
            String res = getRequestContent(baseUrl);
            assertEquals("root", res);
            baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/_exclude/hoge.txt";
            res = getRequestContent(baseUrl);
            assertEquals("hoge", res);
            baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/foo";
            res = getRequestContent(baseUrl);
            assertEquals("foo", res);
        } catch (Throwable t) {
            loader.stop();
            fail();
        }
    }

    protected String getRequestContent(String urlText) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
