package org.jcvi.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An {@code HttpGetRequestBuilder} is a class
 * that builds a {@link HttpURLConnection} for a single
 * HTTP Get request.
 * @author dkatzel
 *
 *
 */
public class HttpGetRequestBuilder {

    private final StringBuilder urlBuilder = new StringBuilder();

    int numberOfProperties = 0;

    /**
     * Constructs a new instance of {@link HttpGetRequestBuilder}.
     * @param urlbase the beginning part of the HTTP URL before
     * any get parameters (everything in the URL before the "?")
     */
    public HttpGetRequestBuilder(String urlbase) {
        urlBuilder.append(urlbase);
    }

    /**
     * Add a Key-Value pair as a Get parameter, the values will be converted
     * into a String and then URL Encoded.
     * @param key the key as a String can not be null.
     * @param value the value of paired with the given key, if this value is
     * {@code null} then only the key is added.
     * @return this.
     * @throws UnsupportedEncodingException if the Key or value 
     * can not be URL encoded.
     * @throws NullPointerException if the key is null.
     */
    public synchronized HttpGetRequestBuilder addVariable(String key, Object value) throws UnsupportedEncodingException {
        if (key == null) {
            throw new NullPointerException("key can not be null");
        }
        if (numberOfProperties == 0) {
            urlBuilder.append("?");
        } else {
            urlBuilder.append(HttpUtil.VAR_SEPARATOR);
        }
        urlBuilder.append(HttpUtil.urlEncode(key));
        if (value != null) {
            urlBuilder.append(HttpUtil.VALUE_SEPARATOR).append(HttpUtil.urlEncode(value.toString()));
        }
        numberOfProperties++;
        return this;
    }

    /**
     * Add the Given variable as a flag, this is the same as
     * {@link #addVariable(String, Object) addVariable(flag,null)}
     * @param flag the flag as a String can not be null.
     * @return this 
     * @throws UnsupportedEncodingException if the key can not
     * be URL encoded.
     * @throws NullPointerException if the flag is null.
     */
    public synchronized HttpGetRequestBuilder addFlag(String flag) throws UnsupportedEncodingException {
        return addVariable(flag, null);
    }

    /**
     * Builds this {@link HttpGetRequestBuilder} into an 
     * open {@link HttpURLConnection}.
     * @return an open {@link HttpURLConnection}.
     * @throws IOException if the URL can not be built or if a connection
     * can not be opened.
     */
    public synchronized HttpURLConnection build() throws IOException {
        return (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
    }
}
