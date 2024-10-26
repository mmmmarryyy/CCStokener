package net.pesahov.remote.socket.proxy.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import net.pesahov.remote.socket.RemoteSocket;
import net.pesahov.remote.socket.UnderlyingSocketFactory;
import net.pesahov.remote.socket.proxy.Connection;

/**
 * @since 29.08.2008
 * @version $Revision: 0 $
 * @author Pesahov Dmitry
 */
public class HttpProxyConnection extends Connection implements HttpRequestHandler {

    /**
     * {@link HttpService} instance.
     */
    private final HttpService service;

    /**
	 * Create {@link HttpProxyConnection} instance by given parameters.
	 * @param factory Parent {@link HttpProxyConnection} instance.
	 * @param clientSocket Client {@link Socket} instance.
	 * @param clientId Client id.
	 * @param serverId Server id.
	 * @param timeout Connection timeout.
	 * @param factory {@link UnderlyingSocketFactory} instance.
	 */
    public HttpProxyConnection(HttpProxyConnectionFactory factory, Socket clientSocket, long clientId, long serverId, long timeout) {
        super(factory, clientSocket, null, clientId, serverId, timeout);
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseConnControl());
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
        registry.register("*", this);
        this.service = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
        this.service.setParams(factory.params);
        this.service.setHandlerResolver(registry);
    }

    @Override
    protected boolean isConnected() throws IOException {
        return localSocket.isConnected();
    }

    @Override
    protected void service() throws IOException {
        DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
        conn.bind(localSocket, this.service.getParams());
        HttpContext context = new BasicHttpContext();
        try {
            this.service.handleRequest(conn, context);
        } catch (HttpException ex) {
            logger.severe(ex.getMessage());
        } finally {
            localSocket.close();
            if (remoteSocket != null) remoteSocket.close();
        }
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        logger.info("Request: " + request.getRequestLine());
        Header hostHeader = request.getFirstHeader("Host");
        if (hostHeader == null || hostHeader.getValue() == null) {
            response.setStatusCode(400);
            return;
        }
        String[] tokens = hostHeader.getValue().split(":", 2);
        String host = tokens[0];
        int port = 80;
        try {
            port = Integer.parseInt(tokens[1]);
        } catch (Exception ex) {
        }
        if (remoteSocket != null) releaseSocket(remoteSocket);
        if ((remoteSocket = allocateSocket(host, port)) == null) {
            remoteSocket = new RemoteSocket(((HttpProxyConnectionFactory) factory).factory, host, port);
        }
        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        conn.bind(remoteSocket, this.service.getParams());
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        HttpResponse remoteResponse = httpexecutor.execute(request, conn, context);
        StatusLine statusLine = remoteResponse.getStatusLine();
        response.setStatusLine(remoteResponse.getProtocolVersion(), statusLine.getStatusCode(), statusLine.getReasonPhrase());
        response.setHeaders(remoteResponse.getAllHeaders());
        response.setEntity(remoteResponse.getEntity());
        logger.info("Response: " + response.getStatusLine());
    }

    /**
	 * Map of pooled sockets.
	 */
    private static Map<SocketAddress, Socket> sockets = new HashMap<SocketAddress, Socket>();

    /**
     * Allocates if possible a {@link Socket} for given host and port values.
     * @param host Host value.
     * @param port Port value.
     * @return {@link Socket} instance of <code>null</code> if unavailable.  
     */
    private static Socket allocateSocket(String host, int port) {
        SocketAddress address = new InetSocketAddress(host, port);
        synchronized (sockets) {
            if (sockets.containsKey(address)) {
                Socket socket = sockets.remove(address);
                return isReuseable(socket) ? socket : null;
            }
        }
        return null;
    }

    /**
     * Release an allocated or (new) {@link Socket} instance.
     * @param socket The {@link Socket} instance to release (add to pool).
     */
    private static void releaseSocket(Socket socket) {
        if (isReuseable(socket)) {
            SocketAddress address = socket.getRemoteSocketAddress();
            synchronized (sockets) {
                if (sockets.containsKey(address)) return;
                sockets.put(address, socket);
            }
        }
    }

    /**
     * Checks is the given {@link Socket} instance is reusable.
     * @param socket {@link Socket} instance to check.
     * @return <code>true</code> if and only if the given {@link Socket} instance is connected and closed.
     */
    private static boolean isReuseable(Socket socket) {
        return socket.isConnected() && !socket.isClosed();
    }
}
