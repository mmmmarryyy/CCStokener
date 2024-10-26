package com.mttaboros.health.authsub;

import com.mttaboros.util.Base64;
import com.mttaboros.util.IOUtils;
import com.mttaboros.util.StringUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility functions to support AuthSub (Account Authentication for Web Applications).
 * <p/>
 * This version of AuthSubUtil is java 1.4 compatible.
 */
public class AuthSubUtil {

    private static final String DEFAULT_PROTOCOL = "https";

    private static final String DEFAULT_DOMAIN = "www.google.com";

    private static final boolean USE_URL_ENCODING = false;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static SignatureAlgorithm DSA_SHA1 = new SignatureAlgorithm("dsa-sha1", "SHA1withDSA");

    private static SignatureAlgorithm RSA_SHA1 = new SignatureAlgorithm("rsa-sha1", "SHA1withRSA");

    /**
     * Signature algorithms supported by AuthSub to sign the requests.
     */
    private static class SignatureAlgorithm {

        private final String authSubName;

        private final String jcaName;

        SignatureAlgorithm(String authSubName, String jcaName) {
            this.authSubName = authSubName;
            this.jcaName = jcaName;
        }

        public String toString() {
            return jcaName;
        }

        public String getAuthSubName() {
            return authSubName;
        }

        public String getJCAName() {
            return jcaName;
        }
    }

    /**
     * Creates the request URL to be used to retrieve an AuthSub token.
     * <p/>
     * On success, the user will be redirected to the next URL with the
     * AuthSub token appended to the URL.  Use {@link #getTokenFromReply(String)}
     * to retrieve the token from the reply.
     *
     * @param nextUrl the URL to redirect to on successful token retrieval
     * @param scope   the scope of the requested AuthSub token
     * @param secure  <code>true</code> if the token will be used securely
     * @param session <code>true</code> if the token will be exchanged for a
     *                session cookie
     * @return the URL to be used to retrieve the AuthSub token
     */
    public static String getRequestUrl(String nextUrl, String scope, boolean secure, boolean session) {
        return getRequestUrl(DEFAULT_PROTOCOL, DEFAULT_DOMAIN, nextUrl, scope, secure, session);
    }

    /**
     * Creates the request URL to be used to retrieve an AuthSub token.
     * <p/>
     * On success, the user will be redirected to the next URL with the
     * AuthSub token appended to the URL.  Use {@link #getTokenFromReply(String)}
     * to retrieve the token from the reply.
     *
     * @param protocol the protocol to use to communicate with the server
     * @param domain   the domain at which the authentication server exists
     * @param nextUrl  the URL to redirect to on successful token retrieval
     * @param scope    the scope of the requested AuthSub token
     * @param secure   <code>true</code> if the token will be used securely
     * @param session  <code>true</code> if the token will be exchanged for a
     *                 session cookie
     * @return the URL to be used to retrieve the AuthSub token
     */
    public static String getRequestUrl(String protocol, String domain, String nextUrl, String scope, boolean secure, boolean session) {
        StringBuffer url = new StringBuffer(protocol).append("://");
        url.append(domain).append("/accounts/AuthSubRequest");
        addParameter(url, "next", nextUrl, USE_URL_ENCODING);
        addParameter(url, "scope", scope, USE_URL_ENCODING);
        addParameter(url, "secure", secure ? "1" : "0", USE_URL_ENCODING);
        addParameter(url, "session", session ? "1" : "0", USE_URL_ENCODING);
        return url.toString();
    }

    public static String getRequestUrl(String protocol, String domain, String customAuthSubRequestPath, String nextUrl, String scope, boolean secure, boolean session) {
        return getRequestUrl(protocol, domain, nextUrl, scope, secure, session).replaceAll("/accounts/AuthSubRequest", customAuthSubRequestPath);
    }

    public static String getRequestUrl(String protocol, String domain, String customAuthSubRequestPath, String nextUrl, String scope, boolean secure, boolean session, HashMap extraParams) {
        StringBuffer url = new StringBuffer(getRequestUrl(protocol, domain, customAuthSubRequestPath, nextUrl, scope, secure, session));
        Iterator it = extraParams.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            addParameter(url, (String) entry.getKey(), (String) entry.getValue(), USE_URL_ENCODING);
        }
        return url.toString();
    }

    /**
     * Parses and returns the AuthSub token returned on a successful
     * AuthSub login request.  The token will be appended as a query parameter
     * to the next URL specified while making the AuthSub request.
     *
     * @param url the redirected-to next URL with the token
     * @return the AuthSub token returned
     */
    public static String getTokenFromReply(URL url) {
        return getTokenFromReply(url.getQuery());
    }

    /**
     * Parses and returns the AuthSub token returned on a successful
     * AuthSub login request.  The token will be appended as a query parameter
     * to the next URL specified while making the AuthSub request.
     *
     * @param queryString the query portion of the redirected-to URL containing
     *                    the token
     * @return the AuthSub token returned
     * @noinspection deprecation
     */
    public static String getTokenFromReply(String queryString) {
        Map params = StringUtils.string2Map(queryString, "&", "=", true);
        params = StringUtils.lowercaseKeys(params);
        return URLDecoder.decode((String) params.get("token"));
    }

    /**
     * Retrieves the private key from the specified keystore.
     *
     * @param keystore     the path to the keystore file
     * @param keystorePass the password that protects the keystore file
     * @param keyAlias     the alias under which the private key is stored
     * @param keyPass      the password protecting the private key
     * @return the private key from the specified keystore
     * @throws GeneralSecurityException if the keystore cannot be loaded
     * @throws IOException              if the file cannot be accessed
     */
    public static PrivateKey getPrivateKeyFromKeystore(String keystore, String keystorePass, String keyAlias, String keyPass) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream keyStream = null;
        try {
            keyStream = new FileInputStream(keystore);
            keyStore.load(keyStream, keystorePass.toCharArray());
            return (PrivateKey) keyStore.getKey(keyAlias, keyPass.toCharArray());
        } finally {
            if (keyStream != null) {
                keyStream.close();
            }
        }
    }

    /**
     * Retrieves the private key from the specified keystore.
     *
     * @param keystoreURL  the path to the keystore file
     * @param keystorePass the password that protects the keystore file
     * @param keyAlias     the alias under which the private key is stored
     * @param keyPass      the password protecting the private key
     * @return the private key from the specified keystore
     * @throws GeneralSecurityException if the keystore cannot be loaded
     * @throws IOException              if the file cannot be accessed
     */
    public static PrivateKey getPrivateKeyFromKeystore(URL keystoreURL, String keystorePass, String keyAlias, String keyPass) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream keyStream = keystoreURL.openStream();
        try {
            keyStore.load(keyStream, keystorePass.toCharArray());
            return (PrivateKey) keyStore.getKey(keyAlias, keyPass.toCharArray());
        } finally {
            if (keyStream != null) {
                keyStream.close();
            }
        }
    }

    /**
     * Exchanges the one time use token returned in the URL for a session
     * token.
     * <p/>
     * If the <code>key</code> is non-null, the token will be used securely
     * and the request to make the exchange will be signed.
     *
     * @param onetimeUseToken the one time use token sent in the URL
     * @param key             the private key to sign the request
     * @return the session token.  <code>null</code> if the request failed
     * @throws IOException              if error in writing/reading the request
     * @throws GeneralSecurityException if error in signing the request
     * @throws AuthenticationException  if one time use token is rejected
     */
    public static String exchangeForSessionToken(String onetimeUseToken, PrivateKey key) throws IOException, GeneralSecurityException, AuthenticationException {
        return exchangeForSessionToken(DEFAULT_PROTOCOL, DEFAULT_DOMAIN, onetimeUseToken, key);
    }

    /**
     * Exchanges the one time use token returned in the URL for a session
     * token.
     * <p/>
     * If the <code>key</code> is non-null, the token will be used securely
     * and the request to make the exchange will be signed.
     *
     * @param protocol        the protocol to use to communicate with the server
     * @param domain          the domain at which the authentication server exists
     * @param onetimeUseToken the one time use token sent in the URL
     * @param key             the private key to sign the request
     * @return the session token.  <code>null</code> if the request failed
     * @throws IOException              if error in writing/reading the request
     * @throws GeneralSecurityException if error in signing the request
     * @throws AuthenticationException  if one time use token is rejected
     */
    public static String exchangeForSessionToken(String protocol, String domain, String onetimeUseToken, PrivateKey key) throws IOException, GeneralSecurityException, AuthenticationException {
        String sessionUrl = getSessionTokenUrl(protocol, domain);
        URL url = new URL(sessionUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        String header = formAuthorizationHeader(onetimeUseToken, key, url, "GET");
        httpConn.setRequestProperty("Authorization", header);
        if (httpConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new AuthenticationException(httpConn.getResponseCode() + ": " + httpConn.getResponseMessage());
        }
        String body = IOUtils.toString(httpConn.getInputStream());
        Map parsedTokens = StringUtils.string2Map(body, "\n", "=", true);
        parsedTokens = StringUtils.lowercaseKeys(parsedTokens);
        return (String) parsedTokens.get("token");
    }

    /**
     * Retrieves information about the AuthSub token.
     * <p/>
     * If the <code>key</code> is non-null, the token will be used securely
     * and the request to revoke the token will be signed.
     *
     * @param token the AuthSub token for which to receive information
     * @param key   the private key to sign the request
     * @return the token information in the form of a Map from the name of the
     *         attribute to the value of the attribute.
     * @throws IOException              if error in writing/reading the request
     * @throws GeneralSecurityException if error in signing the request
     * @throws AuthenticationException  if the token is rejected
     */
    public static Map getTokenInfo(String token, PrivateKey key) throws IOException, GeneralSecurityException, AuthenticationException {
        return getTokenInfo(DEFAULT_PROTOCOL, DEFAULT_DOMAIN, token, key);
    }

    /**
     * Retrieves information about the AuthSub token.
     * <p/>
     * If the <code>key</code> is non-null, the token will be used securely
     * and the request to revoke the token will be signed.
     *
     * @param protocol the protocol to use to communicate with the server
     * @param domain   the domain at which the authentication server exists
     * @param token    the AuthSub token for which to receive information
     * @param key      the private key to sign the request
     * @return the token information in the form of a Map from the name of the
     *         attribute to the value of the attribute.
     * @throws IOException              if error in writing/reading the request
     * @throws GeneralSecurityException if error in signing the request
     * @throws AuthenticationException  if the token is rejected
     */
    public static Map getTokenInfo(String protocol, String domain, String token, PrivateKey key) throws IOException, GeneralSecurityException, AuthenticationException {
        String tokenInfoUrl = getTokenInfoUrl(protocol, domain);
        URL url = new URL(tokenInfoUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        String header = formAuthorizationHeader(token, key, url, "GET");
        httpConn.setRequestProperty("Authorization", header);
        if (httpConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new AuthenticationException(httpConn.getResponseCode() + ": " + httpConn.getResponseMessage());
        }
        String body = IOUtils.toString(httpConn.getInputStream());
        return StringUtils.string2Map(body.trim(), "\n", "=", true);
    }

    /**
     * Revokes the specified token.
     * <p/>
     * If the <code>key</code> is non-null, the token will be used securely
     * and the request to revoke the token will be signed.
     *
     * @param token the AuthSub token to revoke
     * @param key   the private key to sign the request
     * @throws IOException              if error in writing/reading the request
     * @throws GeneralSecurityException if error in signing the request
     * @throws AuthenticationException  if the token is rejected
     */
    public static void revokeToken(String token, PrivateKey key) throws IOException, GeneralSecurityException, AuthenticationException {
        revokeToken(DEFAULT_PROTOCOL, DEFAULT_DOMAIN, token, key);
    }

    /**
     * Revokes the specified token.
     * <p/>
     * If the <code>key</code> is non-null, the token will be used securely
     * and the request to revoke the token will be signed.
     *
     * @param protocol the protocol to use to communicate with the server
     * @param domain   the domain at which the authentication server exists
     * @param token    the AuthSub token to revoke
     * @param key      the private key to sign the request
     * @throws IOException              if error in writing/reading the request
     * @throws GeneralSecurityException if error in signing the request
     * @throws AuthenticationException  if the token is rejected
     */
    public static void revokeToken(String protocol, String domain, String token, PrivateKey key) throws IOException, GeneralSecurityException, AuthenticationException {
        String revokeUrl = getRevokeTokenUrl(protocol, domain);
        URL url = new URL(revokeUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        String header = formAuthorizationHeader(token, key, url, "GET");
        httpConn.setRequestProperty("Authorization", header);
        if (httpConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new AuthenticationException(httpConn.getResponseCode() + ": " + httpConn.getResponseMessage());
        }
    }

    /**
     * Forms the AuthSub authorization header.
     * <p/>
     * If the <code>key</code> is null, the token will be used in insecure mode.
     * If the <code>key</code> is non-null, the token will be used securely and
     * the header will contain a signature.
     *
     * @param token         the AuthSub token to use in the header
     * @param key           the private key used to sign the request
     * @param requestUrl    the URL of the request being issued
     * @param requestMethod the HTTP method being used to issue the request
     * @return the authorization header
     * @throws GeneralSecurityException if error occurs while creating signature
     */
    public static String formAuthorizationHeader(String token, PrivateKey key, URL requestUrl, String requestMethod) throws GeneralSecurityException {
        if (key == null) {
            return "AuthSub token=\"" + token + "\"";
        } else {
            long timestamp = System.currentTimeMillis() / 1000;
            long nonce = RANDOM.nextLong();
            String dataToSign = requestMethod + " " + requestUrl.toExternalForm() + " " + timestamp + " " + StringUtils.unsignedLongToString(nonce);
            SignatureAlgorithm sigAlg = getSigAlg(key);
            byte[] signature = sign(key, dataToSign, sigAlg);
            String encodedSignature = Base64.encode(signature);
            return "AuthSub token=\"" + token + "\" data=\"" + dataToSign + "\" sig=\"" + encodedSignature + "\" sigalg=\"" + sigAlg.getAuthSubName() + "\"";
        }
    }

    /**
     * Adds the query parameter with the given name and value to the URL.
     *
     * @noinspection JavaDoc
     */
    public static void addParameter(StringBuffer url, String name, String value, boolean urlEncode) {
        try {
            name = urlEncode ? URLEncoder.encode(name, "UTF-8") : name;
            value = urlEncode ? URLEncoder.encode(value, "UTF-8") : value;
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Unable to encode parameters", uee);
        }
        if (url.indexOf("?") == -1) {
            url.append('?');
        } else {
            switch(url.charAt(url.length() - 1)) {
                case '?':
                case '&':
                    break;
                default:
                    url.append('&');
            }
        }
        url.append(name).append('=').append(value);
    }

    /**
     * Signs the data with the given key and the provided algorithm.
     *
     * @noinspection JavaDoc
     */
    private static byte[] sign(PrivateKey key, String data, SignatureAlgorithm algorithm) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(algorithm.getJCAName());
        signature.initSign(key);
        signature.update(data.getBytes());
        return signature.sign();
    }

    /**
     * Returns the signature algorithm to be used for the provided private key.
     *
     * @noinspection JavaDoc
     */
    private static SignatureAlgorithm getSigAlg(PrivateKey key) {
        String algorithm = key.getAlgorithm();
        if ("dsa".equalsIgnoreCase(algorithm)) {
            return DSA_SHA1;
        } else if ("rsa".equalsIgnoreCase(algorithm)) {
            return RSA_SHA1;
        } else {
            throw new IllegalArgumentException("Unknown algorithm in private key.");
        }
    }

    /**
     * Returns the URL to use to exchange the one-time-use token for
     * a session token.
     *
     * @param protocol the protocol to use to communicate with the server
     * @param domain   the domain at which the authentication server exists
     * @return the URL to exchange for the session token
     */
    private static String getSessionTokenUrl(String protocol, String domain) {
        return protocol + "://" + domain + "/accounts/AuthSubSessionToken";
    }

    /**
     * Returns the URL that handles token revocation.
     *
     * @param protocol the protocol to use to communicate with the server
     * @param domain   the domain at which the authentication server exists
     * @return the URL that handles token revocation.
     */
    private static String getRevokeTokenUrl(String protocol, String domain) {
        return protocol + "://" + domain + "/accounts/AuthSubRevokeToken";
    }

    /**
     * Returns the URL that handles token revocation.
     *
     * @param protocol the protocol to use to communicate with the server
     * @param domain   the domain at which the authentication server exists
     * @return the URL that handles token revocation.
     */
    private static String getTokenInfoUrl(String protocol, String domain) {
        return protocol + "://" + domain + "/accounts/AuthSubTokenInfo";
    }
}
