package org.codehaus.classworlds.uberjar.protocol.jar;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * <code>URLStreamHandler</code> for <code>jar:</code> protocol <code>URL</code>s.
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @version $Id: Handler.java 126 2006-01-12 04:17:51Z  $
 */
public class Handler extends URLStreamHandler {

    /**
     * Singleton instance.
     */
    private static final Handler INSTANCE = new Handler();

    /**
     * Retrieve the singleton instance.
     *
     * @return The singleton instance.
     */
    public static Handler getInstance() {
        return INSTANCE;
    }

    /**
     * Construct.
     */
    public Handler() {
    }

    /**
     * @see java.net.URLStreamHandler
     */
    public URLConnection openConnection(URL url) throws IOException {
        return new JarUrlConnection(url);
    }

    /**
     * @see java.net.URLStreamHandler
     */
    public void parseURL(URL url, String spec, int start, int limit) {
        String specPath = spec.substring(start, limit);
        String urlPath = null;
        if (specPath.charAt(0) == '/') {
            urlPath = specPath;
        } else if (specPath.charAt(0) == '!') {
            String relPath = url.getFile();
            int bangLoc = relPath.lastIndexOf("!");
            if (bangLoc < 0) {
                urlPath = relPath + specPath;
            } else {
                urlPath = relPath.substring(0, bangLoc) + specPath;
            }
        } else {
            String relPath = url.getFile();
            if (relPath != null) {
                int lastSlashLoc = relPath.lastIndexOf("/");
                if (lastSlashLoc < 0) {
                    urlPath = "/" + specPath;
                } else {
                    urlPath = relPath.substring(0, lastSlashLoc + 1) + specPath;
                }
            } else {
                urlPath = specPath;
            }
        }
        setURL(url, "jar", "", 0, null, null, urlPath, null, null);
    }
}
