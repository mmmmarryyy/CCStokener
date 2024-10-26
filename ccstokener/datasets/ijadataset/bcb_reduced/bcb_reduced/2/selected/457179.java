package cease.http;

import java.io.IOException;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * @author dhf
 */
public class HttpRequestExecutor {

    private HttpClient httpClient;

    private static String CHARSET = "UTF-8";

    public HttpRequestExecutor() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpConnectionParams.setSoTimeout(httpParams, 10000);
        ConnPerRouteBean cprb = new ConnPerRouteBean(20);
        ConnManagerParams.setTimeout(httpParams, 10000);
        ConnManagerParams.setMaxTotalConnections(httpParams, 1000);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, cprb);
        SchemeRegistry sr = new SchemeRegistry();
        Scheme http = new Scheme("http", PlainSocketFactory.getSocketFactory(), 80);
        Scheme https = new Scheme("https", SSLSocketFactory.getSocketFactory(), 443);
        sr.register(http);
        sr.register(https);
        ThreadSafeClientConnManager TSCCM = new ThreadSafeClientConnManager(httpParams, sr);
        httpClient = new DefaultHttpClient(TSCCM, httpParams);
    }

    public HttpParams getHttpParams() {
        return httpClient.getParams();
    }

    public HttpRequestExecutor(HttpClient client) {
        if (null == client) {
            throw new IllegalArgumentException("[client] could not be null");
        }
        this.httpClient = client;
    }

    public HttpRequestResult get(String url, List<NameValuePair> parameters, Header[] headers, String charset) throws IOException {
        if (null == url) {
            throw new IllegalArgumentException("[url] could not be null");
        }
        String queryString = null;
        if (null != parameters && parameters.size() > 0) {
            if (null == charset) {
                charset = CHARSET;
            }
            queryString = URLEncodedUtils.format(parameters, charset);
        }
        String requestUri = url;
        if (null != queryString) {
            if (-1 == url.indexOf("?")) {
                requestUri = url + "?" + queryString;
            } else {
                requestUri = url + "&" + queryString;
            }
        }
        HttpGet get = new HttpGet(requestUri);
        if (null != headers && headers.length > 0) {
            get.setHeaders(headers);
        }
        return execute(get);
    }

    public HttpRequestResult post(String url, List<NameValuePair> parameters, Header[] headers, String charset) throws IOException {
        return post(url, parameters, null, headers, charset);
    }

    public HttpRequestResult post(String url, List<NameValuePair> parameters, List<HttpEntity> entities, Header[] headers, String charset) throws IOException {
        if (null == url) {
            throw new IllegalArgumentException("[url] could not be null");
        }
        HttpPost post = new HttpPost(url);
        if (null != parameters && parameters.size() > 0) {
            if (null == charset) {
                charset = CHARSET;
            }
            UrlEncodedFormEntity urfe = new UrlEncodedFormEntity(parameters, charset);
            post.setEntity(urfe);
        }
        if (null != entities && entities.size() > 0) {
            for (HttpEntity entity : entities) {
                post.setEntity(entity);
            }
        }
        if (null != headers && headers.length > 0) {
            post.setHeaders(headers);
        }
        return execute(post);
    }

    public HttpRequestResult execute(HttpUriRequest request) throws IOException {
        return httpClient.execute(request, new HttpResponseHandler());
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<T> handler) throws IOException {
        return httpClient.execute(request, handler);
    }

    public void shutdown() {
        httpClient.getConnectionManager().shutdown();
    }
}
