package com.google.code.jahath.endpoint.vch;

import java.io.IOException;
import java.io.InputStream;
import com.google.code.jahath.common.http.HttpException;
import com.google.code.jahath.http.HttpRequest;
import com.google.code.jahath.http.HttpResponse;

class VCHInputStream extends InputStream {

    private final HttpClient httpClient;

    private final String connectionId;

    private InputStream in;

    VCHInputStream(HttpClient httpClient, String connectionId) {
        this.httpClient = httpClient;
        this.connectionId = connectionId;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        while (true) {
            if (in == null) {
                try {
                    HttpRequest request = httpClient.createRequest(HttpRequest.Method.POST, "/connections/" + connectionId);
                    HttpResponse response = request.execute();
                    in = response.getInputStream();
                } catch (HttpException ex) {
                    throw new VCHConnectionException(ex);
                }
            }
            int c = in.read(b, off, len);
            if (c == -1) {
                in = null;
            } else {
                return c;
            }
        }
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int c = read(b);
        return c == -1 ? -1 : b[0] & 0xFF;
    }
}
