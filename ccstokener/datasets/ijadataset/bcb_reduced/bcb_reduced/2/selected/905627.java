package net.sf.crispy.properties;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import net.sf.crispy.PropertiesLoader;

/**
 * Load poperty file from a url. Example: <code>file://c:/temp/example.properties</code>.
 * 
 * @author Linke
 *
 */
public class UrlPropertiesLoader implements PropertiesLoader {

    private URL url = null;

    public UrlPropertiesLoader(URL pvUrl) {
        url = pvUrl;
    }

    public UrlPropertiesLoader(String pvUrl) {
        try {
            url = new URL(pvUrl);
        } catch (MalformedURLException e) {
            throw new PropertiesLoadException("Error by create URL with file: " + pvUrl, e);
        }
    }

    public Properties load() {
        Properties lvProperties = new Properties();
        try {
            InputStream lvInputStream = url.openStream();
            lvProperties.load(lvInputStream);
        } catch (Exception e) {
            throw new PropertiesLoadException("Error in load-method:", e);
        }
        return lvProperties;
    }
}
