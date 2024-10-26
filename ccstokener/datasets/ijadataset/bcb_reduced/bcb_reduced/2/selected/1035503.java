package com.androidcommons.webclient.http;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import com.androidcommons.webclient.WebClientBase;

/**
 * @author Denis Migol
 * 
 */
public class HttpWebClient extends WebClientBase {

    protected final HttpClient httpClient;

    protected static DefaultHttpClient newDefaultHttpClient() {
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
        HttpConnectionParams.setSoTimeout(httpParams, 30000);
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        final DefaultHttpClient ret = new DefaultHttpClient(new ThreadSafeClientConnManager(httpParams, registry), httpParams);
        return ret;
    }

    /**
	 * 
	 * @param endPoint
	 */
    public HttpWebClient(final String endPoint) {
        this(endPoint, newDefaultHttpClient());
    }

    /**
	 * 
	 * @param endPoint
	 * @param httpClient
	 */
    public HttpWebClient(final String endPoint, final HttpClient httpClient) {
        super(endPoint);
        this.httpClient = httpClient;
    }

    /**
	 * 
	 * @return
	 */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public InputStream getInputStream(final String uri) throws IOException {
        return httpClient.execute(new HttpGet(uri)).getEntity().getContent();
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpGet newHttpGet(final String path) {
        return new HttpGet(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpPost newHttpPost(final String path) {
        return new HttpPost(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpPut newHttpPut(final String path) {
        return new HttpPut(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpDelete newHttpDelete(final String path) {
        return new HttpDelete(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpHead newHttpHead(final String path) {
        return new HttpHead(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpOptions newHttpOptions(final String path) {
        return new HttpOptions(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param path
	 * @return
	 */
    protected HttpTrace newHttpTrace(final String path) {
        return new HttpTrace(getFullPathUrl(path));
    }

    /**
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
    protected HttpException newHttpException(final HttpUriRequest request, final HttpResponse response) {
        final StatusLine statusLine = response.getStatusLine();
        final int statusCode = statusLine.getStatusCode();
        final String message = new StringBuilder().append(statusCode).append(' ').append(statusLine.getReasonPhrase()).append(" at ").append(request.getURI()).toString();
        return new HttpException(message);
    }

    /**
	 * 
	 * @param request
	 * @param expectedCode
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
    public HttpResponse executeHttp(final HttpUriRequest request, final int expectedCode) throws ClientProtocolException, IOException, HttpException {
        final HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != expectedCode) {
            throw newHttpException(request, response);
        }
        return response;
    }

    /**
	 * 
	 * @param request
	 * @param beginExpectedCode
	 *            inclusive
	 * @param endExpectedCode
	 *            exclusive
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
    public HttpResponse executeHttp(final HttpUriRequest request, final int beginExpectedCode, final int endExpectedCode) throws ClientProtocolException, IOException, HttpException {
        final HttpResponse response = httpClient.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < beginExpectedCode || statusCode >= endExpectedCode) {
            throw newHttpException(request, response);
        }
        return response;
    }

    /**
	 * <code>executeHttp(request, HttpStatus.SC_OK, HttpStatus.SC_MULTIPLE_CHOICES)</code>
	 * 
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpException
	 */
    public HttpResponse executeHttp(final HttpUriRequest request) throws ClientProtocolException, IOException, HttpException {
        return executeHttp(request, HttpStatus.SC_OK, HttpStatus.SC_MULTIPLE_CHOICES);
    }
}
