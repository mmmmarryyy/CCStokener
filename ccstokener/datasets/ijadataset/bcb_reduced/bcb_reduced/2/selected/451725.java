package com.google.health.examples.appengine.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.appengine.repackaged.com.google.common.base.Strings;
import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthRsaSha1Signer;

public class OAuthService {

    private String consumerKey;

    private String consumerSecret;

    private OAuthVersion version;

    private GoogleOAuthHelper helper;

    private final Logger log = Logger.getLogger(this.getClass().getName());

    public enum OAuthVersion {

        v1_0, v1_0a
    }

    /**
   * Anonymous, HMAC-SHA1 signing
   *
   * @param version
   */
    public OAuthService(OAuthVersion version) {
        this.version = version;
        this.consumerKey = "anonymous";
        this.consumerSecret = "anonymous";
        this.helper = new GoogleOAuthHelper(new OAuthHmacSha1Signer());
    }

    /**
   * HMAC-SHA1 signing
   *
   * @param version
   * @param consumerKey
   * @param consumerSecret
   */
    public OAuthService(OAuthVersion version, String consumerKey, String consumerSecret) {
        this.version = version;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.helper = new GoogleOAuthHelper(new OAuthHmacSha1Signer());
    }

    /**
   * RSA-SHA1 signing
   *
   * @param version
   * @param consumerKey The OAuth consumer key
   * @param keystore The file name of the keystore, located in the root of the classpath
   * @param keystorePassword The password for the keystore
   * @param alias The key alias in the keystore
   */
    public OAuthService(OAuthVersion version, String consumerKey, String keystore, String keystorePassword, String alias) throws Exception {
        this.version = version;
        this.consumerKey = consumerKey;
        PrivateKey pk = loadPrivateKey(keystore, keystorePassword, alias);
        log.info("Loaded private key: " + pk.toString());
        this.helper = new GoogleOAuthHelper(new OAuthRsaSha1Signer(pk));
    }

    /**
   * First leg OAuth 1.0
   *
   * @param scope
   * @return Array containing OAuth token (index = 0) and token secret (index = 1)
   * @throws OAuthException
   */
    public String[] getRequestTokens(String scope) throws OAuthException {
        if (version != OAuthVersion.v1_0) {
            throw new IllegalStateException("OAuth version must be 1.0");
        }
        return _getRequestTokens(scope, null);
    }

    /**
   * First leg OAuth 1.0a
   *
   * @param scope
   * @param callback
   * @return Array containing OAuth token (index = 0) and token secret (index = 1)
   * @throws OAuthException
   */
    public String[] getRequestTokens(String scope, String callback) throws OAuthException {
        if (version != OAuthVersion.v1_0a || Strings.isNullOrEmpty(callback)) {
            throw new IllegalStateException("OAuth version must be 1.0a, and the callback cannot be null");
        }
        return _getRequestTokens(scope, callback);
    }

    public String[] _getRequestTokens(String scope, String callback) throws OAuthException {
        GoogleOAuthParameters params = new GoogleOAuthParameters();
        params.setOAuthConsumerKey(consumerKey);
        params.setScope(scope);
        params.setOAuthCallback(callback);
        params.setOAuthConsumerSecret(consumerSecret);
        log.info("Retrieving request tokens.");
        logParameters(params);
        helper.getUnauthorizedRequestToken(params);
        log.info("Retrieved request tokens.");
        logParameters(params);
        return new String[] { params.getOAuthToken(), params.getOAuthTokenSecret() };
    }

    /**
   * Second leg OAuth 1.0
   *
   * @param token
   * @param callback
   * @return
   */
    public String getAuthorizationLink(String token, String callback) {
        if (version != OAuthVersion.v1_0 || Strings.isNullOrEmpty(callback)) {
            throw new IllegalStateException("OAuth version must be 1.0, and the callback cannot be null");
        }
        return _getAuthorizationLink(token, callback);
    }

    /**
   * Second leg OAuth 1.0a
   *
   * @param token
   * @return
   */
    public String getAuthorizationLink(String token) {
        if (version != OAuthVersion.v1_0a) {
            throw new IllegalStateException("OAuth version must be 1.0a");
        }
        return _getAuthorizationLink(token, null);
    }

    private String _getAuthorizationLink(String token, String callback) {
        if (version != OAuthVersion.v1_0) {
            throw new IllegalStateException("OAuth version must be 1.0");
        }
        GoogleOAuthParameters params = new GoogleOAuthParameters();
        params.setOAuthConsumerKey(consumerKey);
        params.setOAuthConsumerSecret(consumerSecret);
        params.setOAuthToken(token);
        params.setOAuthCallback(callback);
        log.info("Generating authorization link.");
        logParameters(params);
        return helper.createUserAuthorizationUrl(params);
    }

    /**
   * Third leg
   *
   * @param tokenSecret
   * @param queryString
   * @return
   * @throws OAuthException
   */
    public String[] getAccessTokens(String tokenSecret, String queryString) throws OAuthException {
        GoogleOAuthParameters params = new GoogleOAuthParameters();
        params.setOAuthConsumerKey(consumerKey);
        params.setOAuthConsumerSecret(consumerSecret);
        params.setOAuthTokenSecret(tokenSecret);
        helper.getOAuthParametersFromCallback(queryString, params);
        log.info("Retrieving access tokens.");
        logParameters(params);
        String accessToken = helper.getAccessToken(params);
        log.info("Retrieved access tokens.");
        logParameters(params);
        return new String[] { accessToken, params.getOAuthTokenSecret() };
    }

    /**
   * Generate an OAuth HTTP request parameter value.
   *
   * @param url
   * @param method
   * @param token
   * @param tokenSecret
   * @return
   * @throws OAuthException
   */
    public String getHttpAuthorizationHeader(String url, String method, String token, String tokenSecret) throws OAuthException {
        log.info("Generating authorization header.");
        GoogleOAuthParameters params = new GoogleOAuthParameters();
        params.setOAuthConsumerKey(consumerKey);
        params.setOAuthConsumerSecret(consumerSecret);
        params.setOAuthToken(token);
        params.setOAuthTokenSecret(tokenSecret);
        logParameters(params);
        String header = helper.getAuthorizationHeader(url.toString(), method, params);
        return header;
    }

    public void revokeToken(String token, String tokenSecret) throws OAuthException {
        GoogleOAuthParameters params = new GoogleOAuthParameters();
        params.setOAuthConsumerKey(consumerKey);
        params.setOAuthConsumerSecret(consumerSecret);
        params.setOAuthToken(token);
        params.setOAuthTokenSecret(tokenSecret);
        helper.revokeToken(params);
    }

    public String getTokenInfo(String token, String tokenSecret) throws Exception {
        GoogleOAuthParameters params = new GoogleOAuthParameters();
        params.setOAuthConsumerKey(consumerKey);
        params.setOAuthConsumerSecret(consumerSecret);
        params.setOAuthToken(token);
        params.setOAuthTokenSecret(tokenSecret);
        URL url = new URL("https://www.google.com/accounts/AuthSubTokenInfo");
        String header = helper.getAuthorizationHeader(url.toString(), "GET", params);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", header);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static PrivateKey loadPrivateKey(String keystore, String keystorePassword, String alias) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        InputStream in = OAuthService.class.getClassLoader().getResourceAsStream(keystore);
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(in, keystorePassword.toCharArray());
        in.close();
        PrivateKey pk = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());
        if (pk == null) {
            throw new IllegalStateException("Null PrivateKey detected. Possibly incorrect alias.");
        }
        return pk;
    }

    private void logParameters(GoogleOAuthParameters params) {
        if (!log.isLoggable(Level.INFO)) return;
        StringBuffer sb = new StringBuffer("Base Parameters: ");
        for (String key : params.getBaseParameters().keySet()) sb.append("[" + key + " : " + params.getBaseParameters().get(key) + "]");
        log.info(sb.toString());
        sb = new StringBuffer("Extra Parameters: ");
        for (String key : params.getExtraParameters().keySet()) sb.append("[" + key + " : " + params.getExtraParameters().get(key) + "]");
        log.info(sb.toString());
    }
}
