package gnu.inet.gopher;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A Gopher URL stream handler.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class Handler extends URLStreamHandler {

    protected int getDefaultPort() {
        return GopherConnection.DEFAULT_PORT;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        return new GopherURLConnection(url);
    }
}
