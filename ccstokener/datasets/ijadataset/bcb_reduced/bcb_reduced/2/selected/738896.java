package com.slychief.javamusicbrainz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public static InputStream getDocumentAsInputStream(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsInputStream(u);
    }

    /**
     *
     * @param url
     * @return
     *
     * @throws IOException
     */
    public static InputStream getDocumentAsInputStream(URL url) throws IOException {
        InputStream in = url.openStream();
        return in;
    }

    /**
     *
     * @param url
     * @return
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public static String getDocumentAsString(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getDocumentAsString(u);
    }

    /**
     *
     * @param url
     * @return
     *
     * @throws IOException
     */
    public static String getDocumentAsString(URL url) throws IOException {
        StringBuffer result = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF8"));
        String line = "";
        while (line != null) {
            result.append(line);
            line = in.readLine();
        }
        return result.toString();
    }
}
