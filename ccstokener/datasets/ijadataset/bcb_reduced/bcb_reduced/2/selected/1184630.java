package NNTP;

import java.io.*;
import java.net.*;

/**
 *
 * @author bogotech
 * @version idontknow
 */
public class NNTPProtocolHandler extends URLStreamHandler {

    /** The reusable connection
     */
    public NNTPConnection m_connection = null;

    /** Creates new NNTPProtocolHandler */
    public NNTPProtocolHandler() {
    }

    /**
     * overridden from URLConnection
     * @return beer
     * @param url The URL to connect to
     * @exception IOException I don't think we actually throw this...
     */
    protected URLConnection openConnection(URL url) throws IOException {
        if (url == null) return null;
        if (!url.getProtocol().equals("nntp")) return null;
        if (m_connection != null) {
            if (m_connection.getURL().getHost().equals(url.getHost()) && (m_connection.getURL().getPort() == url.getPort()) && (m_connection.getURL().getUserInfo().equals(url.getUserInfo()))) {
                return m_connection;
            }
        }
        m_connection = new NNTPConnection(url);
        return m_connection;
    }
}
