package net.sf.slimtimer4j.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import net.sf.slimtimer4j.exception.HttpClientException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

final class HttpHelper {

    public enum RequestType {

        GET, POST, PUT, DELETE
    }

    ;

    /**
     * HTTP status 200 OK.
     */
    private static final int HTTP_OK = 200;

    /**
     * HTTP status 500 Internal Server Error.
     */
    private static final int HTTP_ISE = 500;

    private HttpHelper() {
        super();
    }

    public static String execute(final HttpUriRequest request, final Configuration configuration) {
        HttpClient httpClient = new DefaultHttpClient();
        StringBuilder response = new StringBuilder("");
        try {
            httpClient.getParams().setParameter("http.socket.timeout", configuration.getReadTimeout());
            httpClient.getParams().setParameter("http.protocol.content-charset", "UTF-8");
            HttpResponse httpResponse = httpClient.execute(request);
            HttpEntity httpEntity = httpResponse.getEntity();
            StatusLine statusLine = httpResponse.getStatusLine();
            if ((statusLine.getStatusCode() == HTTP_OK || statusLine.getStatusCode() == HTTP_ISE) && httpEntity.getContentType().getValue().contains("application/xml")) {
                InputStreamReader inputStreamReader = new InputStreamReader(httpEntity.getContent());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                bufferedReader.close();
            } else {
                httpEntity.consumeContent();
                throw new AssertionError("Unexpected HTTP status received: " + statusLine.toString());
            }
        } catch (ClientProtocolException e) {
            throw new HttpClientException(e);
        } catch (IOException e) {
            throw new HttpClientException(e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return response.toString();
    }

    private static Header[] getCommonHttpHeaders() {
        List<Header> retList = new ArrayList<Header>();
        retList.add(new BasicHeader("User-Agent", "slimtimer4j"));
        retList.add(new BasicHeader("Accept", "application/xml"));
        retList.add(new BasicHeader("Content-Type", "application/xml"));
        return retList.toArray(new Header[0]);
    }

    public static HttpUriRequest newHttpUriRequest(final RequestType type, final String uri) {
        return newHttpUriRequest(type, uri, "");
    }

    public static StringEntity newStringEntity(final String contents) {
        StringEntity entity = null;
        try {
            entity = new StringEntity(contents, HTTP.UTF_8);
            entity.setContentType("application/xml");
        } catch (UnsupportedEncodingException e1) {
            throw new AssertionError("Unsupported Encoding: " + HTTP.UTF_8);
        }
        return entity;
    }

    public static HttpUriRequest newHttpUriRequest(final RequestType type, final String uri, final String body) {
        StringEntity stringEntity = null;
        if (body != null) {
            stringEntity = newStringEntity(body);
        }
        HttpUriRequest httpUriRequest = null;
        switch(type) {
            case GET:
                httpUriRequest = new HttpGet(uri);
                break;
            case POST:
                HttpPost httpPost = new HttpPost(uri);
                httpPost.setEntity(stringEntity);
                httpUriRequest = httpPost;
                break;
            case PUT:
                HttpPut httpPut = new HttpPut(uri);
                httpPut.setEntity(stringEntity);
                httpUriRequest = httpPut;
                break;
            case DELETE:
                httpUriRequest = new HttpDelete(uri);
                break;
            default:
                throw new AssertionError("No such HTTP method.");
        }
        httpUriRequest.setHeaders(getCommonHttpHeaders());
        return httpUriRequest;
    }
}
