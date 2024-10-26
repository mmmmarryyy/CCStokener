package jmandelbrot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import jmandelbrot.tools.XMLResourceBundle;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Config {

    private static final String RESOURCE_BUNDLE_BASE = "conf/resources";

    private static Logger logger = Logger.getLogger(Config.class.getCanonicalName());

    private static XMLResourceBundle xmlResources;

    public static void initialize() throws SAXException, IOException {
        logger.fine("Loading mesage resource bundle");
        xmlResources = (XMLResourceBundle) ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE, new MyControl());
    }

    public static XMLResourceBundle getResources() {
        return xmlResources;
    }

    static class MyControl extends ResourceBundle.Control {

        public List<String> getFormats(String baseName) {
            if (baseName == null) throw new NullPointerException();
            System.out.println(Arrays.asList("xml"));
            return Arrays.asList("xml");
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            System.out.println("newBundle");
            if (baseName == null || locale == null || format == null || loader == null) throw new NullPointerException();
            ResourceBundle bundle = null;
            if (format.equals("xml")) {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, format);
                System.out.println(resourceName);
                InputStream stream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    System.out.println(url.toExternalForm());
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    }
                } else {
                    stream = loader.getResourceAsStream(resourceName);
                }
                if (stream != null) {
                    InputSource source = new InputSource(stream);
                    try {
                        bundle = new XMLResourceBundle(source);
                    } catch (SAXException saxe) {
                        throw new IOException(saxe);
                    }
                }
            }
            return bundle;
        }
    }
}
