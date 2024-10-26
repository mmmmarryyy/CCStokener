package net.sf.smailstandalone.config;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.apache.commons.httpclient.HttpClient;

/**
 * 
 * @since 19.02.2011
 * @author S�bastien CHATEL
 */
class HttpClientURLStreamHandler extends URLStreamHandler {

    private final HttpClient httpClient;

    public HttpClientURLStreamHandler(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new HttpClientURLConnection(this.httpClient, url);
    }
}
