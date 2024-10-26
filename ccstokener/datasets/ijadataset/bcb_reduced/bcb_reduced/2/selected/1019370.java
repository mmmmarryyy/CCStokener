package HTTPClient.shttp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import HTTPClient.ProtocolNotSuppException;

/**
 * This class implements a URLStreamHandler for shttp URLs. With this you
 * can use the HTTPClient package as a replacement for the JDKs client.
 * To do so define the property java.protocol.handler.pkgs=HTTPClient .
 *
 * @version	0.3-2  18/06/1999
 * @author	Ronald Tschal�r
 */
public class Handler extends URLStreamHandler {

    public Handler() throws ProtocolNotSuppException {
        new HTTPClient.HTTPConnection("shttp", "", -1);
    }

    public URLConnection openConnection(URL url) throws IOException, ProtocolNotSuppException {
        return new HTTPClient.HttpURLConnection(url);
    }
}
