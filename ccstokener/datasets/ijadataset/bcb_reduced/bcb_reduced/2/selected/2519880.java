package org.gwtrpc4j.http.jse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.gwtrpc4j.http.RequestBuilderAbstract;
import org.gwtrpc4j.http.RequestCallbackAdapter;
import org.gwtrpc4j.http.RpcResponse;
import org.gwtrpc4j.util.IOUtils;
import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

/**
 * HTTP/1.1 200 OK Expires: Thu, 01 Jan 1970 00:00:00 GMT Set-Cookie:
 * JSESSIONID=lx5yyk3cwa8f;Path=/ Content-Encoding: gzip Content-Length: 141
 * Content-Type: application/json; charset=utf-8 Content-Disposition: attachment
 * Server: Jetty(6.1.x)
 **/
public class JSERequestBuilder extends RequestBuilderAbstract implements Runnable {

    private ExecutorService asynchroExecutor;

    private Proxy proxy;

    private CookieManager cookieManager;

    public JSERequestBuilder(Method httpMethod, String url) {
        super(httpMethod, url);
    }

    @Override
    public Request send() throws RequestException {
        final RequestCallbackAdapter callback = (RequestCallbackAdapter) getCallback();
        boolean isSynchroCall = callback.isSynchro();
        if (isSynchroCall) {
            run();
        } else {
            asynchroExecutor.execute(this);
        }
        return null;
    }

    public Object call() throws Exception {
        return null;
    }

    public void run() {
        URL url;
        try {
            url = new URL(getUrl());
            HttpURLConnection connection;
            if (proxy == null) {
                connection = (HttpURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection(proxy);
            }
            Response resp = null;
            try {
                initConnection(connection, this.getHTTPMethod(), this.getRequestData().length());
                cookieManager.setCookies(connection);
                connection.connect();
                IOUtils.write(connection.getOutputStream(), this.getRequestData());
                resp = readResponse(connection);
                cookieManager.storeCookies(connection);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            getCallback().onResponseReceived(null, resp);
        } catch (MalformedURLException e) {
            getCallback().onError(null, e);
        } catch (IOException e) {
            getCallback().onError(null, e);
        } catch (RequestException e) {
            getCallback().onError(null, e);
        }
    }

    private Response readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return new RpcResponse(responseCode, null, null);
        }
        Header[] rcpHeaderArray = toRcpHeader(connection.getHeaderFields());
        InputStream input = connection.getInputStream();
        String data = IOUtils.readAsString(input);
        return new RpcResponse(responseCode, rcpHeaderArray, data);
    }

    private void initConnection(final HttpURLConnection connection, String httpMethod, int contentLength) throws ProtocolException, RequestException {
        connection.setRequestMethod(httpMethod);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        if (this.getTimeoutMillis() != 0) {
            connection.setReadTimeout(this.getTimeoutMillis());
            connection.setConnectTimeout(this.getTimeoutMillis());
        }
        connection.setFixedLengthStreamingMode(contentLength);
        setHeader("Connection", "close");
        addHeaders(connection);
    }

    protected void addHeaders(URLConnection connection) throws RequestException {
        if (headers != null && headers.size() > 0) {
            for (Header header : headers) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }
        } else {
            connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        }
    }

    /**
	 * private void addCookie(HttpURLConnection connection) { for (String cookie
	 * : cookies) { connection.setRequestProperty("Cookie", cookie); } }
	 **/
    public Executor getAsynchroExecutor() {
        return asynchroExecutor;
    }

    public void setAsynchroExecutor(ExecutorService asynchroExecutor) {
        this.asynchroExecutor = asynchroExecutor;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setCookieManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
    }
}
