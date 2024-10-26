package org.cloudlet.web.boot.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BootUtil {

    private static final Logger logger = Logger.getLogger(BootUtil.class.getName());

    private static final String SYSTEM_PROPERTIES = "META-INF/cfg/system.properties";

    static void restoreSystemProperties() {
        Properties props = loadPropertiesFromClasspath(SYSTEM_PROPERTIES);
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            if (System.getProperties().containsKey(key)) {
                String originalValue = System.getProperty(key);
                System.clearProperty(key);
                logger.log(Level.INFO, "override system property " + key + " from " + originalValue + " to " + value);
            } else {
                logger.log(Level.FINER, "set system property " + key + " = " + value);
            }
            System.setProperty(key, value);
        }
    }

    /**
   * Loads all properties from classpath.
   */
    private static Properties loadPropertiesFromClasspath(String path) {
        Enumeration<URL> locations;
        Properties props = new Properties();
        try {
            locations = Thread.currentThread().getContextClassLoader().getResources(path);
            while (locations.hasMoreElements()) {
                URL url = locations.nextElement();
                InputStream in = url.openStream();
                props.load(in);
                in.close();
                logger.config("Load properties from " + url);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "load properties from classpath \"" + path + "\" failed", e);
        }
        return props;
    }
}
