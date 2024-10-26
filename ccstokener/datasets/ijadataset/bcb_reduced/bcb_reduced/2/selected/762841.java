package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.rewrite.image.NoOpImageRewriter;
import com.google.common.collect.Maps;
import com.google.inject.Provider;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.util.Map;

public class DefaultRequestPipelineTest {

    private static final Uri DEFAULT_URI = Uri.parse("http://example.org/gadget.xml");

    private final FakeHttpFetcher fetcher = new FakeHttpFetcher();

    private final FakeHttpCache cache = new FakeHttpCache();

    private final FakeOAuthRequestProvider oauth = new FakeOAuthRequestProvider();

    private final RequestPipeline pipeline = new DefaultRequestPipeline(fetcher, cache, oauth, new NoOpImageRewriter(), new NoOpInvalidationService());

    @Test
    public void authTypeNoneNotCached() throws Exception {
        HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);
        fetcher.response = new HttpResponse("response");
        HttpResponse response = pipeline.execute(request);
        assertEquals(request, fetcher.request);
        assertEquals(fetcher.response, response);
        assertEquals(response, cache.data.get(DEFAULT_URI));
        assertEquals(1, cache.readCount);
        assertEquals(1, cache.writeCount);
        assertEquals(1, fetcher.fetchCount);
    }

    @Test
    public void authTypeNoneWasCached() throws Exception {
        HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);
        HttpResponse cached = new HttpResponse("cached");
        cache.data.put(DEFAULT_URI, cached);
        HttpResponse response = pipeline.execute(request);
        assertEquals(cached, response);
        assertEquals(1, cache.readCount);
        assertEquals(0, cache.writeCount);
        assertEquals(0, fetcher.fetchCount);
    }

    @Test
    public void authTypeNoneWasCachedButStale() throws Exception {
        HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);
        HttpResponse cached = new HttpResponseBuilder().setStrictNoCache().create();
        cache.data.put(DEFAULT_URI, cached);
        HttpResponse fetched = new HttpResponse("fetched");
        fetcher.response = fetched;
        HttpResponse response = pipeline.execute(request);
        assertEquals(fetched, response);
        assertEquals(request, fetcher.request);
        assertEquals(fetched, cache.data.get(DEFAULT_URI));
        assertEquals(1, cache.readCount);
        assertEquals(1, cache.writeCount);
        assertEquals(1, fetcher.fetchCount);
    }

    @Test
    public void authTypeNoneIgnoreCache() throws Exception {
        HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE).setIgnoreCache(true);
        HttpResponse fetched = new HttpResponse("fetched");
        fetcher.response = fetched;
        HttpResponse response = pipeline.execute(request);
        assertEquals(fetched, response);
        assertEquals(request, fetcher.request);
        assertEquals(0, cache.readCount);
        assertEquals(0, cache.writeCount);
        assertEquals(1, fetcher.fetchCount);
    }

    @Test
    public void authTypeOAuthNotCached() throws Exception {
        HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.OAUTH);
        oauth.httpResponse = new HttpResponse("oauth result");
        HttpResponse response = pipeline.execute(request);
        assertEquals(oauth.httpResponse, response);
        assertEquals(request, oauth.httpRequest);
        assertEquals(response, cache.data.get(DEFAULT_URI));
        assertEquals(1, oauth.fetchCount);
        assertEquals(0, fetcher.fetchCount);
        assertEquals(1, cache.readCount);
        assertEquals(1, cache.writeCount);
    }

    @Test
    public void authTypeOAuthWasCached() throws Exception {
        HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.OAUTH);
        HttpResponse cached = new HttpResponse("cached");
        cache.data.put(DEFAULT_URI, cached);
        HttpResponse response = pipeline.execute(request);
        assertEquals(cached, response);
        assertEquals(0, oauth.fetchCount);
        assertEquals(0, fetcher.fetchCount);
        assertEquals(1, cache.readCount);
        assertEquals(0, cache.writeCount);
    }

    public static class FakeHttpFetcher implements HttpFetcher {

        protected HttpRequest request;

        protected HttpResponse response;

        protected int fetchCount = 0;

        protected FakeHttpFetcher() {
        }

        public HttpResponse fetch(HttpRequest request) throws GadgetException {
            fetchCount++;
            this.request = request;
            if (response == null) {
                throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT);
            }
            return response;
        }
    }

    public static class FakeHttpCache implements HttpCache {

        protected final Map<Uri, HttpResponse> data = Maps.newHashMap();

        protected int writeCount = 0;

        protected int readCount = 0;

        protected FakeHttpCache() {
        }

        public boolean addResponse(HttpRequest request, HttpResponse response) {
            writeCount++;
            data.put(request.getUri(), response);
            return true;
        }

        public HttpResponse getResponse(HttpRequest request) {
            readCount++;
            return data.get(request.getUri());
        }

        public HttpResponse removeResponse(HttpRequest key) {
            throw new UnsupportedOperationException();
        }

        public String createKey(HttpRequest request) {
            return request.getUri().getQuery();
        }
    }

    public static class FakeOAuthRequestProvider implements Provider<OAuthRequest> {

        protected int fetchCount = 0;

        protected HttpRequest httpRequest;

        protected HttpResponse httpResponse;

        protected FakeOAuthRequestProvider() {
        }

        private final OAuthRequest oauthRequest = new OAuthRequest(null, null) {

            @Override
            public HttpResponse fetch(HttpRequest request) {
                fetchCount++;
                httpRequest = request;
                return httpResponse;
            }
        };

        public OAuthRequest get() {
            return oauthRequest;
        }
    }
}
