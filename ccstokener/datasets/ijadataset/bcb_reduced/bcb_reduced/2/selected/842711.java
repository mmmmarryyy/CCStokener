package net.sf.jimo.api.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Various network utilities. A simple method to get a resource over 
 * HTTP and return a DataInputStream is provided.
 *
 * @author C. Enrique Ortiz
 */
public class NetworkUtil {

    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";

    public static final String MIMETYPE_TEXT_XML = "text/xml";

    public static final String MIMETYPE_APP_XML = "application/xml";

    public static final String MIMETYPE_IMAGE_PNG = "image/png";

    public static final String MIMETYPE_APP_JAVAARCHIVE = "application/java-archive";

    private static final String HTTPHDR_ACCEPT = "Accept";

    private static final String HTTPHDR_USER_AGENT = "User-Agent";

    private static String HTTPHDR_USER_AGENT_VALUE = sun.net.www.protocol.http.HttpURLConnection.userAgent;

    private static final String HTTPHDR_CONNECTION = "Connection";

    private static final String HTTPHDR_CONNECTION_CLOSE = "close";

    private static final String HTTPHDR_CACHE_CONTROL = "Cache-Control";

    private static final String HTTPHDR_CACHE_CONTROL_NOTRANSFORM = "no-transform";

    private static final String HTTPHDR_CONTENT_LEN = "Content-Length";

    /**
     * Gets a Resource over HTTP, returing an <code>DataInputStream</code> to it
     *
     * @param url is the URI to use to get the resource to retrieve
     * @pararm mimeType is the (possible) mime type(s) of the resource to retrieve
     *
     * @return an <code>DataInputStream</code> for the input resource
     *
     * @throws <code>IOException</code> is thrown if an error is encountered
     */
    public static DataInputStream getResourceOverHTTP(URL url, String mimeType) throws IOException {
        HttpURLConnection connection = null;
        IOException ioException = null;
        DataInputStream is = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty(HTTPHDR_ACCEPT, mimeType);
            connection.setRequestProperty(HTTPHDR_USER_AGENT, HTTPHDR_USER_AGENT_VALUE);
            connection.setRequestProperty(HTTPHDR_CONNECTION, HTTPHDR_CONNECTION_CLOSE);
            connection.setRequestProperty(HTTPHDR_CACHE_CONTROL, HTTPHDR_CACHE_CONTROL_NOTRANSFORM);
            connection.setRequestProperty(HTTPHDR_CONTENT_LEN, String.valueOf("0"));
            int rc = connection.getResponseCode();
            if (rc != HttpURLConnection.HTTP_OK) {
                ioException = new IOException("Http Error, response Code is " + rc);
                throw ioException;
            }
            is = new DataInputStream(connection.getInputStream());
        } catch (IOException ioe) {
            throw ioe;
        } finally {
        }
        return is;
    }
}
