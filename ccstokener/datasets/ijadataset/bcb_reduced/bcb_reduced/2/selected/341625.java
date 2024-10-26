package com.dyuproject.ioc;

import java.io.IOException;
import java.net.URL;

/**
 * A resolver that resolves a resource by loading it from the classpath.
 * 
 * @author David Yu
 * @created Feb 23, 2009
 */
public final class ClasspathResolver extends AbstractResolver {

    /**
     * The type of this resolver. ("classpath")
     */
    public static final String TYPE = generateTypeFromClass(ClasspathResolver.class);

    /**
     * The default instance
     */
    public static final ClasspathResolver DEFAULT = new ClasspathResolver();

    private static boolean __checkParents = Boolean.getBoolean("classpathresolver.check_parents");

    /**
     * Gets the default instance.
     */
    public static ClasspathResolver getDefault() {
        return DEFAULT;
    }

    /**
     * Gets the flag whether to lookup the resource from the classloader including its parent.
     */
    public static boolean isCheckParents() {
        return __checkParents;
    }

    /**
     * Sets the flag whether to lookup the resource from the classloader including its parent.
     */
    public static void setCheckParents(boolean checkParents) {
        __checkParents = checkParents;
    }

    /**
     * Loads a class from the classloader; 
     * If not found, the classloader of the {@code context} class specified will be used.
     */
    public static Class<?> loadClass(String className, Class<?> context) {
        return Parser.loadClass(className, context, __checkParents);
    }

    /**
     * Loads a class from the classloader.
     * 
     * @param className
     * @return the class being loaded
     */
    public static Class<?> loadClass(String className) {
        return Parser.loadClass(className, ClasspathResolver.class, __checkParents);
    }

    /**
     * Loads a {@link URL} resource from the classloader;
     * If not found, the classloader of the {@code context} class specified will be used.
     */
    public static URL getResource(String path, Class<?> context) {
        return Parser.getResource(path, context, __checkParents);
    }

    /**
     * Loads a {@link URL} resource from the classloader.
     */
    public static URL getResource(String path) {
        return Parser.getResource(path, ClasspathResolver.class, __checkParents);
    }

    public ClasspathResolver() {
    }

    public String getType() {
        return TYPE;
    }

    public void resolve(Resource resource, Context context) throws IOException {
        URL url = getResource(resource.getPath());
        if (url == null) throw new IOException(resource.getPath() + " not found in classpath.");
        resource.resolve(newReader(url.openStream()), getType());
    }

    public Resource createResource(String path) throws IOException {
        URL url = getResource(path);
        if (url == null) throw new IOException(path + " not found in classpath.");
        return new Resource(path, getType(), newReader(url.openStream()));
    }
}
