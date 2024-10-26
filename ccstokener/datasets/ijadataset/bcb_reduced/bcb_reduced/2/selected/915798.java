package org.gbif.portal.guice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gbif.ecat.cfg.DataDirConfig;
import org.gbif.ecat.cfg.DataDirConfigFactory;
import org.gbif.portal.struts.RequireAdminInterceptor;
import org.gbif.portal.struts.RequireLoginInterceptor;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

public class AppModule extends AbstractModule {

    private final Log log = LogFactory.getLog(getClass());

    private static final String PROPERTY_FILE = "application.properties";

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    Properties provideCfg() {
        InputStream propStream = null;
        URL url = Thread.currentThread().getContextClassLoader().getResource(PROPERTY_FILE);
        Properties cfg = new Properties();
        if (url != null) {
            try {
                log.info("Loading app config from properties: " + url.toURI());
                propStream = url.openStream();
                cfg.load(propStream);
                return cfg;
            } catch (Exception e) {
                log.warn(e);
            }
        }
        if (cfg.size() < 1) {
            log.info(PROPERTY_FILE + " doesnt contain any configuration for application properties.");
        }
        return cfg;
    }
}
