package org.berlin.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 */
public class URLConnectAdapter {

    private final URL url;

    /**
     * Constructor for URLConnectAdapter.
     * @param url URL
     */
    public URLConnectAdapter(final URL url) {
        this.url = url;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Method openConnection.
     * @return URLConnection
     */
    public URLConnection openConnection() {
        try {
            return this.url.openConnection();
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }
}
