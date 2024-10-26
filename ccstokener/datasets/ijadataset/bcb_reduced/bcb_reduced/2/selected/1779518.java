package org.bing.engine.common.logging;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import org.bing.engine.common.logging.impl.LogFactoryImpl;
import org.bing.engine.common.logging.impl.WeakHashtable;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class LogFactory {

    public static final String PRIORITY_KEY = "priority";

    public static final String TCCL_KEY = "use_tccl";

    public static final String FACTORY_PROPERTY = LogFactory.class.getName();

    public static final String FACTORY_DEFAULT = LogFactoryImpl.class.getName();

    public static final String FACTORY_PROPERTIES = "commons-logging.properties";

    protected static final String SERVICE_ID = "META-INF/services/" + LogFactory.class.getName();

    ;

    public static final String DIAGNOSTICS_DEST_PROPERTY = "seda.logging.diagnostics.dest";

    private static PrintStream diagnosticsStream = null;

    private static String diagnosticPrefix;

    public static final String HASHTABLE_IMPLEMENTATION_PROPERTY = "seda.logging.LogFactory.HashtableImpl";

    /** Name used to load the weak hashtable implementation by names */
    private static final String WEAK_HASHTABLE_CLASSNAME = WeakHashtable.class.getName();

    private static ClassLoader thisClassLoader;

    protected LogFactory() {
    }

    public abstract Object getAttribute(String name);

    public abstract String[] getAttributeNames();

    public abstract Log getInstance(Class clazz) throws LogConfigurationException;

    public abstract Log getInstance(String name) throws LogConfigurationException;

    public abstract void release();

    public abstract void removeAttribute(String name);

    public abstract void setAttribute(String name, Object value);

    protected static Hashtable factories = null;

    protected static LogFactory nullClassLoaderFactory = null;

    private static final Hashtable createFactoryStore() {
        Hashtable result = null;
        String storeImplementationClass;
        try {
            storeImplementationClass = getSystemProperty(HASHTABLE_IMPLEMENTATION_PROPERTY, null);
        } catch (SecurityException ex) {
            storeImplementationClass = null;
        }
        if (storeImplementationClass == null) {
            storeImplementationClass = WEAK_HASHTABLE_CLASSNAME;
        }
        try {
            Class implementationClass = Class.forName(storeImplementationClass);
            result = (Hashtable) implementationClass.newInstance();
        } catch (Throwable t) {
            if (!WEAK_HASHTABLE_CLASSNAME.equals(storeImplementationClass)) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[ERROR] LogFactory: Load of custom hashtable failed");
                } else {
                    System.err.println("[ERROR] LogFactory: Load of custom hashtable failed");
                }
            }
        }
        if (result == null) {
            result = new Hashtable();
        }
        return result;
    }

    /** Utility method to safely trim a string. */
    private static String trim(String src) {
        if (src == null) {
            return null;
        }
        return src.trim();
    }

    public static LogFactory getFactory() throws LogConfigurationException {
        ClassLoader contextClassLoader = getContextClassLoaderInternal();
        if (contextClassLoader == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Context classloader is null.");
            }
        }
        LogFactory factory = getCachedFactory(contextClassLoader);
        if (factory != null) {
            return factory;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[LOOKUP] LogFactory implementation requested for the first time for context classloader " + objectId(contextClassLoader));
            logHierarchy("[LOOKUP] ", contextClassLoader);
        }
        Properties props = getConfigurationFile(contextClassLoader, FACTORY_PROPERTIES);
        ClassLoader baseClassLoader = contextClassLoader;
        if (props != null) {
            String useTCCLStr = props.getProperty(TCCL_KEY);
            if (useTCCLStr != null) {
                if (Boolean.valueOf(useTCCLStr).booleanValue() == false) {
                    baseClassLoader = thisClassLoader;
                }
            }
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[LOOKUP] Looking for system property [" + FACTORY_PROPERTY + "] to define the LogFactory subclass to use...");
        }
        try {
            String factoryClass = getSystemProperty(FACTORY_PROPERTY, null);
            if (factoryClass != null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Creating an instance of LogFactory class '" + factoryClass + "' as specified by system property " + FACTORY_PROPERTY);
                }
                factory = newFactory(factoryClass, baseClassLoader, contextClassLoader);
            } else {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] No system property [" + FACTORY_PROPERTY + "] defined.");
                }
            }
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] A security exception occurred while trying to create an" + " instance of the custom factory class" + ": [" + trim(e.getMessage()) + "]. Trying alternative implementations...");
            }
            ;
        } catch (RuntimeException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] An exception occurred while trying to create an" + " instance of the custom factory class" + ": [" + trim(e.getMessage()) + "] as specified by a system property.");
            }
            throw e;
        }
        if (factory == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] Looking for a resource file of name [" + SERVICE_ID + "] to define the LogFactory subclass to use...");
            }
            try {
                InputStream is = getResourceAsStream(contextClassLoader, SERVICE_ID);
                if (is != null) {
                    BufferedReader rd;
                    try {
                        rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        rd = new BufferedReader(new InputStreamReader(is));
                    }
                    String factoryClassName = rd.readLine();
                    rd.close();
                    if (factoryClassName != null && !"".equals(factoryClassName)) {
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("[LOOKUP]  Creating an instance of LogFactory class " + factoryClassName + " as specified by file '" + SERVICE_ID + "' which was present in the path of the context" + " classloader.");
                        }
                        factory = newFactory(factoryClassName, baseClassLoader, contextClassLoader);
                    }
                } else {
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("[LOOKUP] No resource file with name '" + SERVICE_ID + "' found.");
                    }
                }
            } catch (Exception ex) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] A security exception occurred while trying to create an" + " instance of the custom factory class" + ": [" + trim(ex.getMessage()) + "]. Trying alternative implementations...");
                }
                ;
            }
        }
        if (factory == null) {
            if (props != null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Looking in properties file for entry with key '" + FACTORY_PROPERTY + "' to define the LogFactory subclass to use...");
                }
                String factoryClass = props.getProperty(FACTORY_PROPERTY);
                if (factoryClass != null) {
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("[LOOKUP] Properties file specifies LogFactory subclass '" + factoryClass + "'");
                    }
                    factory = newFactory(factoryClass, baseClassLoader, contextClassLoader);
                } else {
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("[LOOKUP] Properties file has no entry specifying LogFactory subclass.");
                    }
                }
            } else {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] No properties file available to determine" + " LogFactory subclass from..");
                }
            }
        }
        if (factory == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] Loading the default LogFactory implementation '" + FACTORY_DEFAULT + "' via the same classloader that loaded this LogFactory" + " class (ie not looking in the context classloader).");
            }
            factory = newFactory(FACTORY_DEFAULT, thisClassLoader, contextClassLoader);
        }
        if (factory != null) {
            cacheFactory(contextClassLoader, factory);
            if (props != null) {
                Enumeration names = props.propertyNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    String value = props.getProperty(name);
                    factory.setAttribute(name, value);
                }
            }
        }
        return factory;
    }

    public static Log getLog(Class clazz) throws LogConfigurationException {
        return (getFactory().getInstance(clazz));
    }

    public static Log getLog(String name) throws LogConfigurationException {
        return (getFactory().getInstance(name));
    }

    public static void release(ClassLoader classLoader) {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Releasing factory for classloader " + objectId(classLoader));
        }
        synchronized (factories) {
            if (classLoader == null) {
                if (nullClassLoaderFactory != null) {
                    nullClassLoaderFactory.release();
                    nullClassLoaderFactory = null;
                }
            } else {
                LogFactory factory = (LogFactory) factories.get(classLoader);
                if (factory != null) {
                    factory.release();
                    factories.remove(classLoader);
                }
            }
        }
    }

    public static void releaseAll() {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Releasing factory for all classloaders.");
        }
        synchronized (factories) {
            Enumeration elements = factories.elements();
            while (elements.hasMoreElements()) {
                LogFactory element = (LogFactory) elements.nextElement();
                element.release();
            }
            factories.clear();
            if (nullClassLoaderFactory != null) {
                nullClassLoaderFactory.release();
                nullClassLoaderFactory = null;
            }
        }
    }

    protected static ClassLoader getClassLoader(Class clazz) {
        try {
            return clazz.getClassLoader();
        } catch (SecurityException ex) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Unable to get classloader for class '" + clazz + "' due to security restrictions - " + ex.getMessage());
            }
            throw ex;
        }
    }

    protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
        return directGetContextClassLoader();
    }

    private static ClassLoader getContextClassLoaderInternal() throws LogConfigurationException {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return directGetContextClassLoader();
            }
        });
    }

    protected static ClassLoader directGetContextClassLoader() throws LogConfigurationException {
        ClassLoader classLoader = null;
        try {
            Method method = Thread.class.getMethod("getContextClassLoader", (Class[]) null);
            try {
                classLoader = (ClassLoader) method.invoke(Thread.currentThread(), (Object[]) null);
            } catch (IllegalAccessException e) {
                throw new LogConfigurationException("Unexpected IllegalAccessException", e);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof SecurityException) {
                    ;
                } else {
                    throw new LogConfigurationException("Unexpected InvocationTargetException", e.getTargetException());
                }
            }
        } catch (NoSuchMethodException e) {
            classLoader = getClassLoader(LogFactory.class);
        }
        return classLoader;
    }

    private static LogFactory getCachedFactory(ClassLoader contextClassLoader) {
        LogFactory factory = null;
        if (contextClassLoader == null) {
            factory = nullClassLoaderFactory;
        } else {
            factory = (LogFactory) factories.get(contextClassLoader);
        }
        return factory;
    }

    /**
     * Remember this factory, so later calls to LogFactory.getCachedFactory can return the previously created object (together with all its cached Log
     * objects).
     * 
     * @param classLoader
     *            should be the current context classloader. Note that this can be null under some circumstances; this is ok.
     * 
     * @param factory
     *            should be the factory to cache. This should never be null.
     */
    private static void cacheFactory(ClassLoader classLoader, LogFactory factory) {
        if (factory != null) {
            if (classLoader == null) {
                nullClassLoaderFactory = factory;
            } else {
                factories.put(classLoader, factory);
            }
        }
    }

    /**
     * Return a new instance of the specified <code>LogFactory</code> implementation class, loaded by the specified class loader. If that fails, try
     * the class loader used to load this (abstract) LogFactory.
     * <p>
     * <h2>ClassLoader conflicts</h2>
     * Note that there can be problems if the specified ClassLoader is not the same as the classloader that loaded this class, ie when loading a
     * concrete LogFactory subclass via a context classloader.
     * <p>
     * The problem is the same one that can occur when loading a concrete Log subclass via a context classloader.
     * <p>
     * The problem occurs when code running in the context classloader calls class X which was loaded via a parent classloader, and class X then calls
     * LogFactory.getFactory (either directly or via LogFactory.getLog). Because class X was loaded via the parent, it binds to LogFactory loaded via
     * the parent. When the code in this method finds some LogFactoryYYYY class in the child (context) classloader, and there also happens to be a
     * LogFactory class defined in the child classloader, then LogFactoryYYYY will be bound to LogFactory@childloader. It cannot be cast to
     * LogFactory@parentloader, ie this method cannot return the object as the desired type. Note that it doesn't matter if the LogFactory class in
     * the child classloader is identical to the LogFactory class in the parent classloader, they are not compatible.
     * <p>
     * The solution taken here is to simply print out an error message when this occurs then throw an exception. The deployer of the application must
     * ensure they remove all occurrences of the LogFactory class from the child classloader in order to resolve the issue. Note that they do not have
     * to move the custom LogFactory subclass; that is ok as long as the only LogFactory class it can find to bind to is in the parent classloader.
     * <p>
     * 
     * @param factoryClass
     *            Fully qualified name of the <code>LogFactory</code> implementation class
     * @param classLoader
     *            ClassLoader from which to load this class
     * @param contextClassLoader
     *            is the context that this new factory will manage logging for.
     * 
     * @exception LogConfigurationException
     *                if a suitable instance cannot be created
     * @since 1.1
     */
    protected static LogFactory newFactory(final String factoryClass, final ClassLoader classLoader, final ClassLoader contextClassLoader) throws LogConfigurationException {
        Object result = AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return createFactory(factoryClass, classLoader);
            }
        });
        if (result instanceof LogConfigurationException) {
            LogConfigurationException ex = (LogConfigurationException) result;
            if (isDiagnosticsEnabled()) {
                logDiagnostic("An error occurred while loading the factory class:" + ex.getMessage());
            }
            throw ex;
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Created object " + objectId(result) + " to manage classloader " + objectId(contextClassLoader));
        }
        return (LogFactory) result;
    }

    /**
     * Method provided for backwards compatibility; see newFactory version that takes 3 parameters.
     * <p>
     * This method would only ever be called in some rather odd situation. Note that this method is static, so overriding in a subclass doesn't have
     * any effect unless this method is called from a method in that subclass. However this method only makes sense to use from the getFactory method,
     * and as that is almost always invoked via LogFactory.getFactory, any custom definition in a subclass would be pointless. Only a class with a
     * custom getFactory method, then invoked directly via CustomFactoryImpl.getFactory or similar would ever call this. Anyway, it's here just in
     * case, though the "managed class loader" value output to the diagnostics will not report the correct value.
     */
    protected static LogFactory newFactory(final String factoryClass, final ClassLoader classLoader) {
        return newFactory(factoryClass, classLoader, null);
    }

    /**
     * Implements the operations described in the javadoc for newFactory.
     * 
     * @param factoryClass
     * 
     * @param classLoader
     *            used to load the specified factory class. This is expected to be either the TCCL or the classloader which loaded this class. Note
     *            that the classloader which loaded this class might be "null" (ie the bootloader) for embedded systems.
     * 
     * @return either a LogFactory object or a LogConfigurationException object.
     * @since 1.1
     */
    protected static Object createFactory(String factoryClass, ClassLoader classLoader) {
        Class logFactoryClass = null;
        try {
            if (classLoader != null) {
                try {
                    logFactoryClass = classLoader.loadClass(factoryClass);
                    if (LogFactory.class.isAssignableFrom(logFactoryClass)) {
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("Loaded class " + logFactoryClass.getName() + " from classloader " + objectId(classLoader));
                        }
                    } else {
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("Factory class " + logFactoryClass.getName() + " loaded from classloader " + objectId(logFactoryClass.getClassLoader()) + " does not extend '" + LogFactory.class.getName() + "' as loaded by this classloader.");
                            logHierarchy("[BAD CL TREE] ", classLoader);
                        }
                    }
                    return (LogFactory) logFactoryClass.newInstance();
                } catch (ClassNotFoundException ex) {
                    if (classLoader == thisClassLoader) {
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("Unable to locate any class called '" + factoryClass + "' via classloader " + objectId(classLoader));
                        }
                        throw ex;
                    }
                } catch (NoClassDefFoundError e) {
                    if (classLoader == thisClassLoader) {
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("Class '" + factoryClass + "' cannot be loaded" + " via classloader " + objectId(classLoader) + " - it depends on some other class that cannot" + " be found.");
                        }
                        throw e;
                    }
                } catch (ClassCastException e) {
                    if (classLoader == thisClassLoader) {
                        final boolean implementsLogFactory = implementsLogFactory(logFactoryClass);
                        String msg = "The application has specified that a custom LogFactory implementation should be used but " + "Class '" + factoryClass + "' cannot be converted to '" + LogFactory.class.getName() + "'. ";
                        if (implementsLogFactory) {
                            msg = msg + "The conflict is caused by the presence of multiple LogFactory classes in incompatible classloaders. " + "Background can be found in http://commons.apache.org/logging/tech.html. " + "If you have not explicitly specified a custom LogFactory then it is likely that " + "the container has set one without your knowledge. " + "In this case, consider using the commons-logging-adapters.jar file or " + "specifying the standard LogFactory from the command line. ";
                        } else {
                            msg = msg + "Please check the custom implementation. ";
                        }
                        msg = msg + "Help can be found @http://commons.apache.org/logging/troubleshooting.html.";
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic(msg);
                        }
                        ClassCastException ex = new ClassCastException(msg);
                        throw ex;
                    }
                }
            }
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Unable to load factory class via classloader " + objectId(classLoader) + " - trying the classloader associated with this LogFactory.");
            }
            logFactoryClass = Class.forName(factoryClass);
            return (LogFactory) logFactoryClass.newInstance();
        } catch (Exception e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Unable to create LogFactory instance.");
            }
            if (logFactoryClass != null && !LogFactory.class.isAssignableFrom(logFactoryClass)) {
                return new LogConfigurationException("The chosen LogFactory implementation does not extend LogFactory." + " Please check your configuration.", e);
            }
            return new LogConfigurationException(e);
        }
    }

    /**
     * Determines whether the given class actually implements <code>LogFactory</code>. Diagnostic information is also logged.
     * <p>
     * <strong>Usage:</strong> to diagnose whether a classloader conflict is the cause of incompatibility. The test used is whether the class is
     * assignable from the <code>LogFactory</code> class loaded by the class's classloader.
     * 
     * @param logFactoryClass
     *            <code>Class</code> which may implement <code>LogFactory</code>
     * @return true if the <code>logFactoryClass</code> does extend <code>LogFactory</code> when that class is loaded via the same classloader that
     *         loaded the <code>logFactoryClass</code>.
     */
    private static boolean implementsLogFactory(Class logFactoryClass) {
        boolean implementsLogFactory = false;
        if (logFactoryClass != null) {
            try {
                ClassLoader logFactoryClassLoader = logFactoryClass.getClassLoader();
                if (logFactoryClassLoader == null) {
                    logDiagnostic("[CUSTOM LOG FACTORY] was loaded by the boot classloader");
                } else {
                    logHierarchy("[CUSTOM LOG FACTORY] ", logFactoryClassLoader);
                    Class factoryFromCustomLoader = Class.forName("com.alibaba.intl.commons.watcher.logging.LogFactory", false, logFactoryClassLoader);
                    implementsLogFactory = factoryFromCustomLoader.isAssignableFrom(logFactoryClass);
                    if (implementsLogFactory) {
                        logDiagnostic("[CUSTOM LOG FACTORY] " + logFactoryClass.getName() + " implements LogFactory but was loaded by an incompatible classloader.");
                    } else {
                        logDiagnostic("[CUSTOM LOG FACTORY] " + logFactoryClass.getName() + " does not implement LogFactory.");
                    }
                }
            } catch (SecurityException e) {
                logDiagnostic("[CUSTOM LOG FACTORY] SecurityException thrown whilst trying to determine whether " + "the compatibility was caused by a classloader conflict: " + e.getMessage());
            } catch (LinkageError e) {
                logDiagnostic("[CUSTOM LOG FACTORY] LinkageError thrown whilst trying to determine whether " + "the compatibility was caused by a classloader conflict: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                logDiagnostic("[CUSTOM LOG FACTORY] LogFactory class cannot be loaded by classloader which loaded the " + "custom LogFactory implementation. Is the custom factory in the right classloader?");
            }
        }
        return implementsLogFactory;
    }

    /**
     * Applets may run in an environment where accessing resources of a loader is a secure operation, but where the commons-logging library has
     * explicitly been granted permission for that operation. In this case, we need to run the operation using an AccessController.
     */
    private static InputStream getResourceAsStream(final ClassLoader loader, final String name) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                if (loader != null) {
                    return loader.getResourceAsStream(name);
                } else {
                    return ClassLoader.getSystemResourceAsStream(name);
                }
            }
        });
    }

    /**
     * Given a filename, return an enumeration of URLs pointing to all the occurrences of that filename in the classpath.
     * <p>
     * This is just like ClassLoader.getResources except that the operation is done under an AccessController so that this method will succeed when
     * this jarfile is privileged but the caller is not. This method must therefore remain private to avoid security issues.
     * <p>
     * If no instances are found, an Enumeration is returned whose hasMoreElements method returns false (ie an "empty" enumeration). If resources
     * could not be listed for some reason, null is returned.
     */
    private static Enumeration getResources(final ClassLoader loader, final String name) {
        PrivilegedAction action = new PrivilegedAction() {

            public Object run() {
                try {
                    if (loader != null) {
                        return loader.getResources(name);
                    } else {
                        return ClassLoader.getSystemResources(name);
                    }
                } catch (IOException e) {
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("Exception while trying to find configuration file " + name + ":" + e.getMessage());
                    }
                    return null;
                } catch (NoSuchMethodError e) {
                    return null;
                }
            }
        };
        Object result = AccessController.doPrivileged(action);
        return (Enumeration) result;
    }

    /**
     * Given a URL that refers to a .properties file, load that file. This is done under an AccessController so that this method will succeed when
     * this jarfile is privileged but the caller is not. This method must therefore remain private to avoid security issues.
     * <p>
     * Null is returned if the URL cannot be opened.
     */
    private static Properties getProperties(final URL url) {
        PrivilegedAction action = new PrivilegedAction() {

            public Object run() {
                try {
                    InputStream stream = url.openStream();
                    if (stream != null) {
                        Properties props = new Properties();
                        props.load(stream);
                        stream.close();
                        return props;
                    }
                } catch (IOException e) {
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic("Unable to read URL " + url);
                    }
                }
                return null;
            }
        };
        return (Properties) AccessController.doPrivileged(action);
    }

    /**
     * Locate a user-provided configuration file.
     * <p>
     * The classpath of the specified classLoader (usually the context classloader) is searched for properties files of the specified name. If none is
     * found, null is returned. If more than one is found, then the file with the greatest value for its PRIORITY property is returned. If multiple
     * files have the same PRIORITY value then the first in the classpath is returned.
     * <p>
     * This differs from the 1.0.x releases; those always use the first one found. However as the priority is a new field, this change is backwards
     * compatible.
     * <p>
     * The purpose of the priority field is to allow a webserver administrator to override logging settings in all webapps by placing a
     * commons-logging.properties file in a shared classpath location with a priority > 0; this overrides any commons-logging.properties files without
     * priorities which are in the webapps. Webapps can also use explicit priorities to override a configuration file in the shared classpath if
     * needed.
     */
    private static final Properties getConfigurationFile(ClassLoader classLoader, String fileName) {
        Properties props = null;
        double priority = 0.0;
        URL propsUrl = null;
        try {
            Enumeration urls = getResources(classLoader, fileName);
            if (urls == null) {
                return null;
            }
            while (urls.hasMoreElements()) {
                URL url = (URL) urls.nextElement();
                Properties newProps = getProperties(url);
                if (newProps != null) {
                    if (props == null) {
                        propsUrl = url;
                        props = newProps;
                        String priorityStr = props.getProperty(PRIORITY_KEY);
                        priority = 0.0;
                        if (priorityStr != null) {
                            priority = Double.parseDouble(priorityStr);
                        }
                        if (isDiagnosticsEnabled()) {
                            logDiagnostic("[LOOKUP] Properties file found at '" + url + "'" + " with priority " + priority);
                        }
                    } else {
                        String newPriorityStr = newProps.getProperty(PRIORITY_KEY);
                        double newPriority = 0.0;
                        if (newPriorityStr != null) {
                            newPriority = Double.parseDouble(newPriorityStr);
                        }
                        if (newPriority > priority) {
                            if (isDiagnosticsEnabled()) {
                                logDiagnostic("[LOOKUP] Properties file at '" + url + "'" + " with priority " + newPriority + " overrides file at '" + propsUrl + "'" + " with priority " + priority);
                            }
                            propsUrl = url;
                            props = newProps;
                            priority = newPriority;
                        } else {
                            if (isDiagnosticsEnabled()) {
                                logDiagnostic("[LOOKUP] Properties file at '" + url + "'" + " with priority " + newPriority + " does not override file at '" + propsUrl + "'" + " with priority " + priority);
                            }
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("SecurityException thrown while trying to find/read config files.");
            }
        }
        if (isDiagnosticsEnabled()) {
            if (props == null) {
                logDiagnostic("[LOOKUP] No properties file of name '" + fileName + "' found.");
            } else {
                logDiagnostic("[LOOKUP] Properties file of name '" + fileName + "' found at '" + propsUrl + '"');
            }
        }
        return props;
    }

    /**
     * Read the specified system property, using an AccessController so that the property can be read if JCL has been granted the appropriate security
     * rights even if the calling code has not.
     * <p>
     * Take care not to expose the value returned by this method to the calling application in any way; otherwise the calling app can use that info to
     * access data that should not be available to it.
     */
    private static String getSystemProperty(final String key, final String def) throws SecurityException {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return System.getProperty(key, def);
            }
        });
    }

    /**
     * Determines whether the user wants internal diagnostic output. If so, returns an appropriate writer object. Users can enable diagnostic output
     * by setting the system property named {@link #DIAGNOSTICS_DEST_PROPERTY} to a filename, or the special values STDOUT or STDERR.
     */
    private static void initDiagnostics() {
        String dest;
        try {
            dest = getSystemProperty(DIAGNOSTICS_DEST_PROPERTY, null);
            if (dest == null) {
                return;
            }
        } catch (SecurityException ex) {
            return;
        }
        if (dest.equals("STDOUT")) {
            diagnosticsStream = System.out;
        } else if (dest.equals("STDERR")) {
            diagnosticsStream = System.err;
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(dest, true);
                diagnosticsStream = new PrintStream(fos);
            } catch (IOException ex) {
                return;
            }
        }
        String classLoaderName;
        try {
            ClassLoader classLoader = thisClassLoader;
            if (thisClassLoader == null) {
                classLoaderName = "BOOTLOADER";
            } else {
                classLoaderName = objectId(classLoader);
            }
        } catch (SecurityException e) {
            classLoaderName = "UNKNOWN";
        }
        diagnosticPrefix = "[LogFactory from " + classLoaderName + "] ";
    }

    /**
     * Indicates true if the user has enabled internal logging.
     * <p>
     * By the way, sorry for the incorrect grammar, but calling this method areDiagnosticsEnabled just isn't java beans style.
     * 
     * @return true if calls to logDiagnostic will have any effect.
     * @since 1.1
     */
    protected static boolean isDiagnosticsEnabled() {
        return diagnosticsStream != null;
    }

    /**
     * Write the specified message to the internal logging destination.
     * <p>
     * Note that this method is private; concrete subclasses of this class should not call it because the diagnosticPrefix string this method puts in
     * front of all its messages is LogFactory@...., while subclasses should put SomeSubClass@...
     * <p>
     * Subclasses should instead compute their own prefix, then call logRawDiagnostic. Note that calling isDiagnosticsEnabled is fine for subclasses.
     * <p>
     * Note that it is safe to call this method before initDiagnostics is called; any output will just be ignored (as isDiagnosticsEnabled will return
     * false).
     * 
     * @param msg
     *            is the diagnostic message to be output.
     */
    private static final void logDiagnostic(String msg) {
        if (diagnosticsStream != null) {
            diagnosticsStream.print(diagnosticPrefix);
            diagnosticsStream.println(msg);
            diagnosticsStream.flush();
        }
    }

    /**
     * Write the specified message to the internal logging destination.
     * 
     * @param msg
     *            is the diagnostic message to be output.
     * @since 1.1
     */
    protected static final void logRawDiagnostic(String msg) {
        if (diagnosticsStream != null) {
            diagnosticsStream.println(msg);
            diagnosticsStream.flush();
        }
    }

    /**
     * Generate useful diagnostics regarding the classloader tree for the specified class.
     * <p>
     * As an example, if the specified class was loaded via a webapp's classloader, then you may get the following output:
     * 
     * <pre>
     * Class com.acme.Foo was loaded via classloader 11111
     * ClassLoader tree: 11111 -> 22222 (SYSTEM) -> 33333 -> BOOT
     * </pre>
     * <p>
     * This method returns immediately if isDiagnosticsEnabled() returns false.
     * 
     * @param clazz
     *            is the class whose classloader + tree are to be output.
     */
    private static void logClassLoaderEnvironment(Class clazz) {
        if (!isDiagnosticsEnabled()) {
            return;
        }
        try {
            logDiagnostic("[ENV] Extension directories (java.ext.dir): " + System.getProperty("java.ext.dir"));
            logDiagnostic("[ENV] Application classpath (java.class.path): " + System.getProperty("java.class.path"));
        } catch (SecurityException ex) {
            logDiagnostic("[ENV] Security setting prevent interrogation of system classpaths.");
        }
        String className = clazz.getName();
        ClassLoader classLoader;
        try {
            classLoader = getClassLoader(clazz);
        } catch (SecurityException ex) {
            logDiagnostic("[ENV] Security forbids determining the classloader for " + className);
            return;
        }
        logDiagnostic("[ENV] Class " + className + " was loaded via classloader " + objectId(classLoader));
        logHierarchy("[ENV] Ancestry of classloader which loaded " + className + " is ", classLoader);
    }

    /**
     * Logs diagnostic messages about the given classloader and it's hierarchy. The prefix is prepended to the message and is intended to make it
     * easier to understand the logs.
     * 
     * @param prefix
     * @param classLoader
     */
    private static void logHierarchy(String prefix, ClassLoader classLoader) {
        if (!isDiagnosticsEnabled()) {
            return;
        }
        ClassLoader systemClassLoader;
        if (classLoader != null) {
            final String classLoaderString = classLoader.toString();
            logDiagnostic(prefix + objectId(classLoader) + " == '" + classLoaderString + "'");
        }
        try {
            systemClassLoader = ClassLoader.getSystemClassLoader();
        } catch (SecurityException ex) {
            logDiagnostic(prefix + "Security forbids determining the system classloader.");
            return;
        }
        if (classLoader != null) {
            StringBuffer buf = new StringBuffer(prefix + "ClassLoader tree:");
            for (; ; ) {
                buf.append(objectId(classLoader));
                if (classLoader == systemClassLoader) {
                    buf.append(" (SYSTEM) ");
                }
                try {
                    classLoader = classLoader.getParent();
                } catch (SecurityException ex) {
                    buf.append(" --> SECRET");
                    break;
                }
                buf.append(" --> ");
                if (classLoader == null) {
                    buf.append("BOOT");
                    break;
                }
            }
            logDiagnostic(buf.toString());
        }
    }

    /**
     * Returns a string that uniquely identifies the specified object, including its class.
     * <p>
     * The returned string is of form "classname@hashcode", ie is the same as the return value of the Object.toString() method, but works even when
     * the specified object's class has overidden the toString method.
     * 
     * @param o
     *            may be null.
     * @return a string of form classname@hashcode, or "null" if param o is null.
     * @since 1.1
     */
    public static String objectId(Object o) {
        if (o == null) {
            return "null";
        } else {
            return o.getClass().getName() + "@" + System.identityHashCode(o);
        }
    }

    static {
        thisClassLoader = getClassLoader(LogFactory.class);
        initDiagnostics();
        logClassLoaderEnvironment(LogFactory.class);
        factories = createFactoryStore();
        if (isDiagnosticsEnabled()) {
            logDiagnostic("BOOTSTRAP COMPLETED");
        }
    }
}
