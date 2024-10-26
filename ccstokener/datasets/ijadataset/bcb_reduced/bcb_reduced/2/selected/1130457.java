package fi.arcusys.acj.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple service discovery utility that loads all the service description
 * resources using classloader and loads and initializes specified classes.
 * 
 * <p>
 * Services are described in resource "fi/arcusys/acj/Services.properties",
 * which is a standard Java properties file. All the classes listed in value of
 * property "services" are loaded and initialized. Class names are separated
 * with a comma (",") or a semicolon (";"). White spaces (including space, tab,
 * line feed and carriage return) are ignored.
 * </p>
 * 
 * <p>
 * An example of service description resource:
 * </p>
 * 
 * <pre>
 * services=com.example.MyServiceProvider;foo.bar.SomeOtherClass
 * </pre>
 * 
 * @version 1.0 $Rev: 1562 $
 * @author mikko Copyright © 2008 Arcusys Ltd. - http://www.arcusys.fi/
 * @deprecated please use sun.misc.Service instead
 */
@Deprecated
final class Services {

    private static final String SERVICES_RESOURCE = "fi/arcusys/acj/Services.properties";

    private static final String PROPERTY_SERVICES = "services";

    private static final String CLASS_NAME_SEPARATOR = ",;";

    private static final Logger LOG = LoggerFactory.getLogger(Services.class);

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private static boolean initialized;

    private static Vector<Class<?>> classes;

    static {
        initialize();
    }

    private Services() {
    }

    /**
     * Initialize by loading all the service classes. Does nothing if already
     * initialized.
     */
    public static synchronized void initialize() {
        if (!initialized) {
            try {
                classes = new Vector<Class<?>>();
                loadServiceClasses();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            initialized = true;
        }
    }

    /**
     * Find service classes implementing or extending the specified interface or
     * class.
     * 
     * <p>
     * Service classes are those listed in propety <code>serviceas</code> in
     * resource(s) <code>fi/arcusys/acj/Services.properties</code>.
     * </p>
     * 
     * @param <T>
     *            the base class type
     * @param requiredBase
     *            the required base class or interface
     * @return a collection of service classes
     */
    public static <T> Collection<Class<? extends T>> getClasses(Class<T> requiredBase) {
        if (!initialized) {
            initialize();
        }
        ArrayList<Class<? extends T>> results = new ArrayList<Class<? extends T>>();
        for (Class<?> clazz : classes) {
            if (requiredBase.isAssignableFrom(clazz)) {
                results.add(clazz.asSubclass(requiredBase));
            }
        }
        return results;
    }

    private static void loadServiceClass(String name) throws ClassNotFoundException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("loadServiceClass: " + name);
        }
        classes.add(Class.forName(name));
    }

    private static void loadServiceClasses() throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("loadServiceClasses");
        }
        ClassLoader cl = Services.class.getClassLoader();
        Enumeration<URL> urls = cl.getResources(SERVICES_RESOURCE);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found Services.properties: " + url);
            }
            InputStream in = url.openStream();
            try {
                Properties props = new Properties();
                props.load(in);
                String s = props.getProperty(PROPERTY_SERVICES);
                if (null == s) {
                    LOG.warn("Property '" + PROPERTY_SERVICES + "' not specified in resource '" + url + "'");
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("services=" + s);
                    }
                    String[] classNames = s.split(CLASS_NAME_SEPARATOR);
                    for (String className : classNames) {
                        try {
                            loadServiceClass(className);
                        } catch (ClassNotFoundException ex) {
                            LOG.error("An exception occurred while loading class " + className + " from resource " + url, ex);
                        }
                    }
                }
            } finally {
                IOUtil.closeSilently(in);
            }
        }
    }

    /**
     * Return an instance of the specified service class (see
     * {@link #getClasses(Class)}).
     * 
     * <p>
     * The returned instance may be a new instance or reference to a singleton
     * instance, depending on the service class implementation. The instance is
     * obtained by calling the first available public class method of following
     * in the specified order:
     * </p>
     * <ol>
     * <li>default constructor (using <code>Class.newInstance()</code>)</li>
     * <li><code>getInstance()</code></li>
     * <li><code>newInstance()</code></li>
     * <li><code>createInstance()</code></li>
     * 
     * </ol>
     * 
     * <p>
     * Please note that the class does not have to be a <em>service class</em>
     * as described in documenetation of method {@link #getClasses(Class)}. This
     * method can be used to create an instance of <em>any</em> available class.
     * </p>
     * 
     * <p>
     * Created instances are not pooled by this method but classes (or their
     * list of available factory methods) may be cached.
     * </p>
     * 
     * @param <T>
     *            the class type
     * @param clazz
     *            the class to create an instance of
     * @return an instance of the specified class (never <code>null</code>)
     */
    public static <T> T getInstance(Class<T> clazz) {
        T instance = null;
        Constructor<T> ctor = null;
        try {
            ctor = clazz.getConstructor(EMPTY_CLASS_ARRAY);
            instance = ctor.newInstance(EMPTY_OBJECT_ARRAY);
        } catch (NoSuchMethodException ex) {
            instance = null;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Failed to invoke constructor " + ctor, ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Failed to access constructor " + ctor, ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException("Failed to instantiate " + clazz, ex);
        }
        if (null == instance) {
            String[] methodNames = { "getInstance", "newInstance", "createInstance" };
            for (String methodName : methodNames) {
                Method method = lookUpFactoryMethod(clazz, methodName);
                if (null == method) {
                    continue;
                } else {
                    Object obj = invokeFactoryMethod(method);
                    instance = clazz.cast(obj);
                    break;
                }
            }
        }
        if (null == instance) {
            throw new RuntimeException("Don't know how to create an instance of class " + clazz.getName());
        }
        return instance;
    }

    /**
     * Return an instance of a class with the specified name.
     * 
     * <p>
     * See description of method {@link #getInstance(Class)} for more details.
     * </p>
     * 
     * @param className
     *            name of the class to create an instance of
     * @return an instance of the specified class (never <code>null</code>)
     * @throws ClassNotFoundException
     *             if no such class is found
     */
    public static Object getInstance(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return getInstance(clazz);
    }

    /**
     * Invoke the specified factory method (a public static method without
     * arguments) and return its return value.
     * 
     * @param method
     *            the method to invoke
     * @return a result of factory method
     */
    private static Object invokeFactoryMethod(Method method) {
        try {
            return method.invoke(null, EMPTY_OBJECT_ARRAY);
        } catch (IllegalAccessException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("An exception occurred while invoking factory method " + method + ": " + e);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Details of catched exception " + e, e);
            }
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("An exception occurred while invoking factory method " + method + ": " + e);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Details of catched exception " + e, e);
            }
            throw new RuntimeException(e);
        }
    }

    private static Method lookUpFactoryMethod(Class<?> clazz, String name) {
        Method method;
        try {
            method = clazz.getMethod(name, EMPTY_CLASS_ARRAY);
        } catch (NoSuchMethodException ex) {
            method = null;
        }
        if (null != method) {
            int mods = method.getModifiers();
            if (!Modifier.isStatic(mods)) {
                method = null;
            } else if (!Modifier.isPublic(mods)) {
                method = null;
            }
        }
        return method;
    }
}
