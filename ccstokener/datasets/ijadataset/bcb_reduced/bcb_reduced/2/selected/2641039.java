package au.edu.diasb.annotation.danno.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * This base class handles interactions with a generic HTTP server.  It
 * is intended to be specialized for different service types; e.g. Annotea.
 * <p>
 * Spring provides various facilities for accessing remote services over HTTP
 * (e.g. RMI, WSDL, Hessian, Burlap) but I don't think there if supports 'plain
 * old HTTP' services.  Besides, doing this ourselves limits our exposure to
 * the Spring APIs, and I happened to have the code lying around anyway.
 * <p>
 * TODO ... replace with Apache commons equivalents.
 * 
 * @author scrawley
 */
public abstract class GenericHTTPClient<R extends HTTPClient.Response> implements HTTPClient<R> {

    private static Logger logger = Logger.getLogger(GenericHTTPClient.class);

    protected final String loginURL;

    protected final String baseURL;

    private final String user;

    private final String password;

    private Map<String, String> cookieStore = new HashMap<String, String>();

    private Map<String, String> fields = new HashMap<String, String>();

    protected GenericHTTPClient(String baseURL, String loginURL, String user, String password) {
        this.baseURL = baseURL;
        this.loginURL = loginURL;
        this.user = user;
        this.password = password;
    }

    public boolean doAuthenticate() throws IOException {
        URL url = new URL(loginURL + "?j_username=" + user + "&j_password=" + password);
        logger.debug("HTTP POST request: URL=" + url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        try {
            conn.setInstanceFollowRedirects(false);
            conn.setDefaultUseCaches(false);
            conn.setUseCaches(false);
            conn.connect();
            int rc = conn.getResponseCode();
            logger.debug("HTTP POST response: status=" + rc + ",location=" + conn.getHeaderField("Location"));
            if (rc >= 400) {
                logger.error("Login request failed: rc = " + rc + ": url = " + url);
                return false;
            }
            processSetCookie(conn);
            return true;
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Process any Set-Cookie headers in the response
     * 
     * @param conn the connection with a response.
     */
    private void processSetCookie(HttpURLConnection conn) {
        Map<String, String> tempStore = null;
        for (String field : conn.getHeaderFields().get("Set-Cookie")) {
            if (tempStore == null) {
                tempStore = new HashMap<String, String>();
            }
            logger.debug("HTTP POST response: set-cookie=" + field);
            String cookie = field.substring(0, field.indexOf(';'));
            String[] parts = cookie.split("=", 2);
            if (tempStore.get(parts[0]) == null) {
                tempStore.put(parts[0], parts[1]);
            }
        }
        if (tempStore != null) {
            for (Map.Entry<String, String> entry : tempStore.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.length() == 0) {
                    cookieStore.remove(key);
                } else {
                    cookieStore.put(key, value);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : cookieStore.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
            }
            fields.put("Cookie", sb.toString());
        }
    }

    public R doPostCommand(String command) throws IOException {
        if (command == null) {
            return doRequest(new URL(baseURL), "POST", null, null, null);
        } else {
            return doRequest(new URL(baseURL + command), "POST", null, null, null);
        }
    }

    public R doDelete(String target) throws IOException {
        return doRequest(new URL(target), "DELETE", null, null, null);
    }

    public R doPostWithContent(String target, String content) throws IOException {
        if (target == null) {
            return doRequest(new URL(baseURL), "POST", content, null, null);
        } else {
            return doRequest(new URL(baseURL + target), "POST", content, null, null);
        }
    }

    public R doPut(String target, String content) throws IOException {
        if (target == null) {
            return doRequest(new URL(baseURL), "PUT", content, null, null);
        } else {
            return doRequest(new URL(baseURL + target), "PUT", content, null, null);
        }
    }

    public R doGet(String target) throws IOException {
        if (target == null) {
            return doRequest(new URL(baseURL), "GET", null, null, null);
        } else {
            return doRequest(new URL(baseURL + target), "GET", null, null, null);
        }
    }

    protected R doRequest(URL url, String method, String content, String contentType, String accept) throws IOException {
        boolean debug = logger.isDebugEnabled();
        if (debug) {
            logger.debug("HTTP " + method + " request: URL=" + url);
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(content != null);
        byte[] bytes = null;
        try {
            conn.setInstanceFollowRedirects(false);
            conn.setDefaultUseCaches(false);
            conn.setUseCaches(false);
            if (accept != null) {
                if (debug) {
                    logger.debug("HTTP " + method + " request: accept=" + accept);
                }
                conn.setRequestProperty("Accept", accept);
            }
            if (content != null) {
                bytes = content.getBytes();
                if (debug) {
                    logger.debug("HTTP " + method + " request: content-length=" + bytes.length);
                }
                conn.setRequestProperty("Content-Length", Integer.toString(bytes.length));
                if (contentType != null) {
                    if (debug) {
                        logger.debug("HTTP " + method + " request: content-type=" + contentType);
                    }
                    conn.setRequestProperty("Content-Type", contentType);
                }
            }
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (debug) {
                    logger.debug("HTTP " + method + " request: " + entry.getKey() + "=" + entry.getValue());
                }
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            conn.connect();
            if (bytes != null) {
                OutputStream os = conn.getOutputStream();
                os.write(bytes);
                os.close();
            }
            int rc = conn.getResponseCode();
            InputStream is = (rc >= 400) ? conn.getErrorStream() : conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count;
            while ((count = is.read(buffer)) > 0) {
                bos.write(buffer, 0, count);
            }
            is.close();
            return createResponse(conn, bos.toByteArray());
        } finally {
            conn.disconnect();
        }
    }

    protected abstract R createResponse(HttpURLConnection conn, byte[] content) throws IOException;

    /**
     * Set the Cookies field to a previously encoded string without updating the
     * cookie store.
     * 
     * @param cookies the Cookie field value.
     */
    protected void passCookies(String cookies) {
        fields.put("Cookies", cookies);
    }

    protected void setUserAgent(String userAgent) {
        fields.put("User-Agent", userAgent);
    }

    public void setHeaderField(String field, String value) {
        fields.put(field, value);
    }

    public void close() {
    }
}
