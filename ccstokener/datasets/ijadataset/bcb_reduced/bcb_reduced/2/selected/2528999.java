package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.LruCacheProvider;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.image.NoOpImageRewriter;
import com.google.common.collect.ImmutableSet;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultInvalidationServiceTest extends TestCase {

    private static final Uri URI = Uri.parse("http://www.example.org/spec.xml");

    private static final HttpResponse CACHEABLE = new HttpResponseBuilder().setResponseString("ORIGINALCONTENT").setHeader("Cache-Control", "max-age=1000").create();

    IMocksControl control;

    HttpCache cache;

    DefaultInvalidationService service;

    Cache<String, Long> invalidationCache;

    LruCacheProvider cacheProvider;

    FakeGadgetToken appxToken;

    FakeGadgetToken appyToken;

    DefaultRequestPipelineTest.FakeHttpFetcher fetcher;

    DefaultRequestPipelineTest.FakeOAuthRequestProvider oauth;

    DefaultRequestPipeline requestPipeline;

    HttpRequest signedRequest;

    @Override
    public void setUp() {
        cacheProvider = new LruCacheProvider(100);
        cache = new DefaultHttpCache(cacheProvider);
        service = new DefaultInvalidationService(cache, cacheProvider, new AtomicLong());
        appxToken = new FakeGadgetToken();
        appxToken.setAppId("AppX");
        appxToken.setOwnerId("OwnerX");
        appxToken.setViewerId("ViewerX");
        appyToken = new FakeGadgetToken();
        appyToken.setAppId("AppY");
        appyToken.setOwnerId("OwnerY");
        appyToken.setViewerId("ViewerY");
        control = EasyMock.createNiceControl();
        signedRequest = new HttpRequest(URI);
        signedRequest.setAuthType(AuthType.SIGNED);
        signedRequest.setSecurityToken(appxToken);
        signedRequest.setOAuthArguments(new OAuthArguments());
        signedRequest.getOAuthArguments().setUseToken(OAuthArguments.UseToken.NEVER);
        signedRequest.getOAuthArguments().setSignOwner(true);
        signedRequest.getOAuthArguments().setSignViewer(true);
        fetcher = new DefaultRequestPipelineTest.FakeHttpFetcher();
        oauth = new DefaultRequestPipelineTest.FakeOAuthRequestProvider();
        requestPipeline = new DefaultRequestPipeline(fetcher, cache, oauth, new NoOpImageRewriter(), service);
    }

    public void testInvalidateUrl() throws Exception {
        cache.addResponse(new HttpRequest(URI), CACHEABLE);
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
        service.invalidateApplicationResources(ImmutableSet.of(URI), appxToken);
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 0);
    }

    public void testInvalidateUsers() throws Exception {
        service.invalidateUserResources(ImmutableSet.of("example.org:1", "example.org:2"), appxToken);
        service.invalidateUserResources(ImmutableSet.of("example.org:1", "example.org:2"), appyToken);
        assertEquals(cacheProvider.createCache(DefaultInvalidationService.CACHE_NAME).getSize(), 4);
        assertNotNull(cacheProvider.createCache(DefaultInvalidationService.CACHE_NAME).getElement("INV_TOK:AppX:1"));
        assertNotNull(cacheProvider.createCache(DefaultInvalidationService.CACHE_NAME).getElement("INV_TOK:AppX:2"));
        assertNotNull(cacheProvider.createCache(DefaultInvalidationService.CACHE_NAME).getElement("INV_TOK:AppY:1"));
        assertNotNull(cacheProvider.createCache(DefaultInvalidationService.CACHE_NAME).getElement("INV_TOK:AppY:2"));
    }

    public void testFetchWithInvalidationEnabled() throws Exception {
        cache.addResponse(new HttpRequest(URI), CACHEABLE);
        assertEquals(requestPipeline.execute(new HttpRequest(URI)), CACHEABLE);
    }

    public void testFetchInvalidatedContent() throws Exception {
        cache.addResponse(new HttpRequest(URI), CACHEABLE);
        service.invalidateApplicationResources(ImmutableSet.of(URI), appxToken);
        fetcher.response = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT1").create();
        assertEquals(requestPipeline.execute(new HttpRequest(URI)), fetcher.response);
    }

    public void testFetchContentWithMarker() throws Exception {
        oauth.httpResponse = CACHEABLE;
        HttpResponse httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse, CACHEABLE);
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
        service.invalidateUserResources(ImmutableSet.of("OwnerX"), appxToken);
        oauth.httpResponse = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT1").create();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse.getResponseAsString(), "NEWCONTENT1");
        assertEquals(httpResponse.getHeader(DefaultInvalidationService.INVALIDATION_HEADER), "o=1;");
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
        service.invalidateUserResources(ImmutableSet.of("ViewerX"), appxToken);
        oauth.httpResponse = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT2").create();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse.getResponseAsString(), "NEWCONTENT2");
        assertEquals(httpResponse.getHeader(DefaultInvalidationService.INVALIDATION_HEADER), "o=1;v=2;");
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
    }

    public void testFetchContentSignedOwner() throws Exception {
        oauth.httpResponse = CACHEABLE;
        signedRequest.getOAuthArguments().setSignViewer(false);
        HttpResponse httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse, CACHEABLE);
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
        service.invalidateUserResources(ImmutableSet.of("OwnerX"), appxToken);
        oauth.httpResponse = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT1").create();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse.getResponseAsString(), "NEWCONTENT1");
        assertEquals(httpResponse.getHeader(DefaultInvalidationService.INVALIDATION_HEADER), "o=1;");
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
        service.invalidateUserResources(ImmutableSet.of("ViewerX"), appxToken);
        oauth.httpResponse = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT2").create();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse.getResponseAsString(), "NEWCONTENT1");
        assertEquals(httpResponse.getHeader(DefaultInvalidationService.INVALIDATION_HEADER), "o=1;");
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
    }

    public void testFetchContentSignedViewer() throws Exception {
        oauth.httpResponse = CACHEABLE;
        signedRequest.getOAuthArguments().setSignOwner(false);
        HttpResponse httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse, CACHEABLE);
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
        service.invalidateUserResources(ImmutableSet.of("OwnerX"), appxToken);
        oauth.httpResponse = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT1").create();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse, CACHEABLE);
        service.invalidateUserResources(ImmutableSet.of("ViewerX"), appxToken);
        oauth.httpResponse = new HttpResponseBuilder(CACHEABLE).setResponseString("NEWCONTENT2").create();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse.getResponseAsString(), "NEWCONTENT2");
        assertEquals(httpResponse.getHeader(DefaultInvalidationService.INVALIDATION_HEADER), "v=2;");
        assertEquals(cacheProvider.createCache(DefaultHttpCache.CACHE_NAME).getSize(), 1);
    }

    public void testServeInvalidatedContentWithFetcherError() throws Exception {
        oauth.httpResponse = CACHEABLE;
        HttpResponse httpResponse = requestPipeline.execute(signedRequest);
        service.invalidateUserResources(ImmutableSet.of("OwnerX"), appxToken);
        oauth.httpResponse = HttpResponse.error();
        httpResponse = requestPipeline.execute(signedRequest);
        assertEquals(httpResponse, CACHEABLE);
    }
}
