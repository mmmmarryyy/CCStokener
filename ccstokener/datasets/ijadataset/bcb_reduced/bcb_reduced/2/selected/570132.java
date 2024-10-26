package org.eclipse.osgi.framework.internal.core;

import java.io.IOException;
import java.net.*;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

/**
 * URLStreamHandler the bundleentry and bundleresource protocols.
 */
public abstract class BundleResourceHandler extends URLStreamHandler {

    public static final String SECURITY_AUTHORIZED = "SECURITY_AUTHORIZED";

    protected static BundleContext context;

    protected BundleEntry bundleEntry;

    /**
	 * Constructor for a bundle protocol resource URLStreamHandler.
	 */
    public BundleResourceHandler() {
        this(null);
    }

    public BundleResourceHandler(BundleEntry bundleEntry) {
        this.bundleEntry = bundleEntry;
    }

    /** 
	 * Parse reference URL. 
	 */
    protected void parseURL(URL url, String str, int start, int end) {
        if (end < start) return;
        if (url.getPath() != null) bundleEntry = null;
        String spec = "";
        if (start < end) spec = str.substring(start, end);
        end -= start;
        String path = url.getPath();
        String bundleId = url.getHost();
        int resIndex = 0;
        int pathIdx = 0;
        if (spec.startsWith("//")) {
            int bundleIdIdx = 2;
            pathIdx = spec.indexOf('/', bundleIdIdx);
            if (pathIdx == -1) {
                pathIdx = end;
                path = "";
            }
            int bundleIdEnd = spec.indexOf(':', bundleIdIdx);
            if (bundleIdEnd > pathIdx || bundleIdEnd == -1) bundleIdEnd = pathIdx;
            if (bundleIdEnd < pathIdx - 1) try {
                resIndex = Integer.parseInt(spec.substring(bundleIdEnd + 1, pathIdx));
            } catch (NumberFormatException e) {
            }
            bundleId = spec.substring(bundleIdIdx, bundleIdEnd);
        }
        if (pathIdx < end && spec.charAt(pathIdx) == '/') path = spec.substring(pathIdx, end); else if (end > pathIdx) {
            if (path == null || path.equals("")) path = "/";
            int last = path.lastIndexOf('/') + 1;
            if (last == 0) path = spec.substring(pathIdx, end); else path = path.substring(0, last) + spec.substring(pathIdx, end);
        }
        if (path == null) path = "";
        int dotIndex;
        while ((dotIndex = path.indexOf("/./")) >= 0) path = path.substring(0, dotIndex + 1) + path.substring(dotIndex + 3);
        if (path.endsWith("/.")) path = path.substring(0, path.length() - 1);
        while ((dotIndex = path.indexOf("/../")) >= 0) {
            if (dotIndex != 0) path = path.substring(0, path.lastIndexOf('/', dotIndex - 1)) + path.substring(dotIndex + 3); else path = path.substring(dotIndex + 3);
        }
        if (path.endsWith("/..") && path.length() > 3) path = path.substring(0, path.length() - 2);
        checkAdminPermission(context.getBundle(Long.parseLong(bundleId)));
        setURL(url, url.getProtocol(), bundleId, resIndex, SECURITY_AUTHORIZED, null, path, null, null);
    }

    /**
	 * Establishes a connection to the resource specified by <code>URL</code>.
	 * Since different protocols may have unique ways of connecting, it must be
	 * overridden by the subclass.
	 *
	 * @return java.net.URLConnection
	 * @param url java.net.URL
	 *
	 * @exception	IOException 	thrown if an IO error occurs during connection establishment
	 */
    protected URLConnection openConnection(URL url) throws IOException {
        if (bundleEntry != null) return (new BundleURLConnection(url, bundleEntry));
        String bidString = url.getHost();
        if (bidString == null) {
            throw new IOException(NLS.bind(AdaptorMsg.URL_NO_BUNDLE_ID, url.toExternalForm()));
        }
        AbstractBundle bundle = null;
        long bundleID;
        try {
            bundleID = Long.parseLong(bidString);
        } catch (NumberFormatException nfe) {
            throw new MalformedURLException(NLS.bind(AdaptorMsg.URL_INVALID_BUNDLE_ID, bidString));
        }
        bundle = (AbstractBundle) context.getBundle(bundleID);
        if (!url.getAuthority().equals(SECURITY_AUTHORIZED)) {
            checkAdminPermission(bundle);
        }
        if (bundle == null) {
            throw new IOException(NLS.bind(AdaptorMsg.URL_NO_BUNDLE_FOUND, url.toExternalForm()));
        }
        return (new BundleURLConnection(url, findBundleEntry(url, bundle)));
    }

    /**
	 * Finds the bundle entry for this protocal.  This is handled
	 * differently for Bundle.gerResource() and Bundle.getEntry()
	 * because getResource uses the bundle classloader and getEntry
	 * only used the base bundle file.
	 * @param url The URL to find the BundleEntry for.
	 * @return the bundle entry
	 */
    protected abstract BundleEntry findBundleEntry(URL url, AbstractBundle bundle) throws IOException;

    /**
	 * Converts a bundle URL to a String.
	 *
	 * @param   url   the URL.
	 * @return  a string representation of the URL.
	 */
    protected String toExternalForm(URL url) {
        StringBuffer result = new StringBuffer(url.getProtocol());
        result.append("://");
        String bundleId = url.getHost();
        if ((bundleId != null) && (bundleId.length() > 0)) result.append(bundleId);
        int index = url.getPort();
        if (index > 0) result.append(':').append(index);
        String path = url.getPath();
        if (path != null) {
            if ((path.length() > 0) && (path.charAt(0) != '/')) {
                result.append("/");
            }
            result.append(path);
        }
        return (result.toString());
    }

    public static void setContext(BundleContext context) {
        BundleResourceHandler.context = context;
    }

    protected int hashCode(URL url) {
        int hash = 0;
        String protocol = url.getProtocol();
        if (protocol != null) hash += protocol.hashCode();
        String host = url.getHost();
        if (host != null) hash += host.hashCode();
        String path = url.getPath();
        if (path != null) hash += path.hashCode();
        return hash;
    }

    protected boolean equals(URL url1, URL url2) {
        return sameFile(url1, url2);
    }

    protected synchronized InetAddress getHostAddress(URL url) {
        return null;
    }

    protected boolean hostsEqual(URL url1, URL url2) {
        String host1 = url1.getHost();
        String host2 = url2.getHost();
        if (host1 != null && host2 != null) return host1.equalsIgnoreCase(host2);
        return (host1 == null && host2 == null);
    }

    protected boolean sameFile(URL url1, URL url2) {
        String p1 = url1.getProtocol();
        String p2 = url2.getProtocol();
        if (!((p1 == p2) || (p1 != null && p1.equalsIgnoreCase(p2)))) return false;
        if (!hostsEqual(url1, url2)) return false;
        if (url1.getPort() != url2.getPort()) return false;
        String a1 = url1.getAuthority();
        String a2 = url2.getAuthority();
        if (!((a1 == a2) || (a1 != null && a1.equals(a2)))) return false;
        String path1 = url1.getPath();
        String path2 = url2.getPath();
        if (!((path1 == path2) || (path1 != null && path1.equals(path2)))) return false;
        return true;
    }

    protected void checkAdminPermission(Bundle bundle) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AdminPermission(bundle, AdminPermission.RESOURCE));
        }
    }

    protected static BundleClassLoader getBundleClassLoader(AbstractBundle bundle) {
        BundleLoader loader = bundle.getBundleLoader();
        if (loader == null) return null;
        return loader.createClassLoader();
    }
}
