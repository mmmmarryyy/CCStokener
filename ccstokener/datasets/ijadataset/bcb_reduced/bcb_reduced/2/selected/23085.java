package com.googlecode.phisix.api.urlfetch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 
 * @author Edge Dalmacio
 *
 */
public class URLFetchServiceImpl implements URLFetchService {

    @Override
    public InputStream fetch(URL url) throws IOException {
        return url.openStream();
    }
}
