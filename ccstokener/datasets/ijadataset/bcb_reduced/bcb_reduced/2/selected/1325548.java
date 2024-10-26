package org.homemotion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

public final class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    private static Configuration config = new Configuration("homemotion.properties");

    private Map<URL, Properties> configurations = new HashMap<URL, Properties>();

    private Configuration(String resource) {
        Enumeration<URL> configurations;
        try {
            configurations = getClass().getClassLoader().getResources(resource);
        } catch (IOException e1) {
            LOG.fatal("Could not read config.");
            return;
        }
        while (configurations.hasMoreElements()) {
            Properties properties = new Properties();
            InputStream is = null;
            URL url = null;
            try {
                url = configurations.nextElement();
                is = url.openStream();
                properties.load(is);
                this.configurations.put(url, properties);
            } catch (Exception e) {
                LOG.error("Could not load configuration " + url, e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        LOG.warn("Error closing resource " + url, e);
                    }
                }
            }
        }
    }

    public static Configuration getConfiguration() {
        return config;
    }

    public List<String> getValues(String key) {
        List<String> result = new ArrayList<String>(configurations.size());
        for (Properties properties : configurations.values()) {
            String value = properties.getProperty(key);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    public <T> List<T> getConfiguredInstances(String key, Class<T> type) {
        List<Class<T>> classes = getConfiguredClasses(key, type);
        List<T> result = new ArrayList<T>(30);
        for (Class<T> clazz : classes) {
            try {
                LOG.debug("Instantiating '" + clazz + "', using default constructor...");
                result.add(clazz.newInstance());
                LOG.info("'" + clazz + "' instantiated successfully.");
            } catch (Exception e) {
                LOG.error("Creation of '" + clazz + "' failed.", e);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> List<Class<T>> getConfiguredClasses(String key, Class<T> type) {
        LOG.debug("Loading '" + key + "'...");
        List<Class<T>> result = new ArrayList<Class<T>>(30);
        List<String> configs = Configuration.getConfiguration().getValues(key);
        for (String config : configs) {
            String[] classes = config.split(",");
            for (int i = 0; i < classes.length; i++) {
                classes[i] = classes[i].trim();
                try {
                    if (classes[i].trim().isEmpty()) {
                        continue;
                    }
                    LOG.debug("Loading '" + classes[i] + "' ...");
                    Class targetClass = Class.forName(classes[i].trim(), true, Thread.currentThread().getContextClassLoader());
                    if (type == null || type.isAssignableFrom(targetClass)) {
                        result.add(targetClass);
                        LOG.info("'" + classes[i] + "' loaded successfully.");
                    }
                } catch (Exception e) {
                    LOG.error("Load of '" + classes[i] + "' failed.", e);
                }
            }
        }
        return result;
    }

    public String getValue(Class<?> clazz, String key) {
        return getValue(clazz.getName() + '.' + key, null);
    }

    public String getValue(Class<?> clazz, String key, String def) {
        return getValue(clazz.getName() + '.' + key, def);
    }

    public String getValue(String key) {
        return getValue(key, null);
    }

    public String getValue(String key, String def) {
        List<String> result = new ArrayList<String>(configurations.size());
        for (Properties properties : configurations.values()) {
            String value = properties.getProperty(key);
            if (value != null) {
                result.add(value);
            }
        }
        if (result.size() == 0) {
            return def;
        } else if (result.size() == 1) {
            return result.get(0);
        }
        throw new IllegalStateException("Single value '" + key + "' configured multiple times.");
    }

    public static String get(Class<?> clazz, String key) {
        return getConfiguration().getValue(clazz.getName() + '.' + key);
    }

    public static String get(Class<?> clazz, String key, String def) {
        return getConfiguration().getValue(clazz.getName() + '.' + key, def);
    }

    public static String get(String key) {
        return getConfiguration().getValue(key);
    }

    public static String get(String key, String def) {
        return getConfiguration().getValue(key, def);
    }

    public static long getLong(Class<?> clazz, String key) {
        return getLong(clazz, key, 0L);
    }

    public static long getLong(Class<?> clazz, String key, long defaultValue) {
        String value = getConfiguration().getValue(clazz, key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                LOG.error("Problem converting to long: " + value, e);
            }
        }
        return defaultValue;
    }
}
