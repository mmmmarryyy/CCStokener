package de.lichtflut.infra.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Copyright 2009 by lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 * 
 * @author Oliver Tigges
 * 
 * Created: 10.03.2009
 * 
 * Description: very simple ReST-Client
 */
public class HttpResourceClient {

    public static final String HTTP_GET = "GET";

    public HttpResourceClient() {
    }

    public HttpURLConnection get(String url) throws HttpRequestException {
        return connect(url, HTTP_GET);
    }

    public HttpURLConnection connect(String targetUrl, String method) throws HttpRequestException {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            return connection;
        } catch (MalformedURLException e) {
            throw new HttpRequestException(e);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }
}
