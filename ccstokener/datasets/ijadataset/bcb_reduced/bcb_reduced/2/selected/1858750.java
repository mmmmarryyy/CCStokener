package uk.org.beton.util.io;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Holds a URL and subcomponents. It emcapsulates the main mode of URL usage when parsing websites,
 * namely separating the URL stem from any reference, plus providing access to the file extension.
 * This is immutable and therefore thread-safe.
 * <p>
 * The syntax of URL is defined by RFC 2396: Uniform Resource Identifiers (URI): Generic Syntax,
 * amended by RFC 2732: Format for Literal IPv6 Addresses in URLs.
 *
 * @author Rick Beton
 */
public final class URL2 implements Comparable<URL2> {

    private final URL url;

    private final String urlStr;

    private final URL2 urlStem;

    private final String file;

    private final String extension;

    private final int hashCode;

    public URL2(String spec) throws MalformedURLException {
        this(new URL(spec));
    }

    public URL2(URL2 context, String spec) throws MalformedURLException {
        this(new URL(context.url, spec));
    }

    public URL2(URL url) {
        this.url = url;
        this.urlStr = url.toString();
        final int h = urlStr.indexOf('#');
        try {
            this.urlStem = (h > 0) ? new URL2(this.urlStr.substring(0, h)) : this;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String tfile = getPartAfter(url.getPath(), '/', url.getPath());
        this.file = getPartBefore(tfile, '?', tfile);
        this.extension = getPartAfter(this.file, '.', "");
        this.hashCode = this.urlStem.toString().hashCode();
    }

    public URLConnection openConnection() throws IOException {
        return this.url.openConnection();
    }

    /**
     * Gets the bare URL, which is complete except the reference fragment has been removed.
     *
     * @return the URL including protocol, host, port, and path.
     */
    public URL2 getUrlStem() {
        return this.urlStem;
    }

    /**
     * Gets the URL reference (fragment), which is the part after the '#'.
     *
     * @return the reference, or null if none defined
     */
    public String getUrlRef() {
        return this.url.getRef();
    }

    /**
     * Gets the file part of the URL
     *
     * @return the part after the last '/' and before any '#' or '?'.
     */
    public String getFile() {
        return this.file;
    }

    /**
     * Gets the extension of the filename. E.g. "http://localhost/index.html" would return "html".
     *
     * @return the extension
     */
    public String getExtension() {
        return this.extension;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof URL2)) {
            return false;
        }
        URL2 t = (URL2) obj;
        return this.urlStem.toString().equals(t.urlStem.toString());
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return this.urlStr;
    }

    private String getPartBefore(String str, char c, String defaultValue) {
        int d = str.lastIndexOf(c);
        if (d >= 0) {
            return str.substring(0, d);
        } else {
            return defaultValue;
        }
    }

    private String getPartAfter(String str, char c, String defaultValue) {
        int d = str.lastIndexOf(c);
        if (d >= 0) {
            return str.substring(d + 1);
        } else {
            return defaultValue;
        }
    }

    public int compareTo(URL2 other) {
        return this.urlStr.compareTo(other.urlStr);
    }
}
