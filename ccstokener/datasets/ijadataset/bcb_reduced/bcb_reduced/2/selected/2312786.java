package org.neuroph.util.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of OutputAdapter interface for writing neural network outputs to URL.
 * @see OutputAdapter
 * @author Zoran Sevarac <sevarac@gmail.com>
 */
public class URLOutputAdapter extends OutputStreamAdapter {

    /**
     * Creates a new URLOutputAdapter by opening a connection to URL specified by the url input param
     * @param url URL object to connect to.
     * @throws IOException if connection 
     */
    public URLOutputAdapter(URL url) throws IOException {
        super(new BufferedWriter(new OutputStreamWriter(url.openConnection().getOutputStream())));
    }

    /**
     * Creates a new URLOutputAdapter by opening a connection to URL specified by the string url input param
     * @param url URL to connect to as string.
     * @throws IOException if connection 
     */
    public URLOutputAdapter(String url) throws MalformedURLException, IOException {
        this(new URL(url));
    }
}
