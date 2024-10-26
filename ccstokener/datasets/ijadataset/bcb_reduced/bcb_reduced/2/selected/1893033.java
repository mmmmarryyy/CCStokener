package gnu.text;

import java.io.*;
import java.net.*;
import gnu.mapping.WrappedException;

/** A Path that wraps a URL. */
public class URLPath extends URIPath {

    final URL url;

    URLPath(URL url) {
        super(toUri(url));
        ;
        this.url = url;
    }

    public static URLPath valueOf(URL url) {
        return new URLPath(url);
    }

    public boolean isAbsolute() {
        return true;
    }

    public long getLastModified() {
        return getLastModified(url);
    }

    public static long getLastModified(URL url) {
        try {
            return url.openConnection().getLastModified();
        } catch (Throwable ex) {
            return 0;
        }
    }

    public long getContentLength() {
        return getLastModified(url);
    }

    public static int getContentLength(URL url) {
        try {
            return url.openConnection().getContentLength();
        } catch (Throwable ex) {
            return -1;
        }
    }

    public URL toURL() {
        return url;
    }

    public static URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (Throwable ex) {
            throw WrappedException.wrapIfNeeded(ex);
        }
    }

    public URI toUri() {
        return toUri(url);
    }

    public String toURIString() {
        return url.toString();
    }

    public Path resolve(String relative) {
        try {
            return valueOf(new URL(url, relative));
        } catch (Throwable ex) {
            throw WrappedException.wrapIfNeeded(ex);
        }
    }

    public static InputStream openInputStream(URL url) throws IOException {
        return url.openConnection().getInputStream();
    }

    public InputStream openInputStream() throws IOException {
        return openInputStream(url);
    }

    public static OutputStream openOutputStream(URL url) throws IOException {
        String str = url.toString();
        if (str.startsWith("file:")) {
            try {
                return new FileOutputStream(new File(new URI(str)));
            } catch (Throwable ex) {
            }
        }
        URLConnection conn = url.openConnection();
        conn.setDoInput(false);
        conn.setDoOutput(true);
        return conn.getOutputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return openOutputStream(url);
    }

    public static URLPath classResourcePath(Class clas) {
        try {
            return valueOf(ResourceStreamHandler.makeURL(clas));
        } catch (Throwable ex) {
            throw WrappedException.wrapIfNeeded(ex);
        }
    }
}
