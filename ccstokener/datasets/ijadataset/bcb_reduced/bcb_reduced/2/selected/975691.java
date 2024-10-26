package gnu.inet.finger;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A URL stream handler implementing the finger protocol.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class Handler extends URLStreamHandler {

    protected int getDefaultPort() {
        return FingerConnection.DEFAULT_PORT;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return new FingerURLConnection(url);
    }
}
