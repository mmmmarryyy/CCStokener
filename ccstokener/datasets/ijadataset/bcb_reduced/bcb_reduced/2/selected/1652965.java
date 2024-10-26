package com.slychief.javamusicbrainz.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author sly
 */
public class URLGrabber {

    /**
     *
     * @param url
     * @return
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     */
    public static InputStream getDocumentAsInputStream(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsInputStream(u);
    }

    /**
     *
     * @param url
     * @return
     * @throws java.io.IOException
     */
    public static InputStream getDocumentAsInputStream(URL url) throws IOException {
        InputStream in = url.openStream();
        return in;
    }

    /**
     *
     * @param url
     * @return
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     */
    public static String getDocumentAsString(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsString(u);
    }

    /**
     *
     * @param url
     * @return
     * @throws java.io.IOException
     */
    public static String getDocumentAsString(URL url) throws IOException {
        StringBuffer result = new StringBuffer();
        InputStream in = url.openStream();
        int c;
        while ((c = in.read()) != -1) {
            result.append((char) c);
        }
        return result.toString();
    }
}
