package com.netime.commons.standard.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import com.netime.commons.standard.Constants;
import com.netime.commons.standard.exceptions.AOSystemException;
import com.netime.commons.standard.log.LogManager;
import com.netime.commons.standard.log.LogMessage;

public final class ResourceLoader {

    public static final String CLASS_NAME = ResourceLoader.class.getName();

    public static Properties getResourceAsProperties(String fileName) throws IOException {
        Properties defaultProps = new Properties();
        FileInputStream in = new FileInputStream(ResourceLoader.class.getClassLoader().getResource(fileName).getFile());
        defaultProps.load(in);
        in.close();
        return defaultProps;
    }

    public static InputStream getResourceAsStream(String resource) throws AOSystemException {
        return getResourceAsStream(resource, null);
    }

    /**
	 * Load a given resource.
	 * <p/>
	 * This method will try to load the resource using the following methods (in
	 * order):
	 * <ul>
	 * <li>From {@link Thread#getContextClassLoader()
	 * Thread.currentThread().getContextClassLoader()}
	 * <li>From {@link Class#getClassLoader()
	 * ClassLoaderUtil.class.getClassLoader()}
	 * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader()
	 * }
	 * </ul>
	 * 
	 * @param resourceName
	 *            The name of the resource to load
	 * @param callingClass
	 *            The Class object of the calling object
	 */
    public static URL getResource(String resourceName, Class callingClass) throws AOSystemException {
        String METHOD_NAME = "getResource(String resourceName, Class callingClass)";
        URL url = null;
        String[] args = { resourceName };
        try {
            url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
            if (url == null) {
                url = ResourceLoader.class.getClassLoader().getResource(resourceName);
            }
            if (url == null && callingClass != null) {
                url = callingClass.getClassLoader().getResource(resourceName);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new AOSystemException(Constants.RESOURCE_NOT_FOUND, CLASS_NAME, METHOD_NAME, args, ex);
        }
        if (url == null) throw new AOSystemException(Constants.RESOURCE_NOT_FOUND, CLASS_NAME, METHOD_NAME, args, null);
        return url;
    }

    /**
	 * This is a convenience method to load a resource as a stream.
	 * <p/>
	 * The algorithm used to find the resource is given in getResource()
	 * 
	 * @param resourceName
	 *            The name of the resource to load
	 * @param callingClass
	 *            The Class object of the calling object
	 * @throws AOSystemException
	 */
    public static InputStream getResourceAsStream(String resourceName, Class callingClass) throws AOSystemException {
        final String METHOD_NAME = "getResourceAsStream(String resourceName, Class callingClass)";
        URL url = getResource(resourceName, callingClass);
        try {
            return (url != null) ? url.openStream() : null;
        } catch (IOException e) {
            LogManager.log(new LogMessage(LogManager.FATAL, CLASS_NAME, METHOD_NAME, e.getMessage()));
            return null;
        }
    }

    /**
	 * Load a class with a given name.
	 * <p/>
	 * It will try to load the class in the following order:
	 * <ul>
	 * <li>From {@link Thread#getContextClassLoader()
	 * Thread.currentThread().getContextClassLoader()}
	 * <li>Using the basic {@link Class#forName(java.lang.String) }
	 * <li>From {@link Class#getClassLoader()
	 * ClassLoaderUtil.class.getClassLoader()}
	 * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader()
	 * }
	 * </ul>
	 * 
	 * @param className
	 *            The name of the class to load
	 * @param callingClass
	 *            The Class object of the calling object
	 * @throws ClassNotFoundException
	 *             If the class cannot be found anywhere.
	 */
    public static Class loadClass(String className, Class callingClass) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                try {
                    return ResourceLoader.class.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException exc) {
                    return callingClass.getClassLoader().loadClass(className);
                }
            }
        }
    }

    /**
	 * Prints the current classloader hierarchy - useful for debugging.
	 */
    public static void printClassLoader() {
        System.out.println("ClassLoaderUtils.printClassLoader");
        printClassLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
	 * Prints the classloader hierarchy from a given classloader - useful for
	 * debugging.
	 */
    public static void printClassLoader(ClassLoader cl) {
        System.out.println("ClassLoaderUtils.printClassLoader(cl = " + cl + ")");
        if (cl != null) {
            printClassLoader(cl.getParent());
        }
    }

    /**
	 * @param args
	 * @throws IOException
	 * @throws AOSystemException
	 */
    public static void main(String[] args) throws IOException, AOSystemException {
        System.out.println(getResourceAsStream("com/meridian/commons/standard/xml/SpotDeal.xslt", null));
    }
}
