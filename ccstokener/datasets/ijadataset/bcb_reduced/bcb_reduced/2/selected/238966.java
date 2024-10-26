package jhomenet.commons.cfg;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jhomenet.commons.JHomenetException;

/**
 * A simple class to centralize logic needed to locate config files on the system.
 *
 * @author Steve
 */
public final class ConfigHelper {

    private static final Log log = LogFactory.getLog(ConfigHelper.class);

    /** Try to locate a local URL representing the incoming path.  The first attempt
	 * assumes that the incoming path is an actual URL string (file://, etc).  If this
	 * does not work, then the next attempts try to locate this UURL as a java system
	 * resource.
	 *
	 * @param path The path representing the config location.
	 * @return An appropriate URL or null.
	 */
    public static final URL locateConfig(final String path) {
        try {
            return new URL(path);
        } catch (MalformedURLException e) {
            return findAsResource(path);
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
    public static final URL findAsResource(final String path) {
        URL url = null;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            url = contextClassLoader.getResource(path);
        }
        if (url != null) return url;
        url = ConfigHelper.class.getClassLoader().getResource(path);
        if (url != null) return url;
        url = ClassLoader.getSystemClassLoader().getResource(path);
        return url;
    }

    /**
	 * Open an InputStream to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
	 * {@link java.net.URL#openStream()} is then called to obtain the stream.
	 *
	 * @param path The path representing the config location.
	 * @return An input stream to the requested config resource.
	 * @throws JHomenetException Unable to open stream to that resource.
	 */
    public static final InputStream getConfigStream(final String path) throws JHomenetException {
        final URL url = ConfigHelper.locateConfig(path);
        if (url == null) {
            String msg = "Unable to locate config file: " + path;
            log.fatal(msg);
            throw new JHomenetException(msg);
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new JHomenetException("Unable to open config file: " + path, e);
        }
    }

    /**
	 * Open an Reader to the URL represented by the incoming path.  First makes a call
	 * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
	 * {@link java.net.URL#openStream()} is then called to obtain a stream, which is then
	 * wrapped in a Reader.
	 *
	 * @param path The path representing the config location.
	 * @return An input stream to the requested config resource.
	 * @throws JHomenetException Unable to open reader to that resource.
	 */
    public static final Reader getConfigStreamReader(final String path) throws JHomenetException {
        return new InputStreamReader(getConfigStream(path));
    }

    /** Loads a properties instance based on the data at the incoming config location.
	 *
	 * @param path The path representing the config location.
	 * @return The loaded properties instance.
	 * @throws JHomenetException Unable to load properties from that resource.
	 */
    public static final Properties getConfigProperties(String path) throws JHomenetException {
        try {
            Properties properties = new Properties();
            properties.load(getConfigStream(path));
            return properties;
        } catch (IOException e) {
            throw new JHomenetException("Unable to load properties from specified config file: " + path, e);
        }
    }

    private ConfigHelper() {
    }

    /**
	 * 
	 * @param resourcePath
	 * @return
	 */
    public static final InputStream getResourceAsStream(String resource) {
        String resourcePath = Environment.getConfigFolderRelative() + Environment.SEPARATOR + resource;
        String stripped = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            stream = Environment.class.getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            stream = Environment.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            try {
                stream = new FileInputStream(Environment.getConfigFolderAbsolute() + Environment.SEPARATOR + resource);
            } catch (FileNotFoundException fnfe) {
                throw new JHomenetException("Unable to locate resource: " + resourcePath);
            }
        }
        return stream;
    }

    public static InputStream getUserResourceAsStream(String resource) {
        boolean hasLeadingSlash = resource.startsWith("/");
        String stripped = hasLeadingSlash ? resource.substring(1) : resource;
        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(resource);
            if (stream == null && hasLeadingSlash) {
                stream = classLoader.getResourceAsStream(stripped);
            }
        }
        if (stream == null) {
            stream = Environment.class.getClassLoader().getResourceAsStream(resource);
        }
        if (stream == null && hasLeadingSlash) {
            stream = Environment.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new JHomenetException(resource + " not found");
        }
        return stream;
    }
}
