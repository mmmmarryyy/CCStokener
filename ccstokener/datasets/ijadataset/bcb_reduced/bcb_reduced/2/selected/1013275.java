package com.valueteam.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author maurizio
 *
 */
public class PropertiesManager {

    private static Log log = LogFactory.getLog(PropertiesManager.class);

    private static java.util.Properties prop = new java.util.Properties();

    private PropertiesManager() {
        loadProperties();
    }

    private static PropertiesManager instance = null;

    public static PropertiesManager getInstance() {
        if (instance == null) {
            instance = new PropertiesManager();
        }
        return instance;
    }

    public String getApplicationProperties(String key) {
        log.debug("getApplicationProperties START - invoked for key: " + key);
        Object responseMessage = prop.get(key);
        log.debug("getApplicationProperties END - invoked for key: " + key + " returns value: " + responseMessage);
        return "" + responseMessage;
    }

    private void loadProperties() {
        try {
            ClassLoader loader = PropertiesManager.class.getClassLoader();
            if (loader == null) loader = ClassLoader.getSystemClassLoader();
            String propFile = "application.properties";
            java.net.URL url = loader.getResource(propFile);
            try {
                prop.load(url.openStream());
            } catch (Exception e) {
                log.error("Could not load configuration file: " + propFile);
            }
        } catch (Exception e) {
            log.error("ERROR in loading properties file.");
            e.printStackTrace();
        }
    }
}
