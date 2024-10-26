package com.trazere.util.io;

import com.trazere.util.lang.HashCode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * The {@link FileInput} class implements data inputs based on URLs.
 */
public class URLInput implements Input {

    /** URL to read. */
    protected final URL _url;

    /**
	 * Instanciate a new data input with the given URL.
	 * 
	 * @param url URL to read.
	 */
    public URLInput(final URL url) {
        assert null != url;
        _url = url;
    }

    /**
	 * Get the URL read by the receiver data input.
	 * 
	 * @return The url.
	 */
    public URL getUrl() {
        return _url;
    }

    public boolean exists() throws IOException {
        return true;
    }

    public InputStream open() throws IOException {
        return _url.openStream();
    }

    @Override
    public int hashCode() {
        final HashCode result = new HashCode(this);
        result.append(_url);
        return result.get();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        } else if (null != object && getClass().equals(object.getClass())) {
            final URLInput input = (URLInput) object;
            return _url.equals(input._url);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return _url.toString();
    }
}
