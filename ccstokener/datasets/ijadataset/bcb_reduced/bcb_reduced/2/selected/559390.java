package org.jdesktop.http;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.*;
import org.jdesktop.beans.AbstractBean;

/**
 * <p>Represents a user's "session" on the web. Think of it as a "tab" in a
 * tabbed web browser. It may access multiple web sites during one "session",
 * but remembers the cookies for all of them.</p>
 *
 * <p>Sessions also contain all the information needed to keep track of the
 * progress of a request/response cycle (including uploading and downloading
 * of data). The values are reset at the beginning of a request and maintain
 * their values until the next request is made. The property change events are
 * fired on whatever thread called the execute method -- most likely a background
 * thread (not the EDT). Be careful of that when binding GUI widgets to these
 * properties.</p>
 * 
 * @author rbair
 */
public class Session extends AbstractBean {

    /**
     * Specifies a value to use for security, either Low, Medium, or High. This
     * is currently used for determining how to treat SSL connections.
     *
     * @see #setSslSecurityLevel
     */
    public enum SecurityLevel {

        Low, Medium, High
    }

    ;

    private SecurityLevel sslSecurity;

    private SecurityHandler handler;

    /**
     * Keeps track of the state of the Session when performing a request/response
     * cycle.
     */
    private State state = State.READY;

    /**
     * Keeps track of the total number of bytes that are to be sent or receieved.
     * This is reset when DONE, or switching from upload to download. This is
     * only used if the content-length is known, otherwise it is set to -1.
     */
    private long totalBytes = -1;

    /**
     * Keeps track of the total number of bytes transfered upstream or downstream.
     * This is reset when DONE, or when switching from upload to download. This
     * is used whether or not the content-length is known
     */
    private long bytesSoFar = 0;

    /** Creates a new Session. Automatically installs the {@link CookieManager}.*/
    public Session() {
        this(true);
    }

    /** 
     * Creates a new Session. If <code>installCookieManager</code> is true,
     * then the CookieManager is installed automatically. Otherwise, the
     * <code>CookieManager</code> will not be installed, allowing you to use some
     * other cookie manager.
     * 
     * @param installCookieManager
     */
    public Session(boolean installCookieManager) {
        setSslSecurityLevel(SecurityLevel.Medium);
        setMediumSecurityHandler(new DefaultSecurityHandler());
        if (installCookieManager) {
            CookieManager.install();
        }
    }

    /**
     * Sets the security level to use with SSL.
     *
     * @param level one of High, Medium, or Low. Low will not prompt or fail for self signed certs.
     *              Medium will prompt for self signed certs. High will fall back on the default
     *              behavior, and simply fail for self signed certs.
     */
    public void setSslSecurityLevel(SecurityLevel level) {
        SecurityLevel old = getSslSecurityLevel();
        sslSecurity = level;
        firePropertyChange("sslSecurityLevel", old, getSslSecurityLevel());
    }

    /**
     * Gets the SecurityLevel used for SSL connections.
     *
     * @return the SecurityLevel
     * @see #setSslSecurityLevel
     */
    public final SecurityLevel getSslSecurityLevel() {
        return sslSecurity;
    }

    void setMediumSecurityHandler(SecurityHandler h) {
        SecurityHandler old = getMediumSecurityHandler();
        this.handler = h;
        firePropertyChange("mediumSecurityHandler", old, getMediumSecurityHandler());
    }

    SecurityHandler getMediumSecurityHandler() {
        return handler;
    }

    private SSLSocketFactory createSocketFactory(String host) {
        try {
            TrustManager tm = null;
            Session.SecurityLevel level = getSslSecurityLevel();
            if (level == Session.SecurityLevel.Low) {
                tm = new LowSecurityX509TrustManager(null);
            } else if (level == Session.SecurityLevel.Medium) {
                tm = new MediumSecurityX509TrustManager(host, getMediumSecurityHandler(), null);
            } else {
                tm = new HighSecurityX509TrustManager(null);
            }
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[] { tm }, null);
            return context.getSocketFactory();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public final long getTotalBytes() {
        return totalBytes;
    }

    private void setTotalBytes(long bytes) {
        long old = totalBytes;
        float oldProgress = getProgress();
        firePropertyChange("totalBytes", old, this.totalBytes = bytes);
        firePropertyChange("progress", oldProgress, getProgress());
    }

    public final long getBytesSoFar() {
        return bytesSoFar;
    }

    private void setBytesSoFar(long bytes) {
        long old = this.bytesSoFar;
        float oldProgress = getProgress();
        firePropertyChange("bytesSoFar", old, this.bytesSoFar = bytes);
        firePropertyChange("progress", oldProgress, getProgress());
    }

    public final float getProgress() {
        if (totalBytes <= 0) return -1f;
        float total = totalBytes;
        float num = bytesSoFar;
        return num / total;
    }

    public final State getState() {
        return state;
    }

    protected void setState(State s) {
        State old = this.state;
        firePropertyChange("state", old, this.state = s);
    }

    /**
     * Constructs and executes a {@link Request} using the Method.GET method.
     * This method blocks.
     *
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response get(String url) throws Exception {
        return execute(Method.GET, url);
    }

    /**
     * Constructs and executes a {@link Request} using the Method.GET method.
     * This method blocks.
     *
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @param params The params to include in the request. This may be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response get(String url, Parameter... params) throws Exception {
        return execute(Method.GET, url, params);
    }

    /**
     * Constructs and executes a {@link Request} using the Method.POST method.
     * This method blocks.
     *
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response post(String url) throws Exception {
        return execute(Method.POST, url);
    }

    /**
     * Constructs and executes a {@link Request} using the Method.POST method.
     * This method blocks.
     *
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @param params The params to include in the request. This may be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response post(String url, Parameter... params) throws Exception {
        return execute(Method.POST, url, params);
    }

    /**
     * Constructs and executes a {@link Request} using the Method.PUT method.
     * This method blocks.
     *
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response put(String url) throws Exception {
        return execute(Method.PUT, url);
    }

    /**
     * Constructs and executes a {@link Request} using the Method.PUT method.
     * This method blocks.
     *
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @param params The params to include in the request. This may be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response put(String url, Parameter... params) throws Exception {
        return execute(Method.PUT, url, params);
    }

    /**
     * Constructs and executes a {@link Request}, and returns the {@link Response}.
     * This method blocks. The given <code>method</code>, <code>url</code>
     * will be used to construct the <code>Request</code>.
     * All other <code>Request</code> properties are left in their default state.
     *
     * @param method The HTTP {@link Method} to use. This must not be null.
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response execute(Method method, String url) throws Exception {
        return execute(method, url, new Parameter[0]);
    }

    /**
     * Constructs and executes a {@link Request}, and returns the {@link Response}.
     * This method blocks. The given <code>method</code>, <code>url</code>, and
     * <code>params</code> will be used to construct the <code>Request</code>.
     * All other <code>Request</code> properties are left in their default state.
     *
     * @param method The HTTP {@link Method} to use. This must not be null.
     * @param url The url to hit. This url may contain a query string (ie: params).
     *            The url cannot be null.
     * @param params The params to include in the request. This may be null.
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public final Response execute(Method method, String url, Parameter... params) throws Exception {
        if (method == null) {
            throw new NullPointerException("method cannot be null");
        }
        if (url == null) {
            throw new NullPointerException("url cannot be null");
        }
        Request req = new Request();
        req.setParameters(params);
        req.setMethod(method);
        req.setUrl(url);
        return execute(req);
    }

    /**
     * Executes the given {@link Request}, and returns a {@link Response}.
     * This method blocks.
     *
     * @return the {@link Response} to the {@link Request}.
     * @throws Exception if an error occurs while creating or executing the
     *         <code>Request</code> on the client machine. That is, if normal
     *         http errors occur, they will not throw an exception (such as BAD_GATEWAY, etc).
     */
    public Response execute(Request req) throws Exception {
        try {
            setTotalBytes(-1);
            setBytesSoFar(0);
            setState(State.CONNECTING);
            StringBuffer surl = new StringBuffer(req.getUrl());
            if (surl.length() == 0) {
                setState(State.FAILED);
                throw new IllegalStateException("Cannot excecute a request that has no URL specified");
            }
            char delim = '?';
            for (Parameter p : req.getParameters()) {
                surl.append(delim);
                delim = '&';
                String name = URLEncoder.encode(p.getName(), "UTF-8");
                String value = URLEncoder.encode(p.getValue(), "UTF-8");
                surl.append(name + "=" + value);
            }
            URL url = createURL(surl.toString());
            URLConnection conn = url.openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                setState(State.FAILED);
                throw new IllegalStateException("Must be an HTTP or HTTPS based URL");
            }
            HttpURLConnection http = (HttpURLConnection) conn;
            http.setRequestMethod(req.getMethod().name());
            http.setInstanceFollowRedirects(req.getFollowRedirects());
            for (Header h : req.getHeaders()) {
                http.setRequestProperty(h.getName(), h.getValue());
            }
            if (http instanceof HttpsURLConnection) {
                HttpsURLConnection https = (HttpsURLConnection) http;
                https.setSSLSocketFactory(createSocketFactory(url.getHost()));
            }
            long contentLength = -1;
            Header contentLengthHeader = req.getHeader("Content-Length");
            if (contentLengthHeader != null) {
                try {
                    contentLength = Long.parseLong(contentLengthHeader.getValue().trim());
                } catch (NumberFormatException ex) {
                    contentLength = -1;
                }
            }
            setTotalBytes(contentLength);
            setState(State.SENDING);
            OutputStream out = null;
            InputStream body = req.getBody();
            if (body != null) {
                try {
                    http.setDoOutput(true);
                    out = http.getOutputStream();
                    byte[] buffer = new byte[8096];
                    int length = -1;
                    while ((length = body.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                        setBytesSoFar(bytesSoFar + length);
                    }
                } catch (Exception e) {
                    setState(State.FAILED);
                    throw e;
                } finally {
                    if (out != null) out.close();
                    body.close();
                }
            }
            setState(State.SENT);
            http.connect();
            setBytesSoFar(0);
            setTotalBytes(http.getContentLength());
            setState(State.RECEIVING);
            Set<Header> headers = new HashSet<Header>();
            Header contentType = null;
            for (Map.Entry<String, List<String>> entry : http.getHeaderFields().entrySet()) {
                String headerKey = entry.getKey();
                String headerValue = http.getHeaderField(headerKey);
                if (headerKey == null) continue;
                List<String> values = entry.getValue();
                Header.Element[] elements = new Header.Element[values.size()];
                for (int j = 0; j < elements.length; j++) {
                    elements[j] = new Header.Element(new Parameter(values.get(j), values.get(j)));
                }
                Header h = new Header(headerKey, headerValue, elements);
                headers.add(h);
                if ("Content-Type".equalsIgnoreCase(headerKey)) contentType = h;
            }
            byte[] responseBody = null;
            StatusCode responseCode = StatusCode.INTERNAL_SERVER_ERROR;
            InputStream responseStream = null;
            try {
                responseStream = http.getInputStream();
                responseCode = StatusCode.valueOf(http.getResponseCode());
                String contentEncoding = http.getContentEncoding();
                if ("gzip".equals(contentEncoding)) {
                    responseStream = new GZIPInputStream(responseStream);
                }
                responseBody = readFully(responseStream);
            } catch (FileNotFoundException e) {
                responseStream = http.getErrorStream();
                responseBody = readFully(responseStream);
            } catch (HttpRetryException e) {
                setState(State.FAILED);
                return new Response(StatusCode.NOT_FOUND, "HttpRetryException: " + e.getMessage(), null, null, null, req.getUrl());
            } catch (UnknownHostException e) {
                setState(State.FAILED);
                return new Response(StatusCode.NOT_FOUND, "Unknown host", null, null, null, req.getUrl());
            } catch (IOException ex) {
                String msg = ex.getMessage();
                if (msg.contains("Server returned HTTP response code:")) {
                    int startIndex = msg.indexOf("code: ") + 6;
                    String s = msg.substring(startIndex, startIndex + 3);
                    responseCode = StatusCode.valueOf(Integer.parseInt(s));
                    responseStream = http.getErrorStream();
                    responseBody = readFully(responseStream);
                } else {
                    throw ex;
                }
            } finally {
                if (responseStream != null) responseStream.close();
            }
            String foo = "foo";
            URI uri = new URI(req.getUrl());
            URI uu = uri.resolve(new URI(foo));
            String baseUrl = uu.toString().substring(0, uu.toString().length() - foo.length());
            String charset = null;
            if (contentType != null) {
                String tmp = contentType.getValue();
                int index = tmp.indexOf(";");
                if (index >= 0) {
                    index = tmp.indexOf("=", index + 1);
                    if (index > 0) charset = contentType.getValue().substring(index + 1);
                }
            }
            Response response = new Response(responseCode, http.getResponseMessage(), responseBody, charset, headers, baseUrl);
            setState(State.DONE);
            return response;
        } catch (InterruptedException ex) {
            setState(State.ABORTED);
            throw ex;
        } finally {
        }
    }

    /**
     * This method exists for the sake of testing. I can create a url while testing
     * even without having internet access by overriding this method to return
     * an HttpURLConnection subclass that doesn't actually connect to the
     * internet. I can then fake out all the operations of the URL connection.
     * 
     * This method is not to be overridden by any classes other than the
     * test class.
     * 
     * @param conn
     * @return
     * @throws java.io.IOException
     */
    protected URL createURL(String surl) throws MalformedURLException {
        return new URL(surl.toString());
    }

    private byte[] readFully(InputStream in) throws IOException {
        if (in == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream(8096);
        byte[] buffer = new byte[8096];
        int length = -1;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
            setBytesSoFar(bytesSoFar + length);
        }
        in.close();
        return out.toByteArray();
    }
}
