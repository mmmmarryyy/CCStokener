package com.icteam.fiji.configuration;

import com.icteam.fiji.util.LoadingUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class ConfigurationProperties implements Configuration {

    private static Log logger = LogFactory.getLog(ConfigurationProperties.class.getName());

    private static final String DEFAULT_PREFIX = "conf/";

    public static enum Type {

        XML(".xml"), PLAINTEXT(".properties");

        private String m_extension = null;

        private Type(String p_extension) {
            m_extension = p_extension;
        }

        public String getExtension() {
            return m_extension;
        }
    }

    private Type m_type = Type.PLAINTEXT;

    private String m_filePath = null;

    private Properties m_properties = null;

    private String prefix;

    public ConfigurationProperties(String p_filePath) {
        prefix = DEFAULT_PREFIX;
        m_filePath = prefix + p_filePath;
        m_properties = loadProperties();
    }

    public ConfigurationProperties(String p_filePath, Type p_type) {
        m_type = p_type;
        m_filePath = prefix + p_filePath;
        m_properties = loadProperties();
    }

    public ConfigurationProperties(String p_filePath, Type p_type, String p_prefix) {
        m_type = p_type;
        prefix = p_prefix;
        m_filePath = prefix + p_filePath;
        m_properties = loadProperties();
    }

    public String getName() {
        return m_filePath;
    }

    public String getProperty(String p_arg0, String p_arg1) {
        return m_properties.getProperty(p_arg0, p_arg1);
    }

    public String getProperty(String p_arg0) {
        return m_properties.getProperty(p_arg0);
    }

    public Enumeration<?> propertyNames() {
        return m_properties.propertyNames();
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.putAll(m_properties);
        return properties;
    }

    private Properties loadProperties() {
        Properties defaultProperties = System.getProperties();
        String jbossHome = System.getProperty("jboss.server.home.dir");
        if (jbossHome == null) logger.info("Undefined 'jboss.server.home.dir' system property.");
        if (jbossHome != null) {
            try {
                File file = new File(jbossHome, m_filePath);
                if (file.exists()) {
                    logger.info("Loading " + file.getAbsolutePath());
                    final Properties properties = loadProperties(new FileInputStream(file), defaultProperties);
                    logger.info("Loaded " + file.getAbsolutePath() + " from " + jbossHome);
                    return properties;
                }
            } catch (IOException e) {
                logger.fatal("Unable to load " + m_filePath + " from " + jbossHome + ". " + e.getMessage());
            }
        }
        return loadPropertiesFromClasspath(defaultProperties);
    }

    private Properties loadProperties(InputStream p_stream, Properties p_defaultProperties) throws IOException {
        Properties props = new Properties();
        if (p_defaultProperties != null) props.putAll(p_defaultProperties);
        if (p_stream != null) if (m_type == Type.PLAINTEXT) {
            props.load(p_stream);
        } else {
            props.loadFromXML(p_stream);
        } else logger.warn("Unable to load " + m_filePath + " from classpath. ");
        return props;
    }

    private Properties loadPropertiesFromClasspath(Properties p_defaultProperties) {
        Properties props = new Properties();
        if (p_defaultProperties != null) props.putAll(p_defaultProperties);
        try {
            Enumeration<URL> en = LoadingUtils.getResources(m_filePath);
            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                logger.info("Found file " + url.toExternalForm());
                InputStream inputStream = null;
                try {
                    inputStream = url.openStream();
                    logger.info("Opening file " + url.toExternalForm());
                    if (inputStream != null) {
                        props = loadProperties(inputStream, props);
                        logger.info("Loaded " + url.getFile() + " from CLASSPATH " + url.getPath());
                    }
                } catch (IOException e) {
                    logger.warn("Unable to load  \"" + url.toExternalForm() + "\" from CLASSPATH.", e);
                } finally {
                    try {
                        if (inputStream != null) inputStream.close();
                    } catch (IOException e) {
                        logger.debug(e);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Unable to retrieve resource " + m_filePath + " due to a " + e.getClass().getName() + " with message '" + e.getMessage() + "'");
        }
        return props;
    }

    /**
     * Ricarica tutte le properties.
     */
    public synchronized void reload() {
        m_properties = loadProperties();
    }

    public class Property<T> {

        private String propertyName = null;

        private T m_value = null;

        public Property(Class<T> p_class, String p_defaultValue) {
            parse(p_class, p_defaultValue);
        }

        public String getName() {
            return propertyName;
        }

        public Property(Class<T> p_class, String p_property, String p_defaultValue) {
            propertyName = p_property;
            String value = null;
            if (p_property != null) value = getProperty(p_property);
            if (value == null && p_defaultValue != null) value = p_defaultValue;
            if (value != null) parse(p_class, value.trim());
        }

        public T value() {
            return m_value;
        }

        protected void parse(Class<T> p_class, String p_value) {
            try {
                Constructor<T> ctor = p_class.getConstructor(String.class);
                m_value = ctor.newInstance(p_value);
            } catch (SecurityException e) {
                logger.error(e);
            } catch (NoSuchMethodException e) {
                logger.error(e);
            } catch (IllegalArgumentException e) {
                logger.error(e);
            } catch (InstantiationException e) {
                logger.error(e);
            } catch (IllegalAccessException e) {
                logger.error(e);
            } catch (InvocationTargetException e) {
                logger.error(e);
            }
        }
    }
}
