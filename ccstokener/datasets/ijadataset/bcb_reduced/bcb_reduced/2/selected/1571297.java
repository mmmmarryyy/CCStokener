package it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

public class RestLikeTest extends TestCase {

    public void testRestLike() throws Exception {
        SDLoader loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        WebAppContext context = new WebAppContext("/it", "test/it/page3");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        loader.addWebAppContext(context);
        try {
            loader.start();
            String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/rest-like";
            String res = getPostRequestContent(baseUrl, "_method=put&execute=execute");
            System.out.println(res);
            assertEquals("success", res);
        } catch (Throwable t) {
            loader.stop();
            fail();
        }
    }

    protected String getPostRequestContent(String urlText, String... postParams) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestMethod("POST");
        urlcon.setUseCaches(false);
        urlcon.setDoOutput(true);
        PrintStream ps = new PrintStream(urlcon.getOutputStream());
        for (String param : postParams) {
            ps.print(param);
        }
        ps.close();
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
