package org.streets.commons.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class used to loading resources and classes in a fault tolerant manner
 * that works across different applications servers.
 *
 */
public class ClassLoaderUtils {

    /**
	 * 获取当前线程缺省的ClassLoader
	 * @return
	 */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
        }
        if (cl == null) {
            cl = ClassUtils.class.getClassLoader();
        }
        return cl;
    }

    /**
     * Load a given resource.
     *
     * This method will try to load the resource using the following methods (in
     * order):
     * <ul>
     * <li>From Thread.currentThread().getContextClassLoader()
     * <li>From ClassLoaderUtil.class.getClassLoader()
     * <li>callingClass.getClassLoader()
     * </ul>
     *
     * @param resouce
     *            The name of the resource to load
     * @param clazz
     *            The Class object of the calling object
     */
    public static URL getResource(String resouce, Class<?> clazz) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resouce);
        if (url == null) {
            url = ClassLoaderUtils.class.getClassLoader().getResource(resouce);
        }
        if (url == null) {
            ClassLoader cl = clazz.getClassLoader();
            if (cl != null) {
                url = cl.getResource(resouce);
            }
        }
        if ((url == null) && (resouce != null) && (resouce.charAt(0) != '/')) {
            return getResource('/' + resouce, clazz);
        }
        return url;
    }

    /**
     * This is a convenience method to load a resource as a stream.
     *
     * The algorithm used to find the resource is given in getResource()
     *
     * @param resName
     *            The name of the resource to load
     * @param clazz
     *            The Class object of the calling object
     */
    public static InputStream getResourceAsStream(String resName, Class<?> clazz) {
        URL url = getResource(resName, clazz);
        try {
            return (url != null) ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Load one class with a given name.
     *
     * It will try to load the class in the following order:
     * <ul>
     * <li>From Thread.currentThread().getContextClassLoader()
     * <li>Using the basic Class.forName()
     * <li>From ClassLoaderUtil.class.getClassLoader()
     * </ul>
     *
     * @param className
     *            The name of the class to load
     * @throws ClassNotFoundException
     *             If the class cannot be found anywhere.
     */
    @SuppressWarnings("unchecked")
    public static Class loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                try {
                    return ClassLoaderUtils.class.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException exc) {
                    return null;
                }
            }
        }
    }

    /**
     * Load a class with a given name.
     * <p/>
     * It will try to load the class in the following order:
     * <ul>
     * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
     * <li>Using the basic {@link Class#forName(java.lang.String) }
     * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
     * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
     * </ul>
     *
     * @param className    The name of the class to load
     * @param callingClass The Class object of the calling object
     * @throws ClassNotFoundException If the class cannot be found anywhere.
     */
    @SuppressWarnings("unchecked")
    public static Class loadClass(String className, Class<?> callingClass) throws ClassNotFoundException {
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
     * Wait for load from jar file load implement
     * 
     */
    public static Class<?>[] loadClasses(String packageName) {
        List<Class<?>> clzzList = new ArrayList<Class<?>>();
        packageName = packageName.replaceAll("[/]", ".");
        String path = packageName.replaceAll("[.]", "/");
        ClassPathResource r = new ClassPathResource(path);
        File f = null;
        try {
            f = r.getFile();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        ClassLoader loader = getDefaultClassLoader();
        try {
            if (f != null && f.isDirectory()) {
                String[] classNames = f.list();
                for (String c : classNames) {
                    if (c.endsWith(".class")) {
                        String clzzName = StringUtils.substringBeforeLast(c, ".");
                        Class<?> clzz = loader.loadClass(packageName + "." + clzzName);
                        if (clzz != null) {
                            clzzList.add(clzz);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return clzzList.toArray(new Class[clzzList.size()]);
    }
}
