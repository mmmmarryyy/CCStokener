package fi.arcusys.acj.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for loading properties for unit testing from a resource.
 * 
 * @version 1.0 $Rev: 1562 $
 * @author mikko Copyright © 2008 Arcusys Ltd. - http://www.arcusys.fi/
 * 
 * 
 */
public final class TestPropertiesUtil {

    private static final Logger LOG = LoggerFactory.getLogger(TestPropertiesUtil.class);

    private TestPropertiesUtil() {
    }

    /**
     * <p>
     * Properties can be specified in a configuration resource, which is looked
     * up using the following sequence:
     * </p>
     * <ol>
     * <li>Resource <code>/[BaseName].properties</code></li>
     * <li>Resource <code>[BaseName].properties</code></li>
     * <li>Resource <code>/[ClassName].properties</code></li>
     * <li>Resource <code>[ClassName].properties</code></li>
     * <li>Resource <code>/[BaseName]-[ClassName].properties</code></li>
     * <li>Resource <code>[BaseName]-[ClassName].properties</code></li>
     * </ol>
     * <p>
     * The lookup sequence is started from the class hierarchy's base class and
     * repeated for every extended class until the actual runtime class is
     * reached. All the found configuration resources are loaded in the
     * specified order. Latest configuration settings remain effective.
     * </p>
     * 
     * @param props
     *            the Properties to initialize
     * @param callingClazz
     *            the calling class
     * @param hierarchyRootClazz
     *            the hierarchy root class
     * @param resourceBaseName
     *            base name of the properties resource (without the
     *            ".properties" suffix).
     * @return <code>true</code> if properties were loaded or <code>false</code>
     *         if not
     */
    public static boolean loadTestProperties(Properties props, Class<?> callingClazz, Class<?> hierarchyRootClazz, String resourceBaseName) {
        if (!hierarchyRootClazz.isAssignableFrom(callingClazz)) {
            throw new IllegalArgumentException("Class " + callingClazz + " is not derived from " + hierarchyRootClazz);
        }
        if (null == resourceBaseName) {
            throw new NullPointerException("resourceBaseName is null");
        }
        String fqcn = callingClazz.getName();
        String uqcn = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        String callingClassResource = uqcn + ".properties";
        String globalCallingClassResource = "/" + callingClassResource;
        String baseClassResource = resourceBaseName + "-" + uqcn + ".properties";
        String globalBaseClassResource = "/" + baseClassResource;
        String pkgResource = resourceBaseName + ".properties";
        String globalResource = "/" + pkgResource;
        boolean loaded = false;
        final String[] resources = { baseClassResource, globalBaseClassResource, callingClassResource, globalCallingClassResource, pkgResource, globalResource };
        List<URL> urls = new ArrayList<URL>();
        Class<?> clazz = callingClazz;
        do {
            for (String res : resources) {
                URL url = clazz.getResource(res);
                if (null != url && !urls.contains(url)) {
                    urls.add(url);
                }
            }
            if (hierarchyRootClazz.equals(clazz)) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
            }
        } while (null != clazz);
        ListIterator<URL> it = urls.listIterator(urls.size());
        while (it.hasPrevious()) {
            URL url = it.previous();
            InputStream in = null;
            try {
                LOG.info("Loading test properties from resource: " + url);
                in = url.openStream();
                props.load(in);
                loaded = true;
            } catch (IOException ex) {
                LOG.warn("Failed to load properties from resource: " + url, ex);
            }
            IOUtil.closeSilently(in);
        }
        return loaded;
    }
}
