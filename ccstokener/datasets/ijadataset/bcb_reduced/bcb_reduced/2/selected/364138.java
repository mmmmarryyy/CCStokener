package com.googlecode.pondskum.client;

import com.googlecode.pondskum.client.listener.ConnectionListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public final class FormSubmitterImpl implements FormSubmitter {

    private final DefaultHttpClient httpClient;

    public FormSubmitterImpl(final DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void submit(final String url, final ConnectionListener listener, final StageHolder stageHolder, final NameValuePairBuilder nameValuePairBuilder) {
        try {
            listener.updateStatus(stageHolder.getState(), "Connecting to url -> " + url);
            stageHolder.nextState();
            HttpPost httpost = openConnection(url, nameValuePairBuilder);
            listener.updateStatus(stageHolder.getState(), "Submitting form");
            stageHolder.nextState();
            HttpResponse response = httpClient.execute(httpost);
            listener.handleEvent(httpClient, response);
            listener.updateStatus(stageHolder.getState(), "Closing connecting to url -> " + url);
            closeConnection(response);
        } catch (Exception e) {
            List<NameValuePair> nameValuePairList = nameValuePairBuilder.build();
            String error = "Could not submit form to url -> " + url + ", with properties -> " + nameValuePairList;
            listener.onError(stageHolder.getState(), error, e);
            throw new FormSubmitterException(error, e);
        }
    }

    private HttpPost openConnection(final String url, final NameValuePairBuilder nameValuePairBuilder) throws UnsupportedEncodingException {
        HttpPost httpost = new HttpPost(url);
        httpost.setEntity(new UrlEncodedFormEntity(nameValuePairBuilder.build(), HTTP.UTF_8));
        return httpost;
    }

    private void closeConnection(final HttpResponse response) throws IOException {
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
            entity.consumeContent();
        }
    }
}
