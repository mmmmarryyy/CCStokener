package net.sf.jgamelibrary.i18n;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Implementation of <code>ResourceBundle.Control</code>, to load bundles from
 * xml properties.
 * 
 * @author Taras Kostiak
 * 
 */
public class XmlRBC extends ResourceBundle.Control {

    /**
     * Is only one formal for this <code>ResourceBundle.Control</code>.
     */
    public String FORMAT_XML = "xml";

    /**
     * Path where search for bundles.
     */
    protected String i18NPath = null;

    /**
     * Creates <code>XmlRBC</code> with specified path to search for i18n files.
     * 
     * @param i18NPath
     *            Path to search for i18n files.
     */
    public XmlRBC(String i18NPath) {
        this.i18NPath = i18NPath;
    }

    /**
     * Formats of bundles for this <code>ResourceBundle.Control</code>.<br>
     * Contains only one: <code>xml</code>.
     * 
     * @see java.util.ResourceBundle.Control#getFormats(java.lang.String)
     */
    public List<String> getFormats(String baseName) {
        if (baseName == null) throw new NullPointerException();
        return Arrays.asList(FORMAT_XML);
    }

    /**
     * Finds, loads and returns bundle or just returns if one is already loaded. <br>
     * Used example code from Java API.
     * 
     * @see java.util.ResourceBundle.Control#newBundle(java.lang.String,
     *      java.util.Locale, java.lang.String, java.lang.ClassLoader, boolean)
     */
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
        if (baseName == null || locale == null || format == null || loader == null) throw new NullPointerException();
        ResourceBundle bundle = null;
        if (format.equals(FORMAT_XML)) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = i18NPath + "/" + toResourceName(bundleName, format);
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
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
                BufferedInputStream bis = new BufferedInputStream(stream);
                bundle = new XmlResourceBundle(bis);
                bis.close();
            }
        }
        return bundle;
    }
}
