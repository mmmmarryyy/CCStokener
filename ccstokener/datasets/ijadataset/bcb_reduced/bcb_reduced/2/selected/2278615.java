package org.configora.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @auther: sandeep dixit.<a href="mailto:sandeep.dixit@ugs.com">sandeep.dixit@ugs.com</a>
 * @Date: Apr 19, 2008
 * @Time: 2:48:41 AM
 */
public class ResourceLocator {

    private static Logger log = Logger.getLogger(ResourceLocator.class.toString());

    /** Try to locate a local URL representing the incoming path.  The first attempt
	 * assumes that the incoming path is an actual URL string (file://, etc).  If this
	 * does not work, then the next attempts try to locate this UURL as a java system
	 * resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
    protected static final URL locateURL(final String path) {
        try {
            return new URL(path);
        } catch (MalformedURLException e) {
            log.info("could not locate " + path + " as URL. locating as resource");
            return locateURLAsResource(path);
        }
    }

    /**
	 * Try to locate a local URL representing the incoming path.
	 * This method <b>only</b> attempts to locate this URL as a
	 * java system resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
    protected static final URL locateURLAsResource(final String path) {
        URL url = null;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            url = contextClassLoader.getResource(path);
        }
        if (url != null) return url;
        url = ResourceLocator.class.getClassLoader().getResource(path);
        if (url != null) return url;
        url = ClassLoader.getSystemClassLoader().getResource(path);
        return url;
    }

    /** Open an InputStream to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateURL(java.lang.String)} in order to find an appropriate URL.
	 * {@link java.net.URL#openStream()} is then called to obtain the stream.
	 *
	 * @param path The path representing the config location.
	 * @return An input stream to the requested config resource.
	 * @throws ConfigurationException Unable to open stream to that resource.
	 */
    public static final InputStream getInputStreamForURL(final String path) throws ConfigurationException {
        final URL url = ResourceLocator.locateURL(path);
        if (url == null) {
            String msg = "Unable to locate config file: " + path;
            log.log(Level.SEVERE, msg);
            throw new ConfigurationException(msg);
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new ConfigurationException("Unable to open config file: " + path, e);
        }
    }
}
