package org.opennms.protocols.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * The class for handling HTTP URL Connection using Apache HTTP Client
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class HttpUrlHandler extends URLStreamHandler {

    /** The Constant PROTOCOL. */
    public static final String PROTOCOL = "http";

    @Override
    protected int getDefaultPort() {
        return 80;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new HttpUrlConnection(url);
    }
}
