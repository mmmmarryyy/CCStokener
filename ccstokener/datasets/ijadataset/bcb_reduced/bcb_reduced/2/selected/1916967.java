package org.apache.commons.logging.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.commons.logging.AndroidLog;
import android.util.Log;

/**
 * <p>
 * Implementation of Log for Android platform that sends all enabled log messages, for all defined loggers, to the Android logging System. The
 * following system properties are supported to configure the behavior of this logger:
 * </p>
 * <ul>
 * <li><code>org.apache.commons.logging.androidlog.defaultlog</code> - Default logging detail level for all instances of AndroidLog. Must be one of
 * ("trace", "debug", "info", "warn", "error", or "fatal"). If not specified, defaults to "info".</li>
 * <li><code>org.apache.commons.logging.androidlog.log.xxxxx</code> - Logging detail level for a AndroidLog instance named "xxxxx". Must be one of
 * ("trace", "debug", "info", "warn", "error", or "fatal"). If not specified, the default logging detail level is used.</li>
 * <li><code>org.apache.commons.logging.androidlog.showlogname</code> - Set to <code>true</code> if you want the Log instance name to be included in
 * output messages. Defaults to <code>false</code>.</li>
 * <li><code>org.apache.commons.logging.androidlog.showShortLogname</code> - Set to <code>true</code> if you want the last component of the name to be
 * included in output messages. Defaults to <code>true</code>.</li>
 * <li><code>org.apache.commons.logging.androidlog.showdatetime</code> - Set to <code>true</code> if you want the current date and time to be included
 * in output messages. Default is <code>false</code>.</li>
 * <li><code>org.apache.commons.logging.androidlog.dateTimeFormat</code> - The date and time format to be used in the output messages. The pattern
 * describing the date and time format is the same that is used in <code>java.text.SimpleDateFormat</code>. If the format is not specified or is
 * invalid, the default format is used. The default format is <code>yyyy/MM/dd HH:mm:ss:SSS zzz</code>.</li>
 * </ul>
 * 
 * <p>
 * In addition to looking for system properties with the names specified above, this implementation also checks for a class loader resource named
 * <code>"androidlog.properties"</code>, and includes any matching definitions from this resource (if it exists).
 * </p>
 * 
 * @author Nicolas Dos Santos
 */
public class SimpleAndroidLog implements AndroidLog, Serializable {

    private static final long serialVersionUID = -7864001067995532343L;

    /** All system properties used by <code>AndroidLog</code> start with this */
    protected static final String systemPrefix = "org.apache.commons.logging.androidlog.";

    /** Properties loaded from androidlog.properties */
    protected static final Properties androidLogProps = new Properties();

    /** The default format to use when formating dates */
    protected static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS zzz";

    /** Include the instance name in the log message? */
    protected static boolean showLogName = false;

    /**
     * Include the short name ( last component ) of the logger in the log message. Defaults to true - otherwise we'll be lost in a flood of messages
     * without knowing who sends them.
     */
    protected static boolean showShortName = true;

    /** Include the current time in the log message */
    protected static boolean showDateTime = false;

    /** The date and time format to use in the log message */
    protected static String dateTimeFormat = DEFAULT_DATE_TIME_FORMAT;

    /** Include the current time in the log message */
    protected static boolean showLevel = false;

    /** Include the short tag ( last component ) of the logger in the android log cat. Defaults to false */
    protected static boolean showShortTag = false;

    /**
     * Used to format times.
     * <p>
     * Any code that accesses this object should first obtain a lock on it, ie use synchronized(dateFormatter); this requirement was introduced in
     * 1.1.1 to fix an existing thread safety bug (SimpleDateFormat.format is not thread-safe).
     */
    protected static DateFormat dateFormatter = null;

    /** "Trace" level logging. */
    public static final int LOG_LEVEL_TRACE = 1;

    /** "Debug" level logging. */
    public static final int LOG_LEVEL_DEBUG = 2;

    /** "Info" level logging. */
    public static final int LOG_LEVEL_INFO = 3;

    /** "Warn" level logging. */
    public static final int LOG_LEVEL_WARN = 4;

    /** "Error" level logging. */
    public static final int LOG_LEVEL_ERROR = 5;

    /** "Fatal" level logging. */
    public static final int LOG_LEVEL_FATAL = 6;

    /** Enable all logging levels */
    public static final int LOG_LEVEL_ALL = (LOG_LEVEL_TRACE - 1);

    /** Enable no logging levels */
    public static final int LOG_LEVEL_OFF = (LOG_LEVEL_FATAL + 1);

    private static String getStringProperty(String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (SecurityException e) {
            ;
        }
        return (prop == null) ? androidLogProps.getProperty(name) : prop;
    }

    private static String getStringProperty(String name, String dephault) {
        String prop = getStringProperty(name);
        return (prop == null) ? dephault : prop;
    }

    private static boolean getBooleanProperty(String name, boolean dephault) {
        String prop = getStringProperty(name);
        return (prop == null) ? dephault : "true".equalsIgnoreCase(prop);
    }

    static {
        ClassLoader classLoader = getClassLoader(SimpleAndroidLog.class);
        System.out.println("classLoader: " + classLoader);
        Properties props = getConfigurationFile(classLoader, "androidlog.properties");
        if (props != null) {
            androidLogProps.putAll(props);
        }
        System.out.println("Android Log Properties: " + androidLogProps);
        showLogName = getBooleanProperty(systemPrefix + "showlogname", showLogName);
        showShortName = getBooleanProperty(systemPrefix + "showShortLogname", showShortName);
        showDateTime = getBooleanProperty(systemPrefix + "showdatetime", showDateTime);
        if (showDateTime) {
            dateTimeFormat = getStringProperty(systemPrefix + "dateTimeFormat", dateTimeFormat);
            try {
                dateFormatter = new SimpleDateFormat(dateTimeFormat);
            } catch (IllegalArgumentException e) {
                dateTimeFormat = DEFAULT_DATE_TIME_FORMAT;
                dateFormatter = new SimpleDateFormat(dateTimeFormat);
            }
        }
        showLevel = getBooleanProperty(systemPrefix + "showlevel", showLevel);
        showShortTag = getBooleanProperty(systemPrefix + "showShortTag", showShortTag);
    }

    /** The name of this android log instance */
    protected String logName = null;

    /** The current log level */
    protected int currentLogLevel;

    /** The short name of this android log instance */
    private String shortLogName = null;

    /** The tag of this android log instance */
    private String tag = null;

    /**
     * Construct an Android log with given name.
     * 
     * @param name
     *            log name
     */
    public SimpleAndroidLog(String name) {
        logName = tag = name;
        setLevel(SimpleAndroidLog.LOG_LEVEL_INFO);
        String lvl = getStringProperty(systemPrefix + "log." + logName);
        int i = String.valueOf(name).lastIndexOf(".");
        while (null == lvl && i > -1) {
            name = name.substring(0, i);
            lvl = getStringProperty(systemPrefix + "log." + name);
            i = String.valueOf(name).lastIndexOf(".");
        }
        if (null == lvl) {
            lvl = getStringProperty(systemPrefix + "defaultlog");
        }
        if ("all".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_ALL);
        } else if ("trace".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_TRACE);
        } else if ("debug".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_DEBUG);
        } else if ("info".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_INFO);
        } else if ("warn".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_WARN);
        } else if ("error".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_ERROR);
        } else if ("fatal".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_FATAL);
        } else if ("off".equalsIgnoreCase(lvl)) {
            setLevel(SimpleAndroidLog.LOG_LEVEL_OFF);
        }
    }

    protected static ClassLoader getClassLoader(Class<?> clazz) {
        try {
            return clazz.getClassLoader();
        } catch (SecurityException ex) {
            System.err.println("Unable to get classloader for class '" + clazz + "' due to security restrictions - " + ex.getMessage());
            throw ex;
        }
    }

    private static Properties getProperties(final URL url) {
        PrivilegedAction<Properties> action = new PrivilegedAction<Properties>() {

            public Properties run() {
                try {
                    InputStream stream = url.openStream();
                    if (stream != null) {
                        Properties props = new Properties();
                        props.load(stream);
                        stream.close();
                        return props;
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
                return null;
            }
        };
        return AccessController.doPrivileged(action);
    }

    private static final Properties getConfigurationFile(ClassLoader classLoader, String fileName) {
        Properties props = null;
        double priority = 0.0;
        URL propsUrl = null;
        try {
            Enumeration<URL> urls = getResources(classLoader, fileName);
            if (urls == null) {
                return null;
            }
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                Properties newProps = getProperties(url);
                if (newProps != null) {
                    if (props == null) {
                        propsUrl = url;
                        props = newProps;
                        String priorityStr = props.getProperty("priority");
                        priority = 0.0;
                        if (priorityStr != null) {
                            priority = Double.parseDouble(priorityStr);
                        }
                        System.err.println("[LOOKUP] Properties file found at '" + url + "'" + " with priority " + priority);
                    } else {
                        String newPriorityStr = newProps.getProperty("priority");
                        double newPriority = 0.0;
                        if (newPriorityStr != null) {
                            newPriority = Double.parseDouble(newPriorityStr);
                        }
                        if (newPriority > priority) {
                            System.err.println("[LOOKUP] Properties file at '" + url + "'" + " with priority " + newPriority + " overrides file at '" + propsUrl + "'" + " with priority " + priority);
                            propsUrl = url;
                            props = newProps;
                            priority = newPriority;
                        } else {
                            System.err.println("[LOOKUP] Properties file at '" + url + "'" + " with priority " + newPriority + " does not override file at '" + propsUrl + "'" + " with priority " + priority);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("SecurityException thrown while trying to find/read config files.");
        }
        if (props == null) {
            System.err.println("[LOOKUP] No properties file of name '" + fileName + "' found.");
        } else {
            System.err.println("[LOOKUP] Properties file of name '" + fileName + "' found at '" + propsUrl + '"');
        }
        return props;
    }

    private static Enumeration<URL> getResources(final ClassLoader loader, final String name) {
        PrivilegedAction<Enumeration<URL>> action = new PrivilegedAction<Enumeration<URL>>() {

            public Enumeration<URL> run() {
                try {
                    if (loader != null) {
                        return loader.getResources(name);
                    } else {
                        return ClassLoader.getSystemResources(name);
                    }
                } catch (IOException e) {
                    System.err.println("Exception while trying to find configuration file " + name + ":" + e.getMessage());
                    return null;
                } catch (NoSuchMethodError e) {
                    return null;
                }
            }
        };
        return AccessController.doPrivileged(action);
    }

    /**
     * <p>
     * Set logging level.
     * </p>
     * 
     * @param currentLogLevel
     *            new logging level
     */
    public void setLevel(int currentLogLevel) {
        this.currentLogLevel = currentLogLevel;
    }

    /**
     * <p>
     * Get logging level.
     * </p>
     */
    public int getLevel() {
        return currentLogLevel;
    }

    /**
     * <p>
     * Do the actual logging. This method assembles the message and then calls <code>write()</code> to cause it to be written.<br/>
     * Calls {@link android.util.Log} to write log for the Android platform.
     * </p>
     * 
     * @param type
     *            One of the LOG_LEVEL_XXX constants defining the log level
     * @param message
     *            The message itself (typically a String)
     * @param t
     *            The exception whose stack trace should be logged
     */
    protected void log(int type, Object message, Throwable t) {
        StringBuffer buf = new StringBuffer();
        if (showDateTime) {
            Date now = new Date();
            String dateText;
            synchronized (dateFormatter) {
                dateText = dateFormatter.format(now);
            }
            buf.append(dateText);
            buf.append(" ");
        }
        if (showLevel) {
            switch(type) {
                case SimpleAndroidLog.LOG_LEVEL_TRACE:
                    buf.append("[TRACE] ");
                    break;
                case SimpleAndroidLog.LOG_LEVEL_DEBUG:
                    buf.append("[DEBUG] ");
                    break;
                case SimpleAndroidLog.LOG_LEVEL_INFO:
                    buf.append("[INFO] ");
                    break;
                case SimpleAndroidLog.LOG_LEVEL_WARN:
                    buf.append("[WARN] ");
                    break;
                case SimpleAndroidLog.LOG_LEVEL_ERROR:
                    buf.append("[ERROR] ");
                    break;
                case SimpleAndroidLog.LOG_LEVEL_FATAL:
                    buf.append("[FATAL] ");
                    break;
            }
        }
        if (showShortName) {
            initShortLogName();
            buf.append(String.valueOf(shortLogName)).append(" - ");
        } else if (showLogName) {
            buf.append(String.valueOf(logName)).append(" - ");
        }
        buf.append(String.valueOf(message));
        if (t != null) {
            buf.append(" <");
            buf.append(t.toString());
            buf.append(">");
            java.io.StringWriter sw = new java.io.StringWriter(1024);
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            buf.append(sw.toString());
        }
        write(buf);
        if (showShortTag) {
            initShortLogName();
            tag = shortLogName;
        }
        switch(type) {
            case SimpleAndroidLog.LOG_LEVEL_TRACE:
                Log.v(tag, buf.toString());
                break;
            case SimpleAndroidLog.LOG_LEVEL_DEBUG:
                Log.d(tag, buf.toString());
                break;
            case SimpleAndroidLog.LOG_LEVEL_INFO:
                Log.i(tag, buf.toString());
                break;
            case SimpleAndroidLog.LOG_LEVEL_WARN:
                Log.w(tag, buf.toString());
                break;
            case SimpleAndroidLog.LOG_LEVEL_ERROR:
                Log.e(tag, buf.toString());
                break;
            case SimpleAndroidLog.LOG_LEVEL_FATAL:
                Log.e(tag, buf.toString());
                break;
        }
    }

    private void initShortLogName() {
        if (shortLogName == null) {
            shortLogName = logName.substring(logName.lastIndexOf(".") + 1);
            shortLogName = shortLogName.substring(shortLogName.lastIndexOf("/") + 1);
        }
    }

    /**
     * <p>
     * Write the content of the message accumulated in the specified <code>StringBuffer</code> to the appropriate output destination. The default
     * implementation writes to <code>System.err</code>.
     * </p>
     * 
     * @param buffer
     *            A <code>StringBuffer</code> containing the accumulated text to be logged
     */
    protected void write(StringBuffer buffer) {
    }

    /**
     * Is the given log level currently enabled?
     * 
     * @param logLevel
     *            is this level enabled?
     */
    protected boolean isLevelEnabled(int logLevel) {
        return (logLevel >= currentLogLevel);
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_DEBUG</code>.
     * 
     * @param message
     *            to log
     * @see org.apache.commons.logging.Log#debug(Object)
     */
    public final void debug(Object message) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_DEBUG)) {
            log(SimpleAndroidLog.LOG_LEVEL_DEBUG, message, null);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_DEBUG</code>.
     * 
     * @param message
     *            to log
     * @param t
     *            log this cause
     * @see org.apache.commons.logging.Log#debug(Object, Throwable)
     */
    public final void debug(Object message, Throwable t) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_DEBUG)) {
            log(SimpleAndroidLog.LOG_LEVEL_DEBUG, message, t);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_TRACE</code>.
     * 
     * @param message
     *            to log
     * @see org.apache.commons.logging.Log#trace(Object)
     */
    public final void trace(Object message) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_TRACE)) {
            log(SimpleAndroidLog.LOG_LEVEL_TRACE, message, null);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_TRACE</code>.
     * 
     * @param message
     *            to log
     * @param t
     *            log this cause
     * @see org.apache.commons.logging.Log#trace(Object, Throwable)
     */
    public final void trace(Object message, Throwable t) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_TRACE)) {
            log(SimpleAndroidLog.LOG_LEVEL_TRACE, message, t);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_INFO</code>.
     * 
     * @param message
     *            to log
     * @see org.apache.commons.logging.Log#info(Object)
     */
    public final void info(Object message) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_INFO)) {
            log(SimpleAndroidLog.LOG_LEVEL_INFO, message, null);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_INFO</code>.
     * 
     * @param message
     *            to log
     * @param t
     *            log this cause
     * @see org.apache.commons.logging.Log#info(Object, Throwable)
     */
    public final void info(Object message, Throwable t) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_INFO)) {
            log(SimpleAndroidLog.LOG_LEVEL_INFO, message, t);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_WARN</code>.
     * 
     * @param message
     *            to log
     * @see org.apache.commons.logging.Log#warn(Object)
     */
    public final void warn(Object message) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_WARN)) {
            log(SimpleAndroidLog.LOG_LEVEL_WARN, message, null);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_WARN</code>.
     * 
     * @param message
     *            to log
     * @param t
     *            log this cause
     * @see org.apache.commons.logging.Log#warn(Object, Throwable)
     */
    public final void warn(Object message, Throwable t) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_WARN)) {
            log(SimpleAndroidLog.LOG_LEVEL_WARN, message, t);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_ERROR</code>.
     * 
     * @param message
     *            to log
     * @see org.apache.commons.logging.Log#error(Object)
     */
    public final void error(Object message) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_ERROR)) {
            log(SimpleAndroidLog.LOG_LEVEL_ERROR, message, null);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_ERROR</code>.
     * 
     * @param message
     *            to log
     * @param t
     *            log this cause
     * @see org.apache.commons.logging.Log#error(Object, Throwable)
     */
    public final void error(Object message, Throwable t) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_ERROR)) {
            log(SimpleAndroidLog.LOG_LEVEL_ERROR, message, t);
        }
    }

    /**
     * Log a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_FATAL</code>.
     * 
     * @param message
     *            to log
     * @see org.apache.commons.logging.Log#fatal(Object)
     */
    public final void fatal(Object message) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_FATAL)) {
            log(SimpleAndroidLog.LOG_LEVEL_FATAL, message, null);
        }
    }

    /**
     * Logs a message with <code>org.apache.commons.logging.impl.AndroidLog.LOG_LEVEL_FATAL</code>.
     * 
     * @param message
     *            to log
     * @param t
     *            log this cause
     * @see org.apache.commons.logging.Log#fatal(Object, Throwable)
     */
    public final void fatal(Object message, Throwable t) {
        if (isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_FATAL)) {
            log(SimpleAndroidLog.LOG_LEVEL_FATAL, message, t);
        }
    }

    /**
     * <p>
     * Are debug messages currently enabled?
     * </p>
     * 
     * <p>
     * This allows expensive operations such as <code>String</code> concatenation to be avoided when the message will be ignored by the logger.
     * </p>
     */
    public final boolean isDebugEnabled() {
        return isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_DEBUG);
    }

    /**
     * <p>
     * Are error messages currently enabled?
     * </p>
     * 
     * <p>
     * This allows expensive operations such as <code>String</code> concatenation to be avoided when the message will be ignored by the logger.
     * </p>
     */
    public final boolean isErrorEnabled() {
        return isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_ERROR);
    }

    /**
     * <p>
     * Are fatal messages currently enabled?
     * </p>
     * 
     * <p>
     * This allows expensive operations such as <code>String</code> concatenation to be avoided when the message will be ignored by the logger.
     * </p>
     */
    public final boolean isFatalEnabled() {
        return isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_FATAL);
    }

    /**
     * <p>
     * Are info messages currently enabled?
     * </p>
     * 
     * <p>
     * This allows expensive operations such as <code>String</code> concatenation to be avoided when the message will be ignored by the logger.
     * </p>
     */
    public final boolean isInfoEnabled() {
        return isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_INFO);
    }

    /**
     * <p>
     * Are trace messages currently enabled?
     * </p>
     * 
     * <p>
     * This allows expensive operations such as <code>String</code> concatenation to be avoided when the message will be ignored by the logger.
     * </p>
     */
    public final boolean isTraceEnabled() {
        return isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_TRACE);
    }

    /**
     * <p>
     * Are warn messages currently enabled?
     * </p>
     * 
     * <p>
     * This allows expensive operations such as <code>String</code> concatenation to be avoided when the message will be ignored by the logger.
     * </p>
     */
    public final boolean isWarnEnabled() {
        return isLevelEnabled(SimpleAndroidLog.LOG_LEVEL_WARN);
    }
}
