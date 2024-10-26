package test.ssl;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * @author Jan Peters
 * @version "$Id: SSLTest.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class SSLTest {

    public static void main(String[] argv) throws Exception {
        int i;
        URL url = new URL("https://api.sandbox.ebay.com/wsapi");
        URLConnection urlConn = url.openConnection();
        InputStream in = urlConn.getInputStream();
        byte[] buf = new byte[1024];
        while ((i = in.read(buf)) != -1) {
            System.out.print(buf.toString());
        }
    }
}
