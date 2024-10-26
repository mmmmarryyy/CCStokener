package test.cnoja.jmsncn;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import junit.framework.TestCase;

public class TestHttpsConnection extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testHttpsConnection() {
        try {
            URL url = new URL("https://addons.mozilla.org/zh-CN/firefox/");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.getOutputStream().write("hello".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
