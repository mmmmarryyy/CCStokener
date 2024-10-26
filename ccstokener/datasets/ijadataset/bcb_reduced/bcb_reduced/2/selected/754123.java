package com.carey.openoauth.http;

import static com.carey.openoauth.encoders.URL.queryString;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.carey.openoauth.encoders.OAuthException;

/**
 * Represents an HTTP Request object
 * 
 * @author Carey Zhou
 */
public class Request {

    private static final String CONTENT_LENGTH = "Content-Length";

    private String url;

    private Verb verb;

    private Map<String, String> bodyParams;

    private Map<String, String> headers;

    private String payload = null;

    public Request(Verb verb, String url) {
        this.verb = verb;
        this.url = url;
        this.bodyParams = new HashMap<String, String>();
        this.headers = new HashMap<String, String>();
    }

    /**
	 * Execute the request and return a {@link Response}
	 * 
	 * @return Http Response
	 * @throws OAuthException
	 */
    public Response send() throws OAuthException {
        try {
            return doSend();
        } catch (IOException ioe) {
            throw new OAuthException("Problems while creating connection", ioe);
        }
    }

    Response doSend() throws IOException {
        HttpURLConnection connection;
        String str = this.headers.get("Authorization");
        if (str != null) {
            String hs[] = str.split(",");
            if (hs[0].startsWith("OAuth ")) {
                hs[0] = hs[0].substring("OAuth ".length());
            }
            String newUrl = url + "?";
            for (int i = 0; i < hs.length; i++) {
                hs[i] = hs[i].trim().replace("\"", "");
                if (i == hs.length - 1) {
                    newUrl += hs[i];
                } else {
                    newUrl += hs[i] + "&";
                }
            }
            System.out.println("newUrl=" + newUrl);
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestMethod(this.verb.name());
            if (verb.equals(Verb.PUT) || verb.equals(Verb.POST)) {
                addBody(connection, getBodyContents());
            }
            return new Response(connection);
        }
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(this.verb.name());
        addHeaders(connection);
        if (verb.equals(Verb.PUT) || verb.equals(Verb.POST)) {
            addBody(connection, getBodyContents());
        }
        return new Response(connection);
    }

    void addHeaders(HttpURLConnection conn) {
        for (String key : headers.keySet()) {
            conn.setRequestProperty(key, headers.get(key));
        }
    }

    void addBody(HttpURLConnection conn, String content) throws IOException {
        conn.setRequestProperty(CONTENT_LENGTH, String.valueOf(content.getBytes().length));
        conn.setDoOutput(true);
        conn.getOutputStream().write(content.getBytes());
    }

    /**
	 * Add an HTTP Header to the Request
	 * 
	 * @param name
	 * @param value
	 */
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    /**
	 * Add a body Parameter (for POST/ PUT Requests)
	 * 
	 * @param name
	 * @param value
	 */
    public void addBodyParameter(String key, String value) {
        this.bodyParams.put(key, value);
    }

    /**
	 * Add body payload.
	 * 
	 * This method is used when the HTTP body is not a form-url-encoded string,
	 * but another thing. Like for example XML.
	 * 
	 * Note: The contents are not part of the OAuth signature
	 * 
	 * @param payload
	 */
    public void addPayload(String payload) {
        this.payload = payload;
    }

    /**
	 * Get a {@link Map} of the query string parameters.
	 * 
	 * @return a map containing the query string parameters
	 * @throws OAuthException
	 */
    public Set<Map.Entry<String, String>> getQueryStringParams() throws OAuthException {
        try {
            Map<String, String> params = new HashMap<String, String>();
            String query = new URL(url).getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String pair[] = param.split("=");
                    params.put(pair[0], pair[1]);
                }
            }
            return params.entrySet();
        } catch (MalformedURLException mue) {
            throw new OAuthException("Malformed URL", mue);
        }
    }

    /**
	 * Obtains a {@link Map} of the body parameters.
	 * 
	 * @return a map containing the body parameters.
	 */
    public Set<Map.Entry<String, String>> getBodyParams() {
        return bodyParams.entrySet();
    }

    /**
	 * Obtains the URL of the HTTP Request.
	 * 
	 * @return the original URL of the HTTP Request
	 */
    public String getUrl() {
        return url;
    }

    /**
	 * Returns the URL without the port and the query string part.
	 * 
	 * @return the OAuth-sanitized URL
	 */
    public String getSanitizedUrl() {
        return url.replaceAll("\\?.*", "").replace("\\:\\d{4}", "");
    }

    public String getBodyContents() {
        return (payload != null) ? payload : queryString(bodyParams).replaceFirst("\\?", "");
    }

    /**
	 * Returns the HTTP Verb
	 * 
	 * @return the verb
	 */
    public Verb getVerb() {
        return verb;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
	 * An enumeration containing the most common HTTP Verbs.
	 * 
	 * @author Pablo Fernandez
	 */
    public static enum Verb {

        GET, POST, PUT, DELETE
    }

    public void dumpRequest(PrintWriter out) {
        out.println("<BeginRequest>---------------------");
        out.println("url: " + url);
        out.println("Verb: " + verb);
        out.println("************bodyParams************");
        for (String key : bodyParams.keySet()) {
            out.println(key + ": " + bodyParams.get(key));
        }
        out.println("**********************************");
        out.println("************headers************");
        for (String key : headers.keySet()) {
            out.println(key + ": " + headers.get(key));
        }
        out.println("*******************************");
        out.println("payload: " + payload);
        out.println("----------------------<EndRequest>");
    }
}
