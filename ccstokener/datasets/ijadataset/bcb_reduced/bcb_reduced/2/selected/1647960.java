package gnu.java.net.protocol.file;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * This is the protocol handler for the "file" protocol.
 * It implements the abstract openConnection() method from
 * URLStreamHandler by returning a new FileURLConnection object (from
 * this package).  All other methods are inherited
 *
 * @author Aaron M. Renn (arenn@urbanophile.com)
 * @author Warren Levy (warrenl@cygnus.com)
 */
public class Handler extends URLStreamHandler {

    /**
   * A do nothing constructor
   */
    public Handler() {
    }

    /**
   * This method returs a new FileURLConnection for the specified URL
   *
   * @param url The URL to return a connection for
   *
   * @return The URLConnection
   *
   * @exception IOException If an error occurs
   */
    protected URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }

    protected void setHandlerURL(URL u, String protocol, String host, int port, String file, String ref) {
        String authority = null;
        String userInfo = null;
        if (host != null && host.length() != 0) {
            authority = (port == -1) ? host : host + ":" + port;
            int at = host.lastIndexOf('@');
            if (at != -1) {
                userInfo = host.substring(0, at);
                host = host.substring(at + 1);
            }
        }
        String path = null;
        String query = null;
        if (file != null) {
            int q = file.lastIndexOf('?');
            if (q != -1) {
                query = file.substring(q + 1);
                path = file.substring(0, q);
            } else {
                path = file;
            }
        }
        setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }
}
