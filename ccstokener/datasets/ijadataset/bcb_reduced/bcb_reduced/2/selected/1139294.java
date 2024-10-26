package DE.FhG.IGD.logging;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import DE.FhG.IGD.io.VariableSubstitutionInputStream;
import DE.FhG.IGD.util.URL;
import DE.FhG.IGD.util.Variables;
import DE.FhG.IGD.util.VariablesContext;

/**
 * This is a static wrapper class for the current logging configuration.
 * It provides a bunch of methods to access the configuration properties.
 * The configuration will be read from a property file whose name will be
 * taken from the <tt>DE.FhG.IGD.logging.config</tt> Java system property.
 * <p>Supported properties are:
 * <ul>
 * <li><tt>logger=sun|log4j</tt>: The actual logger implementation to be
 * used. (Default value is <tt>sun</tt>.)
 * <li><tt>interval=0|off|&lt;millis&gt;|&lt;secs&gt;s|&lt;mins&gt;m|&lt;hrs&gt;h|&lt;days&gt;d</tt>:
 * The interval between two anchor timestamps (if set to zero or <tt>off</tt>
 * the full timestamp will be written into every log record, default interval
 * is 1 minute).
 * <li><tt>[package-name],output=&lt;URL&gt;[,buffer=0|off|&lt;msgs&gt;|&lt;msgs(/k)&gt;k|&lt;msgs(/M)&gt;m]</tt>
 * (where <tt>k=1,000</tt> and <tt>M=1,000,000</tt>; e.g. <tt>1.5k</tt> means
 * 1,500 messages): The output URL for a specific package and an optional
 * buffersize for asynchronous message dispatching (if set to zero or
 * <tt>off</tt> synchronous dispatching will be used). To configure the root
 * logger simply omit the package name. The root logger always needs to be
 * configured. If omitted the default
 * (<tt>file:///./logging.out,buffer=0</tt>) will be used. Besides
 * <a href="http://sunsite.auc.dk/RFC/rfc/rfc1738.html" target="_blank">RFC 1738</a>
 * compliant URLs the following special output identifiers will be accepted
 * for writing to a TCP socket:
 * <ul>
 * <li><tt>socket://&lt;host&gt;:&lt;port&gt;</tt>
 * <li><tt>raw://&lt;host&gt;:&lt;port&gt;</tt>
 * </ul>
 * In case the maximum buffer size is omitted the default (which depends on
 * the protocol identifier) will be used:
 * <ul>
 * <li>for files: <tt>buffer=0</tt> (buffering disabled)
 * <li>in all other cases: <tt>buffer=10000</tt>
 * </ul>
 * <li><tt>[package-name],loglevel=all|trace|debug|info|warning|error|severe|fatal|off</tt>
 * configures a specific package to get logged at a specific log level.
 * If the package name is omitted the log level of the root logger (which
 * always needs to be configured) will be set. The default log level (for an
 * unconfigured root logger) is <tt>info</tt>.
 * </ul>
 * <p><b>NOTICE:</b> For dynamic configuration system properties and SeMoA
 * variables context may be used like <tt>${&lt;property-name&gt;}</tt>. 
 * For example, to make the output file of the package <tt>foo.bar</tt> 
 * depend on the system property <tt>log.dir</tt> the following line should 
 * be put into the configuration file: 
 * <tt>foo.bar,output=file:///${log.dir}/foobar.log</tt>
 *
 * @author <a href="mailto:mpressfr@igd.fhg.de">Matthias Pressfreund</a>
 * @author Jan Peters
 * @version "$Id: Configuration.java 1306 2004-01-20 11:49:07Z jpeters $"
 */
public class Configuration {

    /**
     * The name of the Java property that provides the configuration
     * file name
     */
    protected static final String CONFIG_FILE_PROPERTY_ = "DE.FhG.IGD.logging.config";

    /**
     * The name of the <i>logger</i> property
     */
    protected static final String PROPERTY_LOGGER_ = "logger";

    /**
     * The <i>Sun</i> logger property
     */
    protected static final String LOGGER_SUN_ = "sun";

    /**
     * The <i>Sun</i> logger class
     */
    protected static final String LOGGER_SUN_CLASS_ = "DE.FhG.IGD.logging.sun.SunLogger";

    /**
     * The <i>Log4J</i> logger property
     */
    protected static final String LOGGER_LOG4J_ = "log4j";

    /**
     * The <i>Log4J</i> logger class
     */
    protected static final String LOGGER_LOG4J_CLASS_ = "DE.FhG.IGD.logging.log4j.Log4jLogger";

    /**
     * The default <i>logger</i> property value
     */
    protected static final String LOGGER_DEFAULT_ = Configuration.LOGGER_SUN_;

    /**
     * The name of the <i>interval</i> property
     */
    protected static final String PROPERTY_INTERVAL_ = "interval";

    /**
     * The default <i>interval</i> property value
     */
    protected static final long INTERVAL_DEFAULT_ = 60000;

    /**
     * The name of the <i>output</i> property
     */
    protected static final String PROPERTY_OUTPUT_ = "output";

    /**
     * The protocol identifier for an output file
     */
    protected static final String OUTPUT_PROTOCOL_IDS_FILE = "file";

    /**
     * The protocol identifier for an output socket
     */
    protected static final String OUTPUT_PROTOCOL_IDS_SOCKET = "socket raw";

    /**
     * The delimiter between the output URL and the buffersize
     */
    protected static final String OUTPUT_BUFFERSIZE_DELIMITER = ",buffer=";

    /**
     * The default <i>buffersize</i> value (except for files)
     */
    protected static final int OUTPUT_BUFFERSIZE_DEFAULT = 10000;

    /**
     * The default <i>output</i> property value
     */
    public static final String OUTPUT_DEFAULT = "file:///./logging.out" + Configuration.OUTPUT_BUFFERSIZE_DELIMITER + "0";

    /**
     * The name of the <i>loglevel</i> property
     */
    protected static final String PROPERTY_LOGLEVEL_ = "loglevel";

    /**
     * The default <i>loglevel</i> property value
     */
    public static final LogLevel LOGLEVEL_DEFAULT = LogLevel.INFO;

    /**
     * The system dependent line separator
     */
    public static final String CR = System.getProperty("line.separator");

    /**
     * The <i>logger</i> property value
     */
    protected static String logger_;

    /**
     * The <i>interval</i> property value
     */
    protected static long interval_;

    /**
     * The storage for user configured output,
     * package names are used as keys
     */
    protected static Map outputs_;

    /**
     * The storage for user configured log levels,
     * package names are used as keys
     */
    protected static Map loglevels_;

    static {
        Properties properties;
        VariablesContext vc;
        Map variables;
        String value;
        String key;
        String pkg;
        Iterator i;
        int prpidx;
        properties = new Properties();
        variables = new HashMap();
        for (i = System.getProperties().keySet().iterator(); i.hasNext(); ) {
            key = (String) i.next();
            value = (String) System.getProperty(key);
            if (value != null) {
                value = value.replace('\\', '/');
            }
            variables.put(key, value);
        }
        if ((vc = Variables.getContext()) != null) {
            for (i = vc.keys(); i.hasNext(); ) {
                key = (String) i.next();
                value = (String) vc.get(key);
                if (value != null) {
                    value = value.replace('\\', '/');
                }
                variables.put(key, value);
            }
        }
        try {
            properties.load(new VariableSubstitutionInputStream(new FileInputStream(System.getProperty(Configuration.CONFIG_FILE_PROPERTY_)), variables));
        } catch (Exception e) {
            throw new LoggerException("Cannot read logging configuration");
        }
        logger_ = properties.getProperty(Configuration.PROPERTY_LOGGER_, Configuration.LOGGER_DEFAULT_);
        if (logger_.equalsIgnoreCase(Configuration.LOGGER_SUN_)) {
            logger_ = Configuration.LOGGER_SUN_CLASS_;
        } else if (logger_.equalsIgnoreCase(Configuration.LOGGER_LOG4J_)) {
            logger_ = Configuration.LOGGER_LOG4J_CLASS_;
        } else {
            throw new LoggerException("Unknown logger configured: " + logger_);
        }
        value = properties.getProperty(Configuration.PROPERTY_INTERVAL_, String.valueOf(Configuration.INTERVAL_DEFAULT_));
        interval_ = Configuration.parseInterval(value);
        outputs_ = new HashMap(0);
        loglevels_ = new HashMap(0);
        for (i = properties.keySet().iterator(); i.hasNext(); ) {
            key = (String) i.next();
            prpidx = key.lastIndexOf(",");
            if (prpidx >= 0) {
                pkg = key.substring(0, prpidx);
                value = properties.getProperty(key);
                if (key.endsWith(Configuration.PROPERTY_OUTPUT_)) {
                    outputs_.put(pkg, Configuration.parseOutput(value));
                } else if (key.endsWith(Configuration.PROPERTY_LOGLEVEL_)) {
                    if (LogLevel.exists(value)) {
                        loglevels_.put(pkg, value);
                    } else {
                        throw new LoggerException("Invalid log level configured: " + value);
                    }
                }
            }
        }
    }

    /**
     * Hidden unused construction.
     */
    private Configuration() {
    }

    /**
     * Get the value of the <i>logger</i> implementation property.
     *
     * @return The <i>logger</i> implementation property value
     */
    public static String getLogger() {
        return Configuration.logger_;
    }

    /**
     * Get the value of the <i>interval</i> property which is the time
     * difference between two anchor timestamps.
     *
     * @return The <i>interval</i> property value
     */
    public static long getInterval() {
        return Configuration.interval_;
    }

    /**
     * Get the output destination (URL and buffersize) of a logger by its
     * package name. Passing an empty <code>String</code> will return the
     * output of the root logger.
     * If there is no output configured for the given package name
     * <code>null</code> will be returned.
     *
     * @param pkg The package name
     * @return The corresponding output (URL with buffersize) or
     *   <code>null</code> if there is no output configured for the
     *   given package name
     */
    public static Output getOutput(String pkg) {
        return (Output) outputs_.get(pkg);
    }

    /**
     * Get the log level of a logger by its package name. An empty
     * <code>String</code> will return the log level of the root logger.
     * If there is no log level configured for the given package name
     * <code>null</code> will be returned.
     *
     * @param pkg The package name
     * @return The corresponding log level or <code>null</code> if there is
     *   no log level configured for the given package name
     */
    public static LogLevel getLogLevel(String pkg) {
        return LogLevel.toLevel((String) loglevels_.get(pkg));
    }

    /**
     * Parse an <i>interval</i> <code>String</code> into a
     * <code>long</code> value. The parser can read <code>int</code> and
     * <code>float</code> values with or without a trailing exponents:
     * <ul>
     * <li><tt>s</tt> for seconds
     * <li><tt>m</tt> for minutes
     * <li><tt>h</tt> for hours
     * <li><tt>d</tt> for days
     * </ul>
     *
     * @param interval The <i>interval</i> <code>String</code> to be parsed
     * @return The resulting <i>interval</i> value in millis
     * @exception DE.FhG.IGD.logging.LoggerException
     *   if the given value could not be parsed
     */
    protected static long parseInterval(String interval) throws LoggerException {
        float tmpsize;
        String value;
        char exp;
        value = interval.trim().toLowerCase();
        if (value.equals("all") || value.equals("off") || value.startsWith("no")) {
            tmpsize = 0;
        } else {
            try {
                tmpsize = Float.parseFloat(value);
                if (tmpsize < 0) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e1) {
                try {
                    tmpsize = Float.parseFloat(value.substring(0, value.length() - 1));
                    if (tmpsize < 0) {
                        throw new IllegalArgumentException();
                    }
                    if ((exp = value.charAt(value.length() - 1)) == 's') {
                        tmpsize *= 1000;
                    } else if (exp == 'm') {
                        tmpsize *= 1000 * 60;
                    } else if (exp == 'h') {
                        tmpsize *= 1000 * 60 * 60;
                    } else if (exp == 'd') {
                        tmpsize *= 1000 * 60 * 60 * 24;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } catch (Exception e2) {
                    throw new LoggerException("Invalid interval configured: " + value);
                }
            }
        }
        return (long) tmpsize;
    }

    /**
     * Parse an <i>output</i> <code>String</code> into an
     * <code>Output</code> value. The parser can read <code>int</code> and
     * <code>float</code> values with or without a trailing exponents for the
     * buffersize:
     * <ul>
     * <li><tt>k</tt> for kil
     * <li><tt>M</tt> for mega
     * </ul>
     *
     * @param value The <i>output</i> <code>String</code> to be parsed
     * @return The resulting <i>output</i> value
     * @exception DE.FhG.IGD.logging.LoggerException
     *   if the given value could not be parsed
     */
    protected static Output parseOutput(String output) throws LoggerException {
        String buftmp;
        String urltmp;
        float tmpsize;
        String value;
        int bufidx;
        char exp;
        URL url;
        value = output.trim();
        bufidx = value.indexOf(Configuration.OUTPUT_BUFFERSIZE_DELIMITER);
        urltmp = bufidx < 0 ? value : value.substring(0, bufidx);
        try {
            url = new URL(urltmp);
        } catch (MalformedURLException e) {
            throw new LoggerException("Invalid output URL configured: " + urltmp);
        }
        if (bufidx >= 0) {
            buftmp = value.substring(bufidx + Configuration.OUTPUT_BUFFERSIZE_DELIMITER.length()).toLowerCase();
            if (buftmp.equals("off") || buftmp.startsWith("no")) {
                tmpsize = 0;
            } else {
                try {
                    tmpsize = Float.parseFloat(buftmp);
                    if (tmpsize < 0) {
                        throw new IllegalArgumentException();
                    }
                } catch (Exception e1) {
                    try {
                        if (buftmp.length() <= 1) {
                            throw new IllegalArgumentException();
                        }
                        tmpsize = Float.parseFloat(buftmp.substring(0, buftmp.length() - 1));
                        if (tmpsize < 0) {
                            throw new IllegalArgumentException();
                        }
                        if ((exp = buftmp.charAt(buftmp.length() - 1)) == 'k') {
                            tmpsize *= 1000;
                        } else if (exp == 'm') {
                            tmpsize *= 1000000;
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } catch (Exception e2) {
                        throw new LoggerException("Invalid buffersize " + "configured: " + buftmp);
                    }
                }
            }
        } else {
            if (Configuration.OUTPUT_PROTOCOL_IDS_FILE.indexOf(url.getProtocol()) >= 0) {
                tmpsize = 0;
            } else {
                tmpsize = (float) Configuration.OUTPUT_BUFFERSIZE_DEFAULT;
            }
        }
        return new Output(url, (int) tmpsize);
    }

    /**
     * This is a wrapper class for an output destination. It contains the
     * target URL and the size of the buffer required for asynchronous
     * data transfer.
     */
    public static class Output {

        /**
         * The target URL
         */
        protected URL url_;

        /**
         * The buffer size
         */
        protected int bufsize_;

        /**
         * Create an <code>Output</code> object.
         *
         * @param url The target URL
         * @param bufsize The buffer size
         */
        public Output(URL url, int bufsize) {
            url_ = url;
            bufsize_ = bufsize;
        }

        public URL getURL() {
            return url_;
        }

        public int getBufferSize() {
            return bufsize_;
        }

        /**
         * Get the output stream of the specified URL.
         *
         * @exception DE.FhG.IGD.logging.LoggerException
         *   if the specified output URL is invalid or does not provide an
         *   output stream or an error occurred while accessing the stream
         */
        public OutputStream openStream() throws LoggerException {
            OutputStream os;
            String protocol;
            protocol = url_.getProtocol().toLowerCase();
            try {
                if (Configuration.OUTPUT_PROTOCOL_IDS_FILE.indexOf(protocol) >= 0) {
                    os = new FileOutputStream(url_.getPath(), false);
                } else if (Configuration.OUTPUT_PROTOCOL_IDS_SOCKET.indexOf(protocol) >= 0) {
                    os = new Socket(url_.getHost(), url_.getPort()).getOutputStream();
                } else {
                    os = new java.net.URL(protocol, url_.getHost(), url_.getPort(), url_.getPath()).openConnection().getOutputStream();
                }
            } catch (Exception e) {
                throw new LoggerException("Failed opening output stream: " + url_);
            }
            return os;
        }
    }
}
