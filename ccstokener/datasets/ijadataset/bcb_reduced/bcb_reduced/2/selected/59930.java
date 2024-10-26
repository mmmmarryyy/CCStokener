package org.dspace.app.util;

import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.apache.log4j.Logger;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import java.beans.Introspector;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * Class to initialize / cleanup resources used by DSpace when the web application
 * is started or stopped
 */
public class DSpaceContextListener implements ServletContextListener {

    private static Logger log = Logger.getLogger(DSpaceContextListener.class);

    /**
     * The DSpace config parameter, this is where the path to the DSpace
     * configuration file can be obtained
     */
    public static final String DSPACE_CONFIG_PARAMETER = "dspace-config";

    /**
     * Initialize any resources required by the application
     * @param event
     */
    public void contextInitialized(ServletContextEvent event) {
        try {
            String osName = System.getProperty("os.name");
            if (osName != null && osName.toLowerCase().contains("windows")) {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        String dspaceConfig = null;
        dspaceConfig = event.getServletContext().getInitParameter(DSPACE_CONFIG_PARAMETER);
        if (dspaceConfig == null || "".equals(dspaceConfig)) {
            throw new IllegalStateException("\n\nDSpace has failed to initialize. This has occurred because it was unable to determine \n" + "where the dspace.cfg file is located. The path to the configuration file should be stored \n" + "in a context variable, '" + DSPACE_CONFIG_PARAMETER + "', in the global context. \n" + "No context variable was found in either location.\n\n");
        }
        try {
            ConfigurationManager.loadConfig(dspaceConfig);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("\n\nDSpace has failed to initialize, during stage 2. Error while attempting to read the \n" + "DSpace configuration file (Path: '" + dspaceConfig + "'). \n" + "This has likely occurred because either the file does not exist, or it's permissions \n" + "are set incorrectly, or the path to the configuration file is incorrect. The path to \n" + "the DSpace configuration file is stored in a context variable, 'dspace-config', in \n" + "either the local servlet or global context.\n\n", e);
        }
    }

    /**
     * Clean up resources used by the application when stopped
     * 
     * @param event
     */
    public void contextDestroyed(ServletContextEvent event) {
        try {
            DatabaseManager.shutdown();
            Introspector.flushCaches();
            for (Enumeration e = DriverManager.getDrivers(); e.hasMoreElements(); ) {
                Driver driver = (Driver) e.nextElement();
                if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
                    DriverManager.deregisterDriver(driver);
                }
            }
        } catch (RuntimeException e) {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        } catch (Exception e) {
            log.error("Failed to cleanup ClassLoader for webapp", e);
        }
    }
}
