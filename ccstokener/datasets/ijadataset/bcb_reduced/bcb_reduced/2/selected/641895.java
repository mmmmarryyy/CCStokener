package org.gloin.resource;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.Serializable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of <code>InputStreamFactory</code> which uses URLs
 * to send resources to requesting clients.  The
 * <code>newInputStream</code> method of this factory returns,
 * <code>URLInputStream</code>s which are serializable and contain a
 * single field reference to a URL object.  On the client side, when a
 * method is invoked on a <code>URLInputStream</code>, the stream will
 * initialize itself by calling <code>URL.openStream()</code>.  The
 * <code>URLInputStream</code> will delegate to the stream return
 * value of that method to carry out the method call on itself.
 */
public class URLInputStreamFactory implements InputStreamFactory, Serializable {

    private String baseURL;

    public URLInputStreamFactory(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Create a new input stream for a given resource
     */
    public InputStream newInputStream(String resourceName) throws MalformedURLException {
        return new URLInputStream(new URL(baseURL + resourceName));
    }

    public String toString() {
        return super.toString() + ": " + baseURL;
    }

    public int hashCode() {
        return baseURL.hashCode();
    }

    /**
     * InputStream which obtains resource bytes remote a remote byte
     * store.
     */
    public class URLInputStream extends InputStream implements Serializable {

        /**
	 * The url from which this stream obtains data
	 */
        private URL url;

        private InputStream delegateStream = null;

        public URLInputStream() {
        }

        public URLInputStream(URL url) {
            this.url = url;
        }

        private void initDelegate() throws IOException {
            if (delegateStream == null) {
                delegateStream = url.openStream();
            }
        }

        public int read() throws IOException {
            initDelegate();
            return delegateStream.read();
        }

        public int available() throws IOException {
            initDelegate();
            return delegateStream.available();
        }

        public void close() throws IOException {
            initDelegate();
            delegateStream.close();
        }

        /**
	 * Return byte store and stream name as string representation.
	 */
        public String toString() {
            return URLInputStreamFactory.this.toString();
        }

        public int hashCode() {
            return URLInputStreamFactory.this.hashCode();
        }

        public boolean equals(Object other) {
            if ((other != null) && (other instanceof URLInputStream)) {
                URL otherURL = ((URLInputStream) other).url;
                if ((url == null) || (otherURL == null)) {
                    return false;
                }
                if (url == otherURL) {
                    return true;
                }
                return url.equals(otherURL);
            }
            return false;
        }
    }
}
