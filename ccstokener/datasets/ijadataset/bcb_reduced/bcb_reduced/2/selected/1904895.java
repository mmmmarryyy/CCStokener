package it.bova.rtmapi;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

class RestClient {

    private Request request;

    RestClient(Request request) {
        this.request = request;
    }

    JSONResponse execute() throws ServerException, RtmApiException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        URI uri;
        try {
            uri = new URI(this.request.getUrl());
            HttpPost httppost = new HttpPost(uri);
            HttpResponse response = httpclient.execute(httppost);
            InputStream is = response.getEntity().getContent();
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader r = new BufferedReader(new InputStreamReader(new DoneHandlerInputStream(is)));
                for (String line = r.readLine(); line != null; line = r.readLine()) {
                    sb.append(line);
                }
                return new JSONResponse(sb.toString());
            } finally {
                is.close();
            }
        } catch (URISyntaxException e) {
            throw new RtmApiException(e.getMessage());
        } catch (ClientProtocolException e) {
            throw new RtmApiException(e.getMessage());
        }
    }

    String test() throws ServerException, RtmApiException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        URI uri;
        try {
            uri = new URI(this.request.getUrl());
            HttpPost httppost = new HttpPost(uri);
            HttpResponse response = httpclient.execute(httppost);
            InputStream is = response.getEntity().getContent();
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(new DoneHandlerInputStream(is)));
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            return sb.toString();
        } catch (URISyntaxException e) {
            throw new RtmApiException(e.getMessage());
        } catch (ClientProtocolException e) {
            throw new RtmApiException(e.getMessage());
        }
    }
}

class DoneHandlerInputStream extends FilterInputStream {

    private boolean done;

    public DoneHandlerInputStream(InputStream stream) {
        super(stream);
    }

    @Override
    public int read(byte[] bytes, int offset, int count) throws IOException {
        if (!done) {
            int result = super.read(bytes, offset, count);
            if (result != -1) {
                return result;
            }
        }
        done = true;
        return -1;
    }
}
