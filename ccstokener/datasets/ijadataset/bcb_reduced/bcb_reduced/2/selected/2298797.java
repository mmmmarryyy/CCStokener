package padrmi.pp;

import java.net.*;
import padrmi.PpURLConnection;

public class Handler extends URLStreamHandler {

    /**
	 * Creates new PpURLConnection object.
	 * 
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
    @Override
    public URLConnection openConnection(URL url) {
        return new PpURLConnection(url);
    }
}
