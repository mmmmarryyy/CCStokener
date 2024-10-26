package org.eclipse.tptp.test.tools.web.runner;

import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.eclipse.hyades.test.http.runner.HttpRequest;

public class WebSSLHttpExecutor {

    private String strLastHost = null;

    private int iLastPort = 0;

    private SSLSocketFactory ssf = null;

    private SSLSocket sslSocket = null;

    private InputStream from_server = null;

    private OutputStream to_server = null;

    private WebHttpRequestHandler httpRequestHandler = null;

    private int socketBufSize = 0;

    public WebSSLHttpExecutor(WebHttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }

    public WebHttpResponse execute(HttpRequest request, WebHttpResponse response) throws Exception {
        String strHost = request.getHost();
        int port = request.getPort();
        if (port != iLastPort || strLastHost == null || strHost.regionMatches(0, strLastHost, 0, strLastHost.length()) != true || sslSocket == null || sslSocket.isClosed()) {
            if (connectToSecureServer(response, strHost, port) == false) return response;
        }
        if (httpRequestHandler.sendRequest(request, to_server) == false) {
            if (connectToSecureServer(response, strHost, port) == false) {
                response.setCode(-1);
                return response;
            } else {
                if (httpRequestHandler.sendRequest(request, to_server) == false) {
                    response.setCode(-1);
                    return response;
                }
            }
        }
        httpRequestHandler.getServerResponse(request, response, from_server, socketBufSize);
        if (response.getCode() == 0) {
            if (connectToSecureServer(response, strHost, port) == true) {
                if (httpRequestHandler.sendRequest(request, to_server) == true) {
                    httpRequestHandler.getServerResponse(request, response, from_server, socketBufSize);
                }
            }
        }
        if (response.getShouldCloseSocket() == true) strLastHost = null;
        return response;
    }

    private boolean connectToSecureServer(WebHttpResponse response, String strHost, int port) {
        try {
            if (sslSocket != null) sslSocket.close();
            if (ssf == null) {
                TrustManager[] myTM = new TrustManager[] { new PlaybackX509TrustManager() };
                SSLContext ctx = SSLContext.getInstance("SSL");
                ctx.init(null, myTM, null);
                ssf = ctx.getSocketFactory();
            }
            sslSocket = (SSLSocket) ssf.createSocket(strHost, port);
            from_server = sslSocket.getInputStream();
            to_server = sslSocket.getOutputStream();
            socketBufSize = sslSocket.getReceiveBufferSize();
            iLastPort = port;
            strLastHost = strHost;
        } catch (Exception e) {
            response.setCode(-1);
            response.setDetail(e.toString());
            return false;
        }
        return true;
    }
}
