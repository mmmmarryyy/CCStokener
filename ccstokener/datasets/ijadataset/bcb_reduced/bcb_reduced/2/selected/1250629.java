package utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * Utilidades relacionadas a E/S, URL/URI y manejo de archivos.
 */
public final class IoUtil {

    private IoUtil() {
    }

    private static boolean isUrlResourceExists(final URL url) {
        try {
            InputStream is = url.openStream();
            try {
                is.close();
            } catch (IOException ioe) {
            }
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    private static boolean isJarResourceExists(final URL url) {
        try {
            String urlStr = url.toExternalForm();
            int p = urlStr.indexOf("!/");
            if (p == -1) {
                return false;
            }
            URL fileUrl = new URL(urlStr.substring(4, p));
            File file = url2file(fileUrl);
            if (file == null) {
                return isUrlResourceExists(url);
            }
            if (!file.canRead()) {
                return false;
            }
            if (p == urlStr.length() - 2) {
                return true;
            }
            JarFile jarFile = new JarFile(file);
            try {
                return jarFile.getEntry(urlStr.substring(p + 2)) != null;
            } finally {
                jarFile.close();
            }
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Utility method to convert local URL to a {@link File} object.
     * @param url an URL
     * @return file object for given URL or <code>null</code> if URL is not
     *         local
     */
    public static File url2file(final URL url) {
        if (!"file".equalsIgnoreCase(url.getProtocol())) return null;
        return new File(url.getFile().replaceAll("%20", " "));
    }

    /**
     * Utility method to convert a {@link File} object to a local URL.
     * @param file a file object
     * @return absolute URL that points to the given file
     * @throws MalformedURLException if file can't be represented as URL for
     *         some reason
     */
    public static URL file2url(final File file) throws MalformedURLException {
        try {
            return file.getCanonicalFile().toURI().toURL();
        } catch (MalformedURLException mue) {
            throw mue;
        } catch (IOException ioe) {
        } catch (NoSuchMethodError nsme) {
        }
        try {
            return new URL("file://" + file.getCanonicalPath().replace('\\', '/').replaceAll(" ", "%20"));
        } catch (MalformedURLException mue) {
            throw mue;
        } catch (IOException ioe) {
        }
        return null;
    }

    public static boolean isResourceExists(final URL url) {
        File file = url2file(url);
        if (file != null) {
            return file.canRead();
        }
        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            return isJarResourceExists(url);
        }
        return isUrlResourceExists(url);
    }
}
