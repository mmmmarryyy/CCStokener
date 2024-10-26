package org.code4flex.cgflexintegration.widgets.utils;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * 
 * @author Facundo Merighi
 * @version $Revision: 1.1 $
 */
public class ImageLoader {

    private static HashMap<URL, Image> m_URLImageMap = new HashMap<URL, Image>();

    private static HashMap<ImageDescriptor, Image> m_DescriptorImageMap = new HashMap<ImageDescriptor, Image>();

    public static Image getPluginImage(Object plugin, String name) {
        try {
            try {
                URL url = getPluginImageURL(plugin, name);
                if (m_URLImageMap.containsKey(url)) return m_URLImageMap.get(url);
                InputStream is = url.openStream();
                Image image;
                try {
                    image = getImage(is);
                    m_URLImageMap.put(url, image);
                } finally {
                    is.close();
                }
                return image;
            } catch (Throwable e) {
            }
        } catch (Throwable e) {
        }
        return null;
    }

    private static URL getPluginImageURL(Object plugin, String name) throws Exception {
        try {
            Class<?> bundleClass = Class.forName("org.osgi.framework.Bundle");
            Class<?> bundleContextClass = Class.forName("org.osgi.framework.BundleContext");
            if (bundleContextClass.isAssignableFrom(plugin.getClass())) {
                Method getBundleMethod = bundleContextClass.getMethod("getBundle", new Class[0]);
                Object bundle = getBundleMethod.invoke(plugin, new Object[0]);
                Class<?> ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path");
                Constructor<?> pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                Object path = pathConstructor.newInstance(new Object[] { name });
                Class<?> platformClass = Class.forName("org.eclipse.core.runtime.Platform");
                Method findMethod = platformClass.getMethod("find", new Class[] { bundleClass, ipathClass });
                return (URL) findMethod.invoke(null, new Object[] { bundle, path });
            }
        } catch (Throwable e) {
        }
        {
            Class<?> pluginClass = Class.forName("org.eclipse.core.runtime.Plugin");
            if (pluginClass.isAssignableFrom(plugin.getClass())) {
                Class<?> ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path");
                Constructor<?> pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                Object path = pathConstructor.newInstance(new Object[] { name });
                Method findMethod = pluginClass.getMethod("find", new Class[] { ipathClass });
                return (URL) findMethod.invoke(plugin, new Object[] { path });
            }
        }
        return null;
    }

    public static Image getImage(ImageDescriptor descriptor) {
        if (descriptor == null) return null;
        Image image = m_DescriptorImageMap.get(descriptor);
        if (image == null) {
            image = descriptor.createImage();
            m_DescriptorImageMap.put(descriptor, image);
        }
        return image;
    }

    protected static Image getImage(InputStream is) {
        Display display = Display.getCurrent();
        ImageData data = new ImageData(is);
        if (data.transparentPixel > 0) return new Image(display, data, data.getTransparencyMask());
        return new Image(display, data);
    }
}
