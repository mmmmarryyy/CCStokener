package it;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Issue187Test extends AbstractItTestCase {

    /**
	 * 
	 * {@.en }
	 * 
	 * <br />
	 * 
	 * {@.ja }
	 * 
	 * @throws Exception
	 * @see http://code.google.com/p/t-2/issues/detail?id=185
	 */
    public void testVarWithMultibyteCharacters() throws Exception {
        String baseUrl = "http://localhost:" + loader.getPort() + context.getContextPath() + "/issue187";
        String s = URLEncoder.encode("ほげほげ", "UTF-8");
        System.out.println(s);
        assertEquals("default:ほげほげ", getRequestContent(baseUrl + "/" + s, "GET"));
    }

    protected String getRequestContent(String urlText, String method) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.setRequestMethod(method);
        urlcon.setUseCaches(false);
        urlcon.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlcon.getInputStream(), "UTF-8"));
        String line = reader.readLine();
        reader.close();
        urlcon.disconnect();
        return line;
    }
}
