package org.jsemantic.services.httpservice.client;

import java.io.IOException;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.jsemantic.jservice.core.component.skeletal.AbstractComponent;
import org.jsemantic.services.httpservice.exception.HttpTestClientException;

public class HttpTestClientImpl extends AbstractComponent implements HttpTestClient {

    private HttpClient httpclient = null;

    public HttpTestClientImpl(HttpParams params) {
        init(params);
    }

    public HttpTestClientImpl() {
        init();
    }

    @Override
    public void postConstruct() {
        httpclient = new DefaultHttpClient();
    }

    @Override
    public void release() {
        httpclient = null;
    }

    public HttpResponse executeHttpGetRequest(String uri) throws HttpTestClientException {
        HttpGet httpGet = new HttpGet(uri);
        return executeHttp(httpGet);
    }

    public HttpResponse executeHttpGetRequest(String uri, Map<String, Object> parameters) throws HttpTestClientException {
        HttpGet httpGet = new HttpGet(uri);
        if (parameters != null) {
            httpGet.setParams(createBasicParameters(parameters));
        }
        return executeHttp(httpGet);
    }

    public HttpResponse executeHttpPostRequest(String uri) throws HttpTestClientException {
        HttpPost httpPost = new HttpPost(uri);
        return executePostHttp(httpPost);
    }

    public HttpResponse executeHttpPostRequest(String uri, Map<String, Object> parameters) throws HttpTestClientException {
        HttpPost httpPost = new HttpPost(uri);
        if (parameters != null) {
            httpPost.setParams(createBasicParameters(parameters));
        }
        return executePostHttp(httpPost);
    }

    public void consumeContent(HttpResponse response) {
        if (response != null && response.getEntity() != null) {
            try {
                response.getEntity().consumeContent();
                response = null;
            } catch (IOException e) {
                throw new HttpTestClientException(e);
            }
        }
    }

    private void init(HttpParams params) {
        if (params != null) {
            httpclient = new DefaultHttpClient(params);
        } else {
            httpclient = new DefaultHttpClient();
        }
    }

    private HttpResponse executeHttp(HttpGet httpGet) {
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpGet);
            return response;
        } catch (Throwable e) {
            throw new HttpTestClientException(e);
        }
    }

    private HttpResponse executePostHttp(HttpPost httpPost) {
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpPost);
            return response;
        } catch (Throwable e) {
            throw new HttpTestClientException(e);
        }
    }

    private HttpParams createBasicParameters(Map<String, Object> parameters) {
        HttpParams httpParams = new BasicHttpParams();
        for (String param : parameters.keySet()) {
            httpParams.setParameter(param, parameters.get(param));
        }
        return httpParams;
    }
}
