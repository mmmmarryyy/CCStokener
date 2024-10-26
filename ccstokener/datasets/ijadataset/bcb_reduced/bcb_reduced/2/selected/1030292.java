package org.epoline.jsf.utils;

import java.net.URL;
import java.util.Properties;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Class for managing LOG4J logging
 * @author Patrick Balm
 * @version $Revision: 1.1.1.1 $
 */
public class Log4jManager {

    private static Logger logger = null;

    private static boolean alreadyLoaded = false;

    private static boolean isLoadedExternally = false;

    /**
	 * If this method is called without a previous call to {@link #setLoggerConfig(Properties)}
	 * then it will use the default logging properties as specified in the jsf-core.jar
	 * <code>(<jsf-core.jar>/config/jsf.log4j.properties)</code> file.
	 * <br>
	 * When calling {@link #setLoggerConfig(Properties)} before this method is called, all output
	 * will be redirected to whatever is specified in the properties of that method.
	 *
	 * @param name Localized name (e.g. fully specified classname)
	 * @return A logger object that allows LOG4J logging
	 */
    public static Logger getLogger(String name) {
        if (!isLoadedExternally) {
            if (LogManager.exists(name) != null) {
                logger = LogManager.exists(name);
            } else {
                if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
                    BasicConfigurator.configure();
                } else {
                    URL url = ClassLoader.getSystemResource("config/jsf.log4j.properties");
                    Properties log4jProps = new Properties();
                    try {
                        log4jProps.load(url.openStream());
                        PropertyConfigurator.configure(log4jProps);
                    } catch (Exception e) {
                        BasicConfigurator.configure();
                    }
                }
                Log4jManager.alreadyLoaded = true;
                logger = Logger.getLogger(name);
            }
        } else {
            logger = Logger.getLogger(name);
        }
        return logger;
    }

    /**
	 * Same as {@link #getLogger(String)}
	 */
    public static Logger getLogger(Class clazz) {
        return Log4jManager.getLogger(clazz.getName());
    }

    /**
	 * Sets the logging properties for logging via LOG4J.
	 * @param serviceSpecificLog4jProperties log4j properties
	 */
    public static void setLoggerConfig(Properties serviceSpecificLog4jProperties) {
        PropertyConfigurator.configure(serviceSpecificLog4jProperties);
        Log4jManager.isLoadedExternally = true;
    }
}
