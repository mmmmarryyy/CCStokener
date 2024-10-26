package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.rewrite.image.ImageRewriter;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * A standard implementation of a request pipeline. Performs request caching and
 * signing on top of standard HTTP requests.
 */
@Singleton
public class DefaultRequestPipeline implements RequestPipeline {

    private final HttpFetcher httpFetcher;

    private final HttpCache httpCache;

    private final Provider<OAuthRequest> oauthRequestProvider;

    private final ImageRewriter imageRewriter;

    private final InvalidationService invalidationService;

    @Inject
    public DefaultRequestPipeline(HttpFetcher httpFetcher, HttpCache httpCache, Provider<OAuthRequest> oauthRequestProvider, ImageRewriter imageRewriter, InvalidationService invalidationService) {
        this.httpFetcher = httpFetcher;
        this.httpCache = httpCache;
        this.oauthRequestProvider = oauthRequestProvider;
        this.imageRewriter = imageRewriter;
        this.invalidationService = invalidationService;
    }

    public HttpResponse execute(HttpRequest request) throws GadgetException {
        normalizeProtocol(request);
        HttpResponse invalidatedResponse = null;
        if (!request.getIgnoreCache()) {
            HttpResponse cachedResponse = httpCache.getResponse(request);
            if (cachedResponse != null) {
                if (!cachedResponse.isStale()) {
                    if (invalidationService.isValid(request, cachedResponse)) {
                        return cachedResponse;
                    } else {
                        invalidatedResponse = cachedResponse;
                    }
                }
            }
        }
        HttpResponse fetchedResponse = null;
        switch(request.getAuthType()) {
            case NONE:
                fetchedResponse = httpFetcher.fetch(request);
                break;
            case SIGNED:
            case OAUTH:
                fetchedResponse = oauthRequestProvider.get().fetch(request);
                break;
            default:
                return HttpResponse.error();
        }
        if (fetchedResponse.isError() && invalidatedResponse != null) {
            return invalidatedResponse;
        }
        if (!fetchedResponse.isError() && !request.getIgnoreCache() && request.getCacheTtl() != 0) {
            fetchedResponse = imageRewriter.rewrite(request, fetchedResponse);
        }
        if (!request.getIgnoreCache()) {
            if (fetchedResponse.getCacheTtl() > 0) {
                fetchedResponse = invalidationService.markResponse(request, fetchedResponse);
            }
            httpCache.addResponse(request, fetchedResponse);
        }
        return fetchedResponse;
    }

    public void normalizeProtocol(HttpRequest request) throws GadgetException {
        if (request.getUri().getScheme() == null) {
            throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, "Url " + request.getUri().toString() + " does not include scheme");
        } else if (!"http".equals(request.getUri().getScheme()) && !"https".equals(request.getUri().getScheme())) {
            throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, "Invalid request url scheme in url: " + Utf8UrlCoder.encode(request.getUri().toString()) + "; only \"http\" and \"https\" supported.");
        }
    }
}
