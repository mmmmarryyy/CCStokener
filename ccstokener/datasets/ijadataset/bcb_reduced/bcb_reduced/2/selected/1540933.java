package workday.server.localization;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Источник базовой идеи - http://javatutor.net/articles/customizing-resource-bundle-loading-with-resourcebundle-control
 *
 */
public class XMLResourceBundleControl extends ResourceBundle.Control {

    private static String XML = "xml";

    @Override
    public List<String> getFormats(String baseName) {
        return Collections.singletonList(XML);
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
        if ((baseName == null) || (locale == null) || (format == null) || (loader == null)) {
            throw new NullPointerException();
        }
        ResourceBundle bundle = null;
        if (format.equals(XML)) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, format);
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    if (reload) {
                        connection.setUseCaches(false);
                    }
                    InputStream stream = connection.getInputStream();
                    if (stream != null) {
                        BufferedInputStream bis = new BufferedInputStream(stream);
                        bundle = new XMLResourceBundle(bis);
                        bis.close();
                    }
                }
            }
        }
        return bundle;
    }

    private static class XMLResourceBundle extends ResourceBundle {

        private Properties properties;

        XMLResourceBundle(InputStream stream) throws IOException {
            properties = new Properties();
            properties.loadFromXML(stream);
        }

        protected Object handleGetObject(String key) {
            return properties.getProperty(key);
        }

        public Enumeration<String> getKeys() {
            Set<String> handleKeys = properties.stringPropertyNames();
            return Collections.enumeration(handleKeys);
        }
    }
}
