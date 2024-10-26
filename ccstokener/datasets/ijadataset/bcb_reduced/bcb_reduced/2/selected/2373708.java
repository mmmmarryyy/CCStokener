package org.isqlviewer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Properties;
import javax.crypto.KeyGenerator;
import org.apache.log4j.Logger;
import org.isqlviewer.xml.SaxResolver;
import org.xml.sax.EntityResolver;

/**
 * Toolkit for providing generally static information regarding the iSQL-Viewer application.
 * <p>
 * 
 * @author Mark A. Kobold &lt;mkobold at isqlviewer dot com&gt;
 * @version 1.0
 */
public final class IsqlToolkit {

    /**
     * Major version of the iSQL-Viewer code base.
     * <p>
     */
    public static final int VERSION_MAJOR = 3;

    /**
     * Minor version of the iSQL-Viewer code base.
     * <p>
     */
    public static final int VERSION_MINOR = 0;

    /**
     * Build number or patch level version of the iSQL-Viewer code base.
     * <p>
     */
    public static final int VERSION_BUILD = 0;

    /**
     * System property for determining if the iSQL-Viewer MRJ Adapter is installed.
     */
    public static final String PROPERTY_MRJ_ENABLED = "isql.mrj.enabled";

    /**
     * System property for the fully qualified path where iSQL-Viewer stores related files.
     */
    public static final String PROPERTY_HOME = "isql.home";

    /**
     * System property for defining the default location for log files.
     */
    public static final String PROPERTY_LOGGING_HOME = "isql.logging.home";

    /**
     * System property for the fully qualified path for the looking for plugins.
     */
    public static final String PROPERTY_PLUGIN_HOME = "isql.plugin.home";

    /**
     * System property for the current version of iSQL-Viewer currently running.
     */
    public static final String PROPERTY_VERSION = "isql.version";

    /**
     * System property flag to indicate that iSQL-Viewer is running embedded or is controlled by another application.
     */
    public static final String PROPERTY_STANDALONE = "isql.stand-alone";

    /**
     * System property flag that contains the preferences root, this can be useful when running embedded.
     */
    public static final String PROPERTY_DEFAULTS_ROOT = "isql.prefs.root";

    private static final String BookmarkDtdPublicId = "-//iSQL-Viewer.org.//DTD JDBC Bookmarks 2.1.8//EN";

    private static final String BookmarkDtdResourcePath = "/org/isqlviewer/resource/xml/bookmarks.dtd";

    private static final String ServiceDtdPublicId_2 = "-//iSQL-Viewer.org.//DTD JDBC Service Definition 2.1.8//EN";

    private static final String ServiceDtdResourcePath_2 = "/org/isqlviewer/resource/xml/service_2_x.dtd";

    private static final String ServiceDtdPublicId_3 = "-//iSQL-Viewer.org.//DTD JDBC Service Definition 3.0.0//EN";

    private static final String ServiceDtdResourcePath_3 = "/org/isqlviewer/resource/xml/service_3_x.dtd";

    private static final String ENCRYPTION_ALGORITHIM = "DESede";

    private static final File baseDirectory;

    private static final File loggingDirectory;

    private static final File pluginDirectory;

    private static final String DRIVER_DEFINITIONS_FILE = "driver.properties";

    private static final String BOOKMARKS_FILE_NAME = "bookmarks.xml";

    private static final SaxResolver entityResolver;

    private static final Key localEncryptionKey;

    static {
        Properties systemProperties = System.getProperties();
        boolean exists = false;
        File location = null;
        systemProperties.put(PROPERTY_VERSION, getVersionInfo());
        exists = systemProperties.containsKey(PROPERTY_HOME);
        if (!exists) {
            String defaultValue = new File(System.getProperty("user.home"), ".iSQL-Viewer").getAbsolutePath();
            systemProperties.setProperty(PROPERTY_HOME, defaultValue);
        }
        location = new File(systemProperties.getProperty(PROPERTY_HOME));
        location.mkdirs();
        baseDirectory = location;
        exists = systemProperties.containsKey(PROPERTY_LOGGING_HOME);
        if (!exists) {
            String defaultValue = new File(System.getProperty(PROPERTY_HOME), "logs").getAbsolutePath();
            systemProperties.setProperty(PROPERTY_LOGGING_HOME, defaultValue);
        }
        location = new File(systemProperties.getProperty(PROPERTY_LOGGING_HOME));
        location.mkdirs();
        loggingDirectory = location;
        exists = systemProperties.containsKey(PROPERTY_PLUGIN_HOME);
        if (!exists) {
            String defaultValue = new File(System.getProperty(PROPERTY_HOME), "plugins").getAbsolutePath();
            systemProperties.setProperty(PROPERTY_PLUGIN_HOME, defaultValue);
        }
        location = new File(systemProperties.getProperty(PROPERTY_PLUGIN_HOME));
        location.mkdirs();
        pluginDirectory = location;
        exists = systemProperties.containsKey(PROPERTY_DEFAULTS_ROOT);
        if (!exists) {
            String defaultValue = "/org/isqlviewer/preferences";
            systemProperties.setProperty(PROPERTY_DEFAULTS_ROOT, defaultValue);
        }
        exists = systemProperties.containsKey(PROPERTY_STANDALONE);
        if (!exists) {
            String defaultValue = Boolean.FALSE.toString();
            systemProperties.setProperty(PROPERTY_STANDALONE, defaultValue);
        }
        Key existingKey = loadExistingPrivateKey();
        if (existingKey == null) {
            try {
                KeyGenerator generator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHIM);
                localEncryptionKey = generator.generateKey();
                saveEncryptionKey();
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(nsae);
            }
        } else {
            localEncryptionKey = existingKey;
        }
        entityResolver = new SaxResolver();
        entityResolver.register(BookmarkDtdPublicId, BookmarkDtdResourcePath, IsqlToolkit.class);
        entityResolver.register(ServiceDtdPublicId_2, ServiceDtdResourcePath_2, IsqlToolkit.class);
        entityResolver.register(ServiceDtdPublicId_3, ServiceDtdResourcePath_3, IsqlToolkit.class);
    }

    private IsqlToolkit() {
    }

    /**
     * Gets the version string for this application instance.
     * <p>
     * 
     * @return version string for the current instance of iSQL-Viewer.
     */
    public static String getVersionInfo() {
        return VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_BUILD;
    }

    /**
     * Gets the base directory for all iSQL-Viewer system files.
     * <p>
     * This value can also be queried by the System property 'isql.home'. Metaphorically speaking this location is where
     * iSQL-Viewer sets up camp and most files loaded by iSQL-Viewer are loaded relative to this location.
     * 
     * @return File instance where iSQL-Viewer stores its main configuration files.
     */
    public static synchronized File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Gets the default location to look for application plug-ins from.
     * <p>
     * 
     * @return file location where plug-in components are loaded from.
     */
    public static synchronized File getPluginDirectory() {
        return pluginDirectory;
    }

    /**
     * Gets the default location to write log files to.
     * <p>
     * 
     * @return file location where log files are written to.
     */
    public static synchronized File getLoggingDirectory() {
        return loggingDirectory;
    }

    /**
     * Gets the root node for all application preferences to reside from.
     * <p>
     * 
     * @return location where to store all preferences from.
     * @see java.util.prefs.Preferences
     */
    public static String getRootPreferencesNode() {
        return System.getProperty(PROPERTY_DEFAULTS_ROOT);
    }

    /**
     * Loads the defaults driver class to example URL mappings file.
     * <p>
     * 
     * @return properties file that contains class names to JDBC connection strings.
     */
    public static Properties getDefaultDriverDefinitions() {
        Properties props = new Properties();
        InputStream inputStream = null;
        try {
            URL url = IsqlToolkit.class.getResource("/org/isqlviewer/resource/".concat(DRIVER_DEFINITIONS_FILE));
            inputStream = url.openStream();
            props.load(inputStream);
        } catch (Throwable t) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return props;
    }

    public static Logger getApplicationLogger() {
        return Logger.getLogger("org.isqlviewer");
    }

    /**
     * Gets an instance of an entity resolver for this runtime.
     * <p>
     * This entity resolver is setup to resolve all the iSQL-Viewer specific DTDs.
     * 
     * @return shared instance of an entity resolver.
     */
    public static EntityResolver getSharedEntityResolver() {
        return entityResolver;
    }

    /**
     * Gets the default file location for the iSQL-Viewer bookmarks XML file.
     * <p>
     * 
     * @return file location of the iSQL-Viewer bookmarks file.
     */
    public static File getDefaultBookmarksFile() {
        return new File(IsqlToolkit.getBaseDirectory(), BOOKMARKS_FILE_NAME);
    }

    public static Key getEncryptionKey() {
        return localEncryptionKey;
    }

    private static void saveEncryptionKey() {
        String fileName = MessageFormat.format(".{0}.key", new Object[] { ENCRYPTION_ALGORITHIM });
        File keyFile = new File(getBaseDirectory(), fileName);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(keyFile);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
            objectOutput.writeObject(localEncryptionKey);
            objectOutput.flush();
        } catch (IOException ignored) {
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Key loadExistingPrivateKey() {
        String fileName = MessageFormat.format(".{0}.key", new Object[] { ENCRYPTION_ALGORITHIM });
        File keyFile = new File(getBaseDirectory(), fileName);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keyFile);
            ObjectInputStream objectInput = new ObjectInputStream(inputStream);
            return (Key) objectInput.readObject();
        } catch (IOException ignored) {
        } catch (ClassNotFoundException ignored) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
