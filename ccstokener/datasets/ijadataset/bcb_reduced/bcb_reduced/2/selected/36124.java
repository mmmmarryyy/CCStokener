package org.apache.ws.jaxme.logging;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/** <p>This class implements access to the Loggers through static
 * methods. The class typically configures itself from the
 * environment. However, you may choose to configure the class
 * explicitly by invoking {@link #setLoggerFactory(LoggerFactory)}.
 *
 * @author <a href="mailto:joe@ispsoft.de">Jochen Wiedmann</a>
 */
public class LoggerAccess {

    private static LoggerFactory theFactory;

    /** <p>Sets the logger factory.</p>
    */
    public static synchronized void setLoggerFactory(LoggerFactory pFactory) {
        theFactory = pFactory;
    }

    /** <p>Instantiates the given {@link LoggerFactory}.</p>
    */
    private static LoggerFactory newLoggerFactory(String pName) {
        Class c = null;
        Throwable t = null;
        try {
            c = Class.forName(pName);
        } catch (Throwable th) {
            t = th;
        }
        if (c == null) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = LoggerAccess.class.getClassLoader();
                    if (cl == null) {
                        cl = ClassLoader.getSystemClassLoader();
                    }
                }
                c = cl.loadClass(pName);
            } catch (Throwable th) {
                if (t == null) {
                    t = th;
                }
            }
        }
        if (c == null) {
            if (t == null) {
                t = new ClassNotFoundException(pName);
            }
            t.printStackTrace();
            return null;
        }
        try {
            return (LoggerFactory) c.newInstance();
        } catch (Throwable th) {
            t.printStackTrace();
            return null;
        }
    }

    /** <p>Creates a new instance of {@link LoggerFactory}. The
    * implementation class is determined as follows:
    * <ol>
    *   <li>If the system property <code>org.apache.ws.jaxme.logging.LoggerFactory</code>
    *     is set, uses the given class name.</li>
    *   <li>If the resource
    *     <code>META-INF/services/org.apache.ws.jaxme.logging.LoggerFactory</code>
    *     exists, uses the given class name.</li>
    *   <li>Otherwise returns a default instance logging to
    *     <code>System.err</code>.</li>
    * </ol>
    */
    public static LoggerFactory newLoggerFactory() {
        String p = LoggerFactory.class.getName();
        String v = System.getProperty(p);
        LoggerFactory result;
        if (v != null) {
            result = newLoggerFactory(v);
            if (result != null) {
                return result;
            }
        }
        String res = "META-INF/services/" + p;
        ClassLoader cl = LoggerAccess.class.getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        URL url = cl.getResource(res);
        if (url == null) {
            cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                url = cl.getResource(res);
            }
        }
        if (url != null) {
            InputStream istream = null;
            try {
                istream = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
                v = reader.readLine();
                if (v != null) {
                    result = newLoggerFactory(v);
                }
                istream.close();
                istream = null;
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (istream != null) {
                    try {
                        istream.close();
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
        return new LoggerFactoryImpl() {

            public Logger newLogger(String pName) {
                return new LoggerImpl(pName);
            }
        };
    }

    /** <p>Returns the logger factory. If a logger factory is set (by
    * previous calls to {@link #newLoggerFactory()} or
    * {@link #setLoggerFactory(LoggerFactory)}), returns that factory. Otherwise
    * invokes these methods and returns the result.</p>
    */
    public static synchronized LoggerFactory getLoggerFactory() {
        if (theFactory == null) {
            setLoggerFactory(newLoggerFactory());
        }
        return theFactory;
    }

    /** <p>Returns a new logger with the given name.</p>
    */
    public static synchronized Logger getLogger(String pName) {
        LoggerFactory factory = getLoggerFactory();
        return factory.getLogger(pName);
    }

    /** <p>Shortcut for <code>getLogger(pClass.getName())</code>.</p>
    */
    public static synchronized Logger getLogger(Class pClass) {
        return getLogger(pClass.getName());
    }
}
