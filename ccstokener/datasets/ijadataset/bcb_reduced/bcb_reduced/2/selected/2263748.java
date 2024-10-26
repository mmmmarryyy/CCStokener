package gnu.java.net.protocol.https;

import gnu.java.net.protocol.http.HTTPConnection;
import gnu.java.net.protocol.http.HTTPURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An HTTPS URL stream handler.
 *
 * @author Chris Burdess (dog@gnu.org)
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
