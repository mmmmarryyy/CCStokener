package org.danann.cernunnos.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class WindowsDriveURLStreamHandler extends URLStreamHandler {

    public URLConnection openConnection(URL url) {
        return new URLConnectionImpl(url);
    }

    private static final class URLConnectionImpl extends URLConnection {

        public URLConnectionImpl(URL url) {
            super(url);
        }

        public void connect() {
        }

        public InputStream getInputStream() {
            InputStream rslt = null;
            try {
                rslt = new URL("file:///" + url.toExternalForm()).openStream();
            } catch (Throwable t) {
                String msg = "Unable to read the specified file:  " + url.toExternalForm();
                throw new RuntimeException(msg, t);
            }
            return rslt;
        }

        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
    }
}
