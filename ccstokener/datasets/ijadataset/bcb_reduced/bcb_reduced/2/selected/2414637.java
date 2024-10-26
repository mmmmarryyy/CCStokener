package freemarker.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.JarURLConnection;
import java.net.URLConnection;

/**
 * Wraps a <code>java.net.URL</code>, and implements methods required for a typical template source.
 * @version $Id: URLTemplateSource.java,v 1.4 2003/04/02 11:43:18 szegedia Exp $
 * @author Daniel Dekany
 * @author TJ Laurenzo
 */
class URLTemplateSource {

    private final URL url;

    private URLConnection conn;

    private InputStream inputStream;

    URLTemplateSource(URL url) throws IOException {
        this.url = url;
        this.conn = url.openConnection();
    }

    public boolean equals(Object o) {
        if (o instanceof URLTemplateSource) {
            return url.equals(((URLTemplateSource) o).url);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return url.hashCode();
    }

    public String toString() {
        return url.toString();
    }

    long lastModified() {
        if (conn instanceof JarURLConnection) {
            URL jarURL = ((JarURLConnection) conn).getJarFileURL();
            if (jarURL.getProtocol().equals("file")) {
                return new File(jarURL.getFile()).lastModified();
            } else {
                URLConnection jarConn = null;
                try {
                    jarConn = jarURL.openConnection();
                    return jarConn.getLastModified();
                } catch (IOException e) {
                    return -1;
                } finally {
                    try {
                        if (jarConn != null) jarConn.getInputStream().close();
                    } catch (IOException e) {
                    }
                }
            }
        } else {
            long lastModified = conn.getLastModified();
            if (lastModified == -1L && url.getProtocol().equals("file")) {
                return new File(url.getFile()).lastModified();
            } else {
                return lastModified;
            }
        }
    }

    InputStream getInputStream() throws IOException {
        inputStream = conn.getInputStream();
        return inputStream;
    }

    void close() throws IOException {
        try {
            if (inputStream != null) {
                inputStream.close();
            } else {
                conn.getInputStream().close();
            }
        } finally {
            inputStream = null;
            conn = null;
        }
    }
}
