package org.itemscript.standard;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.itemscript.core.exceptions.ItemscriptError;

/**
 * A {@link URLStreamHandler} that handles resources on the classpath.
 * */
public class ResourceHandler extends URLStreamHandler {

    private final ClassLoader classLoader;

    /**
     * Create a new ResourceHandler.
     * 
     * @param classLoader The ClassLoader to use.
     */
    public ResourceHandler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    protected URLConnection openConnection(URL url) throws IOException {
        URL resource = classLoader.getResource(url.getPath());
        if (resource == null) {
            throw ItemscriptError.internalError(this, "openConnection.resource.not.found", url.getPath());
        }
        return resource.openConnection();
    }
}
