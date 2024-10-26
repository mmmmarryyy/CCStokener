package com.inetmon.jn.logging;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 * 
 * @author ckng
 */
public class LoggingPlugin extends Plugin implements IStartup {

    private static LoggingPlugin plugin;

    private ResourceBundle resourceBundle;

    private ArrayList<PluginLogManager> logManagers = new ArrayList<PluginLogManager>();

    private static final String LOG_PROPERTIES_FILE = "logger.properties";

    private PluginLogManager logManager;

    private static Logger logger;

    /**
	 * The constructor.
	 */
    public LoggingPlugin() {
        super();
        plugin = this;
        try {
            resourceBundle = ResourceBundle.getBundle("com.inetmon.jn.logging.LoggingPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    /**
	 * This method is called upon plug-in activation
	 */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        configureLogger();
        logger = logManager.getLogger(LoggingPlugin.class.getName());
    }

    /**
	 * This method is called when the plug-in is stopped
	 */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        if (this.logManager != null) {
            this.logManager.shutdown();
            this.logManager = null;
        }
    }

    /**
	 * Returns the shared instance.
	 */
    public static LoggingPlugin getDefault() {
        return plugin;
    }

    /**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
    public static String getResourceString(String key) {
        ResourceBundle bundle = LoggingPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
	 * Returns the plugin's resource bundle,
	 */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
	 * Adds a log manager object to the list of active log managers
	 */
    void addLogManager(PluginLogManager logManager) {
        synchronized (this.logManagers) {
            if (logManager != null) this.logManagers.add(logManager);
        }
    }

    /**
	 * Removes a log manager object from the list of active log managers
	 */
    void removeLogManager(PluginLogManager logManager) {
        synchronized (this.logManagers) {
            if (logManager != null) this.logManagers.remove(logManager);
        }
    }

    public void earlyStartup() {
    }

    /**
	 * Configure centralized logger
	 */
    private void configureLogger() {
        try {
            URL url = getBundle().getEntry("/" + LOG_PROPERTIES_FILE);
            InputStream propertiesInputStream = url.openStream();
            if (propertiesInputStream != null) {
                Properties props = new Properties();
                props.load(propertiesInputStream);
                propertiesInputStream.close();
                this.logManager = new PluginLogManager(this, props);
            }
        } catch (Exception e) {
            String message = "Error while initializing log properties." + e.getMessage();
            IStatus status = new Status(IStatus.ERROR, getDefault().getBundle().getSymbolicName(), IStatus.ERROR, message, e);
            getLog().log(status);
            throw new RuntimeException("Error while initializing log properties.", e);
        }
    }

    /**
	 * @return Returns the logger.
	 */
    public static Logger getLogger() {
        return logger;
    }
}
