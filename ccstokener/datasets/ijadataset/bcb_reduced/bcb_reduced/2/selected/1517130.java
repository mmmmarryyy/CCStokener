package net.sourceforge.jdbclogger.core.util;

import net.sourceforge.jdbclogger.core.config.Environment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * A simple class to centralize logic needed to locate config files on the system.
 *
 * @author Catalin Kormos (latest modification by $Author: catalean $)
 * @version $Revision: 83 $ $Date: 2007-07-07 17:00:58 -0400 (Sat, 07 Jul 2007) $
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

    /** Open an InputStream to the URL represented by the incoming path.  First makes a call
     * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
     * {@link java.net.URL#openStream()} is then called to obtain the stream.
     *
     * @param path The path representing the config location.
     * @return An input stream to the requested config resource.     
     */
    public static final InputStream getConfigStream(final String path) {
        final URL url = ConfigHelper.locateConfig(path);
        if (url == null) {
            String msg = "Unable to locate config file: " + path;
            log.error(msg);
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            log.error("Unable to open config file: " + path, e);
        }
        return null;
    }

    /** Open an Reader to the URL represented by the incoming path.  First makes a call
     * to {@link #locateConfig(java.lang.String)} in order to find an appropriate URL.
     * {@link java.net.URL#openStream()} is then called to obtain a stream, which is then
     * wrapped in a Reader.
     *
     * @param path The path representing the config location.
     * @return An input stream to the requested config resource.     
     */
    public static final Reader getConfigStreamReader(final String path) {
        return new InputStreamReader(getConfigStream(path));
    }

    /** Loads a properties instance based on the data at the incoming config location.
     *
     * @param path The path representing the config location.
     * @return The loaded properties instance.     
     */
    public static final Properties getConfigProperties(String path) {
        try {
            Properties properties = new Properties();
            properties.load(getConfigStream(path));
            return properties;
        } catch (IOException e) {
            log.error("Unable to load properties from specified config file: " + path, e);
        }
        return null;
    }

    private ConfigHelper() {
    }

    public static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;
        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            Environment.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = Environment.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            log.error(resource + " not found");
        }
        return stream;
    }

    /**
     * @param fileName
     * @return
     */
    public static Enumeration<URL> getResources(String fileName) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            return contextClassLoader.getResources(fileName);
        } catch (IOException e) {
            log.error("No custom drivers found");
        }
        return null;
    }
}
