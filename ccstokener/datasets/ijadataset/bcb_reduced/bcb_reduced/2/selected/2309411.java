package org.argouml.application.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Properties;
import org.argouml.application.api.Argo;
import org.argouml.application.api.Configuration;

/**
 *  This class provides a user configuration based upon properties files.
 * Eventually this configuration file will be
 * available to users via a GUI interface to
 * set keyboards
 * memory allocations
 * which modules to load
 * user preferences
 * font sizes
 * user names and data
 * etc.*
 * @author Thierry Lach
 */
public class ConfigurationProperties extends ConfigurationHandler {

    /** The location of Argo's default properties resource.
   */
    private static String PROPERTIES = "/org/argouml/resource/default.properties";

    /** The primary property bundle.
   */
    protected Properties _properties = null;

    /** Flag to ensure that only the first load failure is reported
   *  even though we keep trying because the file or URL may only
   *  be temporarily unavailable.
   */
    private boolean _canComplain = true;

    /** Anonymous constructor.
   */
    public ConfigurationProperties() {
        super(true);
        Properties defaults = new Properties();
        try {
            defaults.load(getClass().getResourceAsStream(PROPERTIES));
            Configuration.cat.debug("Configuration loaded from " + PROPERTIES);
        } catch (Exception ioe) {
            Configuration.cat.warn("Configuration not loaded from " + PROPERTIES, ioe);
        }
        _properties = new Properties(defaults);
    }

    /** Returns the default path for user properties.
   *
   *  @return a generic path string.
   */
    public String getDefaultPath() {
        return System.getProperty("user.home") + "/argo.user.properties";
    }

    /** Load the configuration from a specified location.
   * 
   * @param file  the path to load the configuration from.
   *
   * @return true if the load was successful, false if not.
   */
    public boolean loadFile(File file) {
        try {
            _properties.load(new FileInputStream(file));
            Argo.log.info("Configuration loaded from " + file + "\n");
            return true;
        } catch (Exception e) {
            if (_canComplain) Argo.log.warn("Unable to load configuration " + file + "\n");
            _canComplain = false;
        }
        return false;
    }

    /** Save the configuration to a specified location.
   * 
   * @param file  the path to save the configuration at.
   *
   * @return true if the save was successful, false if not.
   */
    boolean saveFile(File file) {
        try {
            _properties.store(new FileOutputStream(file), "Argo properties");
            Argo.log.info("Configuration saved to " + file);
            return true;
        } catch (Exception e) {
            if (_canComplain) Argo.log.warn("Unable to save configuration " + file + "\n");
            _canComplain = false;
        }
        return false;
    }

    /** Load the configuration from a specified location.
   * 
   * @param url  the path to load the configuration from.
   *
   * @return true if the load was successful, false if not.
   */
    public boolean loadURL(URL url) {
        try {
            _properties.load(url.openStream());
            Argo.log.info("Configuration loaded from " + url + "\n");
            return true;
        } catch (Exception e) {
            if (_canComplain) Argo.log.warn("Unable to load configuration " + url + "\n");
            _canComplain = false;
            return false;
        }
    }

    /** Save the configuration to a specified location.
   * 
   * @param url  the path to save the configuration at.
   *
   * @return true if the save was successful, false if not.
   */
    boolean saveURL(URL url) {
        return false;
    }

    /** Returns the string value of a configuration property.
   *
   *  @param key the key to return the value of.
   *  @param defaultValue the value to return if the key was not found.
   *
   *  @return the string value of the key if found, otherwise null;
   */
    public String getValue(String key, String defaultValue) {
        String result = "";
        try {
            result = _properties.getProperty(key, defaultValue);
        } catch (Exception e) {
            result = defaultValue;
        }
        Configuration.cat.debug("key '" + key + "' returns '" + result + "'");
        return result;
    }

    /** Sets the string value of a configuration property.
   *
   *  @param key the key to set.
   *  @param value the value to set the key to.
   */
    public void setValue(String key, String value) {
        Configuration.cat.debug("key '" + key + "' set to '" + value + "'");
        _properties.setProperty(key, value);
    }
}
