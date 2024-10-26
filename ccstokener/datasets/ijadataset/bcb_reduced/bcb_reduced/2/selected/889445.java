package org.dcm4che2.imageio;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.dcm4che2.data.ConfigurationError;
import org.dcm4che2.util.CloseUtils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug 6, 2007
 */
class ImageReaderWriterFactory {

    protected final Properties config = new Properties();

    protected ImageReaderWriterFactory(String key, String def) {
        String val = System.getProperty(key, def);
        URL url;
        try {
            url = new URL(val);
        } catch (MalformedURLException e) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null || (url = cl.getResource(val)) == null) {
                if ((url = ImageReaderWriterFactory.class.getClassLoader().getResource(val)) == null) {
                    throw new ConfigurationError("missing resource: " + val);
                }
            }
        }
        InputStream is = null;
        try {
            is = url.openStream();
            config.load(is);
        } catch (IOException e) {
            throw new ConfigurationError("failed to load imageio configuration from " + url, e);
        } finally {
            CloseUtils.safeClose(is);
        }
    }
}
