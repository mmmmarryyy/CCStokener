package gnu.java.net.protocol.jar;

import gnu.java.net.URLParseError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Kresten Krab Thorup (krab@gnu.org)
 */
public class Handler extends URLStreamHandler {

    /**
   * A do nothing constructor
   */
    public Handler() {
    }

    /**
   * This method returs a new JarURLConnection for the specified URL
   *
   * @param url The URL to return a connection for
   *
   * @return The URLConnection
   *
   * @exception IOException If an error occurs
   */
    protected URLConnection openConnection(URL url) throws IOException {
        return new Connection(url);
    }

    /**
   * This method overrides URLStreamHandler's for parsing url of protocol "jar"
   *
   * @param url The URL object in which to store the results
   * @param url_string The String-ized URL to parse
   * @param start The position in the string to start scanning from
   * @param end The position in the string to stop scanning
   */
    protected void parseURL(URL url, String url_string, int start, int end) {
        String file = url.getFile();
        if (!file.equals("")) {
            url_string = url_string.substring(start, end);
            if (url_string.startsWith("/")) {
                int idx = file.lastIndexOf("!/");
                if (idx < 0) throw new URLParseError("no !/ in spec");
                file = file.substring(0, idx + 1) + url_string;
            } else if (url_string.length() > 0) {
                int idx = file.lastIndexOf("/");
                if (idx == -1) file = "/" + url_string; else if (idx == (file.length() - 1)) file = file + url_string; else file = file.substring(0, idx + 1) + url_string;
            }
            setURL(url, "jar", url.getHost(), url.getPort(), file, null);
            return;
        }
        if (end < start) return;
        if (end - start < 2) return;
        if (start > url_string.length()) return;
        url_string = url_string.substring(start, end);
        int jar_stop;
        if ((jar_stop = url_string.indexOf("!/")) < 0) throw new URLParseError("no !/ in spec");
        try {
            new URL(url_string.substring(0, jar_stop));
        } catch (MalformedURLException e) {
            throw new URLParseError("invalid inner URL: " + e.getMessage());
        }
        if (!url.getProtocol().equals("jar")) throw new URLParseError("unexpected protocol " + url.getProtocol());
        setURL(url, "jar", url.getHost(), url.getPort(), url_string, null);
    }

    /**
   * This method converts a Jar URL object into a String.
   *
   * @param url The URL object to convert
   */
    protected String toExternalForm(URL url) {
        String file = url.getFile();
        String ref = url.getRef();
        StringBuffer sb = new StringBuffer(file.length() + 5);
        sb.append("jar:");
        sb.append(file);
        if (ref != null) sb.append('#').append(ref);
        return sb.toString();
    }
}
