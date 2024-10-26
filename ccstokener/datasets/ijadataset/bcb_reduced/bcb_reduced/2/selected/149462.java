package com.google.code.jahath.endpoint.vch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.google.code.jahath.AbstractConnection;
import com.google.code.jahath.common.http.HttpConstants;
import com.google.code.jahath.common.http.HttpException;
import com.google.code.jahath.http.HttpRequest;
import com.google.code.jahath.http.HttpResponse;

class ConnectionImpl extends AbstractConnection {

    private final HttpClient httpClient;

    private final String connectionId;

    private final OutputStream out;

    private final InputStream in;

    public ConnectionImpl(HttpClient httpClient, String connectionId) {
        this.httpClient = httpClient;
        this.connectionId = connectionId;
        out = new VCHOutputStream(httpClient, connectionId);
        in = new VCHInputStream(httpClient, connectionId);
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public InputStream getInputStream() {
        return in;
    }

    @Override
    protected void doClose() throws IOException {
        try {
            HttpRequest request = httpClient.createRequest(HttpRequest.Method.POST, "/connections/" + connectionId);
            HttpResponse response = request.execute();
            if (response.getStatusCode() == HttpConstants.StatusCodes.NO_CONTENT) {
                return;
            } else {
                throw Util.createException(response);
            }
        } catch (HttpException ex) {
            throw new VCHConnectionException(ex);
        }
    }
}
