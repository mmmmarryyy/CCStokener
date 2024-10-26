package f06.osgi.framework;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;
import sun.net.util.IPAddressUtil;

class BundleURLStreamHandlerService extends URLStreamHandler implements URLStreamHandlerService {

    public static final String BUNDLE_PROTOCOL = "bundle";

    private Framework framework;

    public BundleURLStreamHandlerService(BundleContext context) {
        this.framework = (Framework) context.getBundle();
    }

    public int getDefaultPort() {
        return 0;
    }

    public URLConnection openConnection(URL url) throws IOException {
        BundleURLConnection conn = new BundleURLConnection(framework, url);
        return conn;
    }

    public void parseURL(URLStreamHandlerSetter handler, URL u, String spec, int start, int limit) {
        String protocol = u.getProtocol();
        String authority = u.getAuthority();
        String userInfo = u.getUserInfo();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        String query = u.getQuery();
        String ref = u.getRef();
        boolean isRelPath = false;
        boolean queryOnly = false;
        if (start < limit) {
            int queryStart = spec.indexOf('?');
            queryOnly = queryStart == start;
            if ((queryStart != -1) && (queryStart < limit)) {
                query = spec.substring(queryStart + 1, limit);
                if (limit > queryStart) limit = queryStart;
                spec = spec.substring(0, queryStart);
            }
        }
        int i = 0;
        boolean isUNCName = (start <= limit - 4) && (spec.charAt(start) == '/') && (spec.charAt(start + 1) == '/') && (spec.charAt(start + 2) == '/') && (spec.charAt(start + 3) == '/');
        if (!isUNCName && (start <= limit - 2) && (spec.charAt(start) == '/') && (spec.charAt(start + 1) == '/')) {
            start += 2;
            i = spec.indexOf('/', start);
            if (i < 0) {
                i = spec.indexOf('?', start);
                if (i < 0) i = limit;
            }
            host = authority = spec.substring(start, i);
            int ind = authority.indexOf('@');
            if (ind != -1) {
                userInfo = authority.substring(0, ind);
                host = authority.substring(ind + 1);
            } else {
                userInfo = null;
            }
            if (host != null) {
                if (host.length() > 0 && (host.charAt(0) == '[')) {
                    if ((ind = host.indexOf(']')) > 2) {
                        String nhost = host;
                        host = nhost.substring(0, ind + 1);
                        if (!IPAddressUtil.isIPv6LiteralAddress(host.substring(1, ind))) {
                            throw new IllegalArgumentException("Invalid host: " + host);
                        }
                        port = -1;
                        if (nhost.length() > ind + 1) {
                            if (nhost.charAt(ind + 1) == ':') {
                                ++ind;
                                if (nhost.length() > (ind + 1)) {
                                    port = Integer.parseInt(nhost.substring(ind + 1));
                                }
                            } else {
                                throw new IllegalArgumentException("Invalid authority field: " + authority);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid authority field: " + authority);
                    }
                } else {
                    ind = host.indexOf(':');
                    port = -1;
                    if (ind >= 0) {
                        if (host.length() > (ind + 1)) {
                            port = Integer.parseInt(host.substring(ind + 1));
                        }
                        host = host.substring(0, ind);
                    }
                }
            } else {
                host = "";
            }
            if (port < -1) throw new IllegalArgumentException("Invalid port number :" + port);
            start = i;
            if (authority != null && authority.length() > 0) path = "";
        }
        if (host == null) {
            host = "";
        }
        if (start < limit) {
            if (spec.charAt(start) == '/') {
                path = spec.substring(start, limit);
            } else if (path != null && path.length() > 0) {
                isRelPath = true;
                int ind = path.lastIndexOf('/');
                String seperator = "";
                if (ind == -1 && authority != null) seperator = "/";
                path = path.substring(0, ind + 1) + seperator + spec.substring(start, limit);
            } else {
                String seperator = (authority != null) ? "/" : "";
                path = seperator + spec.substring(start, limit);
            }
        } else if (queryOnly && path != null) {
            int ind = path.lastIndexOf('/');
            if (ind < 0) ind = 0;
            path = path.substring(0, ind) + "/";
        }
        if (path == null) path = "";
        if (isRelPath) {
            while ((i = path.indexOf("/./")) >= 0) {
                path = path.substring(0, i) + path.substring(i + 2);
            }
            i = 0;
            while ((i = path.indexOf("/../", i)) >= 0) {
                if (i > 0 && (limit = path.lastIndexOf('/', i - 1)) >= 0 && (path.indexOf("/../", limit) != 0)) {
                    path = path.substring(0, limit) + path.substring(i + 3);
                    i = 0;
                } else {
                    i = i + 3;
                }
            }
            while (path.endsWith("/..")) {
                i = path.indexOf("/..");
                if ((limit = path.lastIndexOf('/', i - 1)) >= 0) {
                    path = path.substring(0, limit + 1);
                } else {
                    break;
                }
            }
            if (path.startsWith("./") && path.length() > 2) path = path.substring(2);
            if (path.endsWith("/.")) path = path.substring(0, path.length() - 1);
        }
        handler.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }

    public String toExternalForm(URL u) {
        return super.toExternalForm(u);
    }

    public int hashCode(URL u) {
        return super.hashCode(u);
    }

    public boolean sameFile(URL u1, URL u2) {
        return super.sameFile(u1, u2);
    }

    public boolean equals(URL u1, URL u2) {
        return super.equals(u1, u2);
    }

    public synchronized InetAddress getHostAddress(URL u) {
        return super.getHostAddress(u);
    }

    public boolean hostsEqual(URL u1, URL u2) {
        return super.hostsEqual(u1, u2);
    }

    public static void main(String[] args) throws IOException {
        new URL("http://test:8090/path");
    }
}
