package org.dwgsoftware.raistlin.repository;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Repository URL protocol handler.  
 */
public class BlockHandler extends URLStreamHandler {

    /**
     * Opens a connection to the specified URL.
     *
     * @param url A URL to open a connection to.
     * @return The established connection.
     * @throws IOException If a connection failure occurs.
     */
    protected URLConnection openConnection(final URL url) throws IOException {
        return new BlockURLConnection(url);
    }

    protected int getDefaultPort() {
        return 0;
    }

    protected String toExternalForm(URL url) {
        StringBuffer result = new StringBuffer("block:");
        if (url.getFile() != null) {
            result.append(url.getFile());
        }
        if (url.getRef() != null) {
            result.append("#");
            result.append(url.getRef());
        }
        return result.toString();
    }
}
