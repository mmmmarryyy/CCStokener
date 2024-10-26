package org.apache.harmony.luni.internal.net.www.protocol.https;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.apache.harmony.luni.internal.nls.Messages;

/**
 * Handler for HttpsURLConnection implementation.
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new HttpsURLConnectionImpl(url, getDefaultPort());
    }

    @Override
    protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        if ((url == null) || (proxy == null)) {
            throw new IllegalArgumentException(Messages.getString("luni.1B"));
        }
        return new HttpsURLConnectionImpl(url, getDefaultPort(), proxy);
    }

    @Override
    protected int getDefaultPort() {
        return 443;
    }
}
