package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.hadoop.fs.Path;

/** An implementation of a protocol for accessing filesystems over HTTPS.
 * The following implementation provides a limited, read-only interface
 * to a filesystem over HTTPS.
 * @see org.apache.hadoop.hdfs.server.namenode.ListPathsServlet
 * @see org.apache.hadoop.hdfs.server.namenode.FileDataServlet
 */
public class HsftpFileSystem extends HftpFileSystem {

    @Override
    protected HttpURLConnection openConnection(String path, String query) throws IOException {
        try {
            final URL url = new URI("https", null, nnAddr.getHostName(), nnAddr.getPort(), path, query, null).toURL();
            return (HttpURLConnection) url.openConnection();
        } catch (URISyntaxException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    @Override
    public URI getUri() {
        try {
            return new URI("hsftp", null, nnAddr.getHostName(), nnAddr.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
