package net.sf.cantina.application;

import net.sf.cantina.util.Loader;
import org.apache.log4j.Logger;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;

/**
 * cantina.properties configuration.
 * @author Stephane JAIS
 */
public class Config extends Hashtable {

    private static final String CONFIG_FILENAME = "cantina.properties";

    /**
   * This setting determines which datasource will be used by the system,
   * Valid values are a class name that implements DataSource
   */
    public static final String DS_TYPE = "datasource.type";

    private static final Logger logger = Logger.getLogger(Config.class);

    protected static Properties properties = loadProperties(Loader.getResource(CONFIG_FILENAME));

    /**
   * Loads properties stored in a file.
   * @param url The url to the file
   * @return The Properties object.
   */
    protected static Properties loadProperties(URL url) {
        Properties result = new Properties();
        logger.debug("Reading configuration from URL " + url);
        try {
            result.load(url.openStream());
        } catch (java.io.IOException e) {
            logger.error("Could not read configuration file from URL [" + url + "].", e);
        }
        return result;
    }

    /**
   * Get a config setting
   * @param paramName The setting's name
   * @return The value
   */
    protected static String getString(String paramName) {
        return properties.getProperty(paramName);
    }

    /**
    * Helper method that returns true for 
    * &quot;true&quot; and &quot;yes&quot;
    */
    protected static boolean getBoolean(String paramName) {
        String paramValue = getString(paramName);
        if (paramValue == null) return false;
        if (paramValue.equals("true") || paramValue.equals("yes")) return true;
        return false;
    }
}
