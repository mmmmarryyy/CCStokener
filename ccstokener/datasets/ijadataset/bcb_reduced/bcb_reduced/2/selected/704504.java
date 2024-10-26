package org.bresearch.websec.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectAdapter {

    private final URL url;

    public URLConnectAdapter(final URL url) {
        this.url = url;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    public URLConnection openConnection() {
        try {
            return this.url.openConnection();
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }
}
