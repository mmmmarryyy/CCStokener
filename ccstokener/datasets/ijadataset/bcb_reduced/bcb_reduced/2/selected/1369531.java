package it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ActionParamTest extends AbstractItTestCase {

    public void test1() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/actionparam";
        assertEquals("hoge:AAA", getPostRequestContent(baseUrl, "execute=EXEC&hoge=AAA"));
    }

    public void test2_nosuchmethod() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/actionparam";
        try {
            getPostRequestContent(baseUrl, "hoge=BBB");
            fail();
        } catch (IOException expected) {
        }
    }

    protected String getPostRequestContent(String urlText, String postParam) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestMethod("POST");
        urlcon.setUseCaches(false);
        urlcon.setDoOutput(true);
        PrintStream ps = new PrintStream(urlcon.getOutputStream());
        ps.print(postParam);
        ps.close();
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
