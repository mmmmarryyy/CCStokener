package org.pubcurator.core.managers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Kai Schlamp (schlamp@gmx.de)
 *
 */
public class ProxyManager {

    private static final String HTTP_PROXY_SET = "http.proxySet";

    private static final String HTTP_PROXY_HOST = "http.proxyHost";

    private static final String HTTP_PROXY_PORT = "http.proxyPort";

    private static final String HTTP_PROXY_USER = "http.proxyUser";

    private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";

    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    private static final String HTTPS_PROXY_SET = "https.proxySet";

    private static final String HTTPS_PROXY_HOST = "https.proxyHost";

    private static final String HTTPS_PROXY_PORT = "https.proxyPort";

    private static final String HTTPS_PROXY_USER = "https.proxyUser";

    private static final String HTTPS_PROXY_PASSWORD = "https.proxyPassword";

    private static final String HTTPS_NON_PROXY_HOSTS = "https.nonProxyHosts";

    private static final String SOCKS_PROXY_SET = "socks.proxySet";

    private static final String SOCKS_PROXY_HOST = "socks.proxyHost";

    private static final String SOCKS_PROXY_PORT = "socks.proxyPort";

    private static final String SOCKS_PROXY_USER = "java.net.socks.username";

    private static final String SOCKS_PROXY_PASSWORD = "java.net.socks.password";

    private static final String SOCKS_NON_PROXY_HOSTS = "socks.nonProxyHosts";

    public static ProxyManager INSTANCE = new ProxyManager();

    private final ServiceTracker proxyTracker;

    private IProxyService proxyService;

    private ProxyManager() {
        proxyTracker = new ServiceTracker(FrameworkUtil.getBundle(this.getClass()).getBundleContext(), IProxyService.class.getName(), null);
        proxyTracker.open();
        proxyService = getProxyService();
        proxyService.addProxyChangeListener(new IProxyChangeListener() {

            @Override
            public void proxyInfoChanged(IProxyChangeEvent event) {
                updateSystemProxySettings();
            }
        });
    }

    public void updateSystemProxySettings() {
        boolean httpProxySet = false;
        boolean httpsProxySet = false;
        boolean socksProxySet = false;
        for (IProxyData data : getProxyService().getProxyData()) {
            if (data.getType().equals(IProxyData.HTTP_PROXY_TYPE) && data.getHost() != null) {
                System.setProperty(HTTP_PROXY_SET, "true");
                System.setProperty(HTTP_PROXY_HOST, data.getHost());
                System.setProperty(HTTP_PROXY_PORT, String.valueOf(data.getPort()));
                System.setProperty(HTTP_PROXY_USER, data.getUserId());
                System.setProperty(HTTP_PROXY_PASSWORD, data.getPassword());
                httpProxySet = true;
            }
            if (data.getType().equals(IProxyData.HTTPS_PROXY_TYPE) && data.getHost() != null) {
                System.setProperty(HTTPS_PROXY_SET, "true");
                System.setProperty(HTTPS_PROXY_HOST, data.getHost());
                System.setProperty(HTTPS_PROXY_PORT, String.valueOf(data.getPort()));
                System.setProperty(HTTPS_PROXY_USER, data.getUserId());
                System.setProperty(HTTPS_PROXY_PASSWORD, data.getPassword());
                httpsProxySet = true;
            }
            if (data.getType().equals(IProxyData.SOCKS_PROXY_TYPE) && data.getHost() != null) {
                System.setProperty(SOCKS_PROXY_SET, "true");
                System.setProperty(SOCKS_PROXY_HOST, data.getHost());
                System.setProperty(SOCKS_PROXY_PORT, String.valueOf(data.getPort()));
                System.setProperty(SOCKS_PROXY_USER, data.getUserId());
                System.setProperty(SOCKS_PROXY_PASSWORD, data.getPassword());
                socksProxySet = true;
            }
        }
        if (!httpProxySet) {
            System.setProperty(HTTP_PROXY_SET, "false");
        }
        if (!httpsProxySet) {
            System.setProperty(HTTPS_PROXY_SET, "false");
        }
        if (!socksProxySet) {
            System.setProperty(SOCKS_PROXY_SET, "false");
        }
        String[] nonProxiedHosts = getProxyService().getNonProxiedHosts();
        StringBuffer buffer = new StringBuffer();
        for (String host : nonProxiedHosts) {
            if (buffer.length() > 0) {
                buffer.append("|");
            }
            buffer.append(host);
        }
        if (buffer.length() > 0) {
            System.setProperty(HTTP_NON_PROXY_HOSTS, buffer.toString());
            System.setProperty(HTTPS_NON_PROXY_HOSTS, buffer.toString());
            System.setProperty(SOCKS_NON_PROXY_HOSTS, buffer.toString());
        } else {
            System.setProperty(HTTP_NON_PROXY_HOSTS, null);
            System.setProperty(HTTPS_NON_PROXY_HOSTS, null);
            System.setProperty(SOCKS_NON_PROXY_HOSTS, null);
        }
        Authenticator.setDefault(new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                for (IProxyData data : getProxyService().getProxyData()) {
                    if (data.getHost().equals(getRequestingHost()) && data.getPort() == getRequestingPort()) {
                        return new PasswordAuthentication(data.getUserId(), data.getPassword().toCharArray());
                    }
                }
                return null;
            }
        });
    }

    public IProxyService getProxyService() {
        if (proxyService == null) {
            proxyService = (IProxyService) proxyTracker.getService();
        }
        return proxyService;
    }

    public InputStream openWithProxy(URI uri) throws IOException {
        IProxyService proxyService = getProxyService();
        IProxyData[] proxyDataForHost = proxyService.select(uri);
        for (IProxyData data : proxyDataForHost) {
            if (data.getHost() != null) {
                System.setProperty("http.proxySet", "true");
                System.setProperty("http.proxyHost", data.getHost());
            }
            if (data.getHost() != null) {
                System.setProperty("http.proxyPort", String.valueOf(data.getPort()));
            }
        }
        URL url;
        url = uri.toURL();
        return new BufferedInputStream(url.openStream());
    }

    public InputStream openWithProxy(URL url) throws IOException {
        try {
            IProxyService proxyService = getProxyService();
            IProxyData[] proxyDataForHost = proxyService.select(url.toURI());
            for (IProxyData data : proxyDataForHost) {
                if (data.getHost() != null) {
                    System.setProperty("http.proxySet", "true");
                    System.setProperty("http.proxyHost", data.getHost());
                }
                if (data.getHost() != null) {
                    System.setProperty("http.proxyPort", String.valueOf(data.getPort()));
                }
            }
            return new BufferedInputStream(url.openStream());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
