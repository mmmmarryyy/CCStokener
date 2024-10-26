package org.ikasan.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * A utility class for general resource IO activities
 * 
 * @author Ikasan Development Team
 */
public class ResourceUtils {

    /** The logger instance. */
    private static Logger logger = Logger.getLogger(ResourceUtils.class);

    /**
     * Load properties from the given name. Try loading these properties in the following order, (1) load as XML
     * properties from the classpath; (2) load as NVP properties from the classpath; (3) load as XML properties from the
     * file system; (4) load as NVP properties from the file system; If all above fail then throw IOException.
     * 
     * @param name - properties name
     * @return Properties
     * @throws IOException - IOException in case of failure
     */
    public static Properties getAsProperties(final String name) throws IOException {
        Properties props = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(name);
        if (url != null) {
            try {
                props.loadFromXML(url.openStream());
            } catch (InvalidPropertiesFormatException e) {
                props.load(url.openStream());
            }
        } else {
            try {
                props.loadFromXML(new FileInputStream(name));
            } catch (InvalidPropertiesFormatException e) {
                props.load(new FileInputStream(name));
            }
        }
        return props;
    }

    /**
     * Load given resource and return as an input stream.
     * 
     * Firstly try loading from the classpath, if this fails try loading from the file system. If this fails throw an
     * IOException.
     * 
     * @param name - resource name
     * @return InputStream
     * @throws IOException - Exception if we cannot read source
     */
    public static InputStream loadResource(final String name) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(name);
        if (url != null) {
            return url.openStream();
        }
        return new FileInputStream(name);
    }

    /**
     * Load URL from the classpath. If not found the url will be returned as null.
     * 
     * @param name - resource name
     * @return URL
     */
    public static URL getAsUrl(final String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader.getResource(name);
    }

    /**
     * Load URL from the classpath. If not found the url will be returned as null.
     * 
     * @param name - properties name
     * @param screamOnFail - throw an IOException if we fail to load the resource name.
     * @return URL
     * @throws IOException - Exception if we cannot read
     */
    public static URL getAsUrl(final String name, final boolean screamOnFail) throws IOException {
        URL url = getAsUrl(name);
        if (url == null && screamOnFail) {
            throw new IOException("Failed to load resource [" + name + "]. Not found on classpath.");
        }
        return url;
    }
}
