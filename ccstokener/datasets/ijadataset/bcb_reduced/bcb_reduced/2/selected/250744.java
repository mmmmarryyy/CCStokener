package de.eqc.srcds.core;

import static de.eqc.srcds.core.Constants.MILLIS_PER_SEC;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 
 * @author Holger Cremer
 */
public final class Utils {

    private static long lastModfiedCache = -1;

    /** Hides the constructor of the utility class. */
    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static String getUrlContent(final URL url) throws IOException {
        return getInputStreamContent(url.openStream());
    }

    public static String getFileContent(final File file) throws FileNotFoundException, IOException {
        return getInputStreamContent(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * Get the content of the input stream and closes the stream.
     * 
     * @param input
     * @return
     * @throws IOException
     */
    public static String getInputStreamContent(final InputStream input) throws IOException {
        final StringBuilder builder = new StringBuilder();
        try {
            final byte[] buffer = new byte[1024];
            for (int len = 0; (len = input.read(buffer)) != -1; ) {
                builder.append(new String(buffer, 0, len));
            }
        } finally {
            closeQuietly(input);
        }
        return builder.toString();
    }

    /**
     * @param file
     * @param newContent
     * @throws IOException
     */
    public static void saveToFile(final File file, final String fileContent) throws IOException {
        final BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
        try {
            output.write(fileContent.getBytes());
        } finally {
            closeQuietly(output);
        }
    }

    /**
     * Escapes the characters '>', '<', '&' to ensure valid xml.
     * 
     * @param source
     * @return
     */
    public static String escapeForXml(String source) {
        source = source.replace("&", "&amp;");
        source = source.replace("<", "&lt;");
        source = source.replace(">", "&gt;");
        return source;
    }

    /**
     * The reverse function for {@link #escapeForXml(String)}.
     * 
     * @param source
     * @return
     */
    public static String unEscapeForXml(String source) {
        source = source.replace("&gt;", ">");
        source = source.replace("&lt;", "<");
        source = source.replace("&amp;", "&");
        return source;
    }

    public static void closeQuietly(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException excp) {
        }
    }

    public static long millisToSecs(final long millis) {
        return millis / MILLIS_PER_SEC;
    }

    /**
     * Tries to get the last modified date from the current jar. If it fails
     * 'System.currentTimeMillis()' is used. This value is cached.
     */
    public static long getLastModifiedDate() {
        if (lastModfiedCache == -1) {
            final URL location = Utils.class.getProtectionDomain().getCodeSource().getLocation();
            if (location.toString().endsWith(".jar")) {
                try {
                    final File file = new File(location.toURI());
                    lastModfiedCache = file.lastModified();
                } catch (URISyntaxException excp) {
                }
            }
            if (lastModfiedCache == -1) {
                lastModfiedCache = System.currentTimeMillis();
            }
        }
        return lastModfiedCache;
    }
}
