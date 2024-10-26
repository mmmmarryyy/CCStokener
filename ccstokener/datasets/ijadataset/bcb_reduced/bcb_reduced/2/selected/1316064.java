package net.sourceforge.nconfigurations.util;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * A file based resource expected to exist on the classpath.
 * 
 * @author Petr Novotník
 * @since 1.0
 */
public final class ClasspathResource implements Resource {

    /**
     * Creates a classpath resource that will use the invoking thread's current
     * context classloader to locate the resource. 
     * 
     * @param path the (class-)path of the resource
     * 
     * @throws NullPointerException if {@code path} is {@code null}
     *
     * @see Thread#getContextClassLoader()
     * @see ClassLoader#getResource(String) 
     */
    public ClasspathResource(final String path) {
        this(path, null);
    }

    /**
     * Creates a classpath resource that will use the specified classloader
     * to locate the resource. If the given classloader is {@code null} the
     * invoking thread's current context classloader will be used.
     * 
     * @param path the (class-)path of the underlying resource
     * @param cl the classloader to use; can be {@code null} to use the
     *         invoking thread's context class loader
     *
     * @throws NullPointerException if {@code path} is {@code null}
     * 
     * @see Thread#getContextClassLoader()
     * @see ClassLoader#getResource(String)
     */
    public ClasspathResource(final String path, final ClassLoader cl) {
        if (path == null) {
            throw new NullPointerException();
        }
        _path = path;
        _preferredClassLoader = cl;
    }

    /**
     * Retrieves a brief description of this resource. The exact details of
     * the representation are unspecified and subject to change, but the following
     * may be regarded as typical:
     * <pre>{@code
     * "[classpath-resource; name='net/sourceforge/nconfigurations/application.properties']"
     * }</pre>
     * @return a description of this resource
     */
    @Override
    public String toString() {
        return "[classpath-resource; name='" + getName() + "']";
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return _path;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openAsStream() throws IOException {
        ClassLoader cl = _preferredClassLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        final URL url = (cl == null) ? null : cl.getResource(_path);
        return (url == null) ? null : url.openStream();
    }

    private final String _path;

    private final ClassLoader _preferredClassLoader;
}
