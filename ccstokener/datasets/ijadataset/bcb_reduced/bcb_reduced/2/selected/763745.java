package org.arch4j.logging.sun;

import org.arch4j.logging.LoggingCategory;
import org.arch4j.logging.LoggingFramework;
import org.arch4j.logging.LoggingConstants;
import org.arch4j.property.PropertyProvider;
import org.arch4j.core.EnvironmentalException;
import java.io.*;
import java.net.URL;
import java.util.logging.LogManager;

/**
 * <p> This class wraps the Sun Java 1.4 logging facility.
 *
 * @author Ross E. Greinke
 * @version $Revision: 1.2 $
 */
public class SunLoggingFramework implements LoggingFramework {

    public static final String SUN_LOGGING_FILE_NAME_PROPERTY = "framework.sunlogging.filename";

    /**
   * Retrieve a logging category with name <code>name</code>.
   * @param name The name of the logging category to retrieve.
   * @return The logging category to be used for logging.
   */
    public LoggingCategory getCategory(String name) {
        return new SunLoggingCategory(name);
    }

    /** Configure the logging framework. */
    public void configure() {
        File propertyDirectory = PropertyProvider.getProvider().getPropertyManager().getPropertyDirectory();
        String propertyFile = getFileName();
        try {
            if (propertyFile == null) {
                LogManager.getLogManager().readConfiguration();
                return;
            }
            File configFile = new File(propertyDirectory, propertyFile);
            if (configFile.exists()) {
                InputStream in = new FileInputStream(configFile.getCanonicalPath());
                BufferedInputStream bin = new BufferedInputStream(in);
                try {
                    LogManager.getLogManager().readConfiguration(bin);
                } finally {
                    if (bin != null) {
                        bin.close();
                    }
                }
                return;
            }
            URL url = getClass().getClassLoader().getResource(propertyFile);
            if (url != null) {
                InputStream in = url.openStream();
                try {
                    LogManager.getLogManager().readConfiguration(in);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
                return;
            }
        } catch (Exception e) {
            throw new EnvironmentalException("Unable to configure the Sun Logging Framework.", e);
        }
        throw new EnvironmentalException("Unable to find property file [" + propertyFile + "] in either Property dir [" + propertyDirectory.getPath() + "] or classapth");
    }

    private String getFileName() {
        return (PropertyProvider.getProvider().getPropertyManager().getProperty(LoggingConstants.LOGGING_DOMAIN, SUN_LOGGING_FILE_NAME_PROPERTY));
    }
}
