package gnu.inet.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An HTTP URL stream handler.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class Handler extends URLStreamHandler {

    /**
   * Returns the default HTTP port (80).
   */
    protected int getDefaultPort() {
        return HTTPConnection.HTTP_PORT;
    }

    /**
   * Returns an HTTPURLConnection for the given URL.
   */
    public URLConnection openConnection(URL url) throws IOException {
        return new HTTPURLConnection(url);
    }
}
