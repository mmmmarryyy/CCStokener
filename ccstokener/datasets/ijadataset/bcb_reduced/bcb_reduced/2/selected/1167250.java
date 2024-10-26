package net.sourceforge.crhtetris.i18n;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * Implementation of {@link Control} from its javadoc-example for loading XML-based bundles.
 * 
 * @author croesch
 * @since Date: Aug 17, 2011
 */
final class XMLBundleControl extends Control {

    @Override
    public List<String> getFormats(final String baseName) {
        if (baseName == null) {
            throw new IllegalArgumentException();
        }
        return Arrays.asList("xml");
    }

    @Override
    public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload) throws IllegalAccessException, InstantiationException, IOException {
        if (baseName == null || locale == null || format == null || loader == null) {
            throw new IllegalArgumentException();
        }
        if (!format.equals("xml")) {
            return null;
        }
        ResourceBundle bundle = null;
        final String resourceName = toResourceName(toBundleName(baseName, locale), format);
        InputStream stream = null;
        if (reload) {
            final URL url = loader.getResource(resourceName);
            if (url == null) {
                return null;
            }
            final URLConnection connection = url.openConnection();
            if (connection == null) {
                return null;
            }
            connection.setUseCaches(false);
            stream = connection.getInputStream();
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }
        if (stream != null) {
            final BufferedInputStream bis = new BufferedInputStream(stream);
            try {
                bundle = new XMLResourceBundle(bis);
            } finally {
                bis.close();
            }
        }
        return bundle;
    }
}
