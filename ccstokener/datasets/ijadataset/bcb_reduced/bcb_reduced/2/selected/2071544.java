package org.ibex.net;

import java.io.*;
import java.net.Socket;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.impl.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.ibex.util.*;

public class ApacheHTTP implements HTTP {

    final Logger logger;

    final String url;

    boolean ssl;

    boolean keepAlive = true;

    final String path;

    final HttpHost host;

    final HttpParams params;

    final BasicHttpProcessor httpproc;

    final HttpRequestExecutor httpexecutor;

    final ConnectionReuseStrategy connStrategy;

    final DefaultHttpClientConnection conn;

    static ApacheHTTP stdio = null;

    static {
        try {
            stdio = new ApacheHTTP(DefaultLog.logger, "stdio:");
        } catch (Exception e) {
        }
    }

    public ApacheHTTP(Logger logger, String url) throws IOException {
        this.logger = logger;
        this.url = url;
        if (url.startsWith("https:")) {
            ssl = true;
        } else if (!url.startsWith("http:")) {
            throw new IOException("HTTP only supports http/https urls");
        }
        if (url.indexOf("://") == -1) throw new IOException("URLs must contain a ://");
        String temphost = url.substring(url.indexOf("://") + 3);
        path = temphost.substring(temphost.indexOf('/'));
        temphost = temphost.substring(0, temphost.indexOf('/'));
        int port;
        if (temphost.indexOf(':') != -1) {
            port = Integer.parseInt(temphost.substring(temphost.indexOf(':') + 1));
            temphost = temphost.substring(0, temphost.indexOf(':'));
        } else {
            port = ssl ? 443 : 80;
        }
        host = new HttpHost(temphost, port);
        params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Vexi");
        HttpProtocolParams.setUseExpectContinue(params, true);
        httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        httpproc.addInterceptor(new RequestConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        httpexecutor = new HttpRequestExecutor();
        connStrategy = new DefaultConnectionReuseStrategy();
        conn = new DefaultHttpClientConnection();
    }

    public static void main(String[] args) throws Exception {
    }

    private Socket getSocket(String host, int port, boolean ssl, boolean negotiate) throws IOException {
        Socket ret = ssl ? new SSL(logger, host, port, negotiate) : new Socket(java.net.InetAddress.getByName(host), port);
        ret.setTcpNoDelay(false);
        return ret;
    }

    public HTTPResponse GET() throws IOException {
        BasicHttpRequest request = new BasicHttpRequest("GET", path);
        return makeRequest(request);
    }

    public HTTPResponse POST(String contentType, byte[] content) throws IOException {
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", path);
        ByteArrayEntity entity = new ByteArrayEntity(content);
        entity.setContentType(contentType);
        request.setEntity(entity);
        return makeRequest(request);
    }

    public HTTPResponse makeRequest(BasicHttpRequest request) throws IOException {
        try {
            if (!conn.isOpen()) {
                logger.warn(ApacheHTTP.class, "Creating socket");
                Socket socket = getSocket(host.getHostName(), host.getPort(), ssl, true);
                conn.bind(socket, params);
            }
            HttpContext context = new BasicHttpContext(null);
            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
            context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
            request.setParams(params);
            httpexecutor.preProcess(request, httpproc, context);
            HttpResponse response = httpexecutor.execute(request, conn, context);
            httpexecutor.postProcess(response, httpproc, context);
            if (!connStrategy.keepAlive(response, context)) keepAlive = false;
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity resp = response.getEntity();
            if (statusCode >= 400) {
                HTTPEntityInfo info = new HTTPEntityInfo((int) resp.getContentLength(), "", resp.getContentType().getValue());
                byte[] bytes = IOUtil.toByteArray(resp.getContent());
                throw new HTTPErrorResponse(response.getStatusLine().getReasonPhrase(), statusCode + "", bytes, info);
            } else {
                Header lastmodHeader = response.getLastHeader("last-modified");
                String lastmod = lastmodHeader == null ? "" : lastmodHeader.getValue();
                Header contentType = resp.getContentType();
                HTTPEntityInfo info = new HTTPEntityInfo((int) resp.getContentLength(), lastmod, contentType == null ? null : contentType.getValue());
                return new HTTPResponse(info, resp.getContent());
            }
        } catch (HttpException he) {
            throw new IOException(he);
        }
    }

    public void close() {
        try {
            conn.close();
        } catch (IOException e) {
        }
    }
}
