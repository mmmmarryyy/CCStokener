package org.evertree.breakfast.component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import org.evertree.breakfast.Parameter;
import org.evertree.breakfast.Provider;

public class HTTPSource extends URLSource implements Provider {

    private Parameter proxyHost = new Parameter("proxyHost", String.class);

    private Parameter proxyPort = new Parameter("proxyPort", Integer.class);

    private Proxy proxy;

    public void setProxyHost(Object proxyHost) {
        this.proxyHost.setValue(proxyHost);
    }

    public void setProxyPort(Object proxyPort) {
        this.proxyPort.setValue(proxyPort);
    }

    @Override
    protected URLConnection openConnection() throws Exception {
        if (!proxyHost.isNull()) {
            return ((URL) url.getValue()).openConnection(getProxy());
        }
        return super.openConnection();
    }

    @Override
    protected void handleConnection(URLConnection conn) {
        conn.addRequestProperty("User-Agent", "Mozilla/4.76");
    }

    private Proxy getProxy() {
        if (proxy == null) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) proxyHost.getValue(), (Integer) proxyPort.getValue()));
        }
        return proxy;
    }
}
