package com.krobothsoftware.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import com.krobothsoftware.network.authorization.AuthorizationManager;
import com.krobothsoftware.network.values.NameValuePair;
import com.krobothsoftware.network.values.NetworkCookieManager;

/**
 * The Class NetworkHelper is used to help out with HttpURLConnetions.
 * 
 * @since 1.0
 * @author Kyle Kroboth
 */
public class NetworkHelper {

    /** The Constant DESKTOP for User Agent. */
    public static final String DESKTOP = "Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.11 (KHTML, like Gecko) " + "Ubuntu/11.10 Chromium/17.0.963.56 Chrome/17.0.963.56 Safari/535.11";

    /** The Constant PS3_COMMUNITY for User Agent. */
    public static final String PS3_COMMUNITY = "PS3Community-agent/1.0.0 libhttp/1.0.0";

    /** The Constant PS3_APPLICATION for User Agent. */
    public static final String PS3_APPLICATION = "PS3Application libhttp/3.5.5-000 (CellOS)";

    /** The Constant DEFAULT_ELEMENT_CHARSET. */
    public static final String DEFAULT_ELEMENT_CHARSET = "US-ASCII";

    /** The Constant DEFAULT_CONTENT_CHARSET. */
    public static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";

    /**
	 * The network cookie manager.
	 */
    protected NetworkCookieManager networkCookieManager;

    /**
	 * The authorization manager.
	 */
    protected AuthorizationManager authManager;

    /**
	 */
    private String userAgent;

    /**
	 * Instantiates a new network helper.
	 * 
	 * @param userAgent
	 *            the user agent to be set
	 */
    public NetworkHelper(String userAgent) {
        this.userAgent = userAgent;
        networkCookieManager = new NetworkCookieManager();
        authManager = new AuthorizationManager(this);
    }

    /**
	 * Instantiates a new network helper.
	 */
    public NetworkHelper() {
        this(null);
    }

    /**
	 * Cleans up cookies, authorizations and all activity.
	 */
    public void cleanup() {
        networkCookieManager.clearCookies();
        authManager.clearAuthorizations();
        userAgent = null;
    }

    /**
	 * Setup connection.
	 * 
	 * @param urlConnection
	 *            the url connection
	 * @return the http url connection
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final HttpURLConnection setupConnection(HttpURLConnection urlConnection) throws MalformedURLException, IOException {
        return setupConnection(urlConnection.getURL().toString(), null);
    }

    /**
	 * Setup connection.
	 * 
	 * @param uri
	 *            the uri
	 * @return the http url connection
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final HttpURLConnection setupConnection(URI uri) throws MalformedURLException, IOException {
        return setupConnection(uri.toString(), null);
    }

    /**
	 * Setup connection.
	 * 
	 * @param url
	 *            the url
	 * @return the http url connection
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final HttpURLConnection setupConnection(String url) throws MalformedURLException, IOException {
        return setupConnection(url, null);
    }

    /**
	 * Setup connection.
	 * 
	 * @param url
	 *            the url
	 * @param params
	 *            the url params
	 * @return the http url connection
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public final HttpURLConnection setupConnection(String url, List<NameValuePair> params) throws MalformedURLException, IOException {
        if (params != null && !params.isEmpty()) {
            String param = "";
            for (NameValuePair pair : params) {
                param += "&" + pair.getEncodedPair();
            }
            param = param.substring(1);
            url += "?";
            url += param;
        }
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        if (userAgent != null) urlConnection.setRequestProperty("User-Agent", userAgent);
        return urlConnection;
    }

    /**
	 * Sets the user agent.
	 * 
	 * @param userAgent
	 *            the new user agent
	 */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
	 * Send post.
	 * 
	 * @param urlConnection
	 *            the url connection
	 * @return the input stream
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public InputStream sendPost(HttpURLConnection urlConnection) throws IOException {
        setupMethod(Method.POST, urlConnection, null);
        return checkConnection(Method.POST, urlConnection, null);
    }

    /**
	 * Send post.
	 * 
	 * @param urlConnection
	 *            the url connection
	 * @param params
	 *            the params
	 * @return the input stream
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public InputStream sendPost(HttpURLConnection urlConnection, List<NameValuePair> params) throws IOException {
        setupMethod(Method.POST_PARAMS, urlConnection, params);
        return checkConnection(Method.POST_PARAMS, urlConnection, params);
    }

    /**
	 * Send post.
	 * 
	 * @param urlConnection
	 *            the url connection
	 * @param outputFile
	 *            the output file
	 * @return the input stream
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public InputStream sendPost(HttpURLConnection urlConnection, String outputFile) throws IOException {
        setupMethod(Method.POST_FILE, urlConnection, outputFile);
        return checkConnection(Method.POST_FILE, urlConnection, outputFile);
    }

    /**
	 * Send get.
	 * 
	 * @param urlConnection
	 *            the url connection
	 * @return the input stream
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public InputStream sendGet(HttpURLConnection urlConnection) throws IOException {
        setupMethod(Method.GET, urlConnection, null);
        return checkConnection(Method.GET, urlConnection, null);
    }

    /**
	 * Gets the authorization manager.
	 * 
	 * @return the authorization manager
	 */
    public AuthorizationManager getAuthorizationManager() {
        return authManager;
    }

    /**
	 * Gets the network cookie manager.
	 * 
	 * @return the network cookie manager
	 */
    public NetworkCookieManager getNetworkCookieManager() {
        return networkCookieManager;
    }

    /**
	 * Check connection.
	 * 
	 * @param urlConnection
	 *            the url connection
	 * @param type
	 *            the type
	 * @param params
	 *            the params
	 * @return the input stream
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    private InputStream checkConnection(Method method, HttpURLConnection urlConnection, Object params) throws IOException {
        InputStream inputStream = null;
        try {
            urlConnection.getInputStream();
        } catch (IOException e) {
            if (urlConnection.getResponseCode() == 401) {
                for (int i = 0; i < 3; i++) {
                    authManager.getAuthorization(urlConnection.getURL()).reset();
                    HttpURLConnection urlConnectionRetry = setupConnection(urlConnection);
                    setupMethod(method, urlConnection, params);
                    try {
                        inputStream = urlConnectionRetry.getInputStream();
                    } catch (IOException e2) {
                    }
                    if (urlConnectionRetry.getResponseCode() != 401) {
                        urlConnection = urlConnectionRetry;
                        break;
                    }
                }
                throw new IOException("Authorization failed or required");
            }
        }
        try {
            inputStream = urlConnection.getInputStream();
        } catch (IOException e2) {
        }
        getNetworkCookieManager().storeCookies(urlConnection.getURL(), getNetworkCookieManager().getCookies(urlConnection));
        return inputStream;
    }

    @SuppressWarnings("unchecked")
    private void setupMethod(Method method, HttpURLConnection urlConnection, Object params) throws IOException {
        switch(method) {
            case GET:
            case POST:
                urlConnection.setRequestMethod("POST");
                break;
            case POST_PARAMS:
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                String param = "";
                for (NameValuePair pair : (List<NameValuePair>) params) {
                    param += "&" + pair.getEncodedPair();
                }
                param = param.substring(1);
                urlConnection.setFixedLengthStreamingMode(param.getBytes().length);
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(param);
                out.close();
                break;
            case POST_FILE:
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setFixedLengthStreamingMode(((String) params).getBytes().length);
                DataOutputStream dstream = new DataOutputStream(urlConnection.getOutputStream());
                dstream.writeBytes((String) params);
                dstream.close();
                break;
        }
        urlConnection.setInstanceFollowRedirects(false);
        networkCookieManager.sendCookies(urlConnection);
        authManager.authorizeConnection(urlConnection);
    }

    public enum Method {

        GET, GET_POST, POST, POST_PARAMS, POST_FILE
    }
}
