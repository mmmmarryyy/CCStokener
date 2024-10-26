package com.telstra.ess.logging.spi.log4j;

import java.util.*;
import java.net.*;
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import com.telstra.ess.*;
import com.telstra.ess.configuration.*;
import com.telstra.ess.logging.*;
import com.telstra.ess.logging.spi.*;

/**
 * EssLoggerFactory implementation for providing Log4j logger functionality.
 * This factory is used to integrate Log4j as a logging solution. 
 * 
 * @author Greg
 * @since v1.2 of Ess Services API
 * @see org.apache.log4j.spi.LoggerFactory
 */
public class Log4jEssLoggerFactory extends EssLoggerFactory {

    private static final String LOG_NAME_BASE = "EssServicesAPI.Log4jEssLoggerFactory";

    private static final String LOG4J_CONF_PARAM_NAME = "logging.log4j.config";

    private ConfigurationManager confMgr = null;

    /**
	 * Default constructor
	 */
    public Log4jEssLoggerFactory() {
        super();
    }

    /**
	 * Specific implementation of {@link AphisLoggerFactory#createLogger(AphisOriginator)}<br>
	 * <br>
	 * This factory will first check the Log4j root logger for any active appenders. If none are found it calls {@link BasicConfigurator#configure()}
	 * and creates a subcategory of the root logger called '<code>EssServicesAPI.Log4jEssLoggerFactory</code>' with no appenders 
	 * and <code>false</code> additivity. The Logger eventually returned to the client code is actually a subcategory with user-defined
	 * behaviour, such that its category may be something like:<br>
	 * <br>
	 * <code>EssServicesAPI.Log4jEssLoggerFactory.default</code> or<br>
	 * <br>
	 * <code>EssServicesAPI.Log4jEssLoggerFactory.Application1</code> etc.
	 * 
	 * @return EssLogger instance 
	 * @param component instance for which a Log4j Logger should be configured.
	 * @see AphisLoggerFactory
	 * @see AphisLogger
	 * @see AphisOriginator
	 * @throws AphisFactoryException if a logger could not be configured
	 */
    public EssLogger createLogger(EssComponent component) throws EssFactoryException {
        if (component == null) {
            return (EssLogger) this.getInstance();
        } else {
            return (EssLogger) this.getInstance(component);
        }
    }

    private synchronized Log4jLogger getInstance() {
        return getInstance(null);
    }

    private synchronized void configureLoggingEnvironment(EssComponent comp) throws EssFactoryException {
        if (comp == null) {
            throw new EssFactoryException("Could not configure logging environment, component was null");
        }
        if (confMgr == null) {
            confMgr = ConfigurationManager.getInstance(comp);
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
                            throw new EssFactoryException("Could not configure log4j using remote properties file", ex);
                        }
                    }
                } catch (Exception e) {
                    try {
                        PropertyConfigurator.configure(newConfigLoc);
                    } catch (Exception ex) {
                        throw new EssFactoryException("Could not configure log4j using configuration files", ex);
                    }
                }
            } else {
                throw new EssFactoryException("log4j configuration file not provided, aborting");
            }
        } catch (Exception e) {
            throw new EssFactoryException("Could not configure logging environment", e);
        }
    }

    private synchronized Log4jLogger getInstance(EssComponent comp) {
        if (confMgr == null) {
            confMgr = ConfigurationManager.getInstance(comp);
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
            if (comp instanceof EssServiceComponent) {
                domain += "." + ((EssServiceComponent) comp).getServiceName();
            } else {
                if (comp.getName() != null) {
                    domain += "." + comp.getName();
                    if (comp.getCustomer() != null) {
                        domain += "." + comp.getCustomer();
                    }
                }
            }
        } else {
            domain += ".default";
        }
        return (Log4jLogger) Log4jLogger.getLogger(domain);
    }
}
