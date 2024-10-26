package org.jmonit.internal;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jmonit.log.Log;
import org.jmonit.spi.Factory;
import org.jmonit.spi.PluginManager;

/**
 * Default implementation of <code>PluginManager</code>.
 * 
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DefaultPluginManager implements PluginManager {

    /**
     *
     */
    private static final String CONFIG = "jmonit.properties";

    /**
     *
     */
    private static final String DEFAULT = "META-INF/jmonit/default.properties";

    /**
     *
     */
    private static final String PLUGINS = "META-INF/jmonit/plugins.properties";

    /** logger */
    private static Log log = Log.getLog(DefaultPluginManager.class);

    Map<Class, Factory> features = new ConcurrentHashMap<Class, Factory>();

    Map<String, Collection<Class>> featuresForGroup = new ConcurrentHashMap<String, Collection<Class>>();

    /**
     * Constructor
     */
    public DefaultPluginManager() {
        super();
        try {
            readConfiguration();
        } catch (Exception e) {
            log.error("Failed to load jMonit configuration " + e);
            System.err.println("WARNING : Failed to load jMonit configuration : " + e.getMessage());
        }
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }

    protected void readFeatureGroupsConfiguration(Properties config) {
        for (Map.Entry entry : config.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            Collection<Class> features = featuresForGroup.get(key);
            if (features == null) {
                features = new LinkedList<Class>();
                featuresForGroup.put(key, features);
            }
            String[] plugins = value.split(",");
            for (String plugin : plugins) {
                Class feature = getFeature(plugin);
                if (feature == null) {
                    log.error("Unknown feature " + value + " requested for tag " + key);
                    continue;
                }
                features.add(feature);
                log.info(value + " registered as a feature for monitor group " + key);
            }
        }
    }

    protected void readConfiguration() throws Exception {
        Properties props = new Properties();
        Enumeration<URL> modules = getClassLoader().getResources(PLUGINS);
        while (modules.hasMoreElements()) {
            URL url = (URL) modules.nextElement();
            props.load(url.openStream());
        }
        registerFeatures(props);
        props.clear();
        InputStream config = getClassLoader().getResourceAsStream(DEFAULT);
        if (config != null) {
            props.load(config);
        }
        config = getClassLoader().getResourceAsStream(CONFIG);
        if (config != null) {
            props.load(config);
        }
        readFeatureGroupsConfiguration(props);
    }

    protected void registerFeatures(Properties config) throws Exception {
        Set keys = config.keySet();
        for (Iterator iterator = keys.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            try {
                Class feature = Class.forName(key);
                Factory factory = null;
                String property = config.getProperty(key);
                int dash = property.indexOf("#");
                String field = null;
                if (dash > 0) {
                    field = property.substring(dash + 1).trim();
                    property = property.substring(0, dash);
                }
                Class factoryClass = Class.forName(property);
                if (field != null) {
                    Field f = factoryClass.getField(field);
                    if (!Factory.class.isAssignableFrom(f.getType())) {
                        log.error(property + " in class  " + factoryClass + " is not a plugin Factory");
                        log.error(key + " feature will be disabled");
                        continue;
                    }
                    factory = (Factory) f.get(factoryClass);
                } else {
                    if (!Factory.class.isAssignableFrom(factoryClass)) {
                        log.error(factoryClass + " is not a plugin Factory");
                        log.error(key + " feature will be disabled");
                        continue;
                    }
                    factory = (Factory) factoryClass.newInstance();
                }
                registerPlugin(factory, feature);
            } catch (Exception e) {
                log.error("Fail to register " + key + " : " + e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jmonit.spi.PluginManager#registerPlugin(org.jmonit.spi.Factory,
     * Class)
     */
    public void registerPlugin(Factory factory, Class role) {
        if (log.isDebugEnabled()) {
            String name = factory.getClass().getName();
            log.info("register plugin factory " + name + " for feature " + role);
        }
        features.put(role, factory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jmonit.spi.PluginManager#getFactory(java.lang.Class)
     */
    public <T> Factory<T> getFactory(Class<T> feature) {
        return features.get(feature);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jmonit.spi.PluginManager#getFeatures(java.lang.String)
     */
    public Collection<Class> getFeatures(String tag) {
        if (tag == null) {
            return Collections.<Class>emptySet();
        }
        Collection<Class> features = featuresForGroup.get(tag);
        return features != null ? features : Collections.<Class>emptySet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jmonit.spi.PluginManager#getFeature(java.lang.String)
     */
    public Class getFeature(String name) {
        if (name != null) {
            try {
                return Class.forName("org.jmonit.features." + name);
            } catch (ClassNotFoundException e) {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException ex) {
                    log.error("Unknown feature requested " + name);
                }
            }
        }
        return null;
    }
}
