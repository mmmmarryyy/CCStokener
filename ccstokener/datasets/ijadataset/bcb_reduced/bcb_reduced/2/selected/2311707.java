package rtm.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class NetCall {

    public InputStream call(String address) throws IOException {
        System.out.println(address);
        final URL url = new URL(address);
        final URLConnection conn = url.openConnection();
        return conn.getInputStream();
    }
}
