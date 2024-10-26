package de.psisystems.dmachinery.io.protocols.bytecache;

import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.net.Proxy;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: stefanpudig
 * Date: Jul 31, 2009
 * Time: 1:59:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Handler extends URLStreamHandler {

    public synchronized URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }

    public synchronized URLConnection openConnection(URL url, Proxy p) throws IOException {
        return new URL2ByteCacheConnection(url);
    }

    protected boolean hostsEqual(URL u1, URL u2) {
        String s1 = u1.getHost();
        String s2 = u2.getHost();
        if ("localhost".equalsIgnoreCase(s1) && (s2 == null || "".equals(s2))) return true;
        if ("localhost".equalsIgnoreCase(s2) && (s1 == null || "".equals(s1))) return true;
        return super.hostsEqual(u1, u2);
    }

    private String getHost(URL url) {
        String host = url.getHost();
        if (host == null) host = "";
        return host;
    }
}
