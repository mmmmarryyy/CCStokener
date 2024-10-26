package gnu.inet.ftp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An FTP URL stream handler.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
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
