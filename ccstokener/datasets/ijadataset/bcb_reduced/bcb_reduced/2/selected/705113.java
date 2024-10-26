package gnu.java.net.protocol.ftp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An FTP URL stream handler.
 *
 * @author Chris Burdess (dog@gnu.org)
 */
public class Handler extends URLStreamHandler {

    protected int getDefaultPort() {
        return FTPConnection.FTP_PORT;
    }

    /**
   * Returns an FTPURLConnection for the given URL.
   */
    public URLConnection openConnection(URL url) throws IOException {
        return new FTPURLConnection(url);
    }
}
