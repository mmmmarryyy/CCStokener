package com.dropbox.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.eclipse.remus.common.io.proxy.Proxy;
import org.eclipse.remus.common.io.proxy.ProxyUtil;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RESTUtility {

    protected static String defaultProtocol = "http";

    protected static String secureProtocol = "https";

    protected static int BUFFER_SIZE = 2048;

    protected static HttpParams sParams;

    protected static ThreadSafeClientConnManager sManager;

    static {
        sParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(sParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(sParams, "utf-8");
        sParams.setBooleanParameter("http.protocol.expect-continue", false);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        sslSocketFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        registry.register(new Scheme("https", sslSocketFactory, 443));
        sManager = new ThreadSafeClientConnManager(sParams, registry);
    }

    public static String defaultProtocol() {
        return defaultProtocol;
    }

    public static String secureProtocol() {
        return secureProtocol;
    }

    public static HttpClient getClient(String url) {
        Proxy proxyByUrl = ProxyUtil.getProxyByUrl(url);
        DefaultHttpClient ret = new DefaultHttpClient();
        if (proxyByUrl != null) {
            if (proxyByUrl.getUsername() != null && proxyByUrl.getUsername().length() > 0) {
                ret.getCredentialsProvider().setCredentials(new AuthScope(proxyByUrl.getAddress().getHostName(), proxyByUrl.getAddress().getPort()), new UsernamePasswordCredentials(proxyByUrl.getUsername(), proxyByUrl.getPassword()));
            }
            HttpHost proxy = new HttpHost(proxyByUrl.getAddress().getHostName(), proxyByUrl.getAddress().getPort());
            ret.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        return ret;
    }

    /**
	 * Used internally to URL encode a list of parameters, which makes it easier
	 * to do params than with a map. If you want to use this, the params are
	 * organized as key=value pairs in a row, and should convert to Strings.
	 */
    public static String urlencode(Object[] params) {
        String result = "";
        try {
            boolean firstTime = true;
            for (int i = 0; i < params.length; i += 2) {
                if (params[i + 1] != null) {
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        result += "&";
                    }
                    result += URLEncoder.encode("" + params[i], "UTF-8") + "=" + URLEncoder.encode("" + params[i + 1], "UTF-8");
                }
            }
        } catch (UnsupportedEncodingException e) {
        }
        return result;
    }

    /**
	 * Used internally to simplify reading a response in buffered lines.
	 */
    public static String readResponse(HttpResponse response) throws IOException {
        HttpEntity ent = response.getEntity();
        if (ent != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(ent.getContent()), BUFFER_SIZE);
            String inputLine = null;
            String result = "";
            while ((inputLine = in.readLine()) != null) {
                result += inputLine;
            }
            response.getEntity().consumeContent();
            return result;
        } else {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public static Object parseAsJSON(HttpResponse response) throws IOException {
        Object result;
        String body = readResponse(response);
        if (response.getStatusLine().getStatusCode() != 200) {
            Map hm = new HashMap();
            hm.put("ERROR", response.getStatusLine());
            hm.put("RESULT", body);
            result = hm;
            throw new IOException(hm.toString());
        } else {
            if (body.equals("OK")) {
                Map hm = new HashMap();
                hm.put("RESULT", body);
                result = hm;
            } else {
                try {
                    JSONParser parser = new JSONParser();
                    result = parser.parse(body);
                } catch (ParseException e) {
                    Map hm = new HashMap();
                    StatusLine strangeError = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 999, "Bad JSON From Server: " + e.toString());
                    hm.put("ERROR", strangeError);
                    hm.put("RESULT", body);
                    result = hm;
                }
            }
        }
        return result;
    }

    /**
	 * Used internally to execute the Apache HTTP Components request and pull
	 * out either JSON or a raw response. A Map is always returned that has
	 * either the real data from your call, or an "ERROR" or "RESULT" key. If
	 * "ERROR" is given then you have "RESULT" as the body of the HTTP response
	 * for informational purposes. If you have "RESULT" only then the body
	 * couldn't be parsed as JSON and is just a raw string.
	 */
    @SuppressWarnings("unchecked")
    public static Object executeRequest(HttpUriRequest req) throws IOException {
        HttpClient client = getClient(req.getURI().toString());
        HttpResponse response = client.execute(req);
        Object result = parseAsJSON(response);
        assert result != null : "Invariant: result can't be null.";
        return result;
    }

    /**
	 * Used internally to construct a complete URL to a given host, which can
	 * sometimes be the "API host" or the "content host" depending on the type
	 * of call.
	 */
    public static String buildFullURL(String protocol, String host, int port, String target) {
        String port_string = port == 80 ? "" : ":" + port;
        return protocol + "://" + host + port_string + target;
    }

    /**
	 * Used internally to build a URL path + params (if given) according to the
	 * API_VERSION.
	 */
    public static String buildURL(String url, int apiVersion, Object[] params) {
        try {
            url = URLEncoder.encode("/" + apiVersion + url, "UTF-8");
            url = url.replace("%2F", "/").replace("+", "%20");
            if (params != null && params.length > 0) {
                url += "?" + urlencode(params);
            }
        } catch (UnsupportedEncodingException uce) {
        }
        return url;
    }

    /**
	 * Used internally to simplify making requests to the services, and handling
	 * an error, parsing JSON, or returning a non-JSON body. See executeRequest
	 * to see what's in the returned Map.
	 */
    @SuppressWarnings("unchecked")
    public static Object request(String method, String protocol, String host, int port, String url, int apiVersion, Object params[], Authenticator auth) throws DropboxException {
        assert method.equals("GET") || method.equals("POST") : "Only GET and POST supported.";
        HttpUriRequest req = null;
        String target = null;
        try {
            if (method.equals("GET")) {
                target = buildFullURL(protocol, host, port, buildURL(url, apiVersion, params));
                req = new HttpGet(target);
            } else {
                target = buildFullURL(protocol, host, port, buildURL(url, apiVersion, null));
                HttpPost post = new HttpPost(target);
                if (params != null && params.length > 2) {
                    assert params.length % 2 == 0 : "Params must have an even number of elements.";
                    List nvps = new ArrayList();
                    for (int i = 0; i < params.length; i += 2) {
                        if (params[i + 1] != null) {
                            nvps.add(new BasicNameValuePair("" + params[i], "" + params[i + 1]));
                        }
                    }
                    post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
                    req = post;
                }
            }
            auth.sign(req);
            return executeRequest(req);
        } catch (Exception e) {
            throw new DropboxException(e);
        }
    }
}
