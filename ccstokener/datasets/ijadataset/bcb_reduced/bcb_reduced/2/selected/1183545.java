package org.apache.http.mockup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.impl.nio.SSLClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.ExceptionEvent;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.params.HttpParams;

public class TestHttpSSLClient {

    private final SSLContext sslcontext;

    private final DefaultConnectingIOReactor ioReactor;

    private final HttpParams params;

    private volatile IOReactorThread thread;

    private volatile RequestCount requestCount;

    public TestHttpSSLClient(final HttpParams params) throws Exception {
        super();
        this.params = params;
        this.ioReactor = new DefaultConnectingIOReactor(2, this.params);
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("test.keystore");
        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        this.sslcontext = SSLContext.getInstance("TLS");
        this.sslcontext.init(null, trustmanagers, null);
    }

    public HttpParams getParams() {
        return this.params;
    }

    public IOReactorStatus getStatus() {
        return this.ioReactor.getStatus();
    }

    public List<ExceptionEvent> getAuditLog() {
        return this.ioReactor.getAuditLog();
    }

    public void setRequestCount(final RequestCount requestCount) {
        this.requestCount = requestCount;
    }

    private void execute(final NHttpClientHandler clientHandler) throws IOException {
        IOEventDispatch ioEventDispatch = new SSLClientIOEventDispatch(clientHandler, this.sslcontext, this.params);
        this.ioReactor.execute(ioEventDispatch);
    }

    public void openConnection(final InetSocketAddress address, final Object attachment) {
        this.ioReactor.connect(address, null, attachment, null);
    }

    public void start(final NHttpClientHandler clientHandler) {
        this.thread = new IOReactorThread(clientHandler);
        this.thread.start();
    }

    public Exception getException() {
        if (this.thread != null) {
            return this.thread.getException();
        } else {
            return null;
        }
    }

    public void shutdown() throws IOException {
        this.ioReactor.shutdown();
        try {
            if (this.thread != null) {
                this.thread.join(500);
            }
        } catch (InterruptedException ignore) {
        }
    }

    private class IOReactorThread extends Thread {

        private final NHttpClientHandler clientHandler;

        private volatile Exception ex;

        public IOReactorThread(final NHttpClientHandler clientHandler) {
            super();
            this.clientHandler = clientHandler;
        }

        @Override
        public void run() {
            try {
                execute(this.clientHandler);
            } catch (Exception ex) {
                this.ex = ex;
                if (requestCount != null) {
                    requestCount.failure(ex);
                }
            }
        }

        public Exception getException() {
            return this.ex;
        }
    }
}
