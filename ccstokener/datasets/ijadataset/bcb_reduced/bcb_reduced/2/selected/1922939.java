package it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;

public class Issue216Test extends TestCase {

    private static final String REFERER_STR = "http://www.google.co.jp/search?hl=ja&q=test";

    /**
	 * 
	 * {@.en }
	 * 
	 * <br />
	 * 
	 * {@.ja }
	 * 
	 * @throws Exception
	 * @see http://code.google.com/p/t-2/issues/detail?id=216
	 */
    public void testRefererShouldGet() throws Exception {
        SDLoader loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        WebAppContext context = new WebAppContext("/it", "webapp");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        loader.addWebAppContext(context);
        try {
            loader.start();
            String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/issue216";
            String requestContent = getRequestContent(baseUrl + "/hoge/foo", "GET");
            assertEquals(REFERER_STR, requestContent);
        } finally {
            loader.stop();
        }
    }

    protected String getRequestContent(String urlText, String method) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestProperty("Referer", REFERER_STR);
        urlcon.setRequestMethod(method);
        urlcon.setUseCaches(false);
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
