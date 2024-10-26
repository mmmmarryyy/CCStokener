package gnu.inet.https;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import gnu.inet.http.HTTPConnection;
import gnu.inet.http.HTTPURLConnection;

/**
 * An HTTPS URL stream handler.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class Handler extends URLStreamHandler {

    /**
   * Returns the default HTTPS port (443).
   */
    protected int getDefaultPort() {
        return HTTPConnection.HTTPS_PORT;
    }

    /**
   * Returns an HTTPURLConnection for the given URL.
   */
    public URLConnection openConnection(URL url) throws IOException {
        return new HTTPURLConnection(url);
    }
}
