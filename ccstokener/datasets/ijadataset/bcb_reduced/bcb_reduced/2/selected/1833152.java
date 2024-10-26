package de.wadndadn.deliciousj.impl;

import static de.wadndadn.commons.util.StringUtil.isEmpty;
import static de.wadndadn.commons.util.Util.isEmpty;
import static de.wadndadn.deliciousj.auth.AuthenticationStrategyFactoryImpl.createAuthenticationStrategyFactory;
import static java.lang.System.currentTimeMillis;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.apache.http.client.utils.URLEncodedUtils.format;
import static org.apache.http.conn.params.ConnRoutePNames.DEFAULT_PROXY;
import static org.apache.http.params.HttpProtocolParams.setUserAgent;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import de.wadndadn.commons.auth.UsernamePasswordCredentials;
import de.wadndadn.deliciousj.ApiConfiguration;
import de.wadndadn.deliciousj.ApiVersion;
import de.wadndadn.deliciousj.auth.AuthenticationStrategy;
import de.wadndadn.deliciousj.auth.AuthenticationStrategyFactory;
import de.wadndadn.deliciousj.errorhandling.ApiException;
import de.wadndadn.deliciousj.errorhandling.AuthenticationFailedException;
import de.wadndadn.deliciousj.errorhandling.ConfigurationException;
import de.wadndadn.deliciousj.errorhandling.ThrottledException;
import de.wadndadn.deliciousj.errorhandling.UnauthorizedException;

/**
 * TODO Document.
 * 
 * @author SchubertCh
 */
public abstract class AbstractApiImpl {

    /**
     * TODO Document.
     */
    private static final Log LOGGER = getLog(AbstractApiImpl.class);

    /**
     * TODO Document.
     */
    protected static final String USER_AGENT_VALUE = "wad'n dad'n deliciousJ/0.8";

    /**
     * TODO Document.
     */
    protected static final String RESPONSE_ENCODING = "UTF-8";

    /**
     * Time to wait before the next request (ms).
     */
    protected static final long WAITING_TIME = 1500;

    /**
     * <result>
     */
    private static final String RESULT_TAG = "result";

    /**
     * <result code=...>
     */
    private static final String CODE_ATTRIBUTE = "code";

    /**
     * TODO Document.
     */
    private static long lastRequestTimestamp = currentTimeMillis();

    /**
     * TODO Document.
     * 
     * @return TODO Document
     */
    public static boolean isNextRequestAllowed() {
        return lastRequestTimestamp + WAITING_TIME < currentTimeMillis();
    }

    /**
     * TODO Document.
     */
    public static void updateLastRequestTimestamp() {
        lastRequestTimestamp = currentTimeMillis();
    }

    /**
     * TODO Document.
     */
    private AuthenticationStrategy authenticationStrategy = null;

    /**
     * TODO Document.
     */
    private HttpClient httpClient = null;

    /**
     * TODO Document.
     */
    private DocumentBuilder documentBuilder = null;

    /**
     * TODO Document.
     */
    private ApiVersion apiVersion = null;

    /**
     * Constructor.
     * 
     * @param apiConfiguration
     *            TODO Document
     * 
     * @throws ConfigurationException
     *             TODO Document
     */
    public AbstractApiImpl(final ApiConfiguration apiConfiguration) throws ConfigurationException {
        lastRequestTimestamp = currentTimeMillis();
        apiVersion = apiConfiguration.getApiVersion();
        AuthenticationStrategyFactory authenticationStrategyFactory = createAuthenticationStrategyFactory();
        authenticationStrategy = authenticationStrategyFactory.createAuthenticationStrategy(apiConfiguration);
        HttpParams httpParams = new BasicHttpParams();
        setUserAgent(httpParams, USER_AGENT_VALUE);
        AbstractHttpClient httpClient = new DefaultHttpClient(httpParams);
        this.httpClient = httpClient;
        HttpRequestRetryHandler httpRequestRetryHandler = new DefaultHttpRequestRetryHandler(0, false);
        httpClient.setHttpRequestRetryHandler(httpRequestRetryHandler);
        try {
            authenticationStrategy.setCredentialsProvider(httpClient);
        } catch (AuthenticationFailedException afe) {
            throw new ConfigurationException("TODO", afe);
        }
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        documentBuilderFactory.setIgnoringComments(true);
        documentBuilderFactory.setValidating(false);
        documentBuilderFactory.setCoalescing(true);
        documentBuilderFactory.setNamespaceAware(false);
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new ConfigurationException("TODO", pce);
        }
        if (apiConfiguration.getProxyConfiguration() == null || isEmpty(apiConfiguration.getProxyConfiguration().getHost())) {
            return;
        }
        HttpHost proxyHost = new HttpHost(apiConfiguration.getProxyConfiguration().getHost(), apiConfiguration.getProxyConfiguration().getPort());
        httpClient.getParams().setParameter(DEFAULT_PROXY, proxyHost);
        UsernamePasswordCredentials proxyCredentials = apiConfiguration.getProxyConfiguration().getCredentials();
        if (proxyCredentials == null || (isEmpty(proxyCredentials.getUsername()) && isEmpty(proxyCredentials.getPassword()))) {
            return;
        }
        AuthScope proxyAuthScope = new AuthScope(apiConfiguration.getProxyConfiguration().getHost(), apiConfiguration.getProxyConfiguration().getPort());
        Credentials credentials = new org.apache.http.auth.UsernamePasswordCredentials(proxyCredentials.getUsername(), proxyCredentials.getPassword());
        httpClient.getCredentialsProvider().setCredentials(proxyAuthScope, credentials);
    }

    /**
     * TODO Document.
     * 
     * @param method
     *            TODO Document
     * 
     * @return TODO Document
     * 
     * @throws UnauthorizedException
     *             TODO Document
     * @throws ThrottledException
     *             TODO Document
     * @throws ApiException
     *             TODO Document
     */
    protected final Document executeMethod(final String method) throws UnauthorizedException, ThrottledException, ApiException {
        return executeMethod(method, null);
    }

    /**
     * TODO Document.
     * 
     * @param method
     *            TODO Document
     * @param parameters
     *            TODO Document
     * 
     * @return TODO Document
     * 
     * @throws UnauthorizedException
     *             TODO Document
     * @throws ThrottledException
     *             TODO Document
     * @throws ApiException
     *             TODO Document
     */
    protected final Document executeMethod(final String method, final Map<String, String> parameters) throws UnauthorizedException, ThrottledException, ApiException {
        try {
            HttpUriRequest httpRequest = createHttpRequest(method, parameters);
            Document document = executeHttpMethod(httpRequest);
            return document;
        } catch (AuthenticationFailedException afe) {
            throw new UnauthorizedException("TODO", afe);
        }
    }

    /**
     * TODO Document.
     * 
     * @param response
     *            TODO Document
     * @param expectedValue
     *            TODO Document
     * 
     * @throws ApiException
     *             TODO Document
     */
    protected final void parseResult(final Document response, final String expectedValue) throws ApiException {
        NodeList resultElements = response.getElementsByTagName(RESULT_TAG);
        if (!isEmpty(resultElements)) {
            Node resultElement = resultElements.item(0);
            Node firstChild = resultElement.getFirstChild();
            String resultValue = firstChild.getNodeValue();
            if (!isEmpty(resultValue)) {
                checkResult(resultValue, expectedValue);
            } else {
                throw new ApiException("Missing expected value of <" + RESULT_TAG + "> element");
            }
        } else {
            throw new ApiException("Missing expected <" + RESULT_TAG + "> element");
        }
    }

    /**
     * TODO Document.
     * 
     * @param response
     *            TODO Document
     * @param expectedValue
     *            TODO Document
     * 
     * @throws ApiException
     *             TODO Document
     */
    protected final void parseResultCode(final Document response, final String expectedValue) throws ApiException {
        NodeList resultElements = response.getElementsByTagName(RESULT_TAG);
        if (!isEmpty(resultElements)) {
            Node resultElement = resultElements.item(0);
            String resultValue = resultElement.getAttributes().getNamedItem(CODE_ATTRIBUTE).getNodeValue();
            if (!isEmpty(resultValue)) {
                checkResult(resultValue, expectedValue);
            } else {
                throw new ApiException("Missing expected " + CODE_ATTRIBUTE + " of <" + RESULT_TAG + "> element");
            }
        } else {
            throw new ApiException("Missing expected <" + RESULT_TAG + "> element");
        }
    }

    /**
     * TODO Document.
     * 
     * @param node
     *            TODO Document
     * @return TODO Document
     */
    protected final String getNodeValue(final Node node) {
        if (node == null) {
            return null;
        }
        return node.getNodeValue();
    }

    /**
     * TODO Document.
     * 
     * @param method
     *            TODO Document
     * 
     * @return TODO Document
     * 
     * @throws AuthenticationFailedException
     *             TODO Document
     */
    private HttpUriRequest createHttpRequest(final String method, final Map<String, String> parameters) throws AuthenticationFailedException {
        try {
            URI uri;
            if (isEmpty(parameters)) {
                uri = URIUtils.createURI(apiVersion.getScheme(), apiVersion.getHost(), apiVersion.getPort(), apiVersion.getPath() + method, null, null);
            } else {
                List<NameValuePair> queryParameters = new ArrayList<NameValuePair>(parameters.size());
                for (String paramName : parameters.keySet()) {
                    NameValuePair parameter = new BasicNameValuePair(paramName, parameters.get(paramName));
                    queryParameters.add(parameter);
                }
                uri = URIUtils.createURI(apiVersion.getScheme(), apiVersion.getHost(), apiVersion.getPort(), apiVersion.getPath() + method, format(queryParameters, "UTF-8"), null);
            }
            HttpGet httpGetRequest = new HttpGet(uri);
            authenticationStrategy.sign(httpGetRequest);
            return httpGetRequest;
        } catch (URISyntaxException use) {
            throw new ApiException("Problem creating URI", use);
        }
    }

    /**
     * TODO Document.
     * 
     * @param httpRequest
     *            TODO Document
     * 
     * @return TODO Document
     * 
     * @throws UnauthorizedException
     *             TODO Document
     * @throws ThrottledException
     *             TODO Document
     * @throws ApiException
     *             TODO Document
     */
    private synchronized Document executeHttpMethod(final HttpUriRequest httpRequest) throws UnauthorizedException, ThrottledException, ApiException {
        if (!isNextRequestAllowed()) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Wait " + WAITING_TIME + "ms for request.");
                }
                wait(WAITING_TIME);
            } catch (InterruptedException ie) {
                throw new ApiException("Waiting for request interrupted.", ie);
            }
        }
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Perform request.");
            }
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            switch(httpResponse.getStatusLine().getStatusCode()) {
                case HTTP_OK:
                    HttpEntity httpEntity = httpResponse.getEntity();
                    if (httpEntity != null) {
                        InputStream responseStream = httpEntity.getContent();
                        if (responseStream == null) {
                            throw new ApiException("TODO");
                        } else {
                            String response = null;
                            try {
                                response = IOUtils.toString(responseStream, RESPONSE_ENCODING);
                            } catch (IOException ioe) {
                                throw new ApiException("Problem reading response", ioe);
                            } catch (RuntimeException re) {
                                httpRequest.abort();
                                throw new ApiException("Problem reading response", re);
                            } finally {
                                responseStream.close();
                            }
                            StringReader responseReader = new StringReader(response);
                            Document document = documentBuilder.parse(new InputSource(responseReader));
                            return document;
                        }
                    }
                case HTTP_UNAVAILABLE:
                    throw new ThrottledException("TODO");
                case HTTP_UNAUTHORIZED:
                    throw new UnauthorizedException("TODO");
                default:
                    throw new ApiException("Unexpected HTTP status code: " + httpResponse.getStatusLine().getStatusCode());
            }
        } catch (SAXException se) {
            throw new ApiException("TODO", se);
        } catch (IOException ioe) {
            throw new ApiException("TODO", ioe);
        } finally {
            updateLastRequestTimestamp();
        }
    }

    /**
     * TODO Document.
     * 
     * @param resultValue
     *            TODO Document
     * @param expectedValue
     *            TODO Document
     * 
     * @throws ApiException
     *             TODO Document
     */
    private void checkResult(final String resultValue, final String expectedValue) throws ApiException {
        if (!expectedValue.equals(resultValue)) {
            throw new ApiException("Result value '" + resultValue + "' does not equal the expected  value '" + expectedValue + "'");
        }
    }
}
