package org.tamacat.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import org.tamacat.io.RuntimeIOException;

/**
 * Class of Utilities for I/O
 */
public class IOUtils {

    /**
	 * Get the InputStream from Resource in CLASSPATH.
	 * @param path
	 * @return
	 */
    public static InputStream getInputStream(String path) {
        return getInputStream(path, ClassUtils.getDefaultClassLoader());
    }

    /**
     * Get the InputStream from Resource in CLASSPATH.
     * @param path File path in CLASSPATH
     * @return InputStream
     * @since 0.7
     */
    public static InputStream getInputStream(String path, ClassLoader loader) {
        URL url = ClassUtils.getURL(getClassPathToResourcePath(path), loader);
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        } catch (NullPointerException e) {
            throw new ResourceNotFoundException(path + " is not found.");
        }
        return in;
    }

    /**
     * Convert the format of CLASSPATH('.' seperator) to Resource path('/' separator)
     * @param path
     * @return
     */
    public static String getClassPathToResourcePath(String path) {
        if (path == null || path.indexOf('/') >= 0) return path;
        int idx = path.lastIndexOf(".");
        if (idx >= 0) {
            String name = path.substring(0, idx);
            String ext = path.substring(idx, path.length());
            return name.replace('.', '/') + ext;
        } else {
            return path;
        }
    }

    /**
     * It performs, when the "close()" method is implemented. 
     * @param target
     */
    public static void close(Object target) {
        if (target != null) {
            if (target instanceof Closeable) {
                close((Closeable) target);
            } else {
                try {
                    Method closable = ClassUtils.searchMethod(target.getClass(), "close");
                    if (closable != null) closable.invoke(target);
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause != null && cause instanceof IOException) {
                        throw new RuntimeIOException(e);
                    }
                }
            }
        }
    }

    /**
	 * When an IOException occurs, 
	 * RuntimeIOException will be given up if it is OutputStream or Writer.  
	 * @param closable
	 */
    public static void close(Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (IOException e) {
            if (closable instanceof OutputStream || closable instanceof Writer) {
                throw new RuntimeIOException(e);
            }
        }
    }
}
