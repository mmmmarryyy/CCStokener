package com.googlecode.pondskum.client;

import com.googlecode.pondskum.client.listener.ConnectionListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;

public final class LinkTraverserImpl implements LinkTraverser {

    private final DefaultHttpClient httpClient;

    public LinkTraverserImpl(final DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void traverse(final String url, final StageHolder stageHolder, final ConnectionListener listener) throws LinkTraverserException {
        try {
            listener.updateStatus(stageHolder.getState(), "Connecting to url -> " + url);
            stageHolder.nextState();
            HttpResponse response = openConnection(httpClient, url);
            listener.handleEvent(httpClient, response);
            listener.updateStatus(stageHolder.getState(), "closing connection to url -> " + url);
            closeConnection(response);
        } catch (Exception e) {
            String error = "There was an error connecting to url -> " + url;
            listener.onError(stageHolder.getState(), error, e);
            throw new LinkTraverserException(error, e);
        }
    }

    private HttpResponse openConnection(final DefaultHttpClient httpClient, final String url) throws IOException {
        HttpGet httpget = new HttpGet(url);
        return httpClient.execute(httpget);
    }

    private void closeConnection(final HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            entity.consumeContent();
        }
    }
}
