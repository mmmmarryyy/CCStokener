package com.crowdsourcing.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * <code>ClassLoaderUtils</code> provides functions related to Class loading.
 *
 * @author fbellet
 * @since 1.2
 */
public final class ClassLoaderUtils {

    /**
     * Class Not instantiable. 
     * 
     * @since 1.2
     */
    private ClassLoaderUtils() {
    }

    /**
     * Load and initialize a class in a web application.
     * Load the class with the ClassLoader attached to the current thread 
     * (Thread Context ClassLoader - TCL).
     * In a web application, the TCL is set to the WAR ClassLoader by the web container.
     * The order in which classes are resolved is :
     * <ol>
     * <li>WAR ClassLoader</li>
     * <li>EAR ClassLoader (Parent of the WAR ClassLoader)</li>
     * </ol>
     * In a JUNIT or a main() method, the TCL is the system ClassLoader.<br/>
     * @param pClassName     Class Name. 
     * 
     * @return The initialized Class for the specified Class name. 
     * @throws ClassNotFoundException
     * @since 1.2
     */
    public static Class loadWebAppClass(String pClassName) throws ClassNotFoundException {
        return Class.forName(pClassName, true, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Create a new <code>WebAppObjectInputStream</code> to deserialize objects 
     * in a web application.
     *
     * @param pInputStream Input stream.
     * @return A new <code>WebAppObjectInputStream</code>.
     * @throws IOException
     * @since 1.2
     */
    public static WebAppObjectInputStream createWebAppObjectInputStream(InputStream pInputStream) throws IOException {
        return new WebAppObjectInputStream(pInputStream);
    }

    /**
     * <code>WebAppObjectInputStream</code> uses the Thread Context ClassLoader (TCL)
     * to deserialize classes. Since the Akira classes reside in the EAR, 
     * it is the only way for them to find the classes of the WAR when they
     * deserialize objects. 
     * In a web application, the TCL is set to the WAR ClassLoader by the web container.
     * If the TCL fails to resolve a class, the default method implemented in
     * ObjectInputStream is used (Caller's ClassLoader). 
     * The order in which classes are resolved is :
     * <ol>
     * <li>WAR ClassLoader</li>
     * <li>EAR ClassLoader (Parent of the WAR ClassLoader)</li>
     * </ol>
     *
     * @author fbellet
     * @since 1.2
     */
    public static class WebAppObjectInputStream extends ObjectInputStream {

        /**
         * Constructs a <code>WebObjectInputStream</code>.
         *
         * @throws IOException
         * @throws SecurityException
         * @since 1.2
         */
        protected WebAppObjectInputStream() throws IOException {
            super();
        }

        /**
         * Constructs a <code>WebObjectInputStream</code>.
         *
         * @param pIn Input stream.
         * @throws IOException
         * @throws SecurityException
         * @since 1.2
         */
        protected WebAppObjectInputStream(InputStream pIn) throws IOException {
            super(pIn);
        }

        /**
         * {@inheritDoc}
         */
        protected Class resolveClass(ObjectStreamClass pDesc) throws IOException, ClassNotFoundException {
            Class clazz = null;
            try {
                clazz = Class.forName(pDesc.getName(), false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException cnfe) {
                clazz = super.resolveClass(pDesc);
            }
            return clazz;
        }
    }

    /**
     * Load all resources with a given name, potentially aggregating all results 
     * from the searched classloaders.  If no results are found, the resource name
     * is prepended by '/' and tried again.
     *
     * This method will try to load the resources using the following methods (in order):
     * <ul>
     *  <li>From Thread.currentThread().getContextClassLoader()
     *  <li>From ClassLoaderUtil.class.getClassLoader()
     *  <li>callingClass.getClassLoader()
     * </ul>
     *
     * @param resourceName The name of the resources to load
     * @param callingClass The Class object of the calling object
     */
    public static Iterator<URL> getResources(String resourceName, Class callingClass, boolean aggregate) throws IOException {
        AggregateIterator<URL> iterator = new AggregateIterator<URL>();
        iterator.addEnumeration(Thread.currentThread().getContextClassLoader().getResources(resourceName));
        if (!iterator.hasNext() || aggregate) {
            iterator.addEnumeration(ClassLoaderUtils.class.getClassLoader().getResources(resourceName));
        }
        if (!iterator.hasNext() || aggregate) {
            ClassLoader cl = callingClass.getClassLoader();
            if (cl != null) {
                iterator.addEnumeration(cl.getResources(resourceName));
            }
        }
        if (!iterator.hasNext() && (resourceName != null) && (resourceName.charAt(0) != '/')) {
            return getResources('/' + resourceName, callingClass, aggregate);
        }
        return iterator;
    }

    /**
    * Load a given resource.
    *
    * This method will try to load the resource using the following methods (in order):
    * <ul>
    *  <li>From Thread.currentThread().getContextClassLoader()
    *  <li>From ClassLoaderUtil.class.getClassLoader()
    *  <li>callingClass.getClassLoader()
    * </ul>
    *
    * @param resourceName The name IllegalStateException("Unable to call ")of the resource to load
    * @param callingClass The Class object of the calling object
    */
    public static URL getResource(String resourceName, Class callingClass) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (url == null) {
            url = ClassLoaderUtils.class.getClassLoader().getResource(resourceName);
        }
        if (url == null) {
            ClassLoader cl = callingClass.getClassLoader();
            if (cl != null) {
                url = cl.getResource(resourceName);
            }
        }
        if ((url == null) && (resourceName != null) && (resourceName.charAt(0) != '/')) {
            return getResource('/' + resourceName, callingClass);
        }
        return url;
    }

    /**
    * This is a convenience method to load a resource as a stream.
    *
    * The algorithm used to find the resource is given in getResource()
    *
    * @param resourceName The name of the resource to load
    * @param callingClass The Class object of the calling object
    */
    public static InputStream getResourceAsStream(String resourceName, Class callingClass) {
        URL url = getResource(resourceName, callingClass);
        try {
            return (url != null) ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
    * Load a class with a given name.
    *
    * It will try to load the class in the following order:
    * <ul>
    *  <li>From Thread.currentThread().getContextClassLoader()
    *  <li>Using the basic Class.forName()
    *  <li>From ClassLoaderUtil.class.getClassLoader()
    *  <li>From the callingClass.getClassLoader()
    * </ul>
    *
    * @param className The name of the class to load
    * @param callingClass The Class object of the calling object
    * @throws ClassNotFoundException If the class cannot be found anywhere.
    */
    public static Class loadClass(String className, Class callingClass) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                try {
                    return ClassLoaderUtils.class.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException exc) {
                    return callingClass.getClassLoader().loadClass(className);
                }
            }
        }
    }

    /**
     * Aggregates Enumeration instances into one iterator and filters out duplicates.  Always keeps one
     * ahead of the enumerator to protect against returning duplicates.
     */
    protected static class AggregateIterator<E> implements Iterator<E> {

        LinkedList<Enumeration<E>> enums = new LinkedList<Enumeration<E>>();

        Enumeration<E> cur = null;

        E next = null;

        Set<E> loaded = new HashSet<E>();

        public AggregateIterator addEnumeration(Enumeration<E> e) {
            if (e.hasMoreElements()) {
                if (cur == null) {
                    cur = e;
                    next = e.nextElement();
                    loaded.add(next);
                } else {
                    enums.add(e);
                }
            }
            return this;
        }

        public boolean hasNext() {
            return (next != null);
        }

        public E next() {
            if (next != null) {
                E prev = next;
                next = loadNext();
                return prev;
            } else {
                throw new NoSuchElementException();
            }
        }

        private Enumeration<E> determineCurrentEnumeration() {
            if (cur != null && !cur.hasMoreElements()) {
                if (enums.size() > 0) {
                    cur = enums.removeLast();
                } else {
                    cur = null;
                }
            }
            return cur;
        }

        private E loadNext() {
            if (determineCurrentEnumeration() != null) {
                E tmp = cur.nextElement();
                while (loaded.contains(tmp)) {
                    tmp = loadNext();
                    if (tmp == null) {
                        break;
                    }
                }
                if (tmp != null) {
                    loaded.add(tmp);
                }
                return tmp;
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
