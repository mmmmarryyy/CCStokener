package org.nakedobjects.nof.core.conf;

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.nakedobjects.noa.NakedObjectRuntimeException;
import org.nakedobjects.nof.core.system.ConfigurationLoader;
import org.nakedobjects.nof.core.util.NakedObjectConfiguration;

/**
 * Loads in the specified configuration files. Properties in the later files overrides properties in earlier
 * files.
 */
public class DefaultConfigurationLoader implements ConfigurationLoader {

    private static final Logger LOG = Logger.getLogger(DefaultConfigurationLoader.class);

    private static final String DEFAULT_CONFIG_DIR = "config";

    private static final String DEFAULT_CONFIG_FILE = "nakedobjects.properties";

    protected static final String SHOW_EXPLORATION_OPTIONS = Configuration.ROOT + "exploration.show";

    private boolean includeSystemProperties = false;

    private final Properties additionalProperties = new Properties();

    private final PropertiesConfiguration configuration;

    private final String defaultDirectory;

    public DefaultConfigurationLoader() {
        this(DEFAULT_CONFIG_DIR);
    }

    public DefaultConfigurationLoader(final String dir) {
        this.defaultDirectory = dir;
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
     * that this loader was set up with). If the <code>ensureLoaded<code>
     * flag is set then the method will fail fast if the file cannot be found.
     * 
     * @see #addConfigurationFile(String, boolean)
     */
    public void addConfigurationFile(final String dir, final String fileName, final boolean ensureLoaded) {
        try {
            PropertiesFileReader loader = new PropertiesFileReader(dir, fileName, ensureLoaded);
            if (loader.getFound()) {
                configuration.add(loader.getProperties());
            } else {
                addConfigurationResource(fileName, ensureLoaded);
            }
        } catch (Exception e) {
            addConfigurationResource(fileName, ensureLoaded);
        }
    }

    private void addConfigurationResource(final String fileName, final boolean ensureLoaded) {
        try {
            final ClassLoader cl = this.getClass().getClassLoader();
            final Properties p = new Properties();
            final URL url = cl.getResource(fileName);
            if (url == null) {
                throw new NakedObjectRuntimeException("Failed to load configuration resource: " + fileName);
            }
            p.load(url.openStream());
            configuration.add(p);
        } catch (Exception e) {
            if (ensureLoaded) {
                throw new NakedObjectRuntimeException(e);
            }
            LOG.debug("Resource: " + fileName + " not found, but not needed");
        }
    }

    /**
     * Adds a configuration file with the specified name from the directory that this loader was set up with.
     * If the <code>ensureLoaded<code>
     * flag is set then the method will fail fast if the file cannot be found.
     * 
     * @see #addConfigurationFile(String, String, boolean)
     */
    public void addConfigurationFile(final String fileName, final boolean ensureLoaded) {
        addConfigurationFile(defaultDirectory, fileName, ensureLoaded);
    }

    public void loadDefaultConfigurationFiles() {
        addConfigurationFile(DEFAULT_CONFIG_FILE, true);
    }

    public void setIncludeSystemProperties(boolean includeSystemProperties) {
        this.includeSystemProperties = includeSystemProperties;
    }

    public void add(String property, String value) {
        additionalProperties.setProperty(property, value);
        LOG.debug("added " + property + "=" + value);
    }

    public void add(Properties properties) {
        Enumeration keys = properties.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            add(key, properties.getProperty(key));
        }
    }
}
