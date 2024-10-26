package com.beem.project.beem.smack.avatar;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;

/**
 * An AvatarRetriever which retrieve the avatar over HTTP using the Apache HttpClient.
 */
public class HttpClientAvatarRetriever implements AvatarRetriever {

    private String mUrl;

    private HttpClient mClient;

    /**
	 * Create a HttpAvatarRetriever.
	 * 
	 * @param client
	 *            the custom HttpClient to use to downlowad
	 * @param url
	 *            the url of the avatar to download.
	 */
    public HttpClientAvatarRetriever(final HttpClient client, final String url) {
        mUrl = url;
        mClient = client;
    }

    /**
	 * Create a HttpAvatarRetriever.
	 * 
	 * @param url
	 *            the url of the avatar to download.
	 */
    public HttpClientAvatarRetriever(final String url) {
        mUrl = url;
        mClient = new DefaultHttpClient();
    }

    @Override
    public byte[] getAvatar() throws IOException {
        HttpUriRequest request;
        try {
            request = new HttpGet(mUrl);
        } catch (IllegalArgumentException e) {
            IOException ioe = new IOException("Invalid url " + mUrl);
            ioe.initCause(e);
            throw ioe;
        }
        HttpResponse response = mClient.execute(request);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] data = new byte[1024];
            int nbread;
            while ((nbread = in.read(data)) != -1) {
                os.write(data, 0, nbread);
            }
        } finally {
            in.close();
            os.close();
        }
        return os.toByteArray();
    }
}
