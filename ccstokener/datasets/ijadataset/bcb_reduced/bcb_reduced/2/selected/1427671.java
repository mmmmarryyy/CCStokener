package architecture.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ClassUtils extends org.apache.commons.lang.ClassUtils {

    public static InputStream getResourceAsStream(String name) {
        return loadResource(name);
    }

    public static InputStream loadResource(String name) {
        InputStream in = ClassUtils.class.getResourceAsStream(name);
        if (in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            if (in == null) in = ClassUtils.class.getClassLoader().getResourceAsStream(name);
        }
        if (in == null) try {
        } catch (Throwable e) {
        }
        return in;
    }

    public static InputStream getResourceAsStream(String resourceName, Class callingClass) {
        URL url = getResource(resourceName, callingClass);
        try {
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    public static URL getResource(String resourceName, Class callingClass) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (url == null) url = ClassUtils.class.getClassLoader().getResource(resourceName);
        if (url == null) url = callingClass.getClassLoader().getResource(resourceName);
        return url;
    }

    /**
	 * Finds all super classes and interfaces for a given class
	 * 
	 * @param cls
	 *            The class to scan
	 * @return The collected related classes found
	 */
    public static Set<Class> findAllTypes(Class cls) {
        final Set<Class> types = new HashSet<Class>();
        findAllTypes(cls, types);
        return types;
    }

    /**
	 * Finds all super classes and interfaces for a given class
	 * 
	 * @param cls
	 *            The class to scan
	 * @param types
	 *            The collected related classes found
	 */
    public static void findAllTypes(Class cls, Set<Class> types) {
        if (cls == null) {
            return;
        }
        if (types.contains(cls)) {
            return;
        }
        types.add(cls);
        findAllTypes(cls.getSuperclass(), types);
        for (int x = 0; x < cls.getInterfaces().length; x++) {
            findAllTypes(cls.getInterfaces()[x], types);
        }
    }
}
