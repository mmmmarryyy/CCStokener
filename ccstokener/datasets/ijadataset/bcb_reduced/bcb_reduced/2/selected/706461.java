package syntelos.net.https;

import java.net.URL;
import java.net.URLConnection;

/**
 * <p> This package is a wrapper for its "http" sibling.  This design
 * allows the use of 
 * 
 * <pre>
 * Connection con = new Connection(url);
 * </pre>
 * 
 * for the "http" package Connection over both "http" and "https"
 * urls (as a dynamic, runtime choice).  </p>
 *
 * @author jdp
 * @since 1.5
 */
public final class Handler extends java.net.URLStreamHandler {

    public Handler() {
        super();
    }

    protected URLConnection openConnection(URL url) throws java.io.IOException {
        return new syntelos.net.http.Connection(url);
    }

    protected int getDefaultPort() {
        return 443;
    }
}
