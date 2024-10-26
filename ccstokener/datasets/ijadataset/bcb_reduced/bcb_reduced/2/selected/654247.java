package eu.etaxonomy.security.shibboleth.shibproxy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLKeyException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.VersionInfo;

/**
 * TODO Class SslProxyHandler
 * 
 * @author Lutz Suhrbier (suhrbier@inf.fu-berlin.de)
 * 
 */
public class SSLProxyHandler implements HttpRequestHandler {

    private static final Log logger = LogFactory.getLog(SSLProxyHandler.class);

    protected HttpHost clientProxy = null;

    protected static DefaultHttpClient httpclient = null;

    protected SSLProxyHandler() {
        super();
    }

    public SSLProxyHandler(HttpHost clientProxy, KeyStore trustStore, KeyStore keyStore, String keyStorePassword) throws SSLKeyException {
        super();
        this.clientProxy = clientProxy;
        httpclient = createHttpClient(createClientConnectionManager(createHttpParams(), createSchemeRegistry(createSSLSocketFactory(trustStore, keyStore, keyStorePassword))), createHttpParams());
        LogUtils.trace(logger, "HttpClient created");
        LogUtils.trace(logger, "SSLProxyHandler created");
    }

    protected SSLSocketFactory createSSLSocketFactory(KeyStore trustStore, KeyStore keyStore, String keyStorePassword) throws SSLKeyException {
        try {
            SSLSocketFactory sslSocketFactory = new SSLSocketFactory(keyStore, keyStorePassword, trustStore);
            if (trustStore == null) sslSocketFactory.setHostnameVerifier(new AllowAllHostnameVerifier());
            return sslSocketFactory;
        } catch (Exception e) {
            String message = "SSLSocketFactory creation failed: " + e.getMessage() + "(" + e.getClass().getName() + ")";
            LogUtils.fatal(logger, message);
            throw new SSLKeyException(message);
        }
    }

    protected SchemeRegistry createSchemeRegistry(SSLSocketFactory sslSocketFactory) {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        if (sslSocketFactory == null) sslSocketFactory = SSLSocketFactory.getSocketFactory();
        SchemeFactory.registerScheme(schemeRegistry, SchemeFactory.createHttpsScheme(sslSocketFactory, SchemeFactory.HTTPS_PORT));
        SchemeFactory.registerScheme(schemeRegistry, SchemeFactory.createHttpScheme(SchemeFactory.HTTP_PORT));
        return schemeRegistry;
    }

    protected ClientConnectionManager createClientConnectionManager(HttpParams params, SchemeRegistry schemeRegistry) {
        if (params == null) {
            params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        }
        if (schemeRegistry == null) schemeRegistry = createSchemeRegistry(null);
        return new ThreadSafeClientConnManager(params, schemeRegistry);
    }

    protected DefaultHttpClient createHttpClient(ClientConnectionManager ccm, HttpParams params) {
        if ((ccm == null) && (params == null)) return new DefaultHttpClient(); else if (ccm == null) return new DefaultHttpClient(params); else return new DefaultHttpClient(ccm, params);
    }

    protected HttpParams createHttpParams() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        final VersionInfo vi = VersionInfo.loadVersionInfo("org.apache.http.client", getClass().getClassLoader());
        final String release = (vi != null) ? vi.getRelease() : VersionInfo.UNAVAILABLE;
        HttpProtocolParams.setUserAgent(params, "Apache-HttpClient/" + release + " (java 1.4)");
        return params;
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (method.equalsIgnoreCase("CONNECT")) handleConnectRequest(request, response, context); else if (!((Boolean) context.getAttribute(SSLProxyServerConnection.SSLHANDSHAKE_DONE)).booleanValue()) handleHttpRequest(request, response, context); else handleHttpsRequest(request, response, context);
    }

    protected void handleConnectRequest(HttpRequest request, HttpResponse response, HttpContext context) {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, getConnectRequestProtocolVersion(request));
        try {
            openConnection(request);
            response.setStatusLine(getConnectRequestProtocolVersion(request), HttpStatus.SC_OK, "Connection established");
            context.setAttribute("sslProxyServerConnection.wantSSLHandshake", Boolean.TRUE);
            LogUtils.info(logger, "Connection established to " + getConnectRequestHostname(request) + ":" + getConnectRequestPort(request));
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            String message = "Opening connection to " + getConnectRequestHostname(request) + ":" + getConnectRequestPort(request) + "failed: ";
            LogUtils.error(logger, message);
        }
    }

    private void openConnection(HttpRequest request) throws ConnectException {
        HttpHost target = new HttpHost(getConnectRequestHostname(request), getConnectRequestPort(request), "https");
        HttpRoute route = HttpUtil.createProxyRoute(target, clientProxy);
        LogUtils.trace(logger, "route=" + route);
        try {
            this.httpclient.getConnectionManager().requestConnection(route, null).getConnection(15, TimeUnit.SECONDS).open(route, null, createHttpParams());
        } catch (Exception e) {
            String message = "Opening connection to " + getConnectRequestHostname(request) + ":" + getConnectRequestPort(request) + "failed: " + e.getMessage() + "(" + e.getClass().getName() + ")";
            LogUtils.fatal(logger, message);
            throw new ConnectException(message);
        }
    }

    protected void handleHttpRequest(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        try {
            HttpHost target = HttpUtil.getTargetHost(request, 80, "http");
            request.removeHeaders(HTTP.CONTENT_LEN);
            cleanHopByHopHeaders(request);
            HttpResponse clientResponse = httpclient.execute(target, request, context);
            cleanHopByHopHeaders(clientResponse);
            clientResponse.removeHeaders(HTTP.CONTENT_LEN);
            HttpUtil.copy(response, clientResponse);
        } catch (URISyntaxException e) {
            String message = "Error handling request to " + request.getRequestLine().getUri() + ": " + e.getMessage() + "(" + e.getClass().getName() + ")";
            LogUtils.fatal(logger, message);
            throw new HttpException(message);
        }
    }

    protected void handleHttpsRequest(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        try {
            HttpHost target = HttpUtil.getTargetHost(request, 443, "https");
            request.removeHeaders(HTTP.CONTENT_LEN);
            cleanHopByHopHeaders(request);
            HttpResponse clientResponse = httpclient.execute(target, request, context);
            cleanHopByHopHeaders(clientResponse);
            clientResponse.removeHeaders(HTTP.CONTENT_LEN);
            HttpUtil.copy(response, clientResponse);
        } catch (URISyntaxException e) {
            String message = "Error handling request to " + request.getRequestLine().getUri() + ": " + e.getMessage() + "(" + e.getClass().getName() + ")";
            LogUtils.fatal(logger, message);
            throw new HttpException(message);
        }
    }

    protected String getConnectRequestHostname(HttpRequest request) {
        return request.getRequestLine().getUri().split(":")[0];
    }

    protected int getConnectRequestPort(HttpRequest request) {
        return Integer.parseInt(request.getRequestLine().getUri().split(":")[1]);
    }

    protected ProtocolVersion getConnectRequestProtocolVersion(HttpRequest request) {
        return request.getRequestLine().getProtocolVersion();
    }

    protected void cleanHopByHopHeaders(HttpMessage message) {
        if ((message.getFirstHeader(HTTP.CONN_DIRECTIVE) != null) && (message.getFirstHeader(HTTP.CONN_DIRECTIVE).getValue().indexOf(HTTP.CONN_CLOSE) == -1)) {
            message.removeHeaders(HTTP.CONN_DIRECTIVE);
        }
        message.removeHeaders(HTTP.CONN_KEEP_ALIVE);
        message.removeHeaders("Public");
        message.removeHeaders("Proxy-Authenticate");
        message.removeHeaders(HTTP.TRANSFER_ENCODING);
        message.removeHeaders("Upgrade");
    }
}
