package com.reactiveplot.library.scriptloader;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;

/**
 * Opens an InputStream from a URL.
 * 
 * Each access of the InputStream may cause network traffic.
 * @see CachingURLGetInputStream
 */
public class URLGetInputStream implements ScriptLoader, Serializable {

    private static final long serialVersionUID = 5951607340391387985L;

    private URL scriptURL;

    @Override
    public InputStream getXMLInputStream() {
        try {
            URLConnection urlConn = scriptURL.openConnection();
            return new BufferedInputStream(urlConn.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public URLGetInputStream(URL url) {
        scriptURL = url;
    }
}
