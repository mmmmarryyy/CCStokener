package util.download.async;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import util.download.DownloadManager;
import util.download.DownloadManager.DownloadException;

public class Downloader implements Callable<HttpEntity> {

    private HttpClient httpClient;

    private HttpContext context;

    private HttpRequestBase request;

    public Downloader(HttpClient httpClient, HttpContext context, HttpRequestBase request) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        this.httpClient = httpClient;
        this.context = context;
        this.request = request;
    }

    public URI getRequestURI() {
        return request.getURI();
    }

    @Override
    public HttpEntity call() throws Exception {
        try {
            HttpResponse response = httpClient.execute(request, context);
            HttpEntity entity = response.getEntity();
            return entity;
        } catch (ClientProtocolException e) {
            request.abort();
            throw new DownloadException("protocol error", e);
        } catch (IOException e) {
            request.abort();
            throw new DownloadException("I/O error", e);
        }
    }
}
