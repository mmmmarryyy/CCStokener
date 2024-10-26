package pl.sind.http.basic;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import pl.sind.http.AbstractHttpConnector;
import pl.sind.http.HttpMethods;
import pl.sind.http.HttpRequest;
import pl.sind.http.HttpRequestException;
import pl.sind.http.HttpResponse;
import pl.sind.http.auth.AuthenticationStrategy;

/**
 * Simple HTTP conector based on HttpURLConnection.
 * 
 * @author Lukasz Wozniak
 * 
 */
public class SimpleJavaHttpConnector extends AbstractHttpConnector<HttpURLConnection> {

    public SimpleJavaHttpConnector(Proxy proxy, AuthenticationStrategy auth) {
        super(proxy, auth);
    }

    @Override
    protected HttpRequest<HttpURLConnection> createConnection(HttpMethods method, URI target) throws HttpRequestException {
        try {
            HttpURLConnection con;
            if (proxy != null) {
                con = (HttpURLConnection) getURL(target).openConnection(proxy);
            } else {
                con = (HttpURLConnection) getURL(target).openConnection(Proxy.NO_PROXY);
            }
            return new UrlHttpRequest(con);
        } catch (Exception e) {
            throw new HttpRequestException(e);
        }
    }

    @Override
    protected HttpResponse<HttpURLConnection> execute(HttpRequest<HttpURLConnection> con) throws HttpRequestException {
        HttpURLConnection unwrap = con.unwrap();
        try {
            unwrap.connect();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return new UrlHttpResponse(unwrap);
    }
}
