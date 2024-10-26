package org.firebirdsql.jrt;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URLHandler for blobs in plugin tables.
 * Reference: http://accu.org/index.php/journals/1434
 *
 * @author <a href="mailto:adrianosf@gmail.com">Adriano dos Santos Fernandes</a>
 */
public class Handler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {
        return new BlobConnection(url);
    }
}
