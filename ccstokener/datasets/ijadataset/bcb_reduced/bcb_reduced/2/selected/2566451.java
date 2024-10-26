package javax.xml.bind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import static javax.xml.bind.JAXBContext.JAXB_CONTEXT_FACTORY;

/**
 * This class is package private and therefore is not exposed as part of the 
 * JAXB API.
 *
 * This code is designed to implement the JAXB 1.0 spec pluggability feature
 *
 * @author <ul><li>Ryan Shoemaker, Sun Microsystems, Inc.</li></ul>
 * @version $Revision$
 * @see JAXBContext
 */
class ContextFinder {

    private static final Logger logger;

    static {
        logger = Logger.getLogger("javax.xml.bind");
        try {
            if (AccessController.doPrivileged(new GetPropertyAction("jaxb.debug")) != null) {
                logger.setUseParentHandlers(false);
                logger.setLevel(Level.ALL);
                ConsoleHandler handler = new ConsoleHandler();
                handler.setLevel(Level.ALL);
                logger.addHandler(handler);
            } else {
            }
        } catch (Throwable t) {
        }
    }

    /**
     * If the {@link InvocationTargetException} wraps an exception that shouldn't be wrapped,
     * throw the wrapped exception.
     */
    private static void handleInvocationTargetException(InvocationTargetException x) throws JAXBException {
        Throwable t = x.getTargetException();
        if (t != null) {
            if (t instanceof JAXBException) throw (JAXBException) t;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
        }
    }

    /**
     * Determine if two types (JAXBContext in this case) will generate a ClassCastException.
     *
     * For example, (targetType)originalType
     *
     * @param originalType
     *          The Class object of the type being cast
     * @param targetType
     *          The Class object of the type that is being cast to
     * @return JAXBException to be thrown.
     */
    private static JAXBException handleClassCastException(Class originalType, Class targetType) {
        final URL targetTypeURL = which(targetType);
        return new JAXBException(Messages.format(Messages.ILLEGAL_CAST, originalType.getClassLoader().getResource("javax/xml/bind/JAXBContext.class"), targetTypeURL));
    }

    /**
     * Create an instance of a class using the specified ClassLoader
     */
    static JAXBContext newInstance(String contextPath, String className, ClassLoader classLoader, Map properties) throws JAXBException {
        try {
            Class spiClass = safeLoadClass(className, classLoader);
            Object context = null;
            try {
                Method m = spiClass.getMethod("createContext", String.class, ClassLoader.class, Map.class);
                context = m.invoke(null, contextPath, classLoader, properties);
            } catch (NoSuchMethodException e) {
            }
            if (context == null) {
                Method m = spiClass.getMethod("createContext", String.class, ClassLoader.class);
                context = m.invoke(null, contextPath, classLoader);
            }
            if (!(context instanceof JAXBContext)) {
                handleClassCastException(context.getClass(), JAXBContext.class);
            }
            return (JAXBContext) context;
        } catch (ClassNotFoundException x) {
            throw new JAXBException(Messages.format(Messages.PROVIDER_NOT_FOUND, className), x);
        } catch (InvocationTargetException x) {
            handleInvocationTargetException(x);
            Throwable e = x;
            if (x.getTargetException() != null) e = x.getTargetException();
            throw new JAXBException(Messages.format(Messages.COULD_NOT_INSTANTIATE, className, e), e);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new JAXBException(Messages.format(Messages.COULD_NOT_INSTANTIATE, className, x), x);
        }
    }

    /**
     * Create an instance of a class using the specified ClassLoader
     */
    static JAXBContext newInstance(Class[] classes, Map properties, String className) throws JAXBException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class spi;
        try {
            spi = safeLoadClass(className, cl);
        } catch (ClassNotFoundException e) {
            throw new JAXBException(e);
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("loaded " + className + " from " + which(spi));
        }
        Method m;
        try {
            m = spi.getMethod("createContext", Class[].class, Map.class);
        } catch (NoSuchMethodException e) {
            throw new JAXBException(e);
        }
        try {
            Object context = m.invoke(null, classes, properties);
            if (!(context instanceof JAXBContext)) {
                throw handleClassCastException(context.getClass(), JAXBContext.class);
            }
            return (JAXBContext) context;
        } catch (IllegalAccessException e) {
            throw new JAXBException(e);
        } catch (InvocationTargetException e) {
            handleInvocationTargetException(e);
            Throwable x = e;
            if (e.getTargetException() != null) x = e.getTargetException();
            throw new JAXBException(x);
        }
    }

    static JAXBContext find(String factoryId, String contextPath, ClassLoader classLoader, Map properties) throws JAXBException {
        final String jaxbContextFQCN = JAXBContext.class.getName();
        StringBuilder propFileName;
        StringTokenizer packages = new StringTokenizer(contextPath, ":");
        String factoryClassName;
        if (!packages.hasMoreTokens()) throw new JAXBException(Messages.format(Messages.NO_PACKAGE_IN_CONTEXTPATH));
        logger.fine("Searching jaxb.properties");
        while (packages.hasMoreTokens()) {
            String packageName = packages.nextToken(":").replace('.', '/');
            propFileName = new StringBuilder().append(packageName).append("/jaxb.properties");
            Properties props = loadJAXBProperties(classLoader, propFileName.toString());
            if (props != null) {
                if (props.containsKey(factoryId)) {
                    factoryClassName = props.getProperty(factoryId);
                    return newInstance(contextPath, factoryClassName, classLoader, properties);
                } else {
                    throw new JAXBException(Messages.format(Messages.MISSING_PROPERTY, packageName, factoryId));
                }
            }
        }
        logger.fine("Searching the system property");
        factoryClassName = AccessController.doPrivileged(new GetPropertyAction(jaxbContextFQCN));
        if (factoryClassName != null) {
            return newInstance(contextPath, factoryClassName, classLoader, properties);
        }
        logger.fine("Searching META-INF/services");
        BufferedReader r;
        try {
            final StringBuilder resource = new StringBuilder().append("META-INF/services/").append(jaxbContextFQCN);
            final InputStream resourceStream = classLoader.getResourceAsStream(resource.toString());
            if (resourceStream != null) {
                r = new BufferedReader(new InputStreamReader(resourceStream, "UTF-8"));
                factoryClassName = r.readLine().trim();
                r.close();
                return newInstance(contextPath, factoryClassName, classLoader, properties);
            } else {
                logger.fine("Unable to load:" + resource.toString());
            }
        } catch (UnsupportedEncodingException e) {
            throw new JAXBException(e);
        } catch (IOException e) {
            throw new JAXBException(e);
        }
        logger.fine("Trying to create the platform default provider");
        return newInstance(contextPath, PLATFORM_DEFAULT_FACTORY_CLASS, classLoader, properties);
    }

    static JAXBContext find(Class[] classes, Map properties) throws JAXBException {
        final String jaxbContextFQCN = JAXBContext.class.getName();
        String factoryClassName;
        for (final Class c : classes) {
            ClassLoader classLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                public ClassLoader run() {
                    return c.getClassLoader();
                }
            });
            Package pkg = c.getPackage();
            if (pkg == null) continue;
            String packageName = pkg.getName().replace('.', '/');
            String resourceName = packageName + "/jaxb.properties";
            logger.fine("Trying to locate " + resourceName);
            Properties props = loadJAXBProperties(classLoader, resourceName);
            if (props == null) {
                logger.fine("  not found");
            } else {
                logger.fine("  found");
                if (props.containsKey(JAXB_CONTEXT_FACTORY)) {
                    factoryClassName = props.getProperty(JAXB_CONTEXT_FACTORY).trim();
                    return newInstance(classes, properties, factoryClassName);
                } else {
                    throw new JAXBException(Messages.format(Messages.MISSING_PROPERTY, packageName, JAXB_CONTEXT_FACTORY));
                }
            }
        }
        logger.fine("Checking system property " + jaxbContextFQCN);
        factoryClassName = AccessController.doPrivileged(new GetPropertyAction(jaxbContextFQCN));
        if (factoryClassName != null) {
            logger.fine("  found " + factoryClassName);
            return newInstance(classes, properties, factoryClassName);
        }
        logger.fine("  not found");
        logger.fine("Checking META-INF/services");
        BufferedReader r;
        try {
            final String resource = new StringBuilder("META-INF/services/").append(jaxbContextFQCN).toString();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resourceURL;
            if (classLoader == null) resourceURL = ClassLoader.getSystemResource(resource); else resourceURL = classLoader.getResource(resource);
            if (resourceURL != null) {
                logger.fine("Reading " + resourceURL);
                r = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "UTF-8"));
                factoryClassName = r.readLine().trim();
                return newInstance(classes, properties, factoryClassName);
            } else {
                logger.fine("Unable to find: " + resource);
            }
        } catch (UnsupportedEncodingException e) {
            throw new JAXBException(e);
        } catch (IOException e) {
            throw new JAXBException(e);
        }
        logger.fine("Trying to create the platform default provider");
        return newInstance(classes, properties, PLATFORM_DEFAULT_FACTORY_CLASS);
    }

    private static Properties loadJAXBProperties(ClassLoader classLoader, String propFileName) throws JAXBException {
        Properties props = null;
        try {
            URL url;
            if (classLoader == null) url = ClassLoader.getSystemResource(propFileName); else url = classLoader.getResource(propFileName);
            if (url != null) {
                logger.fine("loading props from " + url);
                props = new Properties();
                InputStream is = url.openStream();
                props.load(is);
                is.close();
            }
        } catch (IOException ioe) {
            logger.log(Level.FINE, "Unable to load " + propFileName, ioe);
            throw new JAXBException(ioe.toString(), ioe);
        }
        return props;
    }

    /**
     * Search the given ClassLoader for an instance of the specified class and
     * return a string representation of the URL that points to the resource.
     *
     * @param clazz
     *          The class to search for
     * @param loader
     *          The ClassLoader to search.  If this parameter is null, then the
     *          system class loader will be searched
     * @return
     *          the URL for the class or null if it wasn't found
     */
    static URL which(Class clazz, ClassLoader loader) {
        String classnameAsResource = clazz.getName().replace('.', '/') + ".class";
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        return loader.getResource(classnameAsResource);
    }

    /**
     * Get the URL for the Class from it's ClassLoader.
     *
     * Convenience method for {@link #which(Class, ClassLoader)}.
     *
     * Equivalent to calling: which(clazz, clazz.getClassLoader())
     *
     * @param clazz
     *          The class to search for
     * @return
     *          the URL for the class or null if it wasn't found
     */
    static URL which(Class clazz) {
        return which(clazz, clazz.getClassLoader());
    }

    /**
     * When JAXB is in J2SE, rt.jar has to have a JAXB implementation.
     * However, rt.jar cannot have META-INF/services/javax.xml.bind.JAXBContext
     * because if it has, it will take precedence over any file that applications have
     * in their jar files.
     *
     * <p>
     * When the user bundles his own JAXB implementation, we'd like to use it, and we
     * want the platform default to be used only when there's no other JAXB provider.
     *
     * <p>
     * For this reason, we have to hard-code the class name into the API.
     */
    private static final String PLATFORM_DEFAULT_FACTORY_CLASS = "com.sun.xml.internal.bind.v2.ContextFactory";

    /**
     * Loads the class, provided that the calling thread has an access to the class being loaded.
     */
    private static Class safeLoadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        logger.fine("Trying to load " + className);
        try {
            SecurityManager s = System.getSecurityManager();
            if (s != null) {
                int i = className.lastIndexOf('.');
                if (i != -1) {
                    s.checkPackageAccess(className.substring(0, i));
                }
            }
            if (classLoader == null) return Class.forName(className); else return classLoader.loadClass(className);
        } catch (SecurityException se) {
            if (PLATFORM_DEFAULT_FACTORY_CLASS.equals(className)) return Class.forName(className);
            throw se;
        }
    }
}
