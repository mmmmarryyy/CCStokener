package weibo4andriod.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.net.Proxy.Type;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import weibo4andriod.Configuration;
import weibo4andriod.Weibo;
import weibo4andriod.WeiboException;

/**
 * A utility class to handle HTTP request/response.
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public class HttpClient implements java.io.Serializable {

    private static final int OK = 200;

    private static final int NOT_MODIFIED = 304;

    private static final int BAD_REQUEST = 400;

    private static final int NOT_AUTHORIZED = 401;

    private static final int FORBIDDEN = 403;

    private static final int NOT_FOUND = 404;

    private static final int NOT_ACCEPTABLE = 406;

    private static final int INTERNAL_SERVER_ERROR = 500;

    private static final int BAD_GATEWAY = 502;

    private static final int SERVICE_UNAVAILABLE = 503;

    private static final boolean DEBUG = Configuration.getDebug();

    private String basic;

    private int retryCount = Configuration.getRetryCount();

    private int retryIntervalMillis = Configuration.getRetryIntervalSecs() * 1000;

    private String userId = Configuration.getUser();

    private String password = Configuration.getPassword();

    private String proxyHost = Configuration.getProxyHost();

    private int proxyPort = Configuration.getProxyPort();

    private String proxyAuthUser = Configuration.getProxyUser();

    private String proxyAuthPassword = Configuration.getProxyPassword();

    private int connectionTimeout = Configuration.getConnectionTimeout();

    private int readTimeout = Configuration.getReadTimeout();

    private static final long serialVersionUID = 808018030183407996L;

    private static boolean isJDK14orEarlier = false;

    private Map<String, String> requestHeaders = new HashMap<String, String>();

    private OAuth oauth = null;

    private String requestTokenURL = Configuration.getScheme() + "api.t.sina.com.cn/oauth/request_token";

    private String authorizationURL = Configuration.getScheme() + "api.t.sina.com.cn/oauth/authorize";

    private String authenticationURL = Configuration.getScheme() + "api.t.sina.com.cn/oauth/authenticate";

    private String accessTokenURL = Configuration.getScheme() + "api.t.sina.com.cn/oauth/access_token";

    private OAuthToken oauthToken = null;

    String token = null;

    static {
        try {
            String versionStr = System.getProperty("java.specification.version");
            if (null != versionStr) {
                isJDK14orEarlier = 1.5d > Double.parseDouble(versionStr);
            }
        } catch (AccessControlException ace) {
            isJDK14orEarlier = true;
        }
    }

    public HttpClient(String userId, String password) {
        this();
        setUserId(userId);
        setPassword(password);
    }

    public HttpClient() {
        this.basic = null;
        setUserAgent(null);
        setOAuthConsumer(null, null);
        setRequestHeader("Accept-Encoding", "gzip");
    }

    public void setUserId(String userId) {
        this.userId = userId;
        encodeBasicAuthenticationString();
    }

    public void setPassword(String password) {
        this.password = password;
        encodeBasicAuthenticationString();
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAuthenticationEnabled() {
        return null != basic || null != oauth;
    }

    /**
     * Sets the consumer key and consumer secret.<br>
     * System property -Dsinat4j.oauth.consumerKey and -Dhttp.oauth.consumerSecret override this attribute.
     * @param consumerKey Consumer Key
     * @param consumerSecret Consumer Secret
     * @since Weibo4J 2.0.0
     * @see <a href="http://open.t.sina.com.cn/wiki/index.php/Oauth">Applications Using Weibo</a>
     */
    public void setOAuthConsumer(String consumerKey, String consumerSecret) {
        consumerKey = Configuration.getOAuthConsumerKey(consumerKey);
        consumerSecret = Configuration.getOAuthConsumerSecret(consumerSecret);
        if (null != consumerKey && null != consumerSecret && 0 != consumerKey.length() && 0 != consumerSecret.length()) {
            this.oauth = new OAuth(consumerKey, consumerSecret);
        }
    }

    public RequestToken setToken(String token, String tokenSecret) {
        this.token = token;
        this.oauthToken = new RequestToken(token, tokenSecret);
        return (RequestToken) this.oauthToken;
    }

    /**
     *
     * @return request token
     * @throws WeiboException tw
     * @since Weibo4J 2.0.0
     */
    public RequestToken getOAuthRequestToken() throws WeiboException {
        this.oauthToken = new RequestToken(httpRequest(requestTokenURL, null, true), this);
        return (RequestToken) this.oauthToken;
    }

    /**
     * @param callback_url callback url
     * @return request token
     * @throws WeiboException tw
     * @since Weibo4J 2.0.9
     */
    public RequestToken getOauthRequestToken(String callback_url) throws WeiboException {
        this.oauthToken = new RequestToken(httpRequest(requestTokenURL, new PostParameter[] { new PostParameter("oauth_callback", callback_url) }, true), this);
        return (RequestToken) this.oauthToken;
    }

    /**
     *
     * @param token request token
     * @return access token
     * @throws WeiboException
     * @since Weibo4J 2.0.0
     */
    public AccessToken getOAuthAccessToken(RequestToken token) throws WeiboException {
        try {
            this.oauthToken = token;
            this.oauthToken = new AccessToken(httpRequest(accessTokenURL, new PostParameter[0], true));
        } catch (WeiboException te) {
            throw new WeiboException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) this.oauthToken;
    }

    /**
     *
     * @param token request token
     * @return access token
     * @throws WeiboException
     * @since Weibo4J 2.0.8
     */
    public AccessToken getOAuthAccessToken(RequestToken token, String pin) throws WeiboException {
        try {
            this.oauthToken = token;
            this.oauthToken = new AccessToken(httpRequest(accessTokenURL, new PostParameter[] { new PostParameter("oauth_verifier", pin) }, true));
        } catch (WeiboException te) {
            throw new WeiboException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) this.oauthToken;
    }

    /**
     *
     * @param token request token
     * @param tokenSecret request token secret
     * @return access token
     * @throws WeiboException
     * @since Weibo4J 2.0.1
     */
    public AccessToken getOAuthAccessToken(String token, String tokenSecret) throws WeiboException {
        try {
            this.oauthToken = new OAuthToken(token, tokenSecret) {

                private static final long serialVersionUID = -8917373259486848565L;
            };
            this.oauthToken = new AccessToken(httpRequest(accessTokenURL, new PostParameter[0], true));
        } catch (WeiboException te) {
            throw new WeiboException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) this.oauthToken;
    }

    /**
     *
     * @param token request token
     * @param tokenSecret request token secret
     * @param oauth_verifier oauth_verifier or pin
     * @return access token
     * @throws WeiboException
     * @since Weibo4J 2.0.8
     */
    public AccessToken getOAuthAccessToken(String token, String tokenSecret, String oauth_verifier) throws WeiboException {
        try {
            this.oauthToken = new OAuthToken(token, tokenSecret) {

                private static final long serialVersionUID = -4565132649535298787L;
            };
            this.oauthToken = new AccessToken(httpRequest(accessTokenURL, new PostParameter[] { new PostParameter("oauth_verifier", oauth_verifier) }, true));
        } catch (WeiboException te) {
            throw new WeiboException("The user has not given access to the account.", te, te.getStatusCode());
        }
        return (AccessToken) this.oauthToken;
    }

    public AccessToken getXAuthAccessToken(String userId, String passWord, String mode) throws WeiboException {
        this.oauthToken = new AccessToken(httpRequest(accessTokenURL, new PostParameter[] { new PostParameter("x_auth_username", userId), new PostParameter("x_auth_password", passWord), new PostParameter("x_auth_mode", mode) }, true));
        return (AccessToken) this.oauthToken;
    }

    /**
     * Sets the authorized access token
     * @param token authorized access token
     * @since Weibo4J 2.0.0
     */
    public void setOAuthAccessToken(AccessToken token) {
        this.oauthToken = token;
    }

    public void setRequestTokenURL(String requestTokenURL) {
        this.requestTokenURL = requestTokenURL;
    }

    public String getRequestTokenURL() {
        return requestTokenURL;
    }

    public void setAuthorizationURL(String authorizationURL) {
        this.authorizationURL = authorizationURL;
    }

    public String getAuthorizationURL() {
        return authorizationURL;
    }

    /**
     * since Weibo4J 2.0.10
     */
    public String getAuthenticationRL() {
        return authenticationURL;
    }

    public void setAccessTokenURL(String accessTokenURL) {
        this.accessTokenURL = accessTokenURL;
    }

    public String getAccessTokenURL() {
        return accessTokenURL;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Sets proxy host.
     * System property -Dsinat4j.http.proxyHost or http.proxyHost overrides this attribute.
     * @param proxyHost
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = Configuration.getProxyHost(proxyHost);
    }

    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets proxy port.
     * System property -Dsinat4j.http.proxyPort or -Dhttp.proxyPort overrides this attribute.
     * @param proxyPort
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = Configuration.getProxyPort(proxyPort);
    }

    public String getProxyAuthUser() {
        return proxyAuthUser;
    }

    /**
     * Sets proxy authentication user.
     * System property -Dsinat4j.http.proxyUser overrides this attribute.
     * @param proxyAuthUser
     */
    public void setProxyAuthUser(String proxyAuthUser) {
        this.proxyAuthUser = Configuration.getProxyUser(proxyAuthUser);
    }

    public String getProxyAuthPassword() {
        return proxyAuthPassword;
    }

    /**
     * Sets proxy authentication password.
     * System property -Dsinat4j.http.proxyPassword overrides this attribute.
     * @param proxyAuthPassword
     */
    public void setProxyAuthPassword(String proxyAuthPassword) {
        this.proxyAuthPassword = Configuration.getProxyPassword(proxyAuthPassword);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced by this URLConnection.
     * System property -Dsinat4j.http.connectionTimeout overrides this attribute.
     * @param connectionTimeout - an int that specifies the connect timeout value in milliseconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = Configuration.getConnectionTimeout(connectionTimeout);
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. System property -Dsinat4j.http.readTimeout overrides this attribute.
     * @param readTimeout - an int that specifies the timeout value to be used in milliseconds
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = Configuration.getReadTimeout(readTimeout);
    }

    private void encodeBasicAuthenticationString() {
        if (null != userId && null != password) {
            this.basic = "Basic " + new String(new BASE64Encoder().encode((userId + ":" + password).getBytes()));
            oauth = null;
        }
    }

    public void setRetryCount(int retryCount) {
        if (retryCount >= 0) {
            this.retryCount = Configuration.getRetryCount(retryCount);
        } else {
            throw new IllegalArgumentException("RetryCount cannot be negative.");
        }
    }

    public void setUserAgent(String ua) {
        setRequestHeader("User-Agent", Configuration.getUserAgent(ua));
    }

    public String getUserAgent() {
        return getRequestHeader("User-Agent");
    }

    public void setRetryIntervalSecs(int retryIntervalSecs) {
        if (retryIntervalSecs >= 0) {
            this.retryIntervalMillis = Configuration.getRetryIntervalSecs(retryIntervalSecs) * 1000;
        } else {
            throw new IllegalArgumentException("RetryInterval cannot be negative.");
        }
    }

    public Response post(String url, PostParameter[] postParameters, boolean authenticated) throws WeiboException {
        PostParameter[] newPostParameters = new PostParameter[postParameters.length + 1];
        for (int i = 0; i < postParameters.length; i++) {
            newPostParameters[i] = postParameters[i];
        }
        newPostParameters[postParameters.length] = new PostParameter("source", Weibo.CONSUMER_KEY);
        return httpRequest(url, newPostParameters, authenticated);
    }

    public Response delete(String url, boolean authenticated) throws WeiboException {
        return httpRequest(url, null, authenticated, "DELETE");
    }

    @SuppressWarnings("deprecation")
    public Response multPartURL(String url, PostParameter[] params, ImageItem item, boolean authenticated) throws WeiboException {
        HttpResponse rps = null;
        try {
            org.apache.http.client.HttpClient client = HttpConfig.newInstance();
            HttpPost post = new HttpPost(url);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
            for (PostParameter entry : params) {
                reqEntity.addPart(entry.getName(), new StringBody(entry.getValue(), Charset.forName("UTF-8")));
            }
            reqEntity.addPart(item.getName(), new ByteArrayContentBody(item.getContent(), item.getImageType()));
            post.setEntity(reqEntity);
            long t = System.currentTimeMillis();
            if (authenticated) {
                if (basic == null && oauth == null) {
                }
                String authorization = null;
                if (null != oauth) {
                    authorization = oauth.generateAuthorizationHeader("POST", url, params, oauthToken);
                } else if (null != basic) {
                    authorization = this.basic;
                } else {
                    throw new IllegalStateException("Neither user ID/password combination nor OAuth consumer key/secret combination supplied");
                }
                post.addHeader("Authorization", authorization);
                log("Authorization: " + authorization);
            }
            rps = client.execute(post);
            Response response = new Response();
            response.setResponseAsString(new String(EntityUtils.toByteArray(rps.getEntity()), "UTF-8"));
            response.setStatusCode(rps.getStatusLine().getStatusCode());
            log("multPartURL URL:" + url + ", result:" + response + ", time:" + (System.currentTimeMillis() - t));
            return response;
        } catch (Exception ex) {
            throw new WeiboException(ex.getMessage(), ex, -1);
        } finally {
            if (rps != null && rps.getEntity() != null) try {
                rps.getEntity().consumeContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    public Response multPartURL(String fileParamName, String url, PostParameter[] params, File file, boolean authenticated) throws WeiboException {
        HttpResponse rps = null;
        try {
            org.apache.http.client.HttpClient client = HttpConfig.newInstance();
            HttpPost post = new HttpPost(url);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
            for (PostParameter entry : params) {
                reqEntity.addPart(entry.getName(), new StringBody(entry.getValue(), Charset.forName("UTF-8")));
            }
            reqEntity.addPart(fileParamName, new FileBody(file, new FileType().getMIMEType(file)));
            post.setEntity(reqEntity);
            long t = System.currentTimeMillis();
            if (authenticated) {
                if (basic == null && oauth == null) {
                }
                String authorization = null;
                if (null != oauth) {
                    authorization = oauth.generateAuthorizationHeader("POST", url, params, oauthToken);
                } else if (null != basic) {
                    authorization = this.basic;
                } else {
                    throw new IllegalStateException("Neither user ID/password combination nor OAuth consumer key/secret combination supplied");
                }
                post.addHeader("Authorization", authorization);
                log("Authorization: " + authorization);
            }
            rps = client.execute(post);
            Response response = new Response();
            response.setResponseAsString(new String(EntityUtils.toByteArray(rps.getEntity()), "UTF-8"));
            response.setStatusCode(rps.getStatusLine().getStatusCode());
            log("multPartURL URL:" + url + ", result:" + response + ", time:" + (System.currentTimeMillis() - t));
            return response;
        } catch (Exception ex) {
            throw new WeiboException(ex.getMessage(), ex, -1);
        } finally {
            if (rps != null && rps.getEntity() != null) try {
                rps.getEntity().consumeContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ByteArrayContentBody extends AbstractContentBody {

        private byte[] data;

        public ByteArrayContentBody(byte[] data, String mimeType) {
            super(mimeType);
            this.data = data;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            out.write(data);
        }

        public String getFilename() {
            return "pic";
        }

        public String getCharset() {
            return "UTF-8";
        }

        public long getContentLength() {
            return data.length;
        }

        public String getTransferEncoding() {
            return "binary";
        }
    }

    public Response post(String url, boolean authenticated) throws WeiboException {
        return httpRequest(url, new PostParameter[0], authenticated);
    }

    public Response post(String url, PostParameter[] PostParameters) throws WeiboException {
        return httpRequest(url, PostParameters, false);
    }

    public Response post(String url) throws WeiboException {
        return httpRequest(url, new PostParameter[0], false);
    }

    public Response get(String url, boolean authenticated) throws WeiboException {
        return httpRequest(url, null, authenticated);
    }

    public Response get(String url) throws WeiboException {
        return httpRequest(url, null, false);
    }

    protected Response httpRequest(String url, PostParameter[] postParams, boolean authenticated) throws WeiboException {
        int len = 1;
        PostParameter[] newPostParameters = postParams;
        String method = "GET";
        if (postParams != null) {
            method = "POST";
            len = postParams.length + 1;
            newPostParameters = new PostParameter[len];
            for (int i = 0; i < postParams.length; i++) {
                newPostParameters[i] = postParams[i];
            }
            newPostParameters[postParams.length] = new PostParameter("source", Weibo.CONSUMER_KEY);
        }
        return httpRequest(url, newPostParameters, authenticated, method);
    }

    public Response httpRequest(String url, PostParameter[] postParams, boolean authenticated, String httpMethod) throws WeiboException {
        org.apache.http.client.HttpClient client = HttpConfig.newInstance();
        HttpRequestBase request;
        if (null != postParams || "POST".equals(httpMethod)) {
            request = new HttpPost(url);
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (PostParameter p : postParams) {
                nvps.add(new BasicNameValuePair(p.name, p.value));
            }
            try {
                ((HttpPost) request).setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if ("DELETE".equals(httpMethod)) {
            request = new HttpDelete(url);
        } else {
            request = new HttpGet(url);
        }
        if (authenticated) {
            if (basic == null && oauth == null) {
            }
            String authorization = null;
            if (null != oauth) {
                authorization = oauth.generateAuthorizationHeader(httpMethod, url, postParams, oauthToken);
            } else if (null != basic) {
                authorization = this.basic;
            } else {
                throw new IllegalStateException("Neither user ID/password combination nor OAuth consumer key/secret combination supplied");
            }
            request.addHeader("Authorization", authorization);
            log("Authorization: " + authorization);
        }
        for (String key : requestHeaders.keySet()) {
            request.addHeader(key, requestHeaders.get(key));
            log(key + ": " + requestHeaders.get(key));
        }
        Response res = null;
        int responseCode = -1;
        try {
            HttpResponse response = client.execute(request);
            res = new Response(response);
            responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != OK) {
                if (responseCode < INTERNAL_SERVER_ERROR) {
                    throw new WeiboException(getCause(responseCode) + "\n" + res.asString(), responseCode);
                }
            }
        } catch (Exception e) {
            throw new WeiboException(e.getMessage(), e, responseCode);
        }
        return res;
    }

    public Response1 httpRequest1(String url, PostParameter[] postParams, boolean authenticated, String httpMethod) throws WeiboException {
        int retriedCount;
        int retry = retryCount + 1;
        Response1 res = null;
        for (retriedCount = 0; retriedCount < retry; retriedCount++) {
            int responseCode = -1;
            try {
                HttpURLConnection con = null;
                OutputStream osw = null;
                try {
                    con = getConnection(url);
                    con.setDoInput(true);
                    setHeaders(url, postParams, con, authenticated, httpMethod);
                    if (null != postParams || "POST".equals(httpMethod)) {
                        con.setRequestMethod("POST");
                        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        con.setDoOutput(true);
                        String postParam = "";
                        if (postParams != null) {
                            postParam = encodeParameters(postParams);
                        }
                        log("Post Params: ", postParam);
                        byte[] bytes = postParam.getBytes("UTF-8");
                        con.setRequestProperty("Content-Length", Integer.toString(bytes.length));
                        osw = con.getOutputStream();
                        osw.write(bytes);
                        osw.flush();
                        osw.close();
                    } else if ("DELETE".equals(httpMethod)) {
                        con.setRequestMethod("DELETE");
                    } else {
                        con.setRequestMethod("GET");
                    }
                    res = new Response1(con);
                    responseCode = con.getResponseCode();
                    if (DEBUG) {
                        log("Response: ");
                        Map<String, List<String>> responseHeaders = con.getHeaderFields();
                        for (String key : responseHeaders.keySet()) {
                            List<String> values = responseHeaders.get(key);
                            for (String value : values) {
                                if (null != key) {
                                    log(key + ": " + value);
                                } else {
                                    log(value);
                                }
                            }
                        }
                    }
                    if (responseCode != OK) {
                        if (responseCode < INTERNAL_SERVER_ERROR || retriedCount == retryCount) {
                            throw new WeiboException(getCause(responseCode) + "\n" + res.asString(), responseCode);
                        }
                    } else {
                        break;
                    }
                } finally {
                    try {
                        osw.close();
                    } catch (Exception ignore) {
                    }
                }
            } catch (IOException ioe) {
                if (retriedCount == retryCount) {
                    throw new WeiboException(ioe.getMessage(), ioe, responseCode);
                }
            }
            try {
                if (DEBUG && null != res) {
                    res.asString();
                }
                log("Sleeping " + retryIntervalMillis + " millisecs for next retry.");
                Thread.sleep(retryIntervalMillis);
            } catch (InterruptedException ignore) {
            }
        }
        return res;
    }

    public static String encodeParameters(PostParameter[] postParams) {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < postParams.length; j++) {
            if (j != 0) {
                buf.append("&");
            }
            try {
                buf.append(URLEncoder.encode(postParams[j].name, "UTF-8")).append("=").append(URLEncoder.encode(postParams[j].value, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException neverHappen) {
            }
        }
        return buf.toString();
    }

    /**
     * sets HTTP headers
     *
     * @param connection    HttpURLConnection
     * @param authenticated boolean
     */
    private void setHeaders(String url, PostParameter[] params, HttpURLConnection connection, boolean authenticated, String httpMethod) {
        log("Request: ");
        log(httpMethod + " ", url);
        if (authenticated) {
            if (basic == null && oauth == null) {
            }
            String authorization = null;
            if (null != oauth) {
                authorization = oauth.generateAuthorizationHeader(httpMethod, url, params, oauthToken);
            } else if (null != basic) {
                authorization = this.basic;
            } else {
                throw new IllegalStateException("Neither user ID/password combination nor OAuth consumer key/secret combination supplied");
            }
            connection.addRequestProperty("Authorization", authorization);
            log("Authorization: " + authorization);
        }
        for (String key : requestHeaders.keySet()) {
            connection.addRequestProperty(key, requestHeaders.get(key));
            log(key + ": " + requestHeaders.get(key));
        }
    }

    public void setRequestHeader(String name, String value) {
        requestHeaders.put(name, value);
    }

    public String getRequestHeader(String name) {
        return requestHeaders.get(name);
    }

    private HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection con = null;
        if (proxyHost != null && !proxyHost.equals("")) {
            if (proxyAuthUser != null && !proxyAuthUser.equals("")) {
                log("Proxy AuthUser: " + proxyAuthUser);
                log("Proxy AuthPassword: " + proxyAuthPassword);
                Authenticator.setDefault(new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType().equals(RequestorType.PROXY)) {
                            return new PasswordAuthentication(proxyAuthUser, proxyAuthPassword.toCharArray());
                        } else {
                            return null;
                        }
                    }
                });
            }
            final Proxy proxy = new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, proxyPort));
            if (DEBUG) {
                log("Opening proxied connection(" + proxyHost + ":" + proxyPort + ")");
            }
            con = (HttpURLConnection) new URL(url).openConnection(proxy);
        } else {
            con = (HttpURLConnection) new URL(url).openConnection();
        }
        if (connectionTimeout > 0 && !isJDK14orEarlier) {
            con.setConnectTimeout(connectionTimeout);
        }
        if (readTimeout > 0 && !isJDK14orEarlier) {
            con.setReadTimeout(readTimeout);
        }
        return con;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpClient)) return false;
        HttpClient that = (HttpClient) o;
        if (connectionTimeout != that.connectionTimeout) return false;
        if (proxyPort != that.proxyPort) return false;
        if (readTimeout != that.readTimeout) return false;
        if (retryCount != that.retryCount) return false;
        if (retryIntervalMillis != that.retryIntervalMillis) return false;
        if (accessTokenURL != null ? !accessTokenURL.equals(that.accessTokenURL) : that.accessTokenURL != null) return false;
        if (!authenticationURL.equals(that.authenticationURL)) return false;
        if (!authorizationURL.equals(that.authorizationURL)) return false;
        if (basic != null ? !basic.equals(that.basic) : that.basic != null) return false;
        if (oauth != null ? !oauth.equals(that.oauth) : that.oauth != null) return false;
        if (oauthToken != null ? !oauthToken.equals(that.oauthToken) : that.oauthToken != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (proxyAuthPassword != null ? !proxyAuthPassword.equals(that.proxyAuthPassword) : that.proxyAuthPassword != null) return false;
        if (proxyAuthUser != null ? !proxyAuthUser.equals(that.proxyAuthUser) : that.proxyAuthUser != null) return false;
        if (proxyHost != null ? !proxyHost.equals(that.proxyHost) : that.proxyHost != null) return false;
        if (!requestHeaders.equals(that.requestHeaders)) return false;
        if (!requestTokenURL.equals(that.requestTokenURL)) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = basic != null ? basic.hashCode() : 0;
        result = 31 * result + retryCount;
        result = 31 * result + retryIntervalMillis;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (proxyHost != null ? proxyHost.hashCode() : 0);
        result = 31 * result + proxyPort;
        result = 31 * result + (proxyAuthUser != null ? proxyAuthUser.hashCode() : 0);
        result = 31 * result + (proxyAuthPassword != null ? proxyAuthPassword.hashCode() : 0);
        result = 31 * result + connectionTimeout;
        result = 31 * result + readTimeout;
        result = 31 * result + requestHeaders.hashCode();
        result = 31 * result + (oauth != null ? oauth.hashCode() : 0);
        result = 31 * result + requestTokenURL.hashCode();
        result = 31 * result + authorizationURL.hashCode();
        result = 31 * result + authenticationURL.hashCode();
        result = 31 * result + (accessTokenURL != null ? accessTokenURL.hashCode() : 0);
        result = 31 * result + (oauthToken != null ? oauthToken.hashCode() : 0);
        return result;
    }

    private static void log(String message) {
        if (DEBUG) {
            System.out.println("[" + new java.util.Date() + "]" + message);
        }
    }

    private static void log(String message, String message2) {
        if (DEBUG) {
            log(message + message2);
        }
    }

    private static String getCause(int statusCode) {
        String cause = null;
        switch(statusCode) {
            case NOT_MODIFIED:
                break;
            case BAD_REQUEST:
                cause = "The request was invalid.  An accompanying error message will explain why. This is the status code will be returned during rate limiting.";
                break;
            case NOT_AUTHORIZED:
                cause = "Authentication credentials were missing or incorrect.";
                break;
            case FORBIDDEN:
                cause = "The request is understood, but it has been refused.  An accompanying error message will explain why.";
                break;
            case NOT_FOUND:
                cause = "The URI requested is invalid or the resource requested, such as a user, does not exists.";
                break;
            case NOT_ACCEPTABLE:
                cause = "Returned by the Search API when an invalid format is specified in the request.";
                break;
            case INTERNAL_SERVER_ERROR:
                cause = "Something is broken.  Please post to the group so the Weibo team can investigate.";
                break;
            case BAD_GATEWAY:
                cause = "Weibo is down or being upgraded.";
                break;
            case SERVICE_UNAVAILABLE:
                cause = "Service Unavailable: The Weibo servers are up, but overloaded with requests. Try again later. The search and trend methods use this to indicate when you are being rate limited.";
                break;
            default:
                cause = "";
        }
        return statusCode + ":" + cause;
    }
}
