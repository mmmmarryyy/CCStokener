package org.fishwife.jrugged.httpclient;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;
import org.fishwife.jrugged.CircuitBreakerException;

public class FailureHandlingHttpClient extends AbstractHttpClientDecorator {

    public FailureHandlingHttpClient(HttpClient backend) {
        super(backend);
    }

    public HttpResponse execute(HttpHost host, HttpRequest req, HttpContext ctx) throws IOException, ClientProtocolException {
        try {
            return backend.execute(host, req, ctx);
        } catch (UnsuccessfulResponseException ure) {
            return ure.getResponse();
        } catch (CircuitBreakerException cbe) {
            throw new IOException(cbe);
        }
    }
}
