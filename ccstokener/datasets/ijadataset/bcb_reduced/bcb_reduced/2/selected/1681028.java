package org.aigebi.rbac.crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.WeakHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ligong Xu
 * @version $Id: PasswordHasherFactory.java 5 2007-10-03 03:30:06Z ligongx $
 */
public abstract class PasswordHasherFactory {

    private static Log log = LogFactory.getLog(PasswordHasherFactory.class);

    /**
	 * Int number to indicate the precedence of the properties file.
	 * default to 0 if not set in Properties.
	 */
    public static final String PROP_PRIORITY = "priority";

    /**
	 * Property name (<code>org.aigebi.rbac.crypto.PasswordHasherFactory</code>) 
	 * It's used to identify the PasswordHasherFactory implementation class name. 
	 * This can be set as system property or as an entry in a properties file.
	 */
    public static final String PROP_FACTORY = "org.aigebi.rbac.crypto.PasswordHasherFactory";

    /**
	 * Class name of the default <code>PasswordHasherFactory</code> implementation class.
	 */
    public static final String PASSWORDHASHERFACTORY_DEFAULT = "org.aigebi.rbac.crypto.PasswordHasherFactoryImpl";

    /**
	 * The name (<code>PasswordHasherFactory.properties</code>) of the properties file to search for.
	 */
    public static final String PASSWORDHASHERFACTORY_PROPERTIES = "PasswordHasherFactory.properties";

    /**
	 * Service provider name as in Java 'Service Provider' specification
	 */
    public static final String PASSWORDHASHERFACTORY_SERVICE_ID = "META-INF/services/org.aigebi.rbac.cryto.PasswordHasherFactory";

    /**Factory method to create password hasher*/
    public static PasswordHasher getPasswordHasher() {
        return getPasswordHasherFactory().getPasswordHasherInstance();
    }

    /**
	 * @return PasswordHasher, Service provider need implement this
	 */
    protected abstract PasswordHasher getPasswordHasherInstance();

    /**Implementation factory need implement this to remove all related resources */
    protected abstract void release();

    /**PasswordHasherFactory method.
	 * Implementation factory discovery follows the order of system property, 
	 * service provider api, properties file and lastly the fallback default implementation.  
	 * Implementation Factory is cached with WeakHashMap. 
	 */
    public static PasswordHasherFactory getPasswordHasherFactory() {
        ClassLoader contextClassLoader = getContextClassLoader();
        PasswordHasherFactory factory = getCachedFactory();
        if (factory != null) {
            return factory;
        }
        String factoryClass = System.getProperty(PROP_FACTORY);
        if (factoryClass != null) {
            try {
                factory = createFactory(factoryClass, contextClassLoader);
            } catch (Exception e) {
                log.error("Error system property " + PASSWORDHASHERFACTORY_PROPERTIES, e);
            }
        }
        if (factory == null) {
            try {
                Enumeration<URL> urls = getResources(contextClassLoader, PASSWORDHASHERFACTORY_SERVICE_ID);
                URL url = null;
                if (urls != null && urls.hasMoreElements()) {
                    url = urls.nextElement();
                    Iterator factorynames = parse(PasswordHasherFactory.class, url);
                    while (factorynames != null && factorynames.hasNext()) {
                        factory = createFactory((String) factorynames.next(), contextClassLoader);
                        if (factory != null) break;
                    }
                }
            } catch (Exception e) {
                log.error("Error services lookup " + PASSWORDHASHERFACTORY_SERVICE_ID, e);
            }
        }
        if (factory == null) {
            Properties props = findConfigProperties(contextClassLoader, PASSWORDHASHERFACTORY_PROPERTIES);
            if (props != null) {
                factoryClass = props.getProperty(PROP_FACTORY);
                if (factoryClass != null) {
                    try {
                        factory = createFactory(factoryClass, contextClassLoader);
                    } catch (Exception ex) {
                        log.error("Error by properties file " + PASSWORDHASHERFACTORY_PROPERTIES, ex);
                    }
                }
            }
        }
        if (factory == null) {
            try {
                factory = createFactory(PASSWORDHASHERFACTORY_DEFAULT, contextClassLoader);
            } catch (Exception ex) {
                log.error("error by factory default", ex);
            }
        }
        if (factory != null) {
            cacheFactory(factory);
        }
        return factory;
    }

    private static PasswordHasherFactory createFactory(String factoryClass, ClassLoader classLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (classLoader != null) {
            return (PasswordHasherFactory) classLoader.loadClass(factoryClass).newInstance();
        }
        return null;
    }

    private static Enumeration<URL> getResources(final ClassLoader loader, final String name) {
        try {
            if (loader == null) {
                return ClassLoader.getSystemResources(name);
            } else {
                return loader.getResources(name);
            }
        } catch (IOException e) {
            log.error("Exception while trying to find configuration file " + name + ":" + e.getMessage(), e);
            return null;
        }
    }

    private static Iterator<String> parse(Class service, URL url) throws IOException {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<String>();
        try {
            in = url.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(service, url, r, lc, names)) >= 0) ;
        } finally {
            if (r != null) r.close();
            if (in != null) in.close();
        }
        return names.iterator();
    }

    private static int parseLine(Class service, URL u, BufferedReader r, int lc, List<String> names) throws IOException {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        int ci = ln.indexOf('#');
        if (ci >= 0) ln = ln.substring(0, ci);
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if (!names.contains(ln)) names.add(ln);
        }
        return lc + 1;
    }

    private static Properties findConfigProperties(ClassLoader classLoader, String fileName) {
        Enumeration urls = getResources(classLoader, fileName);
        if (urls == null) {
            return null;
        }
        Properties props = null;
        int priority = 0;
        URL propsUrl = null;
        while (urls.hasMoreElements()) {
            URL url = (URL) urls.nextElement();
            Properties newProps = loadPropertiesFromUrl(url);
            if (newProps != null) {
                if (props == null) {
                    propsUrl = url;
                    props = newProps;
                    String priorityProp = props.getProperty(PROP_PRIORITY);
                    if (priorityProp != null) {
                        priority = Integer.parseInt(priorityProp);
                    }
                } else {
                    String newPriorityProp = newProps.getProperty(PROP_PRIORITY);
                    if (newPriorityProp != null) {
                        int newPriority = Integer.parseInt(newPriorityProp);
                        if (newPriority > priority) {
                            propsUrl = url;
                            props = newProps;
                            priority = newPriority;
                        }
                    }
                }
            }
        }
        return props;
    }

    /**
	 * Load .properties file from the url
	 */
    private static Properties loadPropertiesFromUrl(final URL url) {
        InputStream stream = null;
        try {
            stream = url.openStream();
            if (stream != null) {
                Properties props = new Properties();
                props.load(stream);
                return props;
            }
        } catch (IOException e) {
            log.error("Unable to read URL " + url, e);
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                log.error("error reading URL" + url, e);
            }
        }
        return null;
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static WeakHashMap<String, PasswordHasherFactory> factories = new WeakHashMap<String, PasswordHasherFactory>();

    /**Hashkey on PROP_FACTORY*/
    private static void cacheFactory(PasswordHasherFactory factory) {
        factories.put(PROP_FACTORY, factory);
    }

    private static PasswordHasherFactory getCachedFactory() {
        return (PasswordHasherFactory) factories.get(PROP_FACTORY);
    }

    /** remove all cache entry, and call individual factory release() */
    public static void releaseAll() {
        synchronized (factories) {
            Collection<PasswordHasherFactory> elements = factories.values();
            if (elements == null) return;
            Iterator ite = elements.iterator();
            while (ite != null && ite.hasNext()) {
                PasswordHasherFactory element = (PasswordHasherFactory) ite.next();
                element.release();
            }
            factories.clear();
        }
    }
}
