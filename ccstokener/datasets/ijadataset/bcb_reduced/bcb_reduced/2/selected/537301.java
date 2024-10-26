package com.google.gdata.client.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Opens HTTP connection using {@link URL.openConnection()}.
 */
public class JdkHttpUrlConnectionSource implements HttpUrlConnectionSource {

    /** Pre-built instance. */
    public static final JdkHttpUrlConnectionSource INSTANCE = new JdkHttpUrlConnectionSource();

    public HttpURLConnection openConnection(URL url) throws IOException {
        if (!url.getProtocol().startsWith("http")) {
            throw new IllegalArgumentException("Not an HTTP url: " + url);
        }
        return (HttpURLConnection) url.openConnection();
    }
}
