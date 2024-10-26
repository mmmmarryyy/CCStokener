package net.sf.jmodule.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Dmitri Koulakov
 */
public class Resources {

    private static Log log = LogFactory.getLog(Resources.class);

    private static String RESOURCES_HOME = "resources";

    private static Map<ClassLoader, Resources> cache = new WeakHashMap<ClassLoader, Resources>();

    public static Resources getResources() {
        return getResources(Thread.currentThread().getContextClassLoader());
    }

    public static Resources getResources(ClassLoader classLoader) {
        Resources resources = cache.get(classLoader);
        if (resources == null) {
            resources = new Resources(classLoader, RESOURCES_HOME);
            cache.put(classLoader, resources);
        }
        return resources;
    }

    private static Map<CacheKey, ResourceBundle> bundleCache = new HashMap<CacheKey, ResourceBundle>();

    protected static class CacheKey {

        protected String base_name;

        protected Locale locale;

        protected CacheKey(String base_name, Locale locale) {
            this.base_name = base_name;
            this.locale = locale;
        }

        public int hashCode() {
            return base_name.hashCode() + locale.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) obj;
            return this.base_name.equals(other.base_name) && this.locale.equals(other.locale);
        }
    }

    protected static Locale ROOT_LOCALE = new Locale("");

    public static List<Locale> getCandidateLocales(String base_name, Locale locale) {
        if (base_name == null) {
            throw new NullPointerException();
        }
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        List<Locale> locales = new ArrayList<Locale>(4);
        if (variant.length() > 0) {
            locales.add(locale);
        }
        if (country.length() > 0) {
            locales.add((locales.size() == 0) ? locale : new Locale(language, country, ""));
        }
        if (language.length() > 0) {
            locales.add((locales.size() == 0) ? locale : new Locale(language, "", ""));
        }
        locales.add(ROOT_LOCALE);
        return locales;
    }

    protected static class XmlResourceBundle extends ResourceBundle {

        private ResourceBundle parent;

        private URL url;

        private Properties properties = new Properties();

        XmlResourceBundle(ResourceBundle parent, URL url) {
            this.parent = parent;
            this.url = url;
            try {
                properties.loadFromXML(url.openStream());
            } catch (Exception ex) {
                log.error("on load properties from " + url);
            }
        }

        protected Object handleGetObject(String key) {
            Object value = properties.get(key);
            if (value == null && parent != null) value = parent.getObject(key);
            return value;
        }

        public Enumeration<String> getKeys() {
            Set<String> keys = new TreeSet<String>();
            for (Object key : properties.keySet()) {
                keys.add((String) key);
            }
            return Collections.enumeration(keys);
        }

        public void setValue(String key, String value) {
            properties.setProperty(key, value);
        }

        public void save() throws IOException {
            if (url != null) {
                try {
                    OutputStream stream = null;
                    try {
                        stream = url.openConnection().getOutputStream();
                    } catch (UnknownServiceException ex) {
                        if ("file".equals(url.getProtocol())) {
                            File file = new File(url.toURI());
                            File dir = file.getParentFile();
                            if (!dir.exists()) dir.mkdirs();
                            stream = new FileOutputStream(file);
                        }
                    }
                    if (stream != null) {
                        properties.storeToXML(stream, "Automatically saved by " + Resources.class.getName(), "UTF-8");
                    }
                } catch (IOException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new IOException(ex.toString());
                }
            }
        }
    }

    ClassLoader classLoader;

    Location resourcesHome;

    public Resources(ClassLoader classLoader, String resourcesHomePath) {
        if (classLoader == null) throw new NullPointerException("classLoader");
        if (resourcesHomePath == null) throw new NullPointerException("resourcesHome");
        this.classLoader = classLoader;
        try {
            resourcesHome = new Location(resourcesHomePath);
            if (!resourcesHome.exists()) {
                log.trace("no resources home (" + resourcesHomePath + "): trying to create it");
                resourcesHome.createFolder();
            }
        } catch (Exception ex) {
            log.trace("can't create resources home", ex);
        }
        if (log.isTraceEnabled()) {
            log.trace("resources home: " + resourcesHome);
        }
    }

    public URL getResource(String resourceName) {
        try {
            if (resourcesHome.exists()) {
                Location resource = resourcesHome.resolve(resourceName);
                if (resource.exists()) return resource.toURL();
            }
        } catch (Exception ex) {
            log.warn("on loading resource " + resourceName, ex);
        }
        return classLoader.getResource(resourceName);
    }

    protected ResourceBundle getBundle(String baseName, Locale locale) {
        CacheKey key = new CacheKey(baseName, locale);
        ResourceBundle bundle = bundleCache.get(key);
        if (bundle != null) return bundle;
        List<Locale> locales = getCandidateLocales(baseName, locale);
        URL url = null;
        for (Locale lo : locales) {
            url = getResource(toTextResourceName(baseName, lo));
            if (url == null) {
                try {
                    return ResourceBundle.getBundle(baseName, lo, classLoader);
                } catch (MissingResourceException ex) {
                }
            }
            if (url != null) break;
        }
        ResourceBundle parent = getParentBundle(baseName, locale);
        if (url == null) throw new MissingResourceException("Can't find bundle for base name " + baseName + ", locale " + locale, baseName, "");
        bundle = new XmlResourceBundle(parent, url);
        bundleCache.put(key, bundle);
        return bundle;
    }

    private ResourceBundle getParentBundle(String baseName, Locale locale) {
        if (baseName.length() > 0) {
            String parentName = null;
            int dot = baseName.lastIndexOf('.');
            if (dot == -1) parentName = ""; else parentName = baseName.substring(0, dot);
            try {
                return getBundle(parentName, locale);
            } catch (MissingResourceException ex) {
            }
        }
        return null;
    }

    protected static String toBundleName(String baseName, Locale locale) {
        if (locale.equals(ROOT_LOCALE)) {
            return baseName;
        }
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        if (language == "" && country == "" && variant == "") {
            return baseName;
        }
        StringBuilder sb = new StringBuilder(baseName);
        sb.append('_');
        if (variant != "") {
            sb.append(language).append('_').append(country).append('_').append(variant);
        } else if (country != "") {
            sb.append(language).append('_').append(country);
        } else {
            sb.append(language);
        }
        return sb.toString();
    }

    protected String toResourceName(String bundleName, String suffix) {
        StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
        sb.append(bundleName.replace('.', '/')).append('.').append(suffix);
        return sb.toString();
    }

    protected String toTextResourceName(String baseName, Locale locale) {
        if (baseName.length() == 0) baseName = "texts"; else baseName += ".texts";
        String bundleName = toBundleName(baseName, locale);
        return toResourceName(bundleName, "xml");
    }

    public String getText(String baseName, Locale locale, String key) {
        try {
            return getBundle(baseName, locale).getString(key);
        } catch (MissingResourceException ex) {
            log.warn("on loading text " + baseName + "[locale=" + locale + ",key=" + key + "]", ex);
            return key;
        }
    }

    public void setText(String baseName, Locale locale, String key, String text) {
        try {
            XmlResourceBundle bundle = (XmlResourceBundle) getBundle(baseName, locale);
            bundle.setValue(key, text);
            bundle.save();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getFormat(URL url) {
        String src = url.getFile();
        try {
            return src.substring(src.lastIndexOf('.'));
        } catch (Exception ex) {
            throw new IllegalArgumentException("unknown format: " + url);
        }
    }

    private void writeFile(String name, URL url) throws IOException {
        Location location = resourcesHome.resolve(name);
        InputStream input = url.openStream();
        OutputStream output = location.getOutputStream();
        try {
            byte buf[] = new byte[1024];
            int read;
            while (true) {
                read = input.read(buf);
                if (read == -1) break;
                output.write(buf, 0, read);
            }
        } finally {
            try {
                input.close();
            } finally {
                output.close();
            }
        }
    }

    protected URL getResource(String baseName, List<String> formats) {
        for (String format : formats) {
            try {
                String resourceName = toResourceName(baseName, format);
                URL url = getResource(resourceName);
                if (url != null) return url;
            } catch (Exception ex) {
                log.error("on loading resource " + baseName, ex);
            }
        }
        return null;
    }

    public static List<String> imageFormats = Arrays.asList("png", "gif", "jpg", "jpeg", "bmp");

    public List<String> getImageFormats() {
        return imageFormats;
    }

    public URL getImage(String name) {
        return getResource(name, imageFormats);
    }

    public void setImage(String name, URL url) throws IOException {
        String resourceName = toResourceName(name, getFormat(url));
        writeFile(resourceName, url);
    }

    public List<String> soundFormats = Arrays.asList("wav", "mp3", "ogg");

    public List<String> getSoundFormats() {
        return soundFormats;
    }

    public URL getSound(String name) {
        return getResource(name, soundFormats);
    }

    public void setSound(String name, URL url) throws IOException {
        String resource_name = toResourceName(name, getFormat(url));
        writeFile(resource_name, url);
    }
}
