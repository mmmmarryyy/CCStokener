package com.gencom.fah.monitor.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import com.gencom.fah.monitor.model.Result;

public class ResultUpdater {

    private HttpClientProvider clientProvider = new HttpClientProvider();

    public void updateResult(Result result) throws UnsupportedEncodingException {
        HttpPost updateRequest = populateUpdateRequest(result);
        HttpClient client = clientProvider.getHttpClient();
        try {
            HttpResponse response = client.execute(updateRequest);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream input = entity.getContent();
                if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                    System.out.println("Request was not accepted by the collection server. Reason:");
                    System.out.println("Status: " + response.getStatusLine().getStatusCode());
                }
                for (int c = 0; (c = input.read()) > -1; ) {
                    System.out.print((char) c);
                }
                entity.consumeContent();
            }
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpPost populateUpdateRequest(Result result) throws UnsupportedEncodingException {
        HttpPost updateRequest = new HttpPost(Settings.getResultUpdateURL());
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("projectNumber", "4141"));
        nvps.add(new BasicNameValuePair("projectName", "aaa"));
        updateRequest.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        return updateRequest;
    }
}
