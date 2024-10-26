package org.travelfusion.xmlclient.impl.transport;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.travelfusion.xmlclient.transport.TfXTransport;
import org.travelfusion.xmlclient.util.TfXAPIUtil;
import org.travelfusion.xmlclient.xobject.XRequest;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * HTTP based transport implementation based on the <a href="http://hc.apache.org">Apache HTTP Components</a> library.
 * 
 * @author Jesse McLaughlin (nzjess@gmail.com)
 */
@Singleton
public class HttpCoreTransport implements TfXTransport {

    private Provider<String> serviceEndPointUrl;

    private HttpClient httpClient;

    public HttpCoreTransport() {
        this(TfXAPIUtil.API_SERVICE_ENDPOINT_URL);
    }

    public HttpCoreTransport(String serviceEndPointUrl) {
        setServiceEndPointUrl(serviceEndPointUrl);
    }

    public void setServiceEndPointUrl(final String serviceEndPointUrl) {
        setServiceEndPointUrlProvider(new Provider<String>() {

            public String get() {
                return serviceEndPointUrl;
            }
        });
    }

    /**
   * <code>bindConstant().annotatedWith(Names.named("ServiceEndPointUrl")).to(...)</code>
   */
    @Inject(optional = true)
    public void setServiceEndPointUrlProvider(@Named("ServiceEndPointUrl") Provider<String> serviceEndPointUrl) {
        this.serviceEndPointUrl = serviceEndPointUrl;
    }

    @Inject
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
   * {@inheritDoc}
   */
    @Send
    public InputStream send(XRequest request, String requestString) throws IOException {
        StringEntity requestEntity = new StringEntity(requestString, "UTF-8");
        requestEntity.setContentType("text/xml");
        HttpPost post = new HttpPost(serviceEndPointUrl.get());
        post.setEntity(requestEntity);
        HttpResponse response = httpClient.execute(post);
        final HttpEntity responseEntity = response.getEntity();
        return new FilterInputStream(responseEntity.getContent()) {

            @Override
            public void close() throws IOException {
                responseEntity.consumeContent();
                super.close();
            }
        };
    }
}
