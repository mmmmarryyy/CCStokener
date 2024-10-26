package com.crowdsourcing.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * <code>ResourceLoader</code> is a utility class for manipulating java resources.
 * There are two ways of loading resources:
 * <ul>
 * <li>Using a ClassLoader</li>
 * <li>Using a Class (that is not a ClassLoader)</li>
 * </ul> 
 * The main difference is that when using a Class object (which is not a ClassLoader), if a 
 * relative path is specified, the Class' package is prefixed to makeup the resource name. 
 * If an absolute path is specified the preceding slash (/) is removed before looking for 
 * the resource. ClassLoader will not find resources that are prefixed with a (/).
 * 
 * The preferred way of loading resources in J2EE application is to use the Thread context 
 * ClassLoader (methods that do not specify a Class or a ClassLoader in this class). In JUnit 
 * test you would use a Class to load a resource with a relative path to override a resource 
 * in the classpath.  
 *  
 * @author  mparenteau, dsavard, <a href="mailto:cquezel@taleo.com">Claude Qu�zel</a>
 * @since   1.0
 */
public final class JavaResourceLoader {

    private static final String RESOURCE_NOT_FOUND = " could not be found by the resource loader." + " Make sure the class path is correctly specified and that the resource loader" + " is a valid class loaded in the correct scope.";

    /** The class' log4j Logger. */
    private static final Logger LOGGER = Logger.getLogger(JavaResourceLoader.class);

    /**
     * A helper class to get the caller's call stack.
     */
    private static final class CallerResolver extends SecurityManager {

        /**
         * Returns the <code>Class</code> at a specific offset in the caller's call stack.
         *
         * @param pOffset Offset of the <code>Class</code> in the call stack. 
         * @return The <code>Class</code> at a specified offset in the caller's call stack.
         * @since 1.2.2
         */
        private Class getCallerClass(int pOffset) {
            Class[] classContext = getClassContext();
            return classContext[pOffset + 2];
        }
    }

    private static final CallerResolver CALLER_RESOLVER = new CallerResolver();

    /**
     * Constructs a <code>JavaResourceLoader</code>.
     *
     * @since 1.0
     */
    private JavaResourceLoader() {
    }

    /**
     * Returns an input stream on the specified resource. This is simply a helper that adds 
     * validation to the standard Java mechanism of loading resources using the Thread context
     * class loader. This method performs the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is not absolute (does not starts with /).</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * Thread.currentThread().getContextClassLoader() is the ClassLoader used by this method. 
     * 
     * @param pResourcePath the resource name (not starting with a /).
     * @return an input steam on the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not absolute.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    public static InputStream getResourceAsStream(String pResourcePath) {
        InputStream is = getResourceAsStream(pResourcePath, Thread.currentThread().getContextClassLoader(), false);
        if (is == null) {
            is = getResourceAsStream(pResourcePath, CALLER_RESOLVER.getCallerClass(1).getClassLoader(), false);
        }
        if (is == null) {
            throw new IllegalArgumentException(pResourcePath + RESOURCE_NOT_FOUND);
        }
        return is;
    }

    /**
     * Returns the URL for the specified resource name.
     * This method performs the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is not absolute (does not starts with /).</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * Thread.currentThread().getContextClassLoader() is the ClassLoader used by this method. 
     * 
     * @param pResourceName the resource name
     * @return the URL for the specified resource name.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not absolute.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    public static URL getResource(String pResourceName) {
        URL url = getResource(pResourceName, Thread.currentThread().getContextClassLoader(), false);
        if (url == null) {
            url = getResource(pResourceName, CALLER_RESOLVER.getCallerClass(1).getClassLoader(), false);
        }
        if (url == null) {
            throw new IllegalArgumentException(pResourceName + RESOURCE_NOT_FOUND);
        }
        return url;
    }

    /**
     * Returns an URL for the specified resource name.
     * This method performs the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * @param pResourceName the resource name
     * @param pResourceLoader the class used to load the resource.
     * 
     * @return The URL for the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not absolute.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    public static URL getResource(String pResourceName, Class pResourceLoader) {
        return getResource(pResourceName, pResourceLoader, true);
    }

    /**
     * Returns an URL for the specified resource name.
     * This method performs the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is not absolute (does not starts with /).</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * @param pResourceName the resource name
     * @param pResourceLoader the class used to load the resource.
     * 
     * @return The URL for the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not absolute.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    public static URL getResource(String pResourceName, ClassLoader pResourceLoader) {
        return getResource(pResourceName, pResourceLoader, true);
    }

    /**
     * Returns an URL for the specified resource name.
     * This method performs the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is relative (does not starts with /) if pResourceLoader 
     * is a ClassLoder.</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * @param pResourceName the resource name
     * @param pResourceLoader the class used to load the resource.
     * @param pThrow Throw an exception if the resource is not found ?
     * 
     * @return The URL for the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not absolute.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    private static URL getResource(String pResourceName, Object pResourceLoader, boolean pThrow) {
        if (pResourceName == null) {
            throw new IllegalArgumentException("pResourcePath cannot be null");
        }
        if (pResourceName.length() == 0) {
            throw new IllegalArgumentException("pResourcePath cannot be empty");
        }
        URL url;
        if (pResourceLoader instanceof ClassLoader) {
            String fix = pResourceName;
            if (pResourceName.startsWith("/")) {
                LOGGER.warn("Call getResource() with absolute path: \"" + pResourceName + "\"", new Throwable("This stack is to help locate the invalid resource name."));
                fix = pResourceName.substring(1);
            }
            url = ((ClassLoader) pResourceLoader).getResource(fix);
        } else if (pResourceLoader instanceof Class) {
            url = ((Class) pResourceLoader).getResource(pResourceName);
        } else {
            throw new IllegalArgumentException("Invalid object type. Must be Class or ClassLoader got \"" + pResourceLoader.getClass().getName() + "\"");
        }
        if ((url == null) && pThrow) {
            throw new IllegalArgumentException(pResourceName + RESOURCE_NOT_FOUND);
        }
        return url;
    }

    /**
     * Returns an input stream on the specified resource. This is simply a helper that adds 
     * validation to the standard Java mechanism of loading resources. This method performs 
     * the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is relative (does not starts with /) if pResourceLoader 
     * is a ClassLoder.</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * @param pResourcePath the resource name
     * @param pResourceLoader the classLoader used to load the resource
     * @param pThrow Throw an exception if the resource is not found ?
     * @return an input steam on the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not relative.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    private static InputStream getResourceAsStream(String pResourcePath, Object pResourceLoader, boolean pThrow) {
        URL url = getResource(pResourcePath, pResourceLoader, pThrow);
        InputStream stream = null;
        if (url != null) {
            try {
                stream = url.openStream();
            } catch (IOException e) {
                LOGGER.warn(null, e);
            }
        }
        return stream;
    }

    /**
     * Returns an input stream on the specified resource. This is simply a helper that adds 
     * validation to the standard Java mechanism of loading resources. This method performs 
     * the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is relative (does not starts with /).</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * @param pResourcePath the resource name
     * @param pResourceLoader the classLoader used to load the resource
     * @return an input steam on the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not relative.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    public static InputStream getResourceAsStream(String pResourcePath, ClassLoader pResourceLoader) {
        return getResourceAsStream(pResourcePath, pResourceLoader, true);
    }

    /**
     * Returns an input stream on the specified resource. This is simply a helper that adds 
     * validation to the standard Java mechanism of loading resources. This method performs 
     * the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * @param pResourcePath the resource name
     * @param pResourceLoader the classLoader used to load the resource
     * @return an input steam on the specified resource.
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not relative.
     * @throws IllegalArgumentException if resource is not found.
     * @since   1.0
     */
    public static InputStream getResourceAsStream(String pResourcePath, Class pResourceLoader) {
        return getResourceAsStream(pResourcePath, pResourceLoader, true);
    }

    /**
     * Loads resource properties and adds them to pProperties. This is simply a helper that adds 
     * validation to the standard Java mechanism of loading properties. The code actually does:
     * <pre><code>
     * pProperties.load(ResourceLoader.getResourceAsStream(pResourcePath))
     * </code></pre>
     * This method performs the following validations:
     * <ul>
     * <li>Make sure the resource name is non-null.</li>
     * <li>Make sure the resource name is not absolute (does not starts with /).</li>
     * <li>Make sure the resource exists.</li>
     * <li>If the resource is taken from the override directory, a message is written to 
     * the log.</li>
     * </ul>
     * 
     * Thread.currentThread().getContextClassLoader() is the ClassLoader used by this method. 
     * 
     * @param pProperties holds the loaded properties  
     * @param pResourcePath the resource path for properties
     *  
     * @throws IllegalArgumentException if resource name is null.
     * @throws IllegalArgumentException if resource name is not absolute.
     * @throws IllegalArgumentException if resource is not found.
     * @throws RuntimeException if an IOException occurs.
     * @since 1.0
     */
    public static void loadProperties(Properties pProperties, String pResourcePath) {
        InputStream stream = getResourceAsStream(pResourcePath);
        try {
            pProperties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtils.close(stream);
        }
    }
}
