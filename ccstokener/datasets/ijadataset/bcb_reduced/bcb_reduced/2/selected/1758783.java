package com.gargoylesoftware.htmlunit.protocol.javascript;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Stream handler for JavaScript URLs.
 *
 * @version $Revision: 6701 $
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public class Handler extends URLStreamHandler {

    /**
     * Returns a new URLConnection for this URL.
     * @param url the JavaScript URL
     * @return the connection
     */
    @Override
    protected URLConnection openConnection(final URL url) {
        return new JavaScriptURLConnection(url);
    }
}
