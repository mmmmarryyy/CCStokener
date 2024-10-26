package org.homeunix.thecave.moss.classpath;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.homeunix.thecave.moss.util.FileFunctions;
import org.homeunix.thecave.moss.util.Log;

/**
 * Mutable class loader, which allows you to load additional jar files at runtime,
 * via the addURL() method.  Most people would want to run this initially from their
 * main method, and use it to load other jars as needed.
 * 
 * This class loader also provides other useful meathods, such as the ability to 
 * return a list of all classes which implement / extend a given interface / class.
 * This can, for instance, be useful for automatic discovery of plugins at runtime.
 *   
 * @author wyatt
 *
 */
public class MutableClassLoader extends URLClassLoader {

    private final Set<URL> loadedURLs = new HashSet<URL>();

    /**
	 * Creates a new instance of ExtensibleClassLoader, with a parent delegate of
	 * the system class loader.
	 */
    public MutableClassLoader() {
        super(new URL[] {});
    }

    /**
	 * Creates a new instance of ExtensibleClassLoader, with a parent delegate of
	 * the system class loader, and load all specified URLs.
	 * @param urls URLs of resources to load initially.  Equivalent to calling
	 * the default constructor, and calling addURL() for each of the URLs.
	 */
    public MutableClassLoader(URL[] urls) {
        super(urls);
    }

    /**
	 * Adds the URL to the current classpath, if it has not already been added.
	 */
    public void addURL(URL url) {
        if (!loadedURLs.contains(url)) super.addURL(url);
        loadedURLs.add(url);
    }

    /**
	 * Instantiates a new object of the specified class, using the constructor which
	 * matches the given arguments.  If successful, we pass back the newly instantiated
	 * object; if not successful, we throw an exception with details of the problem.
	 * @param c The class to load
	 * @param arguments An array of the arguments to the desired constructor
	 * @return
	 * @throws MutableClassLoaderException
	 */
    public Object newInstance(Class<?> c, Object... arguments) throws MutableClassLoaderException {
        return newInstance(c.getCanonicalName(), arguments);
    }

    /**
	 * Instantiates a new object of the specified class name, using the constructor which
	 * matches the given arguments.  If successful, we pass back the newly instantiated
	 * object; if not successful, we throw an exception with details of the problem.
	 * @param className The class name to load
	 * @param arguments An array of the arguments to the desired constructor
	 * @return
	 * @throws MutableClassLoaderException
	 */
    public Object newInstance(String className, Object... arguments) throws MutableClassLoaderException {
        try {
            Class<?> c = this.loadClass(className);
            Constructor<?> requestedConstructor = null;
            Constructor<?>[] constructors = c.getConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterTypes().length == arguments.length) {
                    boolean identicalArgs = true;
                    for (int i = 0; i < arguments.length; i++) {
                        if (!constructor.getParameterTypes()[i].isInstance(arguments[i])) {
                            identicalArgs = false;
                            break;
                        }
                    }
                    if (identicalArgs) requestedConstructor = constructor;
                }
            }
            if (requestedConstructor == null) throw new MutableClassLoaderException("Could not find constructor for " + className + " with specified arguments " + Arrays.toString(arguments) + " (length == " + arguments.length + ")");
            Object o = requestedConstructor.newInstance(arguments);
            return o;
        } catch (Exception e) {
            throw new MutableClassLoaderException("Could not instantiate class " + className + ": " + e.getClass().getName());
        }
    }

    /**
	 * Returns all classes in the current class path (system class path plus any added URLs)
	 * which are descendents of or implement the given class / interface. 
	 * @param ancestor The class or interface to check against.
	 * @return
	 * @throws MutableClassLoaderException
	 */
    public List<Class<?>> getImplementingClasses(Class<?> ancestor, boolean searchAllClasspath) throws MutableClassLoaderException {
        List<Class<?>> classes = new LinkedList<Class<?>>();
        for (URL url : (searchAllClasspath ? getURLs() : getAddedURLs())) {
            Log.verbose("Checking classpath item " + url);
            if (!url.getPath().toLowerCase().endsWith("/")) {
                try {
                    JarInputStream jis = new JarInputStream(url.openStream());
                    JarEntry je;
                    while ((je = jis.getNextJarEntry()) != null) {
                        Log.verbose("Checking resource " + je.getName());
                        try {
                            if (je.getName().endsWith(".class")) {
                                Class<?> c = this.loadClass(je.getName().replaceAll("/", ".").replaceAll(".class$", ""));
                                if (!Modifier.isAbstract(c.getModifiers()) && !Modifier.isInterface(c.getModifiers()) && ancestor.isAssignableFrom(c)) {
                                    Log.verbose("Found class " + c.getCanonicalName() + " which implements class " + ancestor.getCanonicalName());
                                    classes.add(c);
                                }
                            }
                        } catch (Error e) {
                        } catch (RuntimeException re) {
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    Log.error(e);
                }
            } else if (url.getPath().endsWith("/")) {
                File root = new File(url.getPath());
                for (File file : FileFunctions.getFileTree(root)) {
                    try {
                        if (file.getName().toLowerCase().endsWith(".class")) {
                            Class<?> c = this.loadClass(file.getAbsolutePath().replaceAll("^" + root.getAbsolutePath() + "/", "").replaceAll("/", ".").replaceAll(".class$", ""));
                            if (!Modifier.isAbstract(c.getModifiers()) && !Modifier.isInterface(c.getModifiers()) && ancestor.isAssignableFrom(c)) {
                                Log.verbose("Found class " + c.getCanonicalName() + " which implements class " + ancestor.getCanonicalName());
                                classes.add(c);
                            }
                        }
                    } catch (Exception e) {
                        Log.error(e);
                    }
                }
            }
        }
        return classes;
    }

    /**
	 * Returns a list of instantiated obejcts, all of which are instances of the
	 * given class.  The arguments for the constructor used to instantiate the 
	 * objects are given by constructorArguments.
	 * 
	 * This method is meant as a convenience method, if all the object instances
	 * share the same contructor signature.
	 * @param c
	 * @param constructorAguments
	 * @return
	 */
    public List<?> getImplementingObjects(Class<?> c, Object... constructorAguments) {
        List<Object> objects = new LinkedList<Object>();
        try {
            List<Class<?>> classes = getImplementingClasses(c, true);
            for (Class<?> class1 : classes) {
                try {
                    if (!Modifier.isAbstract(class1.getModifiers()) && !Modifier.isInterface(class1.getModifiers()) && c.isAssignableFrom(class1)) {
                        Object o = newInstance(class1, constructorAguments);
                        objects.add(o);
                    }
                } catch (MutableClassLoaderException mcle) {
                    Log.warning(mcle);
                }
            }
        } catch (MutableClassLoaderException mcle) {
            Log.error(mcle);
        }
        return objects;
    }

    /**
	 * Returns all the URLs which were initially in the classpath, and which have been 
	 * added via the addURL() method.  To get only the URLs which have been loaded
	 * via addURL(), use the getAddedURLs() method instead.
	 * @return All URLs in the classpath, including both those on the system 
	 * classpath and those which have been added via the addURL() method.
	 */
    @Override
    public URL[] getURLs() {
        List<URL> urls = new LinkedList<URL>();
        if (getParent() instanceof URLClassLoader) {
            urls.addAll(Arrays.asList(((URLClassLoader) getParent()).getURLs()));
            urls.addAll(Arrays.asList(super.getURLs()));
            return urls.toArray(new URL[] {});
        }
        return super.getURLs();
    }

    /**
	 * Returns all the URLs which have been added via the addURL() method.  To get all
	 * URLs in the classpath, use the getURLs() method instead.
	 * @return All URLs which have been added via the addURL() method.
	 */
    public URL[] getAddedURLs() {
        return super.getURLs();
    }

    /**
	 * An exception which is thrown by MutableClassLoader.
	 * @author wyatt
	 *
	 */
    public static class MutableClassLoaderException extends Exception {

        public static final long serialVersionUID = 0;

        public MutableClassLoaderException(Throwable throwable) {
            super(throwable);
        }

        public MutableClassLoaderException(String message) {
            super(message);
        }

        public MutableClassLoaderException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }
}
