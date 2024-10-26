package cbr2.service;

import java.io.*;
import java.net.*;
import java.security.PrivilegedAction;

/**
 * This class opens an URL input stream with a GET request.
 *
 * @author Volker Roth
 * @version "$Id: PrivilegedGet.java 601 2002-04-17 16:00:44Z upinsdor $"
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
