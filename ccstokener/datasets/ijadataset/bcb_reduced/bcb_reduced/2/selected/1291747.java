package net.assimilator.protocols.rcl;

import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Description:
 * User: Larry Mitchell
 * Date: Oct 16, 2007
 * Time: 12:42:19 PM
 * Version: $id$
 */
public class RCLURLStreamHandler extends URLStreamHandler {

    /**
     * the logger for our stuff
     */
    private static final Logger logger = Logger.getLogger("net.assimilator.protocols.rcl");

    public static final String PROTOCOL = "rcl";

    /**
     * default contructor
     */
    public RCLURLStreamHandler() {
    }

    /**
     * Opens a connection to the object referenced by the
     * <code>URL</code> argument.
     * This method should be overridden by a subclass.
     * <p/>
     * <p>If for the handler's protocol (such as HTTP or JAR), there
     * exists a public, specialized URLConnection subclass belonging
     * to one of the following packages or one of their subpackages:
     * java.lang, java.io, java.util, java.net, the connection
     * returned will be of that subclass. For example, for HTTP an
     * HttpURLConnection will be returned, and for JAR a
     * JarURLConnection will be returned.
     *
     * @param url the URL that this connects to.
     * @return a <code>URLConnection</code> object for the <code>URL</code>.
     * @throws java.io.IOException if an I/O error occurs while opening the
     *                             connection.
     */
    protected URLConnection openConnection(URL url) throws IOException {
        return new RCLURLConnection(url);
    }

    /**
     * This method should return a parseable string form of this URL.
     */
    protected String toExternalForm(final URL url) {
        return PROTOCOL.concat(":").concat(url.getFile());
    }

    /**
     * Must override to prevent default parsing of our URLs as HTTP-like URLs
     * (the base class implementation eventually calls setURL(), which is tied
     * to HTTP URL syntax too much).
     */
    protected void parseURL(final URL context, final String spec, final int start, final int limit) {
        final String resourceName = combineResourceNames(context.getFile(), spec.substring(start));
        setURL(context, context.getProtocol(), "", -1, "", "", resourceName, "", "");
    }

    private static String combineResourceNames(String base, String relative) {
        if ((base == null) || (base.length() == 0)) {
            return relative;
        }
        if ((relative == null) || (relative.length() == 0)) {
            return base;
        }
        if (relative.startsWith("/")) {
            return relative.substring(1);
        }
        if (base.endsWith("/")) {
            return base.concat(relative);
        } else {
            final int lastBaseSlash = base.lastIndexOf('/');
            if (lastBaseSlash < 0) {
                return relative;
            } else {
                return base.substring(0, lastBaseSlash).concat("/").concat(relative);
            }
        }
    }
}
