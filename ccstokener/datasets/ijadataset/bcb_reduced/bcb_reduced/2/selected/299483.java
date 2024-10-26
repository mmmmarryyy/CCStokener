package net.sf.jasperreports.jsf.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.sf.jasperreports.jsf.Constants;

/**
 * Utility class to handle the different kind of pluggable services required
 * by the implementation.
 *
 * @author A. Alonso Dominguez
 */
public final class Services {

    /** The logger. */
    private static final Logger logger = Logger.getLogger(Services.class.getPackage().getName(), Constants.LOG_MESSAGES_BUNDLE);

    /** The root folder from where load service resource files. */
    private static final String SERVICES_ROOT = "META-INF/services/";

    /**
     * Loads a chain of services from the classpath.
     * <p>
     * Services are looked up following convention
     * <tt>META-INF/services/[serviceClassName]</tt>. Service chains must be
     * configured the same way that service sets but implementation classes
     * must have a public constructor accepting another instance of the same
     * service that may used as a delegator when implementing class doesn't know
     * how to handle the request.
     *
     * @param <T>             Service type to be obtained.
     * @param clazz           Service interface class object.
     * @param defaultInstance Last service in the chain. May be
     *        <code>null</code>.
     *
     * @return Chained service implementations offered as a single instance.
     * @throws ServiceException If any error happens when loading the
     *         service chain.
     */
    @SuppressWarnings("unchecked")
    public static <T> T chain(final Class<T> clazz, final T defaultInstance) throws ServiceException {
        final ClassLoader loader = Util.getClassLoader(null);
        final Enumeration<URL> resources = getServiceResources(clazz, loader);
        T current = defaultInstance;
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            BufferedReader reader = null;
            try {
                String line;
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                while (null != (line = reader.readLine())) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    Class<T> serviceClass;
                    try {
                        serviceClass = (Class<T>) loader.loadClass(line);
                    } catch (final ClassNotFoundException e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0014");
                        logRecord.setParameters(new Object[] { line });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    Constructor<T> constructor;
                    try {
                        if (current == null) {
                            constructor = serviceClass.getConstructor();
                        } else {
                            constructor = serviceClass.getConstructor(clazz);
                        }
                    } catch (Exception e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0027");
                        logRecord.setParameters(new Object[] { line });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    T instance;
                    try {
                        if (current == null) {
                            instance = constructor.newInstance();
                        } else {
                            instance = constructor.newInstance(current);
                        }
                    } catch (Exception e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0015");
                        logRecord.setParameters(new Object[] { line });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    current = instance;
                }
            } catch (IOException e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0012");
                logRecord.setParameters(new Object[] { url });
                logRecord.setThrown(e);
                logRecord.setResourceBundleName(logger.getResourceBundleName());
                logRecord.setResourceBundle(logger.getResourceBundle());
                logger.log(logRecord);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        ;
                    }
                    reader = null;
                }
            }
        }
        return current;
    }

    /**
     * Loads a service set from the classpath.
     * <p>
     * Services are looked up following convention
     * <tt>META-INF/services/[serviceClassName]</tt>. Services set configuration
     * files must contain a list of service implementation classes.
     *
     * @param <T>   Service type to be obtained.
     * @param clazz Service interface class object.
     * @return a set of services of the requested type.
     * @throws ServiceException If any error happens when loading the
     *         service chain.
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> set(final Class<T> clazz) throws ServiceException {
        final ClassLoader loader = Util.getClassLoader(null);
        final Enumeration<URL> resources = getServiceResources(clazz, loader);
        final Set<T> serviceSet = new HashSet<T>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            BufferedReader reader = null;
            try {
                String line;
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                while (null != (line = reader.readLine())) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    Class<T> serviceClass;
                    try {
                        serviceClass = (Class<T>) loader.loadClass(line);
                    } catch (final ClassNotFoundException e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0014");
                        logRecord.setParameters(new Object[] { line });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    T instance;
                    try {
                        instance = serviceClass.newInstance();
                    } catch (final Exception e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0015");
                        logRecord.setParameters(new Object[] { line });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    serviceSet.add(instance);
                }
            } catch (final IOException e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0012");
                logRecord.setParameters(new Object[] { url });
                logRecord.setThrown(e);
                logRecord.setResourceBundleName(logger.getResourceBundleName());
                logRecord.setResourceBundle(logger.getResourceBundle());
                logger.log(logRecord);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        ;
                    }
                }
            }
        }
        return Collections.unmodifiableSet(serviceSet);
    }

    /**
     * Loads a singleton instance for the service interface given.
     * <p>
     * This method first tries to find a System property in the form
     * <tt>com.example.ServiceInterface</tt> pointing to the specific
     * implementation of the service.
     * <p>
     * If such property doesn't exist then the services is looked up
     * following convention <tt>META-INF/services/[serviceClassName]</tt>
     * but using a resource immediately inside the current application
     * classpath.
     *
     * @param clazz service interface class object
     * @param defaultInstance default service instance to be used if
     *                        no suitable provider is found.
     * @param <T> service type to be obtained
     * @return an instance of a service provider implementing the given
     *         interface
     * @throws ServiceException If any error happens when loading the
     *         service chain.
     */
    public static <T> T single(final Class<T> clazz, final T defaultInstance) throws ServiceException {
        final ClassLoader loader = Util.getClassLoader(null);
        String providerClassName = System.getProperty(clazz.getName());
        if (providerClassName == null) {
            String serviceResource = SERVICES_ROOT + clazz.getName();
            InputStream stream = loader.getResourceAsStream(serviceResource);
            if (stream != null) {
                BufferedReader reader = null;
                try {
                    String line;
                    reader = new BufferedReader(new InputStreamReader(stream));
                    while (null != (line = reader.readLine())) {
                        if (line.startsWith("#")) {
                            continue;
                        }
                        providerClassName = line;
                        break;
                    }
                } catch (IOException e) {
                    final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0012");
                    logRecord.setParameters(new Object[] { serviceResource });
                    logRecord.setThrown(e);
                    logRecord.setResourceBundleName(logger.getResourceBundleName());
                    logRecord.setResourceBundle(logger.getResourceBundle());
                    logger.log(logRecord);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                        reader = null;
                    }
                }
            }
        }
        T serviceProvider = defaultInstance;
        if (providerClassName != null && providerClassName.length() > 0) {
            Class<T> providerClass = null;
            try {
                providerClass = (Class<T>) loader.loadClass(providerClassName);
            } catch (ClassNotFoundException e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0014");
                logRecord.setParameters(new Object[] { providerClassName });
                logRecord.setThrown(e);
                logRecord.setResourceBundleName(logger.getResourceBundleName());
                logRecord.setResourceBundle(logger.getResourceBundle());
                logger.log(logRecord);
                throw new ServiceException(e);
            }
            try {
                serviceProvider = providerClass.newInstance();
            } catch (Exception e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0015");
                logRecord.setParameters(new Object[] { providerClassName });
                logRecord.setThrown(e);
                logRecord.setResourceBundleName(logger.getResourceBundleName());
                logRecord.setResourceBundle(logger.getResourceBundle());
                logger.log(logRecord);
                throw new ServiceException(e);
            }
        }
        return serviceProvider;
    }

    /**
     * Loads a service map from the classpath.
     * <p>
     * Services are looked up following convention
     * <tt>META-INF/services/[serviceClassName]</tt>. When loading service maps,
     * service configuration file must contain entries as follows: <tt>
     * [key]:[implementation class]
     * </tt>.
     *
     * @param <T>      Service type to be obtained.
     * @param clazz    Service interface class object.
     * @return the map of services.
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> map(final Class<T> clazz) throws ServiceException {
        final ClassLoader loader = Util.getClassLoader(null);
        final Enumeration<URL> resources = getServiceResources(clazz, loader);
        final Map<String, T> serviceMap = new LinkedHashMap<String, T>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            BufferedReader reader = null;
            try {
                String line;
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                while (null != (line = reader.readLine())) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    Class<T> serviceClass;
                    final String[] record = line.split(":");
                    try {
                        serviceClass = (Class<T>) loader.loadClass(record[1]);
                    } catch (final ClassNotFoundException e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0011");
                        logRecord.setParameters(new Object[] { record[1], record[0] });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    T instance;
                    try {
                        instance = serviceClass.newInstance();
                    } catch (final Exception e) {
                        final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0015");
                        logRecord.setParameters(new Object[] { record[1] });
                        logRecord.setThrown(e);
                        logRecord.setResourceBundleName(logger.getResourceBundleName());
                        logRecord.setResourceBundle(logger.getResourceBundle());
                        logger.log(logRecord);
                        continue;
                    }
                    serviceMap.put(("".equals(record[0]) ? null : record[0]), instance);
                }
            } catch (final IOException e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0012");
                logRecord.setParameters(new Object[] { url });
                logRecord.setThrown(e);
                logRecord.setResourceBundleName(logger.getResourceBundleName());
                logRecord.setResourceBundle(logger.getResourceBundle());
                logger.log(logRecord);
                continue;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        ;
                    }
                }
            }
        }
        return Collections.unmodifiableMap(serviceMap);
    }

    /** Utility method to obtain an enumeration of service config files. */
    private static Enumeration<URL> getServiceResources(final Class<?> serviceClass, final ClassLoader classLoader) throws ServiceException {
        Enumeration<URL> resources;
        final String serviceConf = SERVICES_ROOT + serviceClass.getName();
        try {
            resources = classLoader.getResources(serviceConf);
        } catch (final IOException e) {
            throw new ServiceException(e);
        }
        return resources;
    }

    /** Private constructor to prevent instantiation */
    private Services() {
    }
}
