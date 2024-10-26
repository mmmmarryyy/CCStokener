package com.tinywebgears.tuatara.framework.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultI18NPropertyLoader implements I18NPropertyLoaderIF {

    private final Logger logger = LoggerFactory.getLogger(DefaultI18NPropertyLoader.class);

    private final I18NResourceFinderIF resourceLocator;

    private final Map<URL, Properties> cache = new HashMap<URL, Properties>();

    private final String bundleFileName;

    private final String bundleFileExtension;

    public DefaultI18NPropertyLoader() {
        this(new DefaultI18NResourceFinder());
    }

    public DefaultI18NPropertyLoader(I18NResourceFinderIF resourceLocator) {
        this(resourceLocator, GlobalResourceNames.BUNDLE_FILENAME_SUFFIX, GlobalResourceNames.BUNDLE_FILENAME_EXTENSION);
    }

    public DefaultI18NPropertyLoader(I18NResourceFinderIF resourceLocator, String bundleFileNameSuffix, String bundleFileExtension) {
        this.resourceLocator = resourceLocator;
        this.bundleFileName = bundleFileNameSuffix;
        this.bundleFileExtension = bundleFileExtension;
    }

    public String getString(Locale locale, PropertyId propertyId) {
        return getString(locale, propertyId, false);
    }

    private String getString(Locale locale, PropertyId propertyId, Boolean inner) {
        URL url = getBundleURL(locale, propertyId.getPackage());
        String msg = getStringImpl(url, propertyId.getKey());
        if (msg == null && locale != null) {
            msg = getString(null, propertyId, true);
        }
        if (msg == null && !inner) return propertyId.toString();
        return msg;
    }

    private MessageFormat getMessageFormat(Locale locale, PropertyId propertyId) {
        URL url = getBundleURL(locale, propertyId.getPackage());
        if (url == null) return null;
        String msg = getStringImpl(url, propertyId.getKey());
        if (msg == null) {
            if (locale != null) return getMessageFormat(null, propertyId);
            return null;
        }
        return new MessageFormat("url=" + url + ", key=" + propertyId.getKey(), msg);
    }

    private URL getBundleURL(Locale locale, String resourcePackage) {
        URL url = resourceLocator.findResource(locale, ResourceKey.create(resourcePackage, bundleFileName, bundleFileExtension));
        return url;
    }

    private synchronized String getStringImpl(URL url, String propertyKey) {
        if (url == null) return null;
        Properties properties = cache.get(url);
        if (properties != null) return getStringImpl(properties, propertyKey);
        properties = readProperties(url);
        if (properties != null) {
            cache.put(url, properties);
            return getStringImpl(properties, propertyKey);
        }
        return null;
    }

    private Properties readProperties(URL url) {
        try {
            InputStream is = url.openStream();
            Properties loaded = new Properties();
            loaded.load(is);
            Properties result = new Properties();
            for (Object key : loaded.keySet()) {
                String k = (String) key;
                String value = loaded.getProperty(k);
                if (value != null) {
                    value = value.trim();
                    try {
                        byte[] isoBytes = value.getBytes("ISO-8859-1");
                        String unicodeString = new String(isoBytes, "UTF-8");
                        result.put(k, unicodeString);
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("Failed to convert text to unicode: key " + k + " value: " + value + ", err=" + e);
                        continue;
                    }
                }
            }
            is.close();
            return result;
        } catch (IOException e) {
            logger.warn("Failed to load property file: " + url + ", err=" + e);
            return null;
        }
    }

    private String getStringImpl(Properties properties, String propertyKey) {
        String result = properties.getProperty(propertyKey);
        if (result == null || result.length() == 0) return result;
        return result.trim();
    }

    public String getFormattedString(Locale locale, PropertyId propertyId, String... args) {
        MessageFormat mf = getMessageFormat(locale, propertyId);
        if (mf == null) return propertyId.toString();
        return mf.format(args);
    }
}
