package de.spotnik.mail.application;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Utility class for managing OS resources associated with SWT/JFace controls
 * such as colors, fonts, images, etc. !!! IMPORTANT !!! Application code must
 * explicitly invoke the <code>dispose()</code> method to release the
 * operating system resources managed by cached objects when those objects and
 * OS resources are no longer needed (e.g. on application shutdown) This class
 * may be freely distributed as part of any application or plugin.
 * <p>
 * Copyright (c) 2003 - 2005, Instantiations, Inc. <br>
 * All Rights Reserved
 * 
 * @author scheglov_ke
 * @author Dan Rubel
 */
public class ResourceManager extends SWTResourceManager {

    /** the class logger. */
    private static final Logger LOG = Logger.getLogger(ResourceManager.class);

    /** Maps image descriptors to images */
    private static HashMap<ImageDescriptor, Image> mDescriptorImageMap = new HashMap<ImageDescriptor, Image>();

    /** Maps URL to images */
    private static HashMap<URL, Image> mURLImageMap = new HashMap<URL, Image>();

    /**
     * Dispose of cached objects and their underlying OS resources. This should
     * only be called when the cached objects are no longer needed (e.g. on
     * application shutdown)
     */
    public static void dispose() {
        disposeColors();
        disposeFonts();
        disposeImages();
        disposeCursors();
    }

    /**
     * Dispose all of the cached images
     */
    public static void disposeImages() {
        SWTResourceManager.disposeImages();
        for (Iterator it = mDescriptorImageMap.values().iterator(); it.hasNext(); ) {
            ((Image) it.next()).dispose();
        }
        mDescriptorImageMap.clear();
    }

    /**
     * Returns an image based on the specified image descriptor
     * 
     * @param descriptor ImageDescriptor The image descriptor for the image
     * @return Image The image based on the specified image descriptor
     */
    public static Image getImage(ImageDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }
        Image image = mDescriptorImageMap.get(descriptor);
        if (image == null) {
            image = descriptor.createImage();
            mDescriptorImageMap.put(descriptor, image);
        }
        return image;
    }

    /**
     * Returns an image descriptor stored in the file at the specified path
     * relative to the specified class
     * 
     * @param clazz Class The class relative to which to find the image
     *            descriptor
     * @param path String The path to the image file
     * @return ImageDescriptor The image descriptor stored in the file at the
     *         specified path
     */
    public static ImageDescriptor getImageDescriptor(Class clazz, String path) {
        return ImageDescriptor.createFromFile(clazz, path);
    }

    /**
     * Returns an image descriptor stored in the file at the specified path
     * 
     * @param path String The path to the image file
     * @return ImageDescriptor The image descriptor stored in the file at the
     *         specified path
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        try {
            return ImageDescriptor.createFromURL((new File(path)).toURL());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Retuns an image based on a plugin and file path
     * 
     * @param plugin Object The plugin containing the image
     * @param name String The path to th eimage within the plugin
     * @return Image The image stored in the file at the specified path
     */
    public static Image getPluginImage(Object plugin, String name) {
        try {
            try {
                URL url = getPluginImageURL(plugin, name);
                if (mURLImageMap.containsKey(url)) {
                    return mURLImageMap.get(url);
                }
                InputStream is = url.openStream();
                Image image;
                try {
                    image = getImage(is);
                    mURLImageMap.put(url, image);
                } finally {
                    is.close();
                }
                return image;
            } catch (Throwable e) {
                LOG.debug("Ignore any exceptions");
            }
        } catch (Throwable e) {
            LOG.debug("Ignore any exceptions");
        }
        return null;
    }

    /**
     * Retuns an image descriptor based on a plugin and file path
     * 
     * @param plugin Object The plugin containing the image
     * @param name String The path to th eimage within the plugin
     * @return ImageDescriptor The image descriptor stored in the file at the
     *         specified path
     */
    public static ImageDescriptor getPluginImageDescriptor(Object plugin, String name) {
        try {
            try {
                URL url = getPluginImageURL(plugin, name);
                return ImageDescriptor.createFromURL(url);
            } catch (Throwable e) {
                LOG.debug("Ignore any exceptions");
            }
        } catch (Throwable e) {
            LOG.debug("Ignore any exceptions");
        }
        return null;
    }

    /**
     * Retuns an URL based on a plugin and file path
     * 
     * @param plugin Object The plugin containing the file path
     * @param name String The file path
     * @return URL The URL representing the file at the specified path
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private static URL getPluginImageURL(Object plugin, String name) throws Exception {
        try {
            Class bundleClass = Class.forName("org.osgi.framework.Bundle");
            Class bundleContextClass = Class.forName("org.osgi.framework.BundleContext");
            if (bundleContextClass.isAssignableFrom(plugin.getClass())) {
                Method getBundleMethod = bundleContextClass.getMethod("getBundle", new Class[] {});
                Object bundle = getBundleMethod.invoke(plugin, new Object[] {});
                Class ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                Class pathClass = Class.forName("org.eclipse.core.runtime.Path");
                Constructor pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                Object path = pathConstructor.newInstance(new Object[] { name });
                Class platformClass = Class.forName("org.eclipse.core.runtime.Platform");
                Method findMethod = platformClass.getMethod("find", new Class[] { bundleClass, ipathClass });
                return (URL) findMethod.invoke(null, new Object[] { bundle, path });
            }
        } catch (Throwable e) {
            LOG.debug("Ignore any exceptions");
        }
        Class pluginClass = Class.forName("org.eclipse.core.runtime.Plugin");
        if (pluginClass.isAssignableFrom(plugin.getClass())) {
            Class ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
            Class pathClass = Class.forName("org.eclipse.core.runtime.Path");
            Constructor pathConstructor = pathClass.getConstructor(new Class[] { String.class });
            Object path = pathConstructor.newInstance(new Object[] { name });
            Method findMethod = pluginClass.getMethod("find", new Class[] { ipathClass });
            return (URL) findMethod.invoke(plugin, new Object[] { path });
        }
        return null;
    }
}
