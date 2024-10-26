package org.nakedobjects.metamodel.config.loader;

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.nakedobjects.metamodel.commons.exceptions.NakedObjectException;
import org.nakedobjects.metamodel.config.ConfigurationConstants;
import org.nakedobjects.metamodel.config.NakedObjectConfiguration;
import org.nakedobjects.metamodel.config.prop.PropertiesConfiguration;
import org.nakedobjects.metamodel.config.reader.propfile.PropertiesFileReader;

/**
 * Loads in the specified configuration files. Properties in the later files overrides properties in earlier
 * files.
 */
public class ConfigurationLoaderDefault implements ConfigurationLoader {

    private static final Logger LOG = Logger.getLogger(ConfigurationLoaderDefault.class);

    private static final String DEFAULT_CONFIG_DIR = "config";

    private static final String DEFAULT_CONFIG_FILE = "nakedobjects.properties";

    protected static final String SHOW_EXPLORATION_OPTIONS = ConfigurationConstants.ROOT + "exploration.show";

    private boolean includeSystemProperties = false;

    private final Properties additionalProperties = new Properties();

    private final PropertiesConfiguration configuration;

    private final String defaultDirectory;

    public ConfigurationLoaderDefault() {
        this(null);
    }

    public ConfigurationLoaderDefault(final String dir) {
        this.defaultDirectory = dir == null ? DEFAULT_CONFIG_DIR : dir;
        configuration = new PropertiesConfiguration();
        loadDefaultConfigurationFiles();
    }

    public NakedObjectConfiguration load() {
        if (configuration.getString(SHOW_EXPLORATION_OPTIONS) == null) {
            configuration.add(SHOW_EXPLORATION_OPTIONS, "yes");
        }
        if (includeSystemProperties) {
            configuration.add(System.getProperties());
        }
        configuration.add(additionalProperties);
        return configuration;
    }

    /**
     * Adds a configuration file with the specified name from the specified directory (and not the directory
     * that this loader was set up with).
     * 
     * @see #addConfigurationFile(String, NotFoundPolicy)
     */
    public void addConfigurationFile(final String dir, final String fileName, NotFoundPolicy notFoundPolicy) {
        try {
            final PropertiesFileReader loader = new PropertiesFileReader(dir, fileName, notFoundPolicy);
            if (loader.isFound()) {
                configuration.add(loader.getProperties());
            } else {
                addConfigurationResource(fileName, notFoundPolicy);
            }
        } catch (final Exception e) {
            addConfigurationResource(fileName, notFoundPolicy);
        }
    }

    private void addConfigurationResource(final String fileName, NotFoundPolicy notFoundPolicy) {
        try {
            final ClassLoader cl = this.getClass().getClassLoader();
            final Properties p = new Properties();
            final URL url = cl.getResource(fileName);
            if (url == null) {
                throw new NakedObjectException("Failed to load configuration resource: " + fileName);
            }
            p.load(url.openStream());
            LOG.info("configuration resource " + fileName + " loaded");
            configuration.add(p);
        } catch (final Exception e) {
            if (notFoundPolicy == NotFoundPolicy.FAIL_FAST) {
                throw new NakedObjectException(e);
            }
            LOG.info("configuration resource " + fileName + " not found, but not needed");
        }
    }

    /**
     * Adds a configuration file with the specified name from the directory that this loader was set up with.
     * 
     * @see #addConfigurationFile(String, String, NotFoundPolicy)
     */
    public void addConfigurationFile(final String fileName, NotFoundPolicy notFoundPolicy) {
        addConfigurationFile(defaultDirectory, fileName, notFoundPolicy);
    }

    public void loadDefaultConfigurationFiles() {
        addConfigurationFile(DEFAULT_CONFIG_FILE, NotFoundPolicy.FAIL_FAST);
    }

    public void setIncludeSystemProperties(final boolean includeSystemProperties) {
        this.includeSystemProperties = includeSystemProperties;
    }

    public void add(final String key, final String value) {
        if (key != null && value != null) {
            additionalProperties.setProperty(key, value);
            LOG.info("added " + key + "=" + value);
        }
    }

    public void add(final Properties properties) {
        final Enumeration<?> keys = properties.propertyNames();
        while (keys.hasMoreElements()) {
            final String key = (String) keys.nextElement();
            add(key, properties.getProperty(key));
        }
    }
}
