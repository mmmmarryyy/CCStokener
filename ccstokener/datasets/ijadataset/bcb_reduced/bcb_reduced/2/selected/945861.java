package com.pentagaia.tb.start.testbase.tools;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import com.pentagaia.tb.start.api.IPdsClassLoaderExtension;
import com.pentagaia.tb.start.impl.util.StreamUtil;

/**
 * A class loader extension to hack the ServerSocketFactory
 * 
 * @author mepeisen
 * @version 0.1.0
 * @since 0.1.0
 */
public class ExporterCLE implements IPdsClassLoaderExtension {

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getGlobalClassSet()
     */
    public String[] getGlobalClassSet() {
        return new String[] { "com.pentagaia.tb.start.testbase.tools.ExporterServerSocketFactory", "com.pentagaia.tb.start.testbase.tools.Exporter" };
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getResource(java.lang.String)
     */
    public URL getResource(final String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getResourceFirst(java.lang.String)
     */
    public URL getResourceFirst(final String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getResources(java.lang.String)
     */
    public Enumeration<URL> getResources(final String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#loadClass(java.lang.String)
     */
    public byte[] loadClass(final String className) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#loadClassExternal(java.lang.String)
     */
    public Class<?> loadClassExternal(final String className) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#loadClassFirst(java.lang.String)
     */
    public byte[] loadClassFirst(final String className) {
        if (className.equals("com.sun.sgs.impl.util.Exporter")) {
            final URL url = Thread.currentThread().getContextClassLoader().getResource("com/sun/sgs/impl/util/Exporter.class.bin");
            if (url != null) {
                try {
                    return StreamUtil.read(url.openStream());
                } catch (final IOException e) {
                }
            }
            throw new IllegalStateException("Unable to load Exporter.class.bin");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#refactorClass(java.lang.String, byte[])
     */
    public byte[] refactorClass(final String className, final byte[] source) {
        return null;
    }
}
