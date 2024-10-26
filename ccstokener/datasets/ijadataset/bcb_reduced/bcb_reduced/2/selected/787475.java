package net.sourceforge.dbtoolbox.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of resource based on URL
 */
public class URLResource implements Resource {

    /**
     * URL
     */
    private URL url;

    /**
     * Get URL
     * @return URL
     */
    public URL getURL() {
        return url;
    }

    /**
     * Default constructor
     * @param url URL
     */
    public URLResource(URL url) {
        this.url = url;
    }

    public String getName() {
        return url.toExternalForm();
    }

    public Resource createRelativeResource(String name) {
        try {
            URL relativeURL = new URL(url, name);
            return new URLResource(relativeURL);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Malformed Relative URL ", ex);
        }
    }

    /**
     * Open URL input stream
     * @return URL Input Stream
     * @throws java.io.IOException
     */
    public InputStream openInputStream() throws IOException {
        return url.openStream();
    }

    @Override
    public String toString() {
        return url.toString();
    }
}
