package net.sourceforge.javautil.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;

/**
 * Utility for working with {@link URL}'s.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: URLUtil.java 1987 2010-03-24 18:11:21Z ponderator $
 */
public class URLUtil {

    /**
	 * @param <T> The type to coerce to
	 * @param url The URL from which to read the header
	 * @param headerName The name of the header to read
	 * @param expectedType The type expected
	 * @return The value of the header coerced
	 */
    public static <T> T getHeader(URL url, String headerName, Class<T> expectedType) {
        URLConnection connection = null;
        try {
            return ReflectionUtil.coerce(expectedType, (connection = url.openConnection()).getHeaderField("Content-Length"));
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        } finally {
            try {
                connection.getInputStream().close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * This will make sure that any {@link InputStream}'s opened to obtain the
	 * information are closed thus avoiding locks on the source.
	 * 
	 * @param url The URL for which to get last modification info
	 * @return The last modified time stamp for the URL
	 */
    public static long getLastModified(URL url) {
        URLConnection conn = null;
        try {
            return (conn = url.openConnection()).getLastModified();
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        } finally {
            if (conn != null) try {
                conn.getInputStream().close();
            } catch (IOException e) {
                ThrowableManagerRegistry.caught(e);
            }
        }
    }

    /**
	 * For URL's the only way to know if it really exists is trying to open up an input stream.
	 * 
	 * @param url The URL to validate
	 * @return True if the URL exists, an input stream can be opened, otherwise false
	 */
    public static boolean exists(URL url) {
        InputStream input = null;
        try {
            URLConnection conn = url.openConnection();
            input = url.openStream();
            if (conn instanceof HttpURLConnection) {
                return ((HttpURLConnection) conn).getResponseCode() == 200;
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException e) {
                ThrowableManagerRegistry.caught(e);
            }
        }
    }
}
