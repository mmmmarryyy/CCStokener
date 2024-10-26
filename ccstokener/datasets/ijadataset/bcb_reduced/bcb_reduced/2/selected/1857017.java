package nuts.core.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import sun.misc.ClassLoaderUtil;

/**
 * utility class for ClassLoader
 */
public class ClassLoaderUtils {

    /**
	 * Load a given resource.
	 * 
	 * This method will try to load the resource using the following methods (in order):
	 * <ul>
	 * <li>From Thread.currentThread().getContextClassLoader()
	 * <li>From ClassLoaderUtil.class.getClassLoader()
	 * </ul>
	 * 
	 * @param resourceName The name IllegalStateException("Unable to call ")of the resource to load
	 * @return resource URL
	 */
    public static URL getResource(String resourceName) {
        return getResource(resourceName, null);
    }

    /**
	 * Load a given resource.
	 * 
	 * This method will try to load the resource using the following methods (in order):
	 * <ul>
	 * <li>From Thread.currentThread().getContextClassLoader()
	 * <li>From ClassLoaderUtil.class.getClassLoader()
	 * <li>callingClass.getClassLoader()
	 * </ul>
	 * 
	 * @param resourceName The name IllegalStateException("Unable to call ")of the resource to load
	 * @param callingClass The Class object of the calling object
	 * @return resource URL
	 */
    public static URL getResource(String resourceName, Class callingClass) {
        URL url = null;
        ClassLoader classLoader = null;
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            url = classLoader.getResource(resourceName);
        }
        if (url == null) {
            classLoader = ClassLoaderUtil.class.getClassLoader();
            if (classLoader != null) {
                url = classLoader.getResource(resourceName);
            }
        }
        if (url == null && callingClass != null) {
            classLoader = callingClass.getClassLoader();
            if (classLoader != null) {
                url = classLoader.getResource(resourceName);
            }
        }
        if ((url == null) && (resourceName != null) && ((resourceName.length() == 0) || (resourceName.charAt(0) != '/'))) {
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
	 * @return resource InputStream
	 */
    public static InputStream getResourceAsStream(String resourceName) {
        return getResourceAsStream(resourceName, null);
    }

    /**
	 * This is a convenience method to load a resource as a stream.
	 * 
	 * The algorithm used to find the resource is given in getResource()
	 * 
	 * @param resourceName The name of the resource to load
	 * @param callingClass The Class object of the calling object
	 * @return resource InputStream
	 */
    public static InputStream getResourceAsStream(String resourceName, Class callingClass) {
        URL url = getResource(resourceName, callingClass);
        try {
            return (url != null) ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
