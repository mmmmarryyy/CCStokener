package it;

import it.JsonReturnErrorHandler.ErrorObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import junit.framework.TestCase;
import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import com.google.gson.Gson;

/**
 * ErrorHandler return JSON instead of return default error.
 */
public class ExceptionTest extends TestCase {

    public void testErrorHandlerReturnJSON() throws Exception {
        SDLoader loader = new SDLoader();
        loader.setAutoPortDetect(true);
        loader.setUseNoCacheMode(true);
        WebAppContext context = new WebAppContext("/exceptiontest", "test/it/exceptiontest");
        context.addClassPath("target/classes");
        context.addClassPath("target/test-classes");
        loader.addWebAppContext(context);
        try {
            loader.start();
            String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/extest1";
            String res = getRequestContent(baseUrl);
            Gson gson = new Gson();
            ErrorObject ret = gson.fromJson(res, ErrorObject.class);
            assertEquals("error", ret.getMessage());
            assertTrue(ret.getCode() == 123);
        } catch (Throwable t) {
            loader.stop();
            t.printStackTrace();
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
