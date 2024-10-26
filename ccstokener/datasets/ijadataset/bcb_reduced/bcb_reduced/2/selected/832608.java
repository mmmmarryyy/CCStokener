package com.gargoylesoftware.htmlunit.protocol.about;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Stream handler for "about:" URLs.
 *
 * @version $Revision: 6701 $
 * @author <a href="mailto:chen_jun@users.sourceforge.net">Chen Jun</a>
 */
public class Handler extends URLStreamHandler {

    /**
     * Returns a new URLConnection for this URL.
     * @param url the "about:" URL
     * @return the connection
     */
    @Override
    protected URLConnection openConnection(final URL url) {
        return new AboutURLConnection(url);
    }
}
