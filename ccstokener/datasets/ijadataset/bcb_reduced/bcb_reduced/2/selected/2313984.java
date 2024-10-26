package net.sf.jasperreports.jsf.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import net.sf.jasperreports.jsf.util.Util;

public final class Services {

    /** The logger. */
    private static final Logger logger = Logger.getLogger(Services.class.getPackage().getName(), "net.sf.jasperreports.jsf.LogMessages");

    private static final String SERVICES_ROOT = "META-INF/services/";

    /**
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
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
                        logger.log(logRecord);
                        continue;
                    }
                    serviceSet.add(instance);
                }
            } catch (final IOException e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0012");
                logRecord.setParameters(new Object[] { url });
                logRecord.setThrown(e);
                logger.log(logRecord);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            }
        }
        return Collections.unmodifiableSet(serviceSet);
    }

    /**
	 * Loads a service map from the classpath.
	 * <p>
	 * Services are looked up following convention <tt>META-INF/services/[serviceClassName]</tt>.
	 * When loading service maps, service configuration file must contain
	 * entries as follows:
	 * <tt>
	 * [key]:[implementation class]
	 * </tt>
	 * 
	 * @param <T> service type to be obtained
	 * @param resource
	 *            the resource
	 * 
	 * @return the map< string, class< t>>
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
                        logger.log(logRecord);
                        continue;
                    }
                    serviceMap.put(("".equals(record[0]) ? null : record[0]), instance);
                }
            } catch (final IOException e) {
                final LogRecord logRecord = new LogRecord(Level.SEVERE, "JRJSF_0012");
                logRecord.setParameters(new Object[] { url });
                logRecord.setThrown(e);
                logger.log(logRecord);
                continue;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            }
        }
        return Collections.unmodifiableMap(serviceMap);
    }

    private static Enumeration<URL> getServiceResources(Class<?> serviceClass, ClassLoader classLoader) throws ServiceException {
        Enumeration<URL> resources;
        final String serviceConf = SERVICES_ROOT + serviceClass.getName();
        try {
            resources = classLoader.getResources(serviceConf);
        } catch (final IOException e) {
            throw new ServiceException(e);
        }
        return resources;
    }

    private Services() {
    }
}
