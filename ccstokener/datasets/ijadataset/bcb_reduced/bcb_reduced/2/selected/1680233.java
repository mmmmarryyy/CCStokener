package org.bing.adapter.com.caucho.hessian.client;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Internal factory for creating connections to the server.  The default
 * factory is java.net
 */
public class HessianURLConnectionFactory implements HessianConnectionFactory {

    private static final Logger log = Logger.getLogger(HessianURLConnectionFactory.class.getName());

    private HessianProxyFactory _proxyFactory;

    public void setHessianProxyFactory(HessianProxyFactory factory) {
        _proxyFactory = factory;
    }

    /**
   * Opens a new or recycled connection to the HTTP server.
   */
    public HessianConnection open(URL url) throws IOException {
        if (log.isLoggable(Level.FINER)) log.finer(this + " open(" + url + ")");
        URLConnection conn = url.openConnection();
        long connectTimeout = _proxyFactory.getConnectTimeout();
        if (connectTimeout >= 0) conn.setConnectTimeout((int) connectTimeout);
        conn.setDoOutput(true);
        long readTimeout = _proxyFactory.getReadTimeout();
        if (readTimeout > 0) {
            try {
                conn.setReadTimeout((int) readTimeout);
            } catch (Throwable e) {
            }
        }
        return new HessianURLConnection(url, conn);
    }
}
