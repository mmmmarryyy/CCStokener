package au.edu.diasb.annotation.danno.protocol;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthSchemeRegistry;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultTargetAuthenticationHandler;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.params.HttpParams;
import au.edu.diasb.chico.mvc.MimeTypes;

/**
 * This base class is NOT thread-safe and NOT reentrant, but instances are
 * serially reusable by one thread.
 * 
 * @author scrawley
 */
public abstract class DannoClientBase implements DannoClient {

    private static class DannoAuthenticationHandler extends DefaultTargetAuthenticationHandler {

        private static final List<String> DEFAULT_SCHEME_PRIORITY = Collections.unmodifiableList(Arrays.asList(new String[] { "redirect", "ntlm", "digest", "basic" }));

        @Override
        protected List<String> getAuthPreferences() {
            return DEFAULT_SCHEME_PRIORITY;
        }
    }

    private static class DannoHttpClient extends DefaultHttpClient {

        public DannoHttpClient() {
            super();
        }

        public DannoHttpClient(ClientConnectionManager conman, HttpParams params) {
            super(conman, params);
        }

        public DannoHttpClient(HttpParams params) {
            super(params);
        }

        @Override
        protected AuthenticationHandler createTargetAuthenticationHandler() {
            return new DannoAuthenticationHandler();
        }

        @Override
        protected AuthSchemeRegistry createAuthSchemeRegistry() {
            AuthSchemeRegistry registry = super.createAuthSchemeRegistry();
            registry.register("redirect", new RedirectSchemeFactory());
            return registry;
        }
    }

    private DefaultHttpClient client;

    private HttpResponse lastResponse;

    private boolean shutdown;

    private ClientConnectionManager conman;

    private HttpParams httpParams;

    private boolean useHttps;

    public DannoClientBase(boolean useHttps) {
        createHttpClient(useHttps);
    }

    protected void createHttpClient(boolean useHttps) {
        this.useHttps = useHttps;
        this.shutdown = conman == null;
        this.client = httpParams == null ? new DannoHttpClient() : conman == null ? new DannoHttpClient(httpParams) : new DannoHttpClient(conman, httpParams);
        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(this.client.getConnectionManager().getSchemeRegistry(), ProxySelector.getDefault());
        this.client.setRoutePlanner(routePlanner);
    }

    public final void setClientConnectionManager(ClientConnectionManager conman) {
        this.conman = conman;
    }

    public final void setHttpParams(HttpParams httpParams) {
        this.httpParams = httpParams;
    }

    public static URI addQueryParameter(URI uri, String name, String value) throws ProtocolException {
        StringBuilder sb = new StringBuilder();
        try {
            if (value == null || value.isEmpty()) {
                return uri;
            }
            String query = uri.getRawQuery();
            StringBuilder qsb = new StringBuilder();
            if (query != null && query.length() > 0) {
                qsb.append(query).append("&");
            }
            qsb.append(name).append("=").append(URICodec.encodeQuery(value, "UTF-8"));
            query = qsb.toString();
            sb.append(uri.getScheme()).append("://").append(uri.getRawAuthority());
            sb.append(uri.getRawPath()).append('?').append(query);
            String fragment = uri.getRawFragment();
            if (fragment != null) {
                sb.append('#').append(fragment);
            }
            return new URI(sb.toString());
        } catch (URISyntaxException ex) {
            throw new ProtocolException("Invalid request URI: " + sb, ex);
        }
    }

    @Override
    public final RDFResponse executeRDF(HttpUriRequest request) throws IOException, ProtocolException {
        request.addHeader("Accept", MimeTypes.XML_RDF);
        doExecute(request);
        try {
            return new RDFResponseHandler().handleResponse(lastResponse);
        } finally {
            if (lastResponse != null && lastResponse.getEntity() != null) {
                lastResponse.getEntity().consumeContent();
            }
        }
    }

    @Override
    public final String executeHTML(HttpUriRequest request) throws IOException, ProtocolException {
        request.addHeader("Accept", MimeTypes.HTML_MIMETYPE);
        doExecute(request);
        try {
            return new BasicResponseHandler().handleResponse(lastResponse);
        } finally {
            if (lastResponse != null && lastResponse.getEntity() != null) {
                lastResponse.getEntity().consumeContent();
            }
        }
    }

    @Override
    public final boolean executeIgnore(HttpUriRequest request) throws IOException, ProtocolException {
        doExecute(request);
        try {
            return isOK();
        } finally {
            if (lastResponse != null && lastResponse.getEntity() != null) {
                lastResponse.getEntity().consumeContent();
            }
        }
    }

    @Override
    public final HttpResponse execute(HttpUriRequest request) throws IOException, ProtocolException {
        doExecute(request);
        return lastResponse;
    }

    protected void doExecute(HttpUriRequest request) throws IOException, ProtocolException {
        if (useHttps) {
            URI uri = request.getURI();
            if (!"https".equals(uri.getScheme().toLowerCase())) {
                try {
                    uri = new URI("https", uri.getSchemeSpecificPart(), uri.getFragment());
                    uri = new URI(uri.toString().replace("&", "%26"));
                    request = replaceRequestUri(request, uri);
                } catch (URISyntaxException ex) {
                    throw new ProtocolException("Invalid request URI: " + uri, ex);
                }
            }
        }
        lastResponse = client.execute(request);
    }

    private HttpUriRequest replaceRequestUri(HttpUriRequest request, URI uri) throws ProtocolException {
        RequestWrapper res;
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest er = (HttpEntityEnclosingRequest) request;
            EntityEnclosingRequestWrapper wr = new EntityEnclosingRequestWrapper(er);
            wr.setEntity(er.getEntity());
            res = wr;
        } else {
            res = new RequestWrapper(request);
        }
        res.setURI(uri);
        res.resetHeaders();
        return res;
    }

    @Override
    public HttpResponse getLastResponse() {
        return lastResponse;
    }

    @Override
    public boolean isOK() {
        if (lastResponse == null) {
            return false;
        }
        int status = lastResponse.getStatusLine().getStatusCode();
        return status == HttpServletResponse.SC_OK || status == HttpServletResponse.SC_CREATED;
    }

    @Override
    public void close() {
        if (client != null) {
            if (shutdown) {
                client.getConnectionManager().shutdown();
            }
            client = null;
        }
    }
}
