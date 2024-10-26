package org.exolab.jms.net.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;

/**
 * Helper class for establishing connections to the tunnel servlet.
 *
 * @author <a href="mailto:tma@netspace.net.au">Tim Anderson</a>
 * @version $Revision: 1.2 $ $Date: 2005/04/08 15:16:46 $
 */
class TunnelHelper {

    /**
     * Create an {@link HttpURLConnection} to the tunnel servlet.
     * <p/>
     * If the supplied connection request info contains a proxy user and
     * password, these will be encoded and passed in the "Proxy-Authorization"
     * request property.
     *
     * @param url    the tunnel servlet URL
     * @param id     the connection identifier. May be <code>null</code>.
     * @param action the action to perform
     * @param info   the connection request info.
     */
    public static HttpURLConnection create(URL url, String id, String action, HTTPRequestInfo info) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        if (id != null) {
            connection.setRequestProperty("id", id);
        }
        connection.setRequestProperty("action", action);
        connection.setUseCaches(false);
        if (info.getProxyUser() != null && info.getProxyPassword() != null) {
            String pwd = info.getProxyUser() + ":" + info.getProxyPassword();
            String encoded = new String(Base64.encodeBase64(pwd.getBytes()));
            connection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
        }
        return connection;
    }

    /**
     * Create an {@link HttpURLConnection} to the tunnel servlet, and connect
     * to it.
     * <p/>
     * If the supplied connection request info contains a proxy user and
     * password, these will be encoded and passed in the "Proxy-Authorization"
     * header.
     *
     * @param url    the tunnel servlet URL
     * @param id     the connection identifier. May be <code>null</code>.
     * @param action the action to perform
     * @param info   the connection request info.
     * @throws IOException if an I/O error occurs
     */
    public static HttpURLConnection connect(URL url, String id, String action, HTTPRequestInfo info) throws IOException {
        HttpURLConnection connection = create(url, id, action, info);
        connection.setRequestProperty("Content-Length", "0");
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(connection.getResponseCode() + " " + connection.getResponseMessage());
        }
        return connection;
    }
}
