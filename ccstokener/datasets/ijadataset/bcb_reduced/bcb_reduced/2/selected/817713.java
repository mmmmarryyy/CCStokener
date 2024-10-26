package org.aphis.core.logging.spi.log4j;

import java.util.*;
import java.net.*;
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.aphis.core.*;
import org.aphis.core.configuration.*;
import org.aphis.core.logging.AphisLogger;
import org.aphis.core.logging.spi.*;

/**
 * AphisLoggerFactory implementation for providing Log4j logger functionality.
 * This factory is used to integrate Log4j as a logging solution. 
 * 
 * @author Greg
 * @see org.apache.log4j.spi.LoggerFactory
 */
public class AphisLog4jLoggerFactory extends AphisLoggerFactory {

    private static final String LOG_NAME_BASE = "Aphis.Log4jLoggerFactory";

    private static final String LOG4J_CONF_PARAM_NAME = "logging.log4j.config";

    private AphisConfigurationManager confMgr = null;

    /**
	 * Default constructor
	 */
    public AphisLog4jLoggerFactory() {
        super();
    }

    /**
	 * Specific implementation of {@link AphisLoggerFactory#createLogger(AphisOriginator)}<br>
	 * <br>
	 * This factory will first check the Log4j root logger for any active appenders. If none are found it calls {@link BasicConfigurator#configure()}
	 * and creates a subcategory of the root logger called '<code>Aphis.AphisEssLoggerFactory</code>' with no appenders 
	 * and <code>false</code> additivity. The Logger eventually returned to the client code is actually a subcategory with user-defined
	 * behaviour, such that its category may be something like:<br>
	 * <br>
	 * <code>Aphis.AphisLog4jLoggerFactory.default</code> or<br>
	 * <br>
	 * <code>Aphis.AphisLog4jLoggerFactory.Application1</code> etc.
	 * 
	 * @return AphisLogger instance 
	 * @param component instance for which a Log4j Logger should be configured.
	 * @see AphisLoggerFactory
	 * @see AphisLogger
	 * @see AphisOriginator
	 * @throws AphisFactoryException if a logger could not be configured
	 */
    public AphisLogger createLogger(AphisOriginator component) throws AphisFactoryException {
        if (component == null) {
            return (AphisLogger) this.getInstance();
        } else {
            return (AphisLogger) this.getInstance(component);
        }
    }

    private synchronized AphisLog4jLogger getInstance() {
        return getInstance(null);
    }

    private synchronized void configureLoggingEnvironment(AphisOriginator comp) throws AphisFactoryException {
        if (comp == null) {
            throw new AphisFactoryException("Could not configure logging environment, component was null");
        }
        if (confMgr == null) {
            confMgr = AphisConfigurationManager.getInstance(comp);
        }
        try {
            String newConfigLoc = confMgr.getConfigurationItem(LOG4J_CONF_PARAM_NAME, null);
            if (newConfigLoc != null) {
                try {
                    URL url = new URL(newConfigLoc);
                    if (url.toExternalForm().endsWith("xml")) {
                        DOMConfigurator.configure(url);
                    } else {
                        Properties p = new Properties();
                        try {
                            p.load(url.openConnection().getInputStream());
                            PropertyConfigurator.configure(p);
                        } catch (Exception ex) {
                            throw new AphisFactoryException("Could not configure log4j using remote properties file", ex);
                        }
                    }
                } catch (Exception e) {
                    try {
                        PropertyConfigurator.configure(newConfigLoc);
                    } catch (Exception ex) {
                        throw new AphisFactoryException("Could not configure log4j using configuration files", ex);
                    }
                }
            } else {
                throw new AphisFactoryException("log4j configuration file not provided, aborting");
            }
        } catch (Exception e) {
            throw new AphisFactoryException("Could not configure logging environment", e);
        }
    }

    private synchronized AphisLog4jLogger getInstance(AphisOriginator comp) {
        if (confMgr == null) {
            confMgr = AphisConfigurationManager.getInstance(comp);
        }
        try {
            configureLoggingEnvironment(comp);
        } catch (Exception e) {
            Enumeration loggerEnum = LogManager.getCurrentLoggers();
            if (!loggerEnum.hasMoreElements()) {
                BasicConfigurator.configure();
            }
        }
        String domain = LOG_NAME_BASE;
        if (comp != null) {
            if (comp.getName() != null) {
                domain += "." + comp.getName();
            }
        } else {
            domain += ".default";
        }
        return (AphisLog4jLogger) AphisLog4jLogger.getLogger(domain);
    }
}
