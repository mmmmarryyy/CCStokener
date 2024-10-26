package it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

/**
 * POST and GET test.
 */
public class PostGetTest extends TestCase {

    /**
	 * POST and GET or other HTTP method should be separated by method.
	 * 
	 * @throws Exception
	 */
    public void _test_get() throws Exception {
        SDLoader loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        WebAppContext context = new WebAppContext("/hoge", "test/it/postget");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        loader.addWebAppContext(context);
        try {
            loader.start();
            String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/postget/go";
            String res = getRequestContent(baseUrl);
            System.out.println(res);
            baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/postget/go";
            res = getPostRequestContent(baseUrl, null);
            System.out.println(res);
        } catch (Throwable t) {
            loader.stop();
            t.printStackTrace();
            fail();
        }
    }

    public void test_postget() throws Exception {
        SDLoader loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        WebAppContext context = new WebAppContext("/hoge", "test/it/postget");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        loader.addWebAppContext(context);
        try {
            loader.start();
            String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/postget/go2";
            String res = getRequestContent(baseUrl);
            assertEquals("\"go2:GET\"", res);
            baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/postget/go2";
            res = getPostRequestContent(baseUrl, null);
            assertEquals("\"go3:POST\"", res);
        } catch (Throwable t) {
            loader.stop();
            t.printStackTrace();
            fail();
        }
    }

    protected String getPostRequestContent(String urlText, String postParam) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        String line = null;
        try {
            urlcon.setRequestMethod("POST");
            urlcon.setUseCaches(false);
            urlcon.setDoOutput(true);
            PrintStream ps = new PrintStream(urlcon.getOutputStream());
            ps.print(postParam);
            ps.close();
            urlcon.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
            line = reader.readLine();
            reader.close();
        } finally {
            urlcon.disconnect();
        }
        return line;
    }

    protected String getRequestContent(String urlText) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        String line = null;
        try {
            urlcon.setUseCaches(false);
            urlcon.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
            line = reader.readLine();
            reader.close();
        } finally {
            urlcon.disconnect();
        }
        return line;
    }
}
