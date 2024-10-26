package de.lichtflut.infra.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Technical helper class for loading of resources.
 * 
 * Created: 27.10.2008
 *
 * @author Oliver Tigges
 */
public class SystemResourceLoader {

    public static final String RESOURCE_PREFIX = "resource:";

    public static SystemResourceLoader INSTANCE = new SystemResourceLoader();

    /**
	 * Returns the only instance.
	 */
    public static SystemResourceLoader getInstance() {
        return INSTANCE;
    }

    /**
	 * Checks if given Resource exists.
	 * @param uri The URI of the resource.
	 * @return The URL to that resource.
	 * @throws ResourceException
	 */
    public boolean exists(final String uri) throws ResourceException {
        return findResource(uri) != null;
    }

    /**
	 * Finds a resource by u URI and provides the corresponding URL.
	 * @param uri The URI of the resource.
	 * @return The URL to that resource.
	 * @throws ResourceException
	 */
    public URL findResource(final String uri) throws ResourceException {
        if (uri.startsWith(RESOURCE_PREFIX)) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL url = cl.getResource(uri.substring(RESOURCE_PREFIX.length()));
            return url;
        } else {
            try {
                return new URL("file:" + uri);
            } catch (MalformedURLException e) {
                throw new ResourceException(e);
            }
        }
    }

    /**
	 * Provides an {@link InputStream} to the resource identified by given URI. 
	 * @param uri The URI of the resource.
	 * @return The input stream to that resource.
	 * @throws ResourceException
	 */
    public InputStream loadResource(final String uri) throws ResourceException {
        try {
            if (uri.startsWith(RESOURCE_PREFIX)) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                URL url = cl.getResource(uri.substring(RESOURCE_PREFIX.length()));
                if (url == null) {
                    throw new ResourceException("Resource with URL '" + uri + "' not found by ClassLoader " + cl.toString());
                }
                return url.openStream();
            } else {
                return new FileInputStream(uri);
            }
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    /**
	 * Load java property file.
	 * @param uri The URI of the properties file.
	 * @return The properties object.
	 * @throws ResourceException
	 */
    public Properties loadProperties(final String uri) throws ResourceException {
        InputStream in = loadResource(uri);
        Properties props = new Properties();
        try {
            props.load(in);
            in.close();
        } catch (IOException e) {
            throw new ResourceException(e);
        }
        return props;
    }
}
