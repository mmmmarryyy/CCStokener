package de.psisystems.dmachinery.io.protocols.localfile;

import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.net.Proxy;
import java.io.IOException;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: stefanpudig
 * Date: Jul 31, 2009
 * Time: 2:01:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class Handler extends URLStreamHandler {

    public synchronized URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }

    public synchronized URLConnection openConnection(URL url, Proxy p) throws IOException {
        String path;
        String file = url.getFile();
        String host = url.getHost();
        path = file;
        if ((host == null) || host.equals("") || host.equalsIgnoreCase("localhost") || host.equals("~")) {
            return createFileURLConnection(url, new File(path));
        }
        throw new IOException("localfile only supports localhost");
    }

    protected URLConnection createFileURLConnection(URL url, File file) {
        return new FileURLConnection(url, file);
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
