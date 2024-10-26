package com.manning.junitbook.ch07.mocks.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A WebClient implementation that retrieves the content from a URL.
 * 
 * @version $Id: WebClient1.java 503 2009-08-16 17:47:12Z paranoid12 $
 */
public class WebClient1 {

    /**
     * A method to retrieve the content from the given URL.
     * 
     * @param url
     * @return
     */
    public String getContent(URL url) {
        StringBuffer content = new StringBuffer();
        try {
            HttpURLConnection connection = createHttpURLConnection(url);
            InputStream is = connection.getInputStream();
            int count;
            while (-1 != (count = is.read())) {
                content.append(new String(Character.toChars(count)));
            }
        } catch (IOException e) {
            return null;
        }
        return content.toString();
    }

    /**
     * Creates an HTTP connection.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    protected HttpURLConnection createHttpURLConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }
}
