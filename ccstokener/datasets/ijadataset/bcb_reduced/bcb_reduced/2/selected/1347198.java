package foursquare4j.oauth;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.OAuthResponseMessage;
import net.oauth.client.URLConnectionClient;
import net.oauth.http.HttpMessage;
import net.oauth.http.HttpResponseMessage;
import foursquare4j.Foursquare;
import foursquare4j.FoursquareBase;
import foursquare4j.exception.AuthenticationException;
import foursquare4j.exception.FoursquareException;
import foursquare4j.exception.RateLimitingException;
import foursquare4j.types.Credentials;
import foursquare4j.types.Error;
import foursquare4j.xml.handler.CredentialsHandler;
import foursquare4j.xml.handler.ErrorHandler;
import foursquare4j.xml.handler.Handler;

public class FoursquareOAuthImpl extends FoursquareBase {

    private static final OAuthServiceProvider OAUTH_SERVICE_PROVIDER = new OAuthServiceProvider(Foursquare.REQUEST_TOKEN_URL, Foursquare.USER_AUTHORIZATION_URL, Foursquare.ACCESS_TOKEN_URL);

    protected Credentials credentials;

    protected final OAuthAccessor accessor;

    public FoursquareOAuthImpl(final OAuthConsumer consumer) {
        accessor = new OAuthAccessor(new net.oauth.OAuthConsumer(Foursquare.CALLBACK_URL, consumer.consumerKey, consumer.consumerSecret, OAUTH_SERVICE_PROVIDER));
    }

    public FoursquareOAuthImpl(final OAuthConsumer consumer, final Credentials credentials) {
        this(consumer);
        setCredentials(credentials);
    }

    protected void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
        if (credentials == null) return;
        accessor.accessToken = credentials.getAccessToken();
        accessor.tokenSecret = credentials.getTokenSecret();
    }

    public Credentials authexchange(final String username, final String password) throws FoursquareException {
        final Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("fs_username", username);
        parameters.put("fs_password", password);
        return execute(HttpMethod.POST, Foursquare.API_AUTHEXCHANGE_URL, parameters, new CredentialsHandler(), ParameterStyle.BODY);
    }

    public Credentials authentication(final String username, final String password) throws FoursquareException {
        if (credentials != null) throw new IllegalStateException("authenticated.");
        setCredentials(authexchange(username, password));
        return credentials;
    }

    public Credentials getRequestToken() throws FoursquareException {
        final OAuthAccessor accessor = this.accessor.clone();
        final OAuthClient client = new OAuthClient(new URLConnectionClient());
        try {
            client.getRequestToken(accessor);
            final Credentials credentials = new Credentials();
            credentials.setRequestToken(accessor.requestToken);
            credentials.setTokenSecret(accessor.tokenSecret);
            return credentials;
        } catch (final Exception e) {
            throw new FoursquareException(e);
        }
    }

    public String getAuthorizationUrl(final Credentials credentials) throws FoursquareException {
        return OAUTH_SERVICE_PROVIDER.userAuthorizationURL.concat("?oauth_token=").concat(credentials.getRequestToken());
    }

    @Override
    protected <T> T execute(final HttpMethod method, final String url, final Parameters parameters, final Handler<T> handler) throws FoursquareException {
        return execute(method, url, parameters, handler, ParameterStyle.AUTHORIZATION_HEADER);
    }

    protected <T> T execute(final HttpMethod method, final String url, final Map<String, String> parameters, final Handler<T> handler, final ParameterStyle oauthParameterStyle) throws FoursquareException {
        try {
            final OAuthMessage request = accessor.newRequestMessage(method.name(), url, parameters.entrySet());
            final OAuthClient client = new OAuthClient(new URLConnectionClientWithUserAgent(userAgent));
            final OAuthResponseMessage response = client.access(request, oauthParameterStyle);
            final int statusCode = response.getHttpResponse().getStatusCode();
            if (statusCode / 100 != 2) {
                final Error error = parseBody(response.getBodyAsStream(), new ErrorHandler());
                if (error == null) throw response.toOAuthProblemException(); else if ("error".equals(error.getType())) throw new FoursquareException(error.getMessage()); else if ("unauthorized".equals(error.getType())) throw new AuthenticationException(error.getMessage()); else if ("ratelimited".equals(error.getType())) throw new RateLimitingException(error.getMessage()); else throw response.toOAuthProblemException();
            }
            return parseBody(response.getBodyAsStream(), handler);
        } catch (final OAuthException e) {
            throw new FoursquareException(e);
        } catch (final Exception e) {
            throw new FoursquareException(e);
        }
    }

    protected static class URLConnectionClientWithUserAgent extends URLConnectionClient {

        private final String userAgent;

        public URLConnectionClientWithUserAgent(final CharSequence userAgent) {
            if (userAgent == null) throw new NullPointerException("userAgent is null.");
            this.userAgent = userAgent.toString();
        }

        @Override
        public HttpResponseMessage execute(final HttpMessage request, final Map<String, Object> parameters) throws IOException {
            request.headers.add(new OAuth.Parameter("User-Agent", userAgent));
            return super.execute(request, parameters);
        }
    }
}
