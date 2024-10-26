package net.viens.numenor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Proxy {

    public static final String HTTP_USE_PROXY = "http.useProxy";

    public static final String HTTP_PROXY_HOST = "http.proxyHost";

    public static final String HTTP_PROXY_PORT = "http.proxyPort";

    public static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    public static final boolean DEFAULT_HTTP_USE_PROXY = false;

    public static final String DEFAULT_HTTP_PROXY_HOST = "";

    public static final int DEFAULT_HTTP_PROXY_PORT = 80;

    public static final String DEFAULT_HTTP_NON_PROXY_HOSTS = "";

    private final Logger log = LoggerFactory.getLogger(Proxy.class);

    private boolean useProxy;

    private String proxyHost;

    private int proxyPort;

    private String nonProxyHosts;

    public static Proxy getInstance() {
        Config config = Config.getInstance();
        Proxy proxy = new Proxy();
        proxy.setUseProxy(config.getBooleanProperty(HTTP_USE_PROXY, DEFAULT_HTTP_USE_PROXY));
        proxy.setProxyHost(config.getProperty(HTTP_PROXY_HOST, DEFAULT_HTTP_PROXY_HOST));
        proxy.setProxyPort(config.getIntProperty(HTTP_PROXY_PORT, DEFAULT_HTTP_PROXY_PORT));
        proxy.setNonProxyHosts(config.getProperty(HTTP_NON_PROXY_HOSTS, DEFAULT_HTTP_NON_PROXY_HOSTS));
        return proxy;
    }

    private Proxy() {
        super();
    }

    public boolean useProxy() {
        return useProxy;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    private void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    private void setProxyHost(String host) {
        this.proxyHost = host;
    }

    private void setProxyPort(int port) {
        this.proxyPort = port;
    }

    private void setNonProxyHosts(String hosts) {
        this.nonProxyHosts = hosts;
    }

    public InputStream getInputStream(URL url) throws IOException {
        InputStream stream = null;
        if (this.useProxy) {
            SocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, addr);
            stream = url.openConnection(proxy).getInputStream();
        } else {
            stream = url.openConnection(java.net.Proxy.NO_PROXY).getInputStream();
        }
        return stream;
    }

    /**
	 * cacheResource is used to cache local copies of remote resources.
	 * 
	 * @param url
	 * @param fileName
	 * @throws IOException
	 * 
	 *	Example Usage:
	 *
	 *  	URL url = new URL("http://host:port/library/books/treasureisland.txt");
	 *  	String fileName = "temp/library/books/treasureisland.txt";
	 *  	proxy.cacheResource(url,fileName);
	 */
    public void cacheResource(URL url, String fileName) throws IOException {
        OutputStream os = new FileOutputStream(fileName);
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
        InputStream is = getInputStream(url);
        int n = 0;
        while ((n = is.read()) != -1) {
            osw.write((char) n);
        }
        osw.flush();
        os.close();
    }

    public String getHostName() {
        String hostname = null;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            log.error("", ex);
        }
        return hostname;
    }

    public String getIpAddress() {
        StringBuffer buffer = new StringBuffer(15);
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] ipAddr = addr.getAddress();
            for (int i = 0; i < ipAddr.length; i++) {
                if (i > 0) {
                    buffer.append(".");
                }
                buffer.append(ipAddr[i] & 0xFF);
            }
        } catch (UnknownHostException ex) {
            log.error("", ex);
        }
        return buffer.toString();
    }
}
