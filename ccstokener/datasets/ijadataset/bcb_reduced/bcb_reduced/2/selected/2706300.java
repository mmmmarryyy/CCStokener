package pl.sind.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import pl.sind.blip.message.BlipMessagePart;
import pl.sind.http.auth.AuthenticationStrategy;

public abstract class AbstractHttpConnector<E> implements HttpConnector<E> {

    protected Proxy proxy;

    private HashMap<URI, URL> urls = new HashMap<URI, URL>();

    private HttpHeader[] defaultHeaders;

    private HttpBodyGenerator bodyGenerator = new HttpBodyGenerator();

    private AuthenticationStrategy authStrategy;

    public AbstractHttpConnector(Proxy proxy, AuthenticationStrategy auth) {
        super();
        this.proxy = proxy;
        this.authStrategy = auth;
    }

    protected URL getURL(URI target) throws HttpRequestException {
        if (urls.containsKey(target)) {
            return urls.get(target);
        } else {
            URL url;
            try {
                url = target.toURL();
                if (url.getProtocol().equalsIgnoreCase("http")) {
                    urls.put(target, url);
                    return url;
                } else {
                    throw new HttpRequestException("Unknown protocol in URL " + target);
                }
            } catch (MalformedURLException e) {
                throw new HttpRequestException("Bad URL", e);
            }
        }
    }

    private void putHeaders(HttpRequest<E> con, HttpHeader[] headers) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].isSingle()) {
                con.setRequestHeader(headers[i].getName(), headers[i].getValue());
            } else {
                con.addRequestHeaders(headers[i].getName(), headers[i].getValue());
            }
        }
    }

    protected abstract HttpRequest<E> createConnection(HttpMethods method, URI target) throws HttpRequestException;

    public Download doDownload(HttpHeader[] headers, URI target) throws HttpRequestException {
        HttpRequest<E> con = createConnection(HttpMethods.METHOD_GET, target);
        if (defaultHeaders != null) {
            putHeaders(con, defaultHeaders);
        }
        if (headers != null) {
            putHeaders(con, headers);
        }
        HttpResponse<?> res = execute(con);
        if (res.getResponseCode() == 200) {
            return new Download(res);
        } else {
            throw new HttpRequestException(res.getResponseCode(), res.getResponseMessage());
        }
    }

    protected abstract HttpResponse<E> execute(HttpRequest<E> con) throws HttpRequestException;

    public HttpResponse<E> doRequest(HttpMethods method, HttpHeader[] headers, boolean auth, URI target, BlipMessagePart body) throws HttpRequestException {
        HttpRequest<E> con = createConnection(method, target);
        if (defaultHeaders != null) {
            putHeaders(con, defaultHeaders);
        }
        if (headers != null) {
            putHeaders(con, headers);
        }
        try {
            if (auth && authStrategy != null) {
                authStrategy.perform(con);
            }
            if (body != null) {
                bodyGenerator.writeBody(con, body);
            }
            HttpResponse<E> res = execute(con);
            return res;
        } catch (IOException e) {
            throw new HttpRequestException("Error executing request", e);
        }
    }

    public void setDefaultHeaders(HttpHeader[] headers) {
        this.defaultHeaders = headers;
    }
}
