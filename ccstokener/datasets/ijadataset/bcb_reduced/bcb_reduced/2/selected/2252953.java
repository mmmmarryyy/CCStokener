package org.dynamo.database.ui.console;

import java.io.InputStream;
import java.net.URL;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

/**
 * This is the logger used to write <em>DTU</em> prefixed messages so that our
 * informational logging is nice looking.
 */
public class DTULogger {

    private static final String CONVERSION_EXCEPTION = "Conversion exception\n\t";

    private static final String CONVERSION_ERROR = "Conversion error";

    /**
	 * DTU logger.
	 */
    private static final String DTU_PREFIX = "DTU-";

    /**
	 * The default name to give the logger (if one isn't set)
	 */
    private static final String DEFAULT_LOGGER_NAME = "DTU";

    private static Logger logger = Logger.getLogger(DEFAULT_LOGGER_NAME);

    private static boolean INITIALIZED = false;

    private static boolean LOGGER_ENABLED = true;

    /**
	 * Configures logging for DTU application from the the xml resource
	 * "log4j.xml" found within the same package as this class.
	 */
    public static void initialize() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        String defaultConfiguration = "log4j.xml";
        URL url = null;
        final String configuration = loggingConfigurationUri;
        if (configuration != null) {
            try {
                url = new URL(configuration);
                InputStream stream = url.openStream();
                stream.close();
                stream = null;
                configure(url);
                logger.info("Logging configured from external source --> '" + configuration + "'");
            } catch (Throwable throwable) {
                url = DTULogger.class.getResource(defaultConfiguration);
                configure(url);
                logger.warn("Invalid logging configuration uri --> '" + configuration + "'");
            }
        }
        if (url == null) {
            url = DTULogger.class.getResource(defaultConfiguration);
            configure(url);
        }
        if (url == null) {
            throw new RuntimeException("Could not find default logging configuration file '" + defaultConfiguration + "'");
        }
    }

    public static void reinitialize() {
        INITIALIZED = false;
    }

    /**
	 * The URI to an external logging configuration file.
	 */
    private static String loggingConfigurationUri = null;

    /**
	 * Sets the URI to an external logging configuration file. This will
	 * override the default log4j.xml.
	 * 
	 * @param loggingConfigurationUri
	 *            the URI to the logging configuration file.
	 */
    public static void setLoggingConfigurationUri(final String loggingConfigurationUri) {
        DTULogger.loggingConfigurationUri = loggingConfigurationUri;
    }

    /**
	 * Configures the Logger from the passed in logConfigurationXml
	 * 
	 * @param logConfigurationXml
	 */
    protected static void configure(final URL logConfigurationXml) {
        try {
            DOMConfigurator.configure(logConfigurationXml);
        } catch (Exception ex) {
            logger.warn("Unable to initialize logging system " + "with configuration file '" + logConfigurationXml + "' --> using basic configuration.");
            BasicConfigurator.configure();
        }
    }

    /**
	 * Retrieves the namespace logger (if one is available) otherwise returns
	 * the root logger.
	 * 
	 * @param namespaceName
	 *            the name of the namespace for which we'll retrieve the logger
	 *            instance.
	 * @return the namespace or root logger instance.
	 */
    public static Logger getNamespaceLogger(final String namespaceName) {
        Logger logger;
        {
            logger = Logger.getRootLogger();
        }
        return logger;
    }

    /**
	 * Gets the name of the logger.
	 * 
	 * @param namespace
	 *            the name of the namespace for which this logger is used.
	 * @return the logger name.
	 */
    public static String getNamespaceLoggerName(final String namespace) {
        return "org.dynamo.database.reverse.namespaces." + namespace;
    }

    /**
	 * Gets the name of the file to which namespace logging output will be
	 * written.
	 * 
	 * @param namespace
	 *            the name of the namespace for which this logger is used.
	 * @return the namespace logging file name.
	 */
    public static String getNamespaceLogFileName(final String namespace) {
        return "dtu-" + namespace + ".log";
    }

    /**
	 * Allows us to add a suffix to the logger name.
	 * 
	 * @param suffix
	 *            the suffix to append to the logger name.
	 */
    public static void setSuffix(final String suffix) {
        logger = Logger.getLogger(DTU_PREFIX + suffix);
    }

    /**
	 * Resets the logger to the default name.
	 */
    public static void reset() {
        logger = Logger.getLogger(DEFAULT_LOGGER_NAME);
    }

    /**
	 * Shuts down the logger and releases any resources.
	 */
    public static void shutdown() {
        LogManager.shutdown();
    }

    public static void debug(Object object) {
        if (LOGGER_ENABLED) {
            logger.debug(object);
        }
    }

    public static void info(Object object) {
        if (LOGGER_ENABLED) {
            logger.info(object);
        }
    }

    public static void warn(Object object) {
        if (LOGGER_ENABLED) {
            logger.warn(object);
        }
    }

    public static void error(Object object) {
        Object err = object;
        org.osgi.framework.Bundle b = Platform.getBundle(Activator.PLUGIN_ID);
        ILog log = null;
        IStatus status;
        if (b != null) {
            log = Platform.getLog(b);
        }
        if (object instanceof Exception) {
            err = getStackTrace((Exception) object);
        }
        if (LOGGER_ENABLED) {
            logger.error(err);
            if (log != null) {
                status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, object.toString(), (object instanceof Exception) ? (Exception) object : null);
                log.log(status);
            }
        }
    }

    public static void error(Object object, Exception ex) {
        if (LOGGER_ENABLED) {
            logger.error((object != null) ? object : CONVERSION_ERROR);
            logger.error(getStackTrace(ex));
            org.osgi.framework.Bundle b = Platform.getBundle(Activator.PLUGIN_ID);
            if (b != null) {
                ILog log = Platform.getLog(b);
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, (object != null) ? object.toString() : CONVERSION_ERROR, ex);
                log.log(status);
            }
        }
    }

    private static String getStackTrace(Exception e) {
        String s = CONVERSION_EXCEPTION;
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) s += "\n\tat " + trace[i];
        Throwable ourCause = e.getCause();
        if (ourCause != null) s += "\n" + getStackTraceAsCausee(ourCause, trace);
        return s;
    }

    private static String getStackTraceAsCausee(Throwable t, StackTraceElement[] causedTrace) {
        String s = "";
        StackTraceElement[] trace = t.getStackTrace();
        int m = trace.length - 1, n = causedTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(causedTrace[n])) {
            m--;
            n--;
        }
        int framesInCommon = trace.length - 1 - m;
        s += "\nCaused by: " + t;
        for (int i = 0; i <= m; i++) s += "\n\tat " + trace[i];
        if (framesInCommon != 0) s += "\n\t... " + framesInCommon + " more";
        Throwable ourCause = t.getCause();
        if (ourCause != null) s += "\n" + getStackTraceAsCausee(ourCause, trace);
        return s;
    }

    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public static void enableLogger() {
        LOGGER_ENABLED = true;
    }

    public static void disableLogger() {
        LOGGER_ENABLED = false;
    }

    public static boolean isEnableLogger() {
        return LOGGER_ENABLED;
    }

    public static void errorBox(final String errorDialogueTitle, final String errorMessage, String msg, Throwable e) {
        IStatus status;
        status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, msg, e.getCause());
        showError(errorDialogueTitle, errorMessage, status);
    }

    public static void showError(final String errorDialogueTitle, final String errorMessage, final IStatus status) {
        try {
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    ErrorDialog.openError(null, errorDialogueTitle, errorMessage, status, 5);
                }
            });
        } catch (Exception e) {
            DTULogger.error(e.getMessage(), e);
        }
    }

    public static void error(Object object, Throwable t) {
        if (LOGGER_ENABLED) {
            logger.error((object != null) ? object : CONVERSION_ERROR);
            logger.error(getStackTrace(t));
            org.osgi.framework.Bundle b = Platform.getBundle(Activator.PLUGIN_ID);
            if (b != null) {
                ILog log = Platform.getLog(b);
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, (object != null) ? object.toString() : CONVERSION_ERROR, t);
                log.log(status);
            }
        }
    }

    private static String getStackTrace(Throwable t) {
        String s = CONVERSION_EXCEPTION;
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) s += "\n\tat " + trace[i];
        Throwable ourCause = t.getCause();
        if (ourCause != null) s += "\n" + getStackTraceAsCausee(ourCause, trace);
        return s;
    }
}
