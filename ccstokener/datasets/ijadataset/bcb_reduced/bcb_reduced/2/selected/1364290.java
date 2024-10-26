package com.panopset;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Abstract HTTP client.
 *
 * @author Karl Dinwiddie
 *
 */
public abstract class HttpClientAbstract {

    /**
     * URL.
     */
    private final URL url;

    /**
     * @return URL
     */
    public final URL getURL() {
        return url;
    }

    /**
     * response code.
     */
    private int responseCode;

    /**
     *
     * @return Response code.
     */
    public final int getResponseCode() {
        return responseCode;
    }

    /**
     * @param connection
     *            Connection.
     */
    protected abstract void setConnectionProperties(final HttpURLConnection connection);

    /**
     * @param newURL
     *            URL.
     */
    protected HttpClientAbstract(final URL newURL) {
        url = newURL;
    }

    /**
     * @param urlString
     *            URL String.
     */
    protected HttpClientAbstract(final String urlString) {
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Util.log(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Connection.
     */
    private HttpURLConnection con;

    /**
     * @return Connection.
     * @throws Exception
     *             Exception.
     */
    public final HttpURLConnection getConnection() throws Exception {
        if (con == null) {
            con = (HttpURLConnection) url.openConnection();
            setConnectionProperties(con);
        }
        return con;
    }

    /**
     * Close the connection.
     */
    protected final void closeConnection() {
        if (con != null) {
            try {
                responseCode = con.getResponseCode();
            } catch (IOException e) {
                Util.log(e);
            }
            con.disconnect();
        }
    }
}
