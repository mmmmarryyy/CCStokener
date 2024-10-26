package com.googlecode.yoohoo.message;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import com.googlecode.yoohoo.utils.IoUtils;

public class MessageManager implements IMessageManager {

    private static final Log log = LogFactory.getLog(MessageManager.class);

    protected BundleContext bundleContext;

    protected Map<Locale, ResourceBundle> resources;

    private static final String DEFAULT_RESOURCE_BUNDLE_PATH_BASE = IMessageManager.DEFAULT_I18N_PATH + "/messages";

    private static final Locale DEFAULT_LOCALE = new Locale("en", "US");

    private Object resourcesLock = new Object();

    public MessageManager(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        resources = new HashMap<Locale, ResourceBundle>();
    }

    @Override
    public String getMessage(String key, Locale locale) {
        return getMessage(key, new String[] {}, locale);
    }

    @Override
    public String getMessage(String key, String[] args, Locale locale) {
        return getMessage(key, args, null, locale);
    }

    @Override
    public String getMessage(String key, String[] args, String defaultMessage, Locale locale) {
        checkResourceLoaded(locale);
        ResourceBundle resource = getResourceByLocale(locale);
        try {
            return new MessageFormat(resource.getString(key)).format(args);
        } catch (MissingResourceException e) {
            if (defaultMessage != null) return defaultMessage;
            throw e;
        }
    }

    private void checkResourceLoaded(Locale locale) {
        synchronized (resourcesLock) {
            if (!resources.containsKey(locale)) {
                resources.put(locale, createResourceBundle(locale));
            }
        }
    }

    protected ResourceBundle createResourceBundle(Locale locale) {
        InputStream in = null;
        try {
            URL url = bundleContext.getBundle().getEntry(getResourceBundlePath(locale));
            if (url == null) return null;
            return new PropertyResourceBundle(url.openStream());
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Can't open resource " + getResourceBundlePathBase() + "_" + locale.toString() + ".properties");
            }
            return null;
        } finally {
            IoUtils.closeIO(in);
        }
    }

    protected String getResourceBundlePathBase() {
        return DEFAULT_RESOURCE_BUNDLE_PATH_BASE;
    }

    protected String getResourceBundlePath(Locale locale) {
        return getResourceBundlePathBase() + "_" + locale.toString() + ".properties";
    }

    protected ResourceBundle getResourceByLocale(Locale locale) {
        ResourceBundle resource = resources.get(locale);
        if (resource != null) return resource;
        resource = resources.get(DEFAULT_LOCALE);
        if (resource != null) return resource;
        resource = createResourceBundle(DEFAULT_LOCALE);
        if (resource == null) throw new MissingResourceException(String.format("Can't open default i18n resource '%s' of plugin '%s'", getResourceBundlePath(DEFAULT_LOCALE), getClass().getName()), null, null);
        return resource;
    }
}
