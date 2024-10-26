package org.fcrepo.server.utilities;

import java.net.URL;
import java.net.URLConnection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Cleanup listener for general resource handling
 * 
 * @version $Id: CleanupContextListener.java 8493 2010-01-20 02:13:21Z birkland $
 */
public class CleanupContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        try {
            String osName = System.getProperty("os.name");
            if (osName != null && osName.toLowerCase().contains("windows")) {
                URL url = new URL("http://localhost/");
                URLConnection urlConn = url.openConnection();
                urlConn.setDefaultUseCaches(false);
            }
        } catch (Throwable t) {
        }
    }

    /**
     * Clean up resources used by the application when stopped
     * 
     * @seejavax.servlet.ServletContextListener#contextDestroyed(javax.servlet
     * .ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent event) {
        try {
            for (Enumeration<Driver> e = DriverManager.getDrivers(); e.hasMoreElements(); ) {
                Driver driver = e.nextElement();
                if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
                    DriverManager.deregisterDriver(driver);
                }
            }
        } catch (Throwable e) {
        }
        org.apache.axis.utils.cache.MethodCache.getInstance().clearCache();
    }
}
