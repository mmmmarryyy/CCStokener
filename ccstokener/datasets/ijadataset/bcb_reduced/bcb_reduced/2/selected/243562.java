package junit.log4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import junit.framework.TestCase;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

/**
 * Extension of <code>TestCase</code> integrating
 * <a href="http://jakarta.apache.org/log4j/index.html">Log4J</a>.
 * <p>
 *
 *
 * Created: Fri Nov 02 14:21:45 2001
 *
 * @author <a href="mailto:wolfgang@openfuture.de">Wolfgang Reissenberger</a>
 * @version $Revision: 1.4 $
 */
public class LoggedTestCase extends TestCase {

    protected Category category;

    protected boolean isLog4jInClasspath;

    private Properties properties;

    /**
     * <p>Creates a new <code>LoggedTestCase</code> instance. The 
     * <a href="http://jakarta.apache.org/log4j/index.html">Log4J</a>
     * properties are read from the file <code>propertyFile</code> from the
     * classpath. If the file does not exist, the default
     * <a href="http://jakarta.apache.org/log4j/index.html">Log4J</a>
     * properties are used.</p>
     *
     * If <a href="http://jakarta.apache.org/log4j/index.html">Log4J</a>
     * is not in the classpath, all log messages will be written to
     * <code>stdout</code>. This is to make it easy on user
     * who do not want to have to download log4j and put it in their
     * classpath ! 
     *
     * @param name name of the testcase
     * @param propertyFile file name of the property file
     * for the configuration
     */
    public LoggedTestCase(String name, String propertyFile) {
        super(name);
        this.isLog4jInClasspath = true;
        try {
            Class aClass = Class.forName("org.apache.log4j.Category");
            this.category = Category.getInstance(getClass().getName());
            Properties properties = new Properties();
            setProperties(properties);
            try {
                URL url = this.getClass().getResource("/" + propertyFile);
                if (url != null) {
                    InputStream is = url.openStream();
                    properties.load(is);
                    is.close();
                    PropertyConfigurator.configure(properties);
                    debug("Log4J successfully instantiated.");
                } else {
                    System.err.println("ERROR: cannot find " + propertyFile + " in the classpath!");
                }
            } catch (IOException e) {
                System.err.println("ERROR: cannot load " + propertyFile + "!");
            }
        } catch (ClassNotFoundException e) {
            this.isLog4jInClasspath = false;
            debug("Log4J instantiation failed. Using stdout.");
        }
    }

    /**
     * Calls {@link LoggedTestCase#LoggedTestCase(java.lang.String,
     *                                            java.lang.String)
     *              LoggedTestCase(<code>name</code>,
     *                             <code>log4j.properties</code>)}.
     *
     * @param name test case name
     */
    public LoggedTestCase(String name) {
        this(name, "log4j.properties");
    }

    /**
     * Calls {@link LoggedTestCase#LoggedTestCase(String)
     *              LoggedTestCase(<code>null</code>)}.
     *
     */
    public LoggedTestCase() {
        this(null);
    }

    /**
     * Setup of the test case. Simply writes "Set up started."
     * to the logger with {@link #info(Object) priority INFO}.
     *
     * @exception Exception if an error occurs
     */
    protected void setUp() throws Exception {
        info("Set up started.");
    }

    /**
     * Tear down of the test case. Simply writes "Tear down finished."
     * to the logger with {@link #info(Object) priority INFO}.
     *
     * @exception Exception if an error occurs
     */
    protected void tearDown() throws Exception {
        info("Tear down finished.");
    }

    /**
     * Redirection to {@link org.apache.log4j.Category#debug(Object) debug()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     */
    public void debug(Object message) {
        if (isLog4jInClasspath) {
            this.category.debug(message);
        } else {
            System.out.println("[debug]: " + message);
        }
    }

    /**
     * Redirection to
     * {@link org.apache.log4j.Category#debug(Object, Throwable) debug()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public void debug(Object message, Throwable t) {
        if (isLog4jInClasspath) {
            this.category.debug(message, t);
        } else {
            System.out.println("[debug]: " + message);
        }
    }

    /**
     * Redirection to {@link org.apache.log4j.Category#error(Object) error()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     */
    public void error(Object message) {
        if (isLog4jInClasspath) {
            this.category.error(message);
        } else {
            System.out.println("[error]: " + message);
        }
    }

    /**
     * Redirection to
     * {@link org.apache.log4j.Category#error(Object, Throwable) error()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public void error(Object message, Throwable t) {
        if (isLog4jInClasspath) {
            this.category.error(message, t);
        } else {
            System.out.println("[error]: " + message);
        }
    }

    /**
     * Redirection to {@link org.apache.log4j.Category#fatal(Object) fatal()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     */
    public void fatal(Object message) {
        if (isLog4jInClasspath) {
            this.category.fatal(message);
        } else {
            System.out.println("[fatal]: " + message);
        }
    }

    /**
     * Redirection to
     * {@link org.apache.log4j.Category#fatal(Object, Throwable) fatal()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public void fatal(Object message, Throwable t) {
        if (isLog4jInClasspath) {
            this.category.fatal(message, t);
        } else {
            System.out.println("[fatal]: " + message);
        }
    }

    /**
     * Redirection to {@link org.apache.log4j.Category#info(Object) info()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     */
    public void info(Object message) {
        if (isLog4jInClasspath) {
            this.category.info(message);
        } else {
            System.out.println("[info]: " + message);
        }
    }

    /**
     * Redirection to
     * {@link org.apache.log4j.Category#info(Object, Throwable) info()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public void info(Object message, Throwable t) {
        if (isLog4jInClasspath) {
            this.category.info(message, t);
        } else {
            System.out.println("[info]: " + message);
        }
    }

    /**
     * Redirection to {@link org.apache.log4j.Category#warn(Object) warn()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     */
    public void warn(Object message) {
        if (isLog4jInClasspath) {
            this.category.warn(message);
        } else {
            System.out.println("[warn]: " + message);
        }
    }

    /**
     * Redirection to
     * {@link org.apache.log4j.Category#warn(Object, Throwable) warn()}
     * in  the testcase {@link #category category}.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public void warn(Object message, Throwable t) {
        if (isLog4jInClasspath) {
            this.category.warn(message, t);
        } else {
            System.out.println("[warn]: " + message);
        }
    }

    /**
     * Get the value of properties.
     * @return value of properties.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Set the value of properties.
     * @param v  Value to assign to properties.
     */
    public void setProperties(Properties v) {
        this.properties = v;
    }
}
