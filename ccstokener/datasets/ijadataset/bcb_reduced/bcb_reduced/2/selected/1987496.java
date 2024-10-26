package de.ddnews.reader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public abstract class DefaultReader {

    public InputStream getStream(String url) {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));
            HttpResponse response = client.execute(request);
            return response.getEntity().getContent();
        } catch (URISyntaxException e) {
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        return null;
    }
}
