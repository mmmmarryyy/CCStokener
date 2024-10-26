package com.google.code.jahath.http.impl;

import java.io.InputStream;
import com.google.code.jahath.common.http.HttpException;
import com.google.code.jahath.common.http.HttpOutMessageImpl;
import com.google.code.jahath.common.http.HttpOutputStream;
import com.google.code.jahath.http.HttpRequest;
import com.google.code.jahath.http.HttpResponse;

public class HttpRequestImpl extends HttpOutMessageImpl implements HttpRequest {

    private final InputStream response;

    HttpRequestImpl(HttpOutputStream request, InputStream response) {
        super(request, null);
        this.response = response;
    }

    public HttpResponse getResponse() throws HttpException {
        return new HttpResponseImpl(response);
    }

    public HttpResponse execute() throws HttpException {
        commit();
        return getResponse();
    }
}
