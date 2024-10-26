package com.volantis.shared.net.impl.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Default URL content that uses the standard {@link URLConnection} to retrieve
 * the content.
 */
public class DefaultContent extends AbstractURLContent {

    /**
     * The connection.
     */
    private final URLConnection connection;

    /**
     * The last modified time at content creation.
     */
    private final long lastModified;

    /**
     * Initialise.
     *
     * @param url     The URL whose content this represents.
     * @throws IOException If there was a problem getting the content.
     */
    public DefaultContent(final URL url) throws IOException {
        super(url);
        this.connection = url.openConnection();
        lastModified = connection.getLastModified();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public String getCharacterEncoding() {
        return null;
    }

    public boolean isFresh() {
        return lastModified == connection.getLastModified();
    }
}
