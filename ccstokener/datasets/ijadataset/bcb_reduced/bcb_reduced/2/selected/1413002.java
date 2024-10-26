package net.boogie.calamari.domain.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import net.boogie.calamari.domain.exception.ExceptionUtils;

public class ObjectUtils {

    /**
     * Get the Reader for a co-located resource given the Class to use as the resource locator, the
     * name of the resource, and a boolean specifying whether it is OK to search up the class path.
     * <p>
     * NOTE: If either of the input params are <code>null</code>, this throws a
     * NullPointerException; if the resource cannot be located, this throws an IOException, which is
     * different from the behaviour of <code>getResource()</code>.
     * 
     * @param resourceClass the Class used to perform resource location; must not be
     *            <code>null</code>
     * @param resourceName the name of the co-located resource whose InputStream contains the
     *            configuration data; must not be <code>null</code>
     * @param searchParents <code>true</code> to search up the path of the resourceClass if the
     *            resource cannot be located directly; <code>false</code> to seach only for the
     *            specified resource relative to the resourceClass
     * @throws NullPointerException if any of the inputs are <code>null</code>
     * @throws IOException if the resource cannot be located or errors are encountered opening an
     *             InputStream to the resource
     * @return an InputStream to the requested resource
     */
    public static Reader getReader(Class<?> resourceClass, String resourceName, boolean searchParents) throws IOException {
        return new InputStreamReader(getInputStream(resourceClass, resourceName, searchParents));
    }

    /**
     * Get the InputStream for a co-located resource given the Class to use as the resource locator,
     * the name of the resource, and a boolean specifying whether it is OK to search up the class
     * path. to use as the resource locator, and the name of the resource.
     * <p>
     * NOTE: If either of the input params are <code>null</code>, this throws a
     * NullPointerException; if the resource cannot be located, this throws an IOException, which is
     * different from the behaviour of <code>getResource()</code>.
     * 
     * @param resourceClass the Class used to perform resource location; must not be
     *            <code>null</code>
     * @param resourceName the name of the co-located resource whose InputStream contains the
     *            configuration data; must not be <code>null</code>
     * @param searchParents <code>true</code> to search up the path of the resourceClass if the
     *            resource cannot be located directly; <code>false</code> to seach only for the
     *            specified resource relative to the resourceClass
     * @throws NullPointerException if any of the inputs are <code>null</code>
     * @throws IOException if the resource cannot be located or errors are encountered opening an
     *             InputStream to the resource
     * @return an InputStream to the requested resource
     */
    public static InputStream getInputStream(Class<?> resourceClass, String resourceName, boolean searchParents) throws IOException {
        ExceptionUtils.throwIfNull(resourceClass, "resourceClass");
        ExceptionUtils.throwIfNull(resourceName, "resourceName");
        URL url = getResource(resourceClass, resourceName, searchParents);
        if (url == null) throw new IOException("unable to locate resource '" + resourceName + "' off of Class " + resourceClass == null ? "" : resourceClass.getName());
        return url.openStream();
    }

    /**
     * Get the URL for a co-located resource given the Class to use as the resource locator, and the
     * name of the resource. If the resource cannot be found, this returns <code>null</code>. If
     * either of the input params are <code>null</code> this returns <code>null</code>.
     * 
     * @param resourceClass the Class used to perform resource location; should not be
     *            <code>null</code>
     * @param resourceName the name of the co-located resource whose InputStream contains the
     *            configuration data; should not be <code>null</code>
     * @return a URL for the requested resource, or <code>null</code> if none can be found
     */
    public static URL getResource(Class<?> resourceClass, String resourceName) {
        return getResource(resourceClass, resourceName, false);
    }

    /**
     * Get the URL for a co-located resource given the Class to use as the resource locator, the
     * name of the resource, and a boolean specifying whether it is OK to search up the class path.
     * If the resource cannot be found, this returns <code>null</code>. If either of the input
     * params are <code>null</code> this returns <code>null</code>.
     * 
     * @param resourceClass the Class used to perform resource location; should not be
     *            <code>null</code>
     * @param resourceName the name of the co-located resource whose InputStream contains the
     *            configuration data; should not be <code>null</code>
     * @param searchParents <code>true</code> to search up the path of the resourceClass if the
     *            resource cannot be located directly; <code>false</code> to seach only for the
     *            specified resource relative to the resourceClass
     * @return a URL for the requested resource
     */
    public static URL getResource(Class<?> resourceClass, String resourceName, boolean searchParents) {
        if ((resourceClass == null) || (resourceName == null)) return null;
        URL url = resourceClass.getResource(resourceName);
        if ((url == null) && searchParents && !resourceName.startsWith("/")) {
            int classDepth = StringUtils.numOccurrences(resourceClass.getName(), ".");
            int searchDepth = StringUtils.numOccurrences(resourceName, "../");
            if (searchDepth < classDepth) {
                if (searchDepth < classDepth - 1) {
                    resourceName = "../" + resourceName;
                } else {
                    resourceName = "/" + StringUtils.replaceAll(resourceName, "../", "");
                }
                url = getResource(resourceClass, resourceName, searchParents);
            }
        }
        return url;
    }

    /**
     * Convert an Object to a String. This will return <code>null</code> if the input is
     * <code>null</code>, else returns the input's <code>toString()</code> value.
     * 
     * @param o the Object to convert to String
     * @return the String rep of the input
     */
    public static String toString(Object o) {
        return (o == null) ? null : o.toString();
    }
}
