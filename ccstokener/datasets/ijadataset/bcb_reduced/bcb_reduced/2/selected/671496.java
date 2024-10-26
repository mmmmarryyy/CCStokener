package org.dcm4cheri.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.dcm4che.util.SystemUtils;

/**
 * @author Gunter Zeilinger<gunterze@gmail.com>
 * @version $Id
 * @since Jun 26, 2006
 */
class ConfigurationUtils {

    public static void loadPropertiesForClass(Properties map, Class c) {
        String key = c.getName();
        String val = SystemUtils.getSystemProperty(key, null);
        URL url;
        if (val == null) {
            val = key.replace('.', '/') + ".properties";
            url = getResource(c, val);
        } else {
            try {
                url = new URL(val);
            } catch (MalformedURLException e) {
                url = getResource(c, val);
            }
        }
        try {
            InputStream is = url.openStream();
            try {
                map.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new ConfigurationException("failed not load resource:", e);
        }
    }

    private static URL getResource(Class c, String val) {
        URL url;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null || (url = cl.getResource(val)) == null) {
            if ((url = c.getClassLoader().getResource(val)) == null) {
                throw new ConfigurationException("missing resource: " + val);
            }
        }
        return url;
    }
}
