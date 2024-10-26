package mobi.ilabs.restroom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * RestRoom configurations, all of them gotten from a properties-file named
 * "restroom.properties" somewhere in the classpath.
 */
public final class Config {

    /**
     * The file where we find the configuration properties
     * for the restroom restlet.  The file must be located
     * somewhere in the classpath.
     */
    private static final String RESTROOM_PROPERTIES_FILENAME = "restroom.properties";

    /**
     * The actual properties.
     */
    private Properties properties;

    /**
     * Config is implementing a singelton pattern, and
     * this is the instance.
     */
    private static Config myInstance = null;

    /**
     * Create a new config object, getting the properties from the resource
     * given as parameter.
     * @param resourceName the properties-resource
     */
    private Config(final String resourceName) {
        loadProperties(resourceName);
    }

    /**
     * Get the properties.
     * @return properties
     */
    private static synchronized Properties getProperties() {
        return getInstance().properties;
    }

    /**
     * Retrieve the properties  from a particular resource with
     * a particular filename stored somewhere in the classpath.
     * @param propertyFilename where to get the properties
     */
    private void loadProperties(final String propertyFilename) {
        final URL url = ClassLoader.getSystemResource(propertyFilename);
        if (url == null) {
            throw new RuntimeException("Couldn't find properties in classpath: \"" + propertyFilename + "\"");
        }
        properties = new Properties();
        try {
            InputStream s = url.openStream();
            properties.load(s);
            s.close();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load properties from URL " + url);
        }
    }

    /**
     * Implementing the getter part of the singelton pattern.
     * @return the instance
     */
    public static synchronized Config getInstance() {
        if (myInstance == null) {
            myInstance = new Config(RESTROOM_PROPERTIES_FILENAME);
        }
        return myInstance;
    }

    private static String getProp(final String propname) {
        final Properties p = getProperties();
        if (p.containsKey(propname)) {
            return p.getProperty(propname);
        } else {
            return null;
        }
    }

    /**
     * Return an integer property, and null if it doesn't exist.
     * @param propname The name of the property we are querying.
     * @return The property value. Null if not specified in the properties storage.
     */
    private static Integer getIntProp(final String propname) {
        final String p = getProp(propname);
        if (p == null) {
            return null;
        } else {
            return new Integer(p);
        }
    }

    /**
     * Get the root URI for the static files served by the restroom server.
     *
     * @return the root URI
     */
    public static String getDocsHome() {
        return getProp("restroom.docs-home");
    }

    /**
     * Get the root location for the restroom server.
     * @return the root location
     */
    public static String getRootLocation() {
        return getProp("restroom.rootLocation");
    }

    /**
     * Get the name of the logger that will be used to log for this application.
     * @return the logger name
     */
    public static String getLoggerName() {
        return getProp("restroom.loggerName");
    }

    /**
     * The password to unlock the keystore.
     * @return password
     */
    public static String getKeystorePassword() {
        return getProp("restroom.keystorePassword");
    }

    /**
     *  Noelios code indicates that this should work:
     *  keystoreFile.toURI().toASCIIString()
     *  but I don't know what that means yet. Currently
     *  path location is implementd using a static string
     *  only.
     *  @return the path
     */
    public static String getKeystorePath() {
        return getProp("restroom.keystorePath");
    }

    /**
     * The password to unlock the key.
     * @return password
     */
    public static String getKeyPassword() {
        return getProp("restroom.keyPassword");
    }

    /**
     * The password to unlock the key.
     * @return password
     */
    public static String getGoogleXmppServerHostname() {
        return getProp("restroom.GoogleXmppServerHostname");
    }

    /**
     * The persistence unit providing persistence for the metadata,
     * and in some cases also the "real" data.
     * @return name of the persistence unit
     */
    public static String getJpaPersistenceUnit() {
        return getProp("restroom.jpaPersistenceUnit");
    }

    /**
     * The number of bytes used to store content elements in an LRU cache.
     * @return number of bytes
     */
    public static Integer getContentLruCacheSize() {
        return getIntProp("restroom.contentLruBytes");
    }
}
