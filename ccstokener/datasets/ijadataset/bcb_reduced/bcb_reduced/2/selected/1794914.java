package bookez.view.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.faces.context.FacesContext;

public abstract class UnicodeResourceBundle extends ResourceBundle {

    protected static final String BUNDLE_EXTENSION = "properties";

    protected static final Control UTF8_CONTROL = new Utf8Control();

    public UnicodeResourceBundle(String bundleName) {
        setBundleName(bundleName);
        ResourceBundle initParent = ResourceBundle.getBundle(getBundleName(), FacesContext.getCurrentInstance().getViewRoot().getLocale(), UTF8_CONTROL);
        setParent(initParent);
    }

    public String getBundleName() {
        return this.bundleName;
    }

    protected void setBundleName(String value) {
        this.bundleName = value;
    }

    private String bundleName;

    @Override
    protected Object handleGetObject(String key) {
        return parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return parent.getKeys();
    }

    protected static class Utf8Control extends Control {

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, BUNDLE_EXTENSION);
            ResourceBundle bundle = null;
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
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
