package net.sf.provisioner.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper class for loading files.  Makes an attempt at interpreting 
 * {@linkplain String} paths into URLs, local jars, local files, or 
 * {@linkplain ClassLoader} resources.  
 *
 * @author     <a href="mailto:g_pearson@users.sourceforge.net">Gordon Pearson</a>
 * @version $Revision: 1.1.2.1 $
 * @created    Nov 11, 2007
 */
public class PathHelper {

    private static void validatePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("cannot process null path.");
        }
        if ("".equals(path.trim())) {
            throw new IllegalArgumentException("cannot process empty path.");
        }
    }

    static boolean containsProtocol(String path) {
        return path.matches("^[a-zA-Z]+:.+");
    }

    static boolean isJarFile(String path) {
        if (path.startsWith("jar:")) return true;
        return path.matches(".+!/.+");
    }

    /**
	 * Attempts to convert a path to an InputStream.  In order, attempts to:
	 * <ul>
	 * <li>open the path as a URL, if it contains protocol information (e.g.
	 * ftp:)
	 * <li>open the path as a jarfile in the local filesystem if it includes
	 * "!/".
	 * <li>open the path as a local file.
	 * <li>open the path as a resource using a {@linkplain ClasLoader}.
	 * </ul>
	 * @param path a URI, local filesystem reference, or resource reference. 
	 * @return an {@linkplain InputStream} to <code>path</code>'s content, or
	 * null if the path doesn't point to something.
	 * @throws IOException 
	 * @throws MalformedURLException if the path contains a protocol, but 
	 * is not a valid URL.
	 */
    public static InputStream pathToStream(String path) throws IOException, MalformedURLException {
        validatePath(path);
        URL url = pathToURL(path);
        if (url != null) {
            return url.openStream();
        }
        InputStream in = PathHelper.class.getResourceAsStream(path);
        if (in == null && !path.startsWith("/")) {
            in = PathHelper.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    /**
	 * Attempts to convert a path to a URL.  In order, checks if :
	 * <ul>
	 * <li>path contains protocol information (e.g. ftp:)
	 * <li>path contains "!/" and is a jarfile in the local filesystem.
	 * <li>path is a local file.
	 * </ul>
	 * @param path a URL string, local filesystem reference, or resource 
	 * reference. 
	 * @return a {@linkplain URL} or null if the path cannot be converted.
	 * @throws IOException 
	 * @throws MalformedURLException if the path contains a protocol, but 
	 * is not a valid URL.
	 */
    public static URL pathToURL(String path) throws IOException, MalformedURLException {
        validatePath(path);
        if (containsProtocol(path)) return new URL(path);
        if (isJarFile(path)) {
            String jarFile = path.replaceAll("(.+)!.+", "$1");
            if (!new File(jarFile).exists()) {
                throw new IOException("cannot find jar file \"" + jarFile + "\".");
            }
            return new URL("jar:file:" + path);
        }
        if (new File(path).exists()) {
            return new URL("file:" + path);
        }
        URL url = PathHelper.class.getResource(path);
        if (url == null && !path.startsWith("/")) {
            url = PathHelper.class.getResource("/" + path);
        }
        return url;
    }
}
