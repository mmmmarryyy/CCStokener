package joj.core;

import static joj.web.controller.ControllerUtils.*;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

/**
 * The delegate class loader that actually does the work, designed so it can be dropped
 * and the classes within reloaded.
 * @author Jason Miller (heinousjay@gmail.com)
 *
 */
final class ModuleClassLoader extends ClassLoader {

    /**
	 * The config URL of this {@link ClassLoader}
	 */
    private final URL configURL;

    /**
	 * The core Logger
	 */
    private final Logger coreLogger;

    /**
	 * The base package name of this {@link ClassLoader}
	 */
    private String basePackageName;

    /**
	 * The base URL of this {@link ClassLoader}
	 */
    private URL baseURL;

    /**
	 * The Set of listeners, implemented as a Map cause there is no ConcurrentSet.
	 */
    private final ConcurrentMap<ModuleClassLoaderLifecycleListener, ModuleClassLoaderLifecycleListener> lifecycleListeners = new ConcurrentHashMap<ModuleClassLoaderLifecycleListener, ModuleClassLoaderLifecycleListener>(4, 0.75F, 1);

    /**
	 * The controllers we've already found.  Can be updated by
	 * up to 4 threads without contention.  Gonna have to figure
	 * out a way to configure this.
	 */
    private final ConcurrentMap<String, byte[]> foundControllers = new ConcurrentHashMap<String, byte[]>(20, 0.75F, 4);

    ModuleClassLoader(final URL configURL, final Logger coreLogger) {
        super(DeploymentResourceLoader.class.getClassLoader());
        this.configURL = configURL;
        this.coreLogger = coreLogger;
    }

    public void addLifecycleListener(final ModuleClassLoaderLifecycleListener listener) {
        lifecycleListeners.putIfAbsent(listener, listener);
    }

    /**
	 * Accessor to get the base package name of this {@link ClassLoader}
	 * @return
	 */
    public String getBasePackageName() {
        return basePackageName;
    }

    /**
	 * Accessor to get the base URL of this {@link ClassLoader}
	 * @return
	 */
    public URL getBaseURL() {
        return baseURL;
    }

    /**
	 * Accessor to get the config URL of this {@link ClassLoader}
	 * @return
	 */
    public URL getConfigURL() {
        return configURL;
    }

    @Override
    public URL getResource(final String name) {
        URL result = null;
        try {
            result = new URL(baseURL, name.substring(name.startsWith("/") ? 1 : 0));
            result.openConnection().connect();
        } catch (final Exception failure) {
            result = null;
        }
        if (result == null) {
            result = getParent().getResource(name);
        }
        return result;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        try {
            return readURLAndDefineClass(new URL(baseURL, name.replaceAll("\\.", "/") + ".class"));
        } catch (final Exception eaten) {
        }
        return super.loadClass(name);
    }

    /**
	 * Indicate that a former {@link ModuleClassLoaderLifecycleListener} is no longer
	 * interested in events.
	 * @param listener The {@link ModuleClassLoaderLifecycleListener} to notify.
	 */
    public void removeLifecycleListener(final ModuleClassLoaderLifecycleListener listener) {
        lifecycleListeners.remove(listener, listener);
    }

    /**
	 * Turns the class name into a resource name that can be looked up in the context
	 * of the {@link #baseURL}.
	 * @param className
	 * @param resource
	 * @return
	 */
    private String classNameToResourceName(final String className, final Resource resource) {
        final String resourcePackage = resource.getRequiredSubpackage();
        final StringBuilder resourceName = new StringBuilder(basePackageName.replaceAll("\\.", "/"));
        if (resourcePackage != null) {
            resourceName.append("/").append(resourcePackage);
        }
        resourceName.append("/").append(className).append(".class");
        return resourceName.toString();
    }

    /**
	 * Reads the package name from the class byte array and defines the package, if
	 * we have not already done so.  This is done without loading the class.
	 * @param classBytes The byte array representing the class
	 * @return The package name of the class
	 */
    private String definePackageIfNeeded(final byte[] classBytes) {
        final ClassReader reader = new ClassReader(classBytes);
        final String className = Type.getObjectType(reader.getClassName()).getClassName();
        final String packageName = className.substring(0, className.lastIndexOf('.'));
        if (getPackage(packageName) == null) {
            definePackage(packageName, null, null, null, null, null, null, null);
        }
        return packageName;
    }

    /**
	 * Helper method to fire the defined class event to interested listeners
	 * @param clazz The Class that was just defined.
	 */
    private void fireDefinedClassEvent(final Class<?> clazz) {
        for (final ModuleClassLoaderLifecycleListener listener : lifecycleListeners.keySet()) {
            listener.definedClass(this, clazz);
        }
    }

    /**
	 * Helper method to fire the defining class event to interested listeners
	 * @param classBytes The byte[] holding the bytes of the class about to be defined.
	 */
    private void fireDefiningClassEvent(final byte[] classBytes) {
        for (final ModuleClassLoaderLifecycleListener listener : lifecycleListeners.keySet()) {
            listener.definingClass(this, classBytes);
        }
    }

    /**
	 * Helper method to fire the started event to interested listeners.
	 */
    private void fireStartedEvent() {
        for (final ModuleClassLoaderLifecycleListener listener : lifecycleListeners.keySet()) {
            listener.started(this);
        }
    }

    private byte[] getControllerBytes(final String controllerName) {
        byte[] result = foundControllers.get(controllerName);
        if (result == null) {
            final String[] candidates = generateControllerClassCandidateNames(controllerName);
            for (final String candidate : candidates) {
                URL url = null;
                if ((url = getResource(classNameToResourceName(candidate, Resource.CONTROLLER))) != null) {
                    result = toByteArray(url);
                    foundControllers.putIfAbsent(controllerName, result);
                    break;
                }
            }
        }
        return result;
    }

    /**
	 * <p>
	 * Loads the given {@link URL} as a class, defines it, and returns it.
	 * </p>
	 * @param url The {@link URL} pointing to the class to load.
	 * @return The defined {@link Class} object pointed to by the {@link URL}
	 * @throws Exception If anything goes wrong loading the class or defining it.
	 */
    private Class<?> readURLAndDefineClass(final URL url) throws Exception {
        final byte[] classBytes = toByteArray(url);
        definePackageIfNeeded(classBytes);
        fireDefiningClassEvent(classBytes);
        final Class<?> clazz = defineClass(null, classBytes, 0, classBytes.length);
        fireDefinedClassEvent(clazz);
        return clazz;
    }

    /**
	 * <p>Helper method to read a {@link URL} and return it as a byte array.
	 * @param url The {@link URL} to read.
	 * @return A byte array with the contents of the resource at the {@link URL} location.
	 * @throws Exception If anything goes wrong reading the {@link URL}
	 */
    private byte[] toByteArray(final URL url) {
        InputStream input = null;
        try {
            input = url.openStream();
            return IOUtils.toByteArray(input);
        } catch (final Exception eaten) {
            return null;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    /**
	 * Special direct method to define the config class
	 *
	 * @param configURL
	 * @return
	 */
    Class<?> getConfigClass() throws Exception {
        final byte[] classBytes = toByteArray(configURL);
        basePackageName = definePackageIfNeeded(classBytes);
        final String url = configURL.toString();
        baseURL = new URL(url.substring(0, url.lastIndexOf(basePackageName.replaceAll("\\.", "/"))));
        fireStartedEvent();
        coreLogger.info("Defining Config.class located at {}", configURL);
        fireDefiningClassEvent(classBytes);
        final Class<?> clazz = defineClass(null, classBytes, 0, classBytes.length);
        resolveClass(clazz);
        fireDefinedClassEvent(clazz);
        return clazz;
    }

    boolean hasController(final String controllerName) {
        return getControllerBytes(controllerName) != null;
    }
}
