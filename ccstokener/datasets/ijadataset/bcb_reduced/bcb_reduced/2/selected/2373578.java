package org.opennms.netmgt.provision.detector.web.client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map.Entry;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.provision.detector.web.request.WebRequest;
import org.opennms.netmgt.provision.detector.web.response.WebResponse;
import org.opennms.netmgt.provision.support.Client;

/**
 * <p>WebClient class.</p>
 *
 * @author Alejandro Galue <agalue@opennms.org>
 * @version $Id: $
 */
public class WebClient implements Client<WebRequest, WebResponse> {

    private DefaultHttpClient m_httpClient;

    private HttpGet m_httpMethod;

    private String schema;

    private String path;

    public WebClient() {
        m_httpClient = new DefaultHttpClient();
    }

    @Override
    public void connect(InetAddress address, int port, int timeout) throws IOException, Exception {
        m_httpMethod = new HttpGet(URIUtils.createURI(schema, InetAddressUtils.str(address), port, path, null, null));
        setTimeout(timeout);
    }

    @Override
    public void close() {
    }

    @Override
    public WebResponse receiveBanner() throws IOException, Exception {
        return null;
    }

    @Override
    public WebResponse sendRequest(WebRequest request) throws IOException, Exception {
        for (Entry<String, String> entry : request.getHeaders().entrySet()) {
            m_httpMethod.addHeader(entry.getKey(), entry.getValue());
        }
        try {
            HttpResponse response = m_httpClient.execute(m_httpMethod);
            return new WebResponse(request, response);
        } catch (Exception e) {
            log().info(e.getMessage(), e);
            return new WebResponse(request, null);
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setTimeout(int timeout) {
        if (timeout > 0) {
            m_httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
            m_httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
        }
    }

    public void setUserAgent(String userAgent) {
        m_httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
    }

    public void setVirtualHost(String virtualHost, int virtualPort) {
        if (virtualHost == null || virtualPort == 0) return;
        m_httpClient.getParams().setParameter(ClientPNames.VIRTUAL_HOST, new HttpHost(virtualHost, virtualPort));
    }

    public void setUseHttpV1(boolean useHttpV1) {
        if (useHttpV1) {
            m_httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
        }
    }

    public void setAuth(String userName, String password) {
        log().debug("enabling user authentication using credentials for " + userName);
        m_httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
    }

    public void setAuthPreemtive(boolean authPreemtive) {
        HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {

            public void process(final HttpRequest request, final HttpContext context) throws IOException {
                AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
                    Credentials creds = credsProvider.getCredentials(authScope);
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }
        };
        m_httpClient.addRequestInterceptor(preemptiveAuth, 0);
    }

    protected ThreadCategory log() {
        return ThreadCategory.getInstance(getClass());
    }
}
