package gnu.java.net.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;

/**
 * Loader for remote directories.
 */
public final class RemoteURLLoader extends URLLoader {

    private final String protocol;

    public RemoteURLLoader(URLClassLoader classloader, URLStreamHandlerCache cache, URLStreamHandlerFactory factory, URL url) {
        super(classloader, cache, factory, url);
        protocol = url.getProtocol();
    }

    /**
   * Get a remote resource.
   * Returns null if no such resource exists.
   */
    public Resource getResource(String name) {
        try {
            URL url = new URL(baseURL, name, cache.get(factory, protocol));
            URLConnection connection = url.openConnection();
            int length = connection.getContentLength();
            InputStream stream = connection.getInputStream();
            if (connection instanceof HttpURLConnection) {
                int response = ((HttpURLConnection) connection).getResponseCode();
                if (response / 100 != 2) return null;
            }
            if (stream != null) return new RemoteResource(this, name, url, stream, length); else return null;
        } catch (IOException ioe) {
            return null;
        }
    }
}
