package org.danann.cernunnos.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class ClasspathURLStreamHandler extends URLStreamHandler {

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
            String s = url.toExternalForm();
            s = s.substring((url.getProtocol() + "://").length(), s.length());
            InputStream rslt = ClasspathURLStreamHandler.class.getClassLoader().getResourceAsStream(s);
            if (rslt == null) {
                String msg = "Unable to read the specified resource from the classpath:  " + s;
                throw new RuntimeException(msg);
            }
            return rslt;
        }

        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
    }
}
