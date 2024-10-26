package org.jfree.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * A collection of useful static utility methods for handling classes and object
 * instantiation.
 *
 * @author Thomas Morgner
 */
public final class ObjectUtilities {

    /**
     * A constant for using the TheadContext as source for the classloader.
     */
    public static final String THREAD_CONTEXT = "ThreadContext";

    /**
     * A constant for using the ClassContext as source for the classloader.
     */
    public static final String CLASS_CONTEXT = "ClassContext";

    /**
     * By default use the thread context.
     */
    private static String classLoaderSource = THREAD_CONTEXT;

    /**
     * The custom classloader to be used (if not null).
     */
    private static ClassLoader classLoader;

    /**
     * Default constructor - private.
     */
    private ObjectUtilities() {
    }

    /**
     * Returns the internal configuration entry, whether the classloader of
     * the thread context or the context classloader should be used.
     *
     * @return the classloader source, either THREAD_CONTEXT or CLASS_CONTEXT.
     */
    public static String getClassLoaderSource() {
        return classLoaderSource;
    }

    /**
     * Defines the internal configuration entry, whether the classloader of
     * the thread context or the context classloader should be used.
     * <p/>
     * This setting can only be defined using the API, there is no safe way
     * to put this into an external configuration file.
     *
     * @param classLoaderSource the classloader source,
     *                          either THREAD_CONTEXT or CLASS_CONTEXT.
     */
    public static void setClassLoaderSource(final String classLoaderSource) {
        ObjectUtilities.classLoaderSource = classLoaderSource;
    }

    /**
     * Returns <code>true</code> if the two objects are equal OR both
     * <code>null</code>.
     *
     * @param o1 object 1 (<code>null</code> permitted).
     * @param o2 object 2 (<code>null</code> permitted).
     * @return <code>true</code> or <code>false</code>.
     */
    public static boolean equal(final Object o1, final Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for an object, or zero if the object is
     * <code>null</code>.
     *
     * @param object the object (<code>null</code> permitted).
     * @return The object's hash code (or zero if the object is
     *         <code>null</code>).
     */
    public static int hashCode(final Object object) {
        int result = 0;
        if (object != null) {
            result = object.hashCode();
        }
        return result;
    }

    /**
     * Returns a clone of the specified object, if it can be cloned, otherwise
     * throws a CloneNotSupportedException.
     *
     * @param object the object to clone (<code>null</code> not permitted).
     * @return A clone of the specified object.
     * @throws CloneNotSupportedException if the object cannot be cloned.
     */
    public static Object clone(final Object object) throws CloneNotSupportedException {
        if (object == null) {
            throw new IllegalArgumentException("Null 'object' argument.");
        }
        if (object instanceof PublicCloneable) {
            final PublicCloneable pc = (PublicCloneable) object;
            return pc.clone();
        } else {
            try {
                final Method method = object.getClass().getMethod("clone", (Class[]) null);
                if (Modifier.isPublic(method.getModifiers())) {
                    return method.invoke(object, (Object[]) null);
                }
            } catch (NoSuchMethodException e) {
                Log.warn("Object without clone() method is impossible.");
            } catch (IllegalAccessException e) {
                Log.warn("Object.clone(): unable to call method.");
            } catch (InvocationTargetException e) {
                Log.warn("Object without clone() method is impossible.");
            }
        }
        throw new CloneNotSupportedException("Failed to clone.");
    }

    /**
     * Returns a new collection containing clones of all the items in the
     * specified collection.
     *
     * @param collection the collection (<code>null</code> not permitted).
     * @return A new collection containing clones of all the items in the
     *         specified collection.
     * @throws CloneNotSupportedException if any of the items in the collection
     *                                    cannot be cloned.
     */
    public static Collection deepClone(final Collection collection) throws CloneNotSupportedException {
        if (collection == null) {
            throw new IllegalArgumentException("Null 'collection' argument.");
        }
        final Collection result = (Collection) ObjectUtilities.clone(collection);
        result.clear();
        final Iterator iterator = collection.iterator();
        while (iterator.hasNext()) {
            final Object item = iterator.next();
            if (item != null) {
                result.add(clone(item));
            } else {
                result.add(null);
            }
        }
        return result;
    }

    /**
     * Redefines the custom classloader.
     *
     * @param classLoader the new classloader or null to use the default.
     */
    public static synchronized void setClassLoader(final ClassLoader classLoader) {
        ObjectUtilities.classLoader = classLoader;
    }

    /**
     * Returns the custom classloader or null, if no custom classloader is defined.
     *
     * @return the custom classloader or null to use the default.
     */
    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the classloader, which was responsible for loading the given
     * class.
     *
     * @param c the classloader, either an application class loader or the
     *          boot loader.
     * @return the classloader, never null.
     * @throws SecurityException if the SecurityManager does not allow to grab
     *                           the context classloader.
     */
    public static ClassLoader getClassLoader(final Class c) {
        final String localClassLoaderSource;
        synchronized (ObjectUtilities.class) {
            if (classLoader != null) {
                return classLoader;
            }
            localClassLoaderSource = classLoaderSource;
        }
        if ("ThreadContext".equals(localClassLoaderSource)) {
            final ClassLoader threadLoader = Thread.currentThread().getContextClassLoader();
            if (threadLoader != null) {
                return threadLoader;
            }
        }
        final ClassLoader applicationCL = c.getClassLoader();
        if (applicationCL == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return applicationCL;
        }
    }

    /**
     * Returns the resource specified by the <strong>absolute</strong> name.
     *
     * @param name the name of the resource
     * @param c    the source class
     * @return the url of the resource or null, if not found.
     */
    public static URL getResource(final String name, final Class c) {
        final ClassLoader cl = getClassLoader(c);
        if (cl == null) {
            return null;
        }
        return cl.getResource(name);
    }

    /**
     * Returns the resource specified by the <strong>relative</strong> name.
     *
     * @param name the name of the resource relative to the given class
     * @param c    the source class
     * @return the url of the resource or null, if not found.
     */
    public static URL getResourceRelative(final String name, final Class c) {
        final ClassLoader cl = getClassLoader(c);
        final String cname = convertName(name, c);
        if (cl == null) {
            return null;
        }
        return cl.getResource(cname);
    }

    /**
     * Transform the class-relative resource name into a global name by
     * appending it to the classes package name. If the name is already a
     * global name (the name starts with a "/"), then the name is returned
     * unchanged.
     *
     * @param name the resource name
     * @param c    the class which the resource is relative to
     * @return the tranformed name.
     */
    private static String convertName(final String name, Class c) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }
        while (c.isArray()) {
            c = c.getComponentType();
        }
        final String baseName = c.getName();
        final int index = baseName.lastIndexOf('.');
        if (index == -1) {
            return name;
        }
        final String pkgName = baseName.substring(0, index);
        return pkgName.replace('.', '/') + "/" + name;
    }

    /**
     * Returns the inputstream for the resource specified by the
     * <strong>absolute</strong> name.
     *
     * @param name the name of the resource
     * @param context the source class
     * @return the url of the resource or null, if not found.
     */
    public static InputStream getResourceAsStream(final String name, final Class context) {
        final URL url = getResource(name, context);
        if (url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the inputstream for the resource specified by the
     * <strong>relative</strong> name.
     *
     * @param name the name of the resource relative to the given class
     * @param context the source class
     * @return the url of the resource or null, if not found.
     */
    public static InputStream getResourceRelativeAsStream(final String name, final Class context) {
        final URL url = getResourceRelative(name, context);
        if (url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Tries to create a new instance of the given class. This is a short cut
     * for the common bean instantiation code.
     *
     * @param className the class name as String, never null.
     * @param source    the source class, from where to get the classloader.
     * @return the instantiated object or null, if an error occured.
     */
    public static Object loadAndInstantiate(final String className, final Class source) {
        try {
            final ClassLoader loader = getClassLoader(source);
            final Class c = loader.loadClass(className);
            return c.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Tries to create a new instance of the given class. This is a short cut
     * for the common bean instantiation code. This method is a type-safe method
     * and will not instantiate the class unless it is an instance of the given
     * type.
     *
     * @param className the class name as String, never null.
     * @param source    the source class, from where to get the classloader.
     * @param type  the type.
     * @return the instantiated object or null, if an error occurred.
     */
    public static Object loadAndInstantiate(final String className, final Class source, final Class type) {
        try {
            final ClassLoader loader = getClassLoader(source);
            final Class c = loader.loadClass(className);
            if (type.isAssignableFrom(c)) {
                return c.newInstance();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Returns <code>true</code> if this is version 1.4 or later of the
     * Java runtime.
     *
     * @return A boolean.
     */
    public static boolean isJDK14() {
        try {
            final ClassLoader loader = getClassLoader(ObjectUtilities.class);
            if (loader != null) {
                try {
                    loader.loadClass("java.util.RandomAccess");
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        try {
            final String version = System.getProperty("java.vm.specification.version");
            if (version == null) {
                return false;
            }
            String[] versions = parseVersions(version);
            String[] target = new String[] { "1", "4" };
            return (ArrayUtilities.compareVersionArrays(versions, target) >= 0);
        } catch (Exception e) {
            return false;
        }
    }

    private static String[] parseVersions(String version) {
        if (version == null) {
            return new String[0];
        }
        final ArrayList versions = new ArrayList();
        final StringTokenizer strtok = new StringTokenizer(version, ".");
        while (strtok.hasMoreTokens()) {
            versions.add(strtok.nextToken());
        }
        return (String[]) versions.toArray(new String[versions.size()]);
    }
}
