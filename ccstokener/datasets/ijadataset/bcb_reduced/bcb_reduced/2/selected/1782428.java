package com.pentagaia.tb.tx.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import com.pentagaia.tb.start.api.IPdsClassLoaderExtension;
import com.pentagaia.tb.start.api.IPdsKernel;
import com.pentagaia.tb.start.api.IPdsKernelProvider;
import com.pentagaia.tb.start.api.IPdsPropertyParser;
import com.pentagaia.tb.start.impl.util.PropertyUtil;
import com.pentagaia.tb.start.impl.util.StreamUtil;

/**
 * A kernel provider to register the transaction service
 * 
 * @author mepeisen
 * @version 0.1.0
 * @since 0.1.0
 */
public class KernelProvider implements IPdsKernelProvider, IPdsPropertyParser, IPdsClassLoaderExtension {

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsKernelProvider#onPreStartup(com.pentagaia.tb.start.api.IPdsKernel)
     */
    public void onPreStartup(final IPdsKernel kernel) {
        kernel.installPropertyParser(this);
        kernel.installClassLoaderExtension(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsPropertyParser#refactor(java.util.Properties, java.util.Properties)
     */
    public void refactor(final Properties systemProperties, final Properties applicationProperties) {
        PropertyUtil.addServiceManager(applicationProperties, "com.pentagaia.tb.tx.impl.TransactionManager", "com.pentagaia.tb.tx.impl.TransactionService");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getGlobalClassSet()
     */
    public String[] getGlobalClassSet() {
        return null;
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
        if (className.startsWith("com.sun.sgs.impl.service.transaction.TransactionCoordinatorImpl")) {
            String resource = null;
            if (className.equals("com.sun.sgs.impl.service.transaction.TransactionCoordinatorImpl")) {
                resource = "com/sun/sgs/impl/service/transaction/TransactionCoordinatorImpl.class.bin";
            }
            if (resource != null) {
                final URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
                if (url != null) {
                    try {
                        return StreamUtil.read(url.openStream());
                    } catch (final IOException e) {
                    }
                }
                throw new IllegalStateException("Unable to load binary class");
            }
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
