package net.sf.logdistiller.util;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Helper to load extensions for <b>LogDistiller</b>. Extensions are declared in <code>logdistiller.properties</code>
 * resource file, as properties. Each extensible part can be declared as a property which key value specifies the part
 * extended (<code>logtypes</code>, <code>plugins</code>, <code>publishers</code> or <code>reportformats</code>), the
 * value is the full class name (with package) of the extension.
 *
 * @since 0.7
 */
public class ExtensionHelper {

    private static List<Properties> extensionProperties;

    public static List<String> findExtensions(String type) {
        if (extensionProperties == null) {
            extensionProperties = findExtensions();
        }
        List<String> extensions = new ArrayList<String>();
        for (Properties prop : extensionProperties) {
            String extension = prop.getProperty(type);
            if (extension != null) {
                extensions.add(extension);
            }
        }
        return extensions;
    }

    private static List<Properties> findExtensions() {
        URL url = null;
        try {
            List<Properties> extensions = new ArrayList<Properties>();
            Enumeration<URL> res = ExtensionHelper.class.getClassLoader().getResources("logdistiller.properties");
            while (res.hasMoreElements()) {
                url = res.nextElement();
                Properties prop = new Properties();
                prop.load(url.openStream());
                extensions.add(prop);
            }
            return extensions;
        } catch (IOException ioe) {
            String msg = (url == null) ? "unable to list resources logdistiller.properties" : "unable to load resource " + url.toExternalForm();
            throw new RuntimeException(msg, ioe);
        }
    }
}
