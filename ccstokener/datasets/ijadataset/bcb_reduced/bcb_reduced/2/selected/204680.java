package com.myspace.myspaceid.oauth;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.servlet.http.HttpServletRequest;
import com.myspace.myspaceid.*;
import com.myspace.myspaceid.util.*;

/** 
 * Encapsulates an OAuthServer against which we send requests for tokens, etc.
 */
public class OAuthServer {

    private static final String VERSION = "0.8.1";

    protected OAuthConsumer consumer;

    protected OAuthToken requestToken;

    protected OAuthToken accessToken;

    /**
	 * Constructor.
	 * @param consumer The OAuthConsumer that we will use to query this OAuthServer.
	 */
    public OAuthServer(OAuthConsumer consumer) {
        this.consumer = consumer;
    }

    /**
	 * Returns the request token
	 * @return
	 */
    public OAuthToken getRequestToken() {
        return requestToken;
    }

    /**
	 * 
	 * @param requestToken
	 */
    public void setRequestToken(OAuthToken requestToken) {
        this.requestToken = requestToken;
    }

    /**
	 * 
	 * @return
	 */
    public OAuthToken getAccessToken() {
        return accessToken;
    }

    /**
	 * Sets the access token to use.
	 * @param accessToken The access token that has been obtained and which we will use against this server.
	 */
    public void setAccessToken(OAuthToken accessToken) {
        this.accessToken = accessToken;
    }

    /**
	 * Generates request URL with a given path (i.e., request_token, authorize, etc.).
	 * @param path The initial URL path, e.g., http://api.myspace.com/request_token.
	 * @return The URL generated.
	 */
    public String generateRequestUrl(String path) {
        return generateRequestUrl(path, new HashMap<String, String>());
    }

    /**
	 * Generates request URL with a given path (i.e., request_token, authorize, etc.).
	 * @param path The initial URL path, e.g., http://api.myspace.com/request_token.
	 * @param tokenSecret A token secret to use, if any.  This is required for obtaining the access token and for subsequent data access.
	 * @param args A Map containing any additional parameters required for this particular request.
	 * @return The URL generated.
	 */
    public String generateRequestUrl(String path, Map<String, String> args) {
        return generateRequestUrl(path, args, "GET", null);
    }

    public String generateRequestUrl(String path, Map<String, String> args, String method, Set<String> removeParams) {
        long randomNum = new Random().nextLong();
        long timestamp = 0;
        if (consumer.getTimeZone() == null) {
            timestamp = (long) System.currentTimeMillis() / 1000;
        } else {
            Utilities utilObj = new Utilities();
            timestamp = utilObj.getTimeStamp(consumer.getTimeZone());
        }
        args.put("oauth_consumer_key", consumer.getKey());
        args.put("oauth_nonce", Long.toString(randomNum));
        args.put("oauth_signature_method", "HMAC-SHA1");
        args.put("oauth_timestamp", Long.toString(timestamp));
        args.put("oauth_version", "1.0");
        args.put("msid_sdk", "java_" + VERSION);
        if (accessToken != null && accessToken.getKey() != null) args.put("oauth_token", accessToken.getKey());
        String part3 = buildParams(args);
        String tokenSecret = computeTokenSecret();
        String part1 = method;
        String part2 = path;
        String baseString = encode(part1) + "&" + encode(part2) + "&" + encode(part3);
        String combinedSecret = consumer.getSecret() + "&" + tokenSecret;
        String sig = getHMACSHA1(combinedSecret, baseString);
        if (removeParams != null) {
            for (String key : removeParams) {
                args.remove(key);
            }
        }
        String params = buildParams(args);
        String result = part2 + "?" + params + "&oauth_signature=" + encode(sig);
        return result;
    }

    /**
	 * Build parameters as a string from Map.  Parameters are sorted.
	 * @param args parameters
	 * @return URL parameters in string form.
	 */
    public static String buildParams(Map<String, String> args) {
        List<String> argList = new ArrayList<String>();
        for (String key : args.keySet()) {
            String arg = key + "=" + encode(args.get(key));
            argList.add(arg);
        }
        Collections.sort(argList);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < argList.size(); i++) {
            s.append(argList.get(i));
            if (i != argList.size() - 1) {
                s.append("&");
            }
        }
        return s.toString();
    }

    /**
	 * Returns signature from an Http request.  Use this for checking the signature for an iframed app.  Iframing is the 
	 * standard and recommended way for using this SDK to build the server component of your on-site MySpace app.  However, 
	 * to verify that requests to your server are coming from MySpace, you need to verify that the signature in those 
	 * requests are correct.  This method is provided as a convenience method to help you verify the signature of the requests.
	 * @param The Http request passed to your iframed app.
	 * @return The signature computed from the given Http request.
	 */
    public String buildSignature(HttpServletRequest req) {
        HashMap<String, String> urlParams = new HashMap<String, String>();
        Map map = req.getParameterMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String[] values = (String[]) map.get(key);
            if (!key.equals("oauth_signature")) {
                urlParams.put(key, values[0]);
            }
        }
        String part3 = buildParams(urlParams);
        String part1 = req.getMethod();
        int port = req.getServerPort();
        String portStr = port == 80 ? "" : ":" + port;
        String part2 = req.getScheme() + "://" + req.getServerName() + portStr + req.getRequestURI();
        String baseString = encode(part1) + "&" + encode(part2) + "&" + encode(part3);
        System.out.println("base string = '" + baseString + "'");
        String combinedSecret = consumer.getSecret() + "&" + computeTokenSecret();
        String sig = getHMACSHA1(combinedSecret, baseString);
        return sig;
    }

    protected String computeTokenSecret() {
        String tokenSecret = null;
        if (getAccessToken() == null) {
            if (getRequestToken() == null) {
                tokenSecret = "";
            } else {
                tokenSecret = getRequestToken().getSecret();
            }
        } else {
            tokenSecret = getAccessToken().getSecret();
        }
        return tokenSecret;
    }

    /**
	 * Returns the HMAC-SHA1 signature of a given string.
	 * @param key Key (or secret) to use in obtaining the signature.
	 * @param data The string whose signature we want to compute.
	 * @return The computed signature.
	 */
    protected static String getHMACSHA1(String key, String data) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes("UTF-8"), "HMAC-SHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes("UTF-8"));
            return Base64Util.encodeBytes(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate HMAC-SHA1", e);
        }
    }

    /**
	 * URL-encodes the given string.
	 * @param value String to encode.
	 * @return The encoded string.
	 */
    public static String encode(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, "utf-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    /**
	 * Does an HTTP GET request.
	 * @param urlStr URL to send request to.
	 * @return The response from the remote server.
	 */
    public String doHttpReq(String urlStr) {
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            is = url.openStream();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            throw new MySpaceException(sw.toString(), MySpaceException.CONNECT_FAILED);
        }
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        do {
            String line = null;
            try {
                line = br.readLine();
            } catch (IOException e) {
                throw new MySpaceException(e.getMessage(), MySpaceException.REQUEST_FAILED);
            }
            if (line == null) break;
            sb.append(line).append("\n");
        } while (true);
        String response = sb.toString();
        return response;
    }

    /**
	 * Does an HTTP request with customizable method type.
	 * @param urlStr URL to send request to.
	 * @param requestMethod
	 * @return The response from the remote server.
	 */
    public String doHttpMethodReq(String urlStr, String requestMethod, String paramStr) {
        return doHttpMethodReq(urlStr, requestMethod, paramStr, null);
    }

    /**
	 * Does an HTTP request with customizable method type.
	 * @param urlStr URL to send request to.
	 * @param header Header request properties to send.
	 * @return The response from the remote server.
	 */
    public String doHttpMethodReq(String urlStr, String requestMethod, String paramStr, Map<String, String> header) {
        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            if (requestMethod != null) conn.setRequestMethod(requestMethod);
            if (header != null) {
                for (String key : header.keySet()) {
                    conn.setRequestProperty(key, header.get(key));
                }
            }
            OutputStreamWriter wr = null;
            if (requestMethod != null && !requestMethod.equals("GET") && !requestMethod.equals("DELETE")) {
                wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(paramStr);
                wr.flush();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            do {
                String line = null;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    throw new MySpaceException(e.getMessage(), MySpaceException.REQUEST_FAILED);
                }
                if (line == null) break;
                sb.append(line).append("\n");
            } while (true);
            if (wr != null) wr.close();
            if (br != null) br.close();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            throw new MySpaceException(sw.toString(), MySpaceException.REMOTE_ERROR);
        }
        String response = sb.toString();
        return response;
    }
}
