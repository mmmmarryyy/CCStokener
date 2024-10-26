package net.disy.legato.testing.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

public class URLUtils {

    private URLUtils() {
    }

    public static URL getURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException murlex) {
            throw new RuntimeException(murlex);
        }
    }

    public static String getContentAsString(final URL url) throws IOException {
        return getContentAsString(url, "UTF-8");
    }

    public static String getContentAsString(final URL url, final String draftEncoding) throws IOException {
        Validate.notNull(url, "URL must not be null.");
        final String encoding = draftEncoding == null ? "UTF-8" : draftEncoding;
        InputStream inputStream = null;
        try {
            final URLConnection connection = url.openConnection();
            inputStream = connection.getInputStream();
            return IOUtils.toString(inputStream, encoding);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
