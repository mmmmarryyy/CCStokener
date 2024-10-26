package com.risertech.xdav.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import com.risertech.xdav.http.type.StatusLine;

/**
 * A simple HttpClient based on Apache's HttpCore 4.0
 * 
 * @author phil
 */
public class HttpClient implements IHttpClient {

    private boolean debug = true;

    private DefaultHttpClient client;

    private HttpHost host;

    public HttpClient() {
        this("localhost", 8080);
    }

    public HttpClient(String hostname, int port) {
        this("http", hostname, port);
    }

    public HttpClient(String protocol, String hostname, int port) {
        host = new HttpHost(hostname, port, protocol);
    }

    public HttpClient(String protocol, String hostname, int port, String username, String password) {
        client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(new AuthScope(hostname, port), new UsernamePasswordCredentials(username, password));
        host = new HttpHost(hostname, port, protocol);
    }

    public HttpClient(String hostname, int port, String username, String password) {
        this("http", hostname, port, username, password);
    }

    public IResponse executeMethod(IMethod method) {
        return executeMethod(null, method);
    }

    public IResponse executeMethod(HeaderBlock headerBlock, IMethod method) {
        Method request = new Method(method);
        if (headerBlock != null) {
            for (IHeader header : headerBlock.getHeaders()) {
                request.addHeader(header.getName(), header.getValue());
            }
        }
        if (method instanceof IEntityMethod) {
            try {
                request.setEntity(new StringEntity(((IEntityMethod) method).getEntity()));
                request.getParams().setParameter("http.protocol.expect-continue", false);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        if (debug) {
            System.out.println(request);
        }
        try {
            HttpResponse response = client.execute(host, request);
            return new StreamResponse(response);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class Method extends HttpEntityEnclosingRequestBase {

        private IMethod method;

        public Method(IMethod method) {
            this.method = method;
        }

        @Override
        public String getMethod() {
            return method.getName();
        }

        @Override
        public URI getURI() {
            return method.getURI();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getRequestLine() + "\r\n");
            for (Header header : getAllHeaders()) {
                builder.append(header.getName() + ": " + header.getValue() + "\r\n");
            }
            HttpEntity entity = getEntity();
            if (entity != null) {
                if (entity instanceof StringEntity) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        ((StringEntity) entity).writeTo(outputStream);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    builder.append(new String(outputStream.toByteArray()));
                } else {
                    builder.append(entity.toString());
                }
            }
            return builder.toString();
        }
    }

    private class StreamResponse implements IResponse {

        private HttpResponse response;

        private byte[] entity = null;

        public StreamResponse(HttpResponse response) {
            this.response = response;
        }

        public InputStream getEntity() {
            updateEntity();
            return new ByteArrayInputStream(entity);
        }

        private void updateEntity() {
            try {
                if (entity == null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    HttpEntity httpEntity = response.getEntity();
                    if (httpEntity != null) {
                        InputStream inputStream = httpEntity.getContent();
                        int bite;
                        while ((bite = inputStream.read()) != -1) {
                            outputStream.write(bite);
                        }
                        entity = outputStream.toByteArray();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public HeaderBlock getHeaders() {
            HeaderBlock headerBlock = new HeaderBlock();
            for (Header header : response.getAllHeaders()) {
                headerBlock.addHeader(new ResponseHeader(header));
            }
            return headerBlock;
        }

        public StatusLine getStatusLine() {
            org.apache.http.StatusLine statusLine = response.getStatusLine();
            return new StatusLine(statusLine.getProtocolVersion().getProtocol(), statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getStatusLine() + "\r\n");
            for (IHeader header : getHeaders().getHeaders()) {
                builder.append(header.getName() + ": " + header.getValue() + "\r\n");
            }
            updateEntity();
            if (entity != null) {
                builder.append(new String(entity));
            }
            return builder.toString();
        }
    }

    private class ResponseHeader implements IHeader {

        private Header header;

        public ResponseHeader(Header header) {
            this.header = header;
        }

        public String getName() {
            return header.getName();
        }

        public String getValue() {
            return header.getValue();
        }
    }
}
