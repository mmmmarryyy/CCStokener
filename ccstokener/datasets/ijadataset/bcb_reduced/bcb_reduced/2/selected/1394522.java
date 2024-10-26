package net.sf.japi.progs.jeduca.swing.io;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.IOException;

/**
 * Utility class for I/O operations.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 * @since 0.1
 */
public final class IOUtilities {

    /** Utility class - do not instantiate. */
    private IOUtilities() {
    }

    /** Get the last modified value for a URL.
     * This method is a utility method provided for subclasses.
     * @param uri URL to get last modified value from
     * @return last modified value for <var>uri</var> or 0 if not found
     */
    public static long lastMod(final String uri) {
        try {
            final URL url = new URL(uri);
            final URLConnection con = url.openConnection();
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).setRequestMethod("HEAD");
            }
            return con.getLastModified();
        } catch (final IOException ignore) {
            return 0L;
        }
    }
}
