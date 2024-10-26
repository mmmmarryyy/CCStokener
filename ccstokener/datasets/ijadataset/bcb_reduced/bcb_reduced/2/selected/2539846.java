package eu.etaxonomy.security.shibboleth.shibproxy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStore;
import javax.net.ssl.SSLKeyException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CookieStore;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * TODO Class ShibbolethProxyHandler
 * 
 * @author Lutz Suhrbier (suhrbier@inf.fu-berlin.de)
 * 
 */
public class ShibbolethProxyHandler extends SSLProxyHandler {

    private static final Log logger = LogFactory.getLog(ShibbolethProxyHandler.class);

    private Credentials userCredentials;

    public ShibbolethProxyHandler(HttpHost clientProxy, KeyStore trustStore, KeyStore keyStore, String keyStorePassword, Credentials userCredentials, CookieStore cookieStore) throws SSLKeyException {
        this.clientProxy = clientProxy;
        this.userCredentials = userCredentials;
        httpclient = createHttpClient(createClientConnectionManager(createHttpParams(), createSchemeRegistry(createSSLSocketFactory(trustStore, keyStore, keyStorePassword))), createHttpParams());
        httpclient.setCookieStore(cookieStore);
        LogUtils.trace(logger, "HTTPClient created");
        LogUtils.trace(logger, "SSLProxyHandler created");
    }

    protected DefaultHttpClient createHttpClient(ClientConnectionManager ccm, HttpParams params) {
        LogUtils.trace(logger, "ShibbolethHttpClient created");
        return new ShibbolethHttpClient(ccm, params, this.userCredentials);
    }

    protected void handleHttpsRequest(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        try {
            HttpHost target = HttpUtil.getTargetHost(request, 443, "https");
            cleanHopByHopHeaders(request);
            request.removeHeaders(HTTP.CONTENT_LEN);
            HttpResponse clientResponse = ((ShibbolethHttpClient) httpclient).executeShibboleth(target, request);
            cleanHopByHopHeaders(clientResponse);
            clientResponse.removeHeaders(HTTP.CONTENT_LEN);
            HttpUtil.copy(response, clientResponse);
        } catch (URISyntaxException e) {
            String message = "Error handling request to " + request.getRequestLine().getUri() + ": " + e.getMessage() + "(" + e.getClass().getName() + ")";
            LogUtils.fatal(logger, message);
            throw new HttpException(message);
        } catch (InterruptedException e) {
            String message = "Error handling request to " + request.getRequestLine().getUri() + ": " + e.getMessage() + "(" + e.getClass().getName() + ")";
            LogUtils.fatal(logger, message);
            throw new HttpException(message);
        }
    }
}
