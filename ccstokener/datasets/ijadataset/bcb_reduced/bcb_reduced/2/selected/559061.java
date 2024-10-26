package com.google.code.javastorage.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 
 * @author thomas.scheuchzer@gmail.com
 * 
 */
public class URLOpener {

    public InputStream openStream(URL url) throws IOException {
        return url.openStream();
    }
}
