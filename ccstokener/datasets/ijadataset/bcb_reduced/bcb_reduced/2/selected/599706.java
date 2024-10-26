package com.ibm.net.protocol.atp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A stream protocol handler for atp protocol.
 * 
 * @version 1.00 96/06/20
 * @author Danny B. Lange
 * @author Mitsuru Oshima
 */
public class Handler extends URLStreamHandler {

    /**
     * Opens a connection to the object referenced by the URL argument.
     * 
     * @param url
     *            the URL that this connect to.
     * @return an AtpURLConnection object for the URL.
     */
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return new URLConnectionForATP(url);
    }
}
