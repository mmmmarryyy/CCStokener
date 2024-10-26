package org.apache.http.impl.nio.reactor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.mockup.SimpleHttpRequestHandlerResolver;
import org.apache.http.mockup.TestHttpSSLServer;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

public class TestBaseIOReactorSSL extends TestCase {

    private TestHttpSSLServer server;

    @Override
    protected void setUp() throws Exception {
        HttpParams serverParams = new BasicHttpParams();
        serverParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 10).setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true).setParameter(CoreProtocolPNames.ORIGIN_SERVER, "TEST-SERVER/1.1");
        this.server = new TestHttpSSLServer(serverParams);
    }

    @Override
    protected void tearDown() throws Exception {
        this.server.shutdown();
    }

    public static Test suite() {
        return new TestSuite(TestBaseIOReactorSSL.class);
    }

    private NHttpServiceHandler createHttpServiceHandler(final HttpRequestHandler requestHandler, final HttpExpectationVerifier expectationVerifier, final EventListener eventListener) {
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());
        BufferingHttpServiceHandler serviceHandler = new BufferingHttpServiceHandler(httpproc, new DefaultHttpResponseFactory(), new DefaultConnectionReuseStrategy(), this.server.getParams());
        serviceHandler.setHandlerResolver(new SimpleHttpRequestHandlerResolver(requestHandler));
        serviceHandler.setExpectationVerifier(expectationVerifier);
        serviceHandler.setEventListener(eventListener);
        return serviceHandler;
    }

    public void testBufferedInput() throws Exception {
        final int[] result = new int[1];
        HttpRequestHandler requestHandler = new HttpRequestHandler() {

            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                result[0]++;
                synchronized (result) {
                    result.notify();
                }
            }
        };
        NHttpServiceHandler serviceHandler = createHttpServiceHandler(requestHandler, null, null);
        this.server.start(serviceHandler);
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("test.keystore");
        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, trustmanagers, null);
        ListenerEndpoint endpoint = this.server.getListenerEndpoint();
        endpoint.waitFor();
        InetSocketAddress serverAddress = (InetSocketAddress) endpoint.getAddress();
        Socket socket = sslcontext.getSocketFactory().createSocket("localhost", serverAddress.getPort());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        writer.write("GET / HTTP/1.1\r\n");
        writer.write("Header:                   \r\n");
        writer.write("Header:                   \r\n");
        writer.write("Header:                   \r\n");
        writer.write("\r\n");
        writer.flush();
        synchronized (result) {
            result.wait(500);
        }
        assertEquals(1, result[0]);
    }
}
