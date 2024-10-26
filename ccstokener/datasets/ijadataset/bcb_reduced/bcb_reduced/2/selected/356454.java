package com.googlecode.lighthttp.impl;

import com.googlecode.lighthttp.EntityEnclosingWebRequest;
import com.googlecode.lighthttp.HttpConstants;
import com.googlecode.lighthttp.WebBrowser;
import com.googlecode.lighthttp.WebRequest;
import com.googlecode.lighthttp.WebResponse;
import com.googlecode.lighthttp.impl.request.HttpGetWebRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Default implementation of {@link WebBrowser}
 * Has such configurable parameters:
 * - default headers    : Collection of HTTP headers which will be sent with every http request
 * - retry count        : count of repeating http request if previous one was unsuccessful
 * - connection timeout : time in milliseconds which determine connection timeout of HTTP request
 * - socket timeout     : time in milliseconds which determine socket timeout of HTTP request
 *
 * @author Sergey Prilukin
 * @version $Id: DefaultWebBrowser.java 41 2011-10-13 12:33:21Z sprilukin@gmail.com $
 */
public class DefaultWebBrowser implements WebBrowser {

    public static final Log log = LogFactory.getLog(DefaultWebBrowser.class);

    protected HttpClient httpClient;

    protected CookieStore cookieStore = new BasicCookieStore();

    protected Map<String, String> defaultHeaders = new HashMap<String, String>();

    protected int retryCount = WebBrowserConstants.DEFAULT_RETRY_COUNT;

    protected int socketTimeout = WebBrowserConstants.DEFAULT_SOCKET_TIMEOUT;

    protected int connectionTimeout = WebBrowserConstants.DEFAULT_CONNECTION_TIMEOUT;

    protected ThreadLocal<HttpRequestBase> httpRequest = new ThreadLocal<HttpRequestBase>();

    private HttpParams httpParams;

    private String clientConnectionFactoryClassName = WebBrowserConstants.DEFAULT_CLIENT_CONNECTION_FACTORY_CLASS_NAME;

    private boolean threadSafe = false;

    private boolean initialized = false;

    static class GzipDecompressingEntity extends HttpEntityWrapper {

        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }

    /**
     * Allows to set httpClient implementation directly
     * @param httpClient instance of {@link HttpClient}
     */
    public void setHttpClient(HttpClient httpClient) {
        synchronized (this) {
            this.httpClient = httpClient;
            this.initialized = false;
        }
    }

    /**
     * Allows to set {@link HttpParams}
     * Will take effect only if httpClient is initialized inside DefaultWebBrowser
     * @param httpParams {@link HttpParams} to set.
     */
    public void setHttpParams(HttpParams httpParams) {
        this.httpParams = httpParams;
        if (this.httpParams != null && this.httpParams.getParameter(ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME) == null) {
            this.httpParams.setParameter(ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME, clientConnectionFactoryClassName);
        }
        if (httpClient != null && httpClient instanceof DefaultHttpClient) {
            synchronized (this) {
                httpClient = null;
                this.initialized = false;
            }
        }
    }

    private HttpParams getBasicHttpParams() {
        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
        params.setParameter(CoreProtocolPNames.USER_AGENT, WebBrowserConstants.DEFAULT_USER_AGENT);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
        params.setParameter(ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME, clientConnectionFactoryClassName);
        params.setParameter(ClientConnectionManagerFactoryImpl.THREAD_SAFE_CONNECTION_MANAGER, this.threadSafe);
        return params;
    }

    private void addGZIPResponseInterceptor(HttpClient httpClient) {
        if (AbstractHttpClient.class.isAssignableFrom(httpClient.getClass())) {
            ((AbstractHttpClient) httpClient).addResponseInterceptor(new HttpResponseInterceptor() {

                public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
                    HttpEntity entity = response.getEntity();
                    if (entity == null) {
                        log.debug("LIGHTHTTP. Response entity is NULL");
                        return;
                    }
                    Header contentEncodingHeader = entity.getContentEncoding();
                    if (contentEncodingHeader != null) {
                        HeaderElement[] codecs = contentEncodingHeader.getElements();
                        for (HeaderElement codec : codecs) {
                            if (codec.getName().equalsIgnoreCase(HttpConstants.GZIP)) {
                                response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                                return;
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Default constructor
     * apache http client will initialized here as not thread safe http client
     */
    public DefaultWebBrowser() {
        this(false);
    }

    /**
     * Constructor which allows to specify if webBrowser should be thread Safe
     * @param threadSafe if {@code true} then webBrowser will be thread safe
     * and not thread safe - if {@code false}
     */
    public DefaultWebBrowser(boolean threadSafe) {
        this.threadSafe = threadSafe;
    }

    /**
     * Initialize new instance of httpClient
     */
    private void initHttpClient() {
        if (!this.initialized) {
            synchronized (this) {
                if (!this.initialized) {
                    if (httpClient == null) {
                        if (httpParams == null) {
                            httpParams = getBasicHttpParams();
                        }
                        httpClient = new DefaultHttpClient(null, getBasicHttpParams());
                        addGZIPResponseInterceptor(httpClient);
                    }
                    this.initialized = true;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultHeaders(final Map<String, String> defaultHeaders) {
        this.defaultHeaders.clear();
        for (Map.Entry<String, String> entryObject : defaultHeaders.entrySet()) {
            String headerName = entryObject.getKey();
            String headerValue = entryObject.getValue();
            this.addHeader(headerName, headerValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultHeaders(Properties defaultHeaders) {
        this.defaultHeaders.clear();
        for (Map.Entry<Object, Object> entry : defaultHeaders.entrySet()) {
            String headerName = String.valueOf(entry.getKey());
            String headerValue = String.valueOf(entry.getValue());
            this.addHeader(headerName, headerValue);
        }
    }

    /**
     * Makes default initialization of HttpMethodBase before any request
     * such as cookie policy, retrycount, timeout
     *
     * @param httpMethodBase {@link HttpRequestBase} for making default initialization
     */
    private void setDefaultMethodParams(final HttpRequestBase httpMethodBase) {
        httpMethodBase.getParams().setBooleanParameter(CookieSpecPNames.SINGLE_COOKIE_HEADER, true);
        httpMethodBase.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        if (AbstractHttpClient.class.isAssignableFrom(httpClient.getClass())) {
            ((AbstractHttpClient) httpClient).setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(retryCount, true));
        }
        httpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
    }

    /**
     * Add default headers to web request and
     * then add specific headers for current request
     *
     * @param httpMethodBase http method for adding headers
     * @param methodHeaders  headers specific for current request
     */
    private void setHeaders(final HttpRequestBase httpMethodBase, final Map<String, String> methodHeaders) {
        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            httpMethodBase.setHeader(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : methodHeaders.entrySet()) {
            httpMethodBase.setHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Execute specified request
     *
     * @param httpUriRequest request to execute
     * @return code of http response
     * @throws java.io.IOException if errors occurs during request
     * @throws java.net.SocketTimeoutException if timeout occurs see params
     *          settings in {@link #setDefaultMethodParams} method
     */
    private HttpResponse executeMethod(HttpUriRequest httpUriRequest) throws IOException {
        if (log.isDebugEnabled()) {
            for (Header header : httpUriRequest.getAllHeaders()) {
                log.debug(String.format("LIGHTHTTP. Request header: [%s: %s]", header.getName(), header.getValue()));
            }
        }
        HttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        return httpClient.execute(httpUriRequest, localContext);
    }

    /**
     * {@inheritDoc}
     */
    public WebResponse getResponse(String url) throws IOException {
        return getResponse(url, null);
    }

    public WebResponse getResponse(String url, String expectedResponseCharset) throws IOException {
        WebRequest req = new HttpGetWebRequest(url);
        return getResponse(req, expectedResponseCharset);
    }

    /**
     * Return {@link HttpRequestBase} from WebRequest which is
     * shell on http GET or DELETE request
     *
     * @param webRequest shell under http GET request
     * @param httpRequest HttpRequestBase instance
     * @return HttpMethodBase for specified shell on http GET request
     */
    private HttpRequestBase populateHttpRequestBaseMethod(WebRequest webRequest, HttpRequestBase httpRequest) {
        setDefaultMethodParams(httpRequest);
        setHeaders(httpRequest, webRequest.getHeaders());
        return httpRequest;
    }

    /**
     * Return {@link HttpRequestBase} from WebRequest which is
     * shell on http POST or PUT request
     *
     * @param webRequest shell under http POST request
     * @param httpRequest HttpEntityEnclosingRequestBase instance
     * @return HttpMethodBase for specified shell on http POST request
     */
    private HttpRequestBase populateHttpEntityEnclosingRequestBaseMethod(WebRequest webRequest, HttpEntityEnclosingRequestBase httpRequest) {
        EntityEnclosingWebRequest webRequestWithBody = (EntityEnclosingWebRequest) webRequest;
        setDefaultMethodParams(httpRequest);
        setHeaders(httpRequest, webRequestWithBody.getHeaders());
        HttpEntity entity = null;
        if (webRequestWithBody.getFormParams() != null && webRequestWithBody.getFormParams().size() > 0) {
            StringBuilder contentType = (new StringBuilder(HttpConstants.MIME_FORM_ENCODED)).append("; charset=").append(webRequestWithBody.getFormParamsCharset());
            httpRequest.addHeader(HTTP.CONTENT_TYPE, contentType.toString());
            List<NameValuePair> nameValuePairList = null;
            Map<String, String> requestParams = webRequestWithBody.getFormParams();
            if ((requestParams != null) && (requestParams.size() > 0)) {
                nameValuePairList = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry : requestParams.entrySet()) {
                    nameValuePairList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            }
            if (nameValuePairList != null) {
                try {
                    entity = new UrlEncodedFormEntity(nameValuePairList, HTTP.UTF_8);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (webRequestWithBody.getParts().size() > 0) {
            entity = new MultipartEntity();
            for (Map.Entry<String, ContentBody> entry : webRequestWithBody.getParts().entrySet()) {
                ((MultipartEntity) entity).addPart(entry.getKey(), entry.getValue());
            }
        }
        if (entity != null) {
            httpRequest.setEntity(entity);
        }
        return httpRequest;
    }

    /**
     * If {@link HttpRequestBase} httpMethodBase has :location: header in response headers then
     * redirect will be perfirmed
     *
     * @param response {@link org.apache.http.HttpResponse} which will be wrapped into {@link WebResponse}
     * @param httpMethodBase {@link HttpRequestBase} with original request
     * @param charset charset of response text content
     * @return web response which is not needed in redirects
     * @throws java.io.IOException if errors occured during executing redirect
     */
    private WebResponse processResponse(HttpResponse response, HttpRequestBase httpMethodBase, String charset) throws IOException {
        if (log.isDebugEnabled()) {
            for (Header header : response.getAllHeaders()) {
                log.debug(String.format("LIGHTHTTP. Response header: [%s: %s]", header.getName(), header.getValue()));
            }
        }
        return new HttpWebResponse(response, httpMethodBase, charset);
    }

    /**
     * {@inheritDoc}
     */
    public WebResponse getResponse(WebRequest webRequest) throws IOException {
        return getResponse(webRequest, null);
    }

    /**
     * {@inheritDoc}
     */
    public WebResponse getResponse(WebRequest webRequest, String charset) throws IOException {
        initHttpClient();
        switch(webRequest.getRequestMethod()) {
            case GET:
                httpRequest.set(populateHttpRequestBaseMethod(webRequest, new HttpGet(webRequest.getUrl())));
                break;
            case HEAD:
                httpRequest.set(populateHttpRequestBaseMethod(webRequest, new HttpHead(webRequest.getUrl())));
                break;
            case OPTIONS:
                httpRequest.set(populateHttpRequestBaseMethod(webRequest, new HttpOptions(webRequest.getUrl())));
                break;
            case TRACE:
                httpRequest.set(populateHttpRequestBaseMethod(webRequest, new HttpTrace(webRequest.getUrl())));
                break;
            case DELETE:
                httpRequest.set(populateHttpRequestBaseMethod(webRequest, new HttpDelete(webRequest.getUrl())));
                break;
            case POST:
                httpRequest.set(populateHttpEntityEnclosingRequestBaseMethod(webRequest, new HttpPost(webRequest.getUrl())));
                break;
            case PUT:
                httpRequest.set(populateHttpEntityEnclosingRequestBaseMethod(webRequest, new HttpPut(webRequest.getUrl())));
                break;
            default:
                throw new RuntimeException("Method not yet supported: " + webRequest.getRequestMethod());
        }
        WebResponse resp;
        HttpResponse response = executeMethod(httpRequest.get());
        if (response == null) {
            throw new IOException("LIGHTHTTP. An empty response received from server. Possible reason: host is offline");
        }
        resp = processResponse(response, httpRequest.get(), charset);
        httpRequest.set(null);
        return resp;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(defaultHeaders);
    }

    /**
     * {@inheritDoc}
     */
    public String getHeader(String headerName) {
        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void addHeaders(Map<String, String> headers) {
        defaultHeaders.putAll(headers);
    }

    /**
     * {@inheritDoc}
     */
    public void addHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getRetryCount() {
        return retryCount;
    }

    /**
     * {@inheritDoc}
     */
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public void setConnectionTimeout(Integer connectionTimeout) {
        initHttpClient();
        this.connectionTimeout = connectionTimeout;
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
    }

    /**
     * {@inheritDoc}
     */
    public List<Cookie> getCookies() {
        return cookieStore.getCookies();
    }

    public Cookie getCookieByName(String name) {
        for (Cookie cookie : cookieStore.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void addCookie(Cookie cookie) {
        cookieStore.addCookie(cookie);
    }

    /**
     * {@inheritDoc}
     */
    public void addCookies(List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            BasicClientCookie apacheCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            apacheCookie.setPath(cookie.getPath());
            apacheCookie.setDomain(cookie.getDomain());
            cookieStore.addCookie(apacheCookie);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clearAllCookies() {
        cookieStore.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void setProxy(String url, int port) {
        initHttpClient();
        HttpHost proxy = new HttpHost(url, port);
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    /**
     * {@inheritDoc}
     */
    public void clearProxy() {
        initHttpClient();
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
    }

    /**
     * {@inheritDoc}
     */
    public void abort() {
        if (httpRequest.get() != null && !httpRequest.get().isAborted()) {
            httpRequest.get().abort();
        }
    }
}
