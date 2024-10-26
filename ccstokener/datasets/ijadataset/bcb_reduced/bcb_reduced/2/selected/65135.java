package gnu.java.net.protocol.core;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Anthony Green <green@redhat.com>
 * @date August 13, 2001.
 */
public class Handler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }
}
