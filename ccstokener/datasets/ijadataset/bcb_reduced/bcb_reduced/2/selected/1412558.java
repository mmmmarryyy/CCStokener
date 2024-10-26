package org.gbif.ecat.cfg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Constant values used throughout ECAT applications.
 */
public class DataDirConfigFactory {

    protected static final Logger log = LoggerFactory.getLogger(DataDirConfigFactory.class);

    private static final String PROPERTY_FILE = "application.properties";

    private static DataDirConfig ref;

    public static final DataDirConfig getDefault() {
        if (ref == null) {
            try {
                InputStream propStream = null;
                URL url = Thread.currentThread().getContextClassLoader().getResource(PROPERTY_FILE);
                if (url != null) {
                    log.info("Building DataDirConfig singleton from properties: " + url.toURI());
                    try {
                        propStream = url.openStream();
                        ref = new DataDirConfig(propStream);
                    } catch (IOException e) {
                        log.warn("Error reading DataDirConfig properties", e);
                    }
                } else {
                    log.info("Couldn't find the " + PROPERTY_FILE + " resource for building a AppConfig singleton.");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read application configuration " + PROPERTY_FILE, e);
            }
        }
        return ref;
    }

    public static void main(String[] args) {
        DataDirConfigFactory f = new DataDirConfigFactory();
        DataDirConfig cfg = f.getCfg();
        DataDirConfig cfg2 = DataDirConfigFactory.getDefault();
    }

    public final synchronized DataDirConfig getCfg() {
        if (ref == null) {
            try {
                InputStream propStream = null;
                URL url = getClass().getClassLoader().getResource(PROPERTY_FILE);
                if (url != null) {
                    try {
                        propStream = url.openStream();
                        log.info("Building DataDirConfig singleton from properties: " + url.toURI());
                        ref = new DataDirConfig(propStream);
                    } catch (IOException e) {
                        log.warn("Error reading DataDirConfig properties", e);
                        ref = getDefault();
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read application configuration " + PROPERTY_FILE, e);
            }
        }
        return ref;
    }
}
