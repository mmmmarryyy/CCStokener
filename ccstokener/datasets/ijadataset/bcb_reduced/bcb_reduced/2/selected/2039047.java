package net.anydigit.jiliu.protocols.http;

import java.io.IOException;
import java.util.Map.Entry;
import net.anydigit.jiliu.ProxyRequest;
import net.anydigit.jiliu.RequestExecutor;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DynamicChannelBuffer;

/**
 * @author xingfei [xingfei0831 AT gmail.com]
 * 
 */
public class HttpRequestExecutor implements RequestExecutor {

    public Object execute(String backendAddress, int backendPort, ProxyRequest request, String remoteAddr) throws org.apache.http.client.ClientProtocolException, IOException {
        org.apache.http.impl.client.DefaultHttpClient httpclient = new org.apache.http.impl.client.DefaultHttpClient();
        org.apache.http.HttpHost target = new org.apache.http.HttpHost(backendAddress, backendPort, "http");
        HttpProxyRequest proxyRequest = (HttpProxyRequest) request;
        org.apache.http.client.methods.HttpGet httpget = new org.apache.http.client.methods.HttpGet(proxyRequest.getQuery());
        boolean forwardedfor = false;
        for (Entry<String, String> en : proxyRequest.getHeaders()) {
            String key = en.getKey();
            if (key.equals("Connection")) {
                httpget.addHeader(key, "Close");
            } else if (key.equals("X-Forwarded-For")) {
                forwardedfor = true;
                httpget.addHeader(key, en.getValue());
            } else {
                httpget.addHeader(key, en.getValue());
            }
        }
        if (!forwardedfor) {
            httpget.addHeader("X-Forwarded-For", remoteAddr);
        }
        org.apache.http.HttpResponse response = httpclient.execute(target, httpget);
        return convert(response);
    }

    private org.jboss.netty.handler.codec.http.HttpVersion getHttpVersion(org.apache.http.StatusLine status) {
        String protocol = status.getProtocolVersion().toString();
        org.jboss.netty.handler.codec.http.HttpVersion version = null;
        if (protocol.equals("HTTP/1.0")) {
            version = org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_0;
        } else if (protocol.equals("HTTP/1.1")) {
            version = org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
        } else {
            throw new IllegalArgumentException("unknown http version:" + protocol);
        }
        return version;
    }

    private Object convert(org.apache.http.HttpResponse res) throws IllegalStateException, IOException {
        org.apache.http.StatusLine status = res.getStatusLine();
        org.jboss.netty.handler.codec.http.HttpResponse response = new org.jboss.netty.handler.codec.http.DefaultHttpResponse(getHttpVersion(status), new org.jboss.netty.handler.codec.http.HttpResponseStatus(status.getStatusCode(), status.getReasonPhrase()));
        for (org.apache.http.Header h : res.getAllHeaders()) {
            response.addHeader(h.getName(), h.getValue());
        }
        response.addHeader("Via", "localhost");
        response.addHeader("X-Powered-By", "jiliu/1.0 load balance");
        int contentLength = (int) res.getEntity().getContentLength();
        ChannelBuffer buffer = new DynamicChannelBuffer(contentLength);
        buffer.writeBytes(res.getEntity().getContent(), contentLength);
        response.setContent(buffer);
        return response;
    }
}
