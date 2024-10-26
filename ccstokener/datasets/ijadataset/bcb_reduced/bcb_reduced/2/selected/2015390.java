package cbr.service;

import java.net.URL;
import java.security.PrivilegedAction;

/**
 * This class opens an URL input stream with a GET request.
 *
 * @author Volker Roth
 * @version "$Id: PrivilegedGet.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class PrivilegedGet extends Object implements PrivilegedAction {

    /**
     * The URL from which the data is read.
     */
    private URL url_;

    /**
     * Creates an instance that opens the given URL.
     *
     * @param url The url to open.
     * @exception NullPointerException if no URL is given.
     */
    public PrivilegedGet(URL url) {
        if (url == null) {
            throw new NullPointerException("Need an URL!");
        }
        url_ = url;
    }

    /**
      * Opens the URL and returns an input stream to it.
      *
      * @return The InputStream or <code>null</code>.
      */
    public Object run() {
        try {
            return url_.openStream();
        } catch (Exception e) {
            return null;
        }
    }
}
