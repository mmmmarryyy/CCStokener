package consciouscode.seedling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
    A URL handler for the <code>seedling</code> protocol.

    Any request for a URL using this protocol are handled as requests for files
    from the configuration tree of the application's main Seedling.  This is
    automatically configured when the {@link Seedling} facade is
    used to start the application.

    <h3>TODO</h3>
    <ul>
      <li>Validate that URL has empty host, port, and reference</li>
    </ul>

    @see URL#URL(String, String, int, String) for details on how URL protocol
    handlers work.
*/
public class Handler extends URLStreamHandler {

    public static final String PROTOCOL_PROP = "java.protocol.handler.pkgs";

    private static Seedling ourSeedling;

    /**
       Registers the given Seedling as the source for <code>seedling</code>
       URLs.

       @see URL#URL(String, String, int, String) for details on how this works.
     */
    static synchronized void registerSeedling(Seedling seedling) {
        if (ourSeedling != null) {
            throw new IllegalStateException("A Seedling is already registered");
        }
        ourSeedling = seedling;
        String pkgs = System.getProperty(PROTOCOL_PROP, "");
        if (pkgs.indexOf("consciouscode") == -1) {
            if (pkgs.length() == 0) {
                pkgs = "consciouscode";
            } else {
                pkgs += "|consciouscode";
            }
            System.setProperty(PROTOCOL_PROP, pkgs);
        }
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (ourSeedling == null) {
            String message = ("No Seedling is running to service URL " + url.toExternalForm());
            throw new IOException(message);
        }
        String path = url.getPath();
        URL resourceUrl = ourSeedling.getUrlResources().getResourceUrl(path);
        if (resourceUrl == null) {
            String message = ("Could not find resource with URL " + url.toExternalForm());
            throw new FileNotFoundException(message);
        }
        return resourceUrl.openConnection();
    }
}
