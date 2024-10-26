package com.lehphyro.gamemcasa.scrapper.httpclient;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.util.*;

public class HttpHelper {

    private HttpHelper() {
    }

    public static final String get(URI uri, HttpClient httpClient) throws IOException {
        return execute(new HttpGet(uri), httpClient);
    }

    public static final String post(URI uri, List<NameValuePair> params, HttpClient httpClient) throws IOException {
        HttpEntity entity = new UrlEncodedFormEntity(params);
        HttpPost post = new HttpPost(uri);
        post.setEntity(entity);
        return execute(post, httpClient);
    }

    public static final String execute(HttpUriRequest request, HttpClient httpClient) throws IOException {
        HttpResponse response = httpClient.execute(request);
        if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
            HttpEntity entity = response.getEntity();
            try {
                return EntityUtils.toString(entity);
            } finally {
                entity.consumeContent();
            }
        } else if (HttpStatus.SC_MOVED_TEMPORARILY == response.getStatusLine().getStatusCode()) {
            Header header = response.getFirstHeader("Location");
            if (header == null) {
                throw new IllegalStateException("Server returned redirect without Location header");
            }
            String location = header.getValue();
            request.abort();
            URI locationUri = URI.create(location);
            if (locationUri.getHost() == null) {
                locationUri = request.getURI().resolve(location);
            }
            return execute(new HttpGet(locationUri), httpClient);
        } else {
            throw new IOException("Unexpected status code [" + response.getStatusLine().getStatusCode() + "]");
        }
    }
}
