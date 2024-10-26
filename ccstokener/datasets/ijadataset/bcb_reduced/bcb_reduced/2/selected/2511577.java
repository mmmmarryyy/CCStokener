package net.sourceforge.javautil.common.locale;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;

/**
 * A standard locator that will include the
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: DefaultBundleLocator.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class DefaultBundleLocator implements IResourceBundleLocator {

    protected Map<String, ResourceBundle> bundles = new HashMap<String, ResourceBundle>();

    public ResourceBundle getFor(String bundleName, Locale locale) {
        String key = this.getKey(bundleName, locale);
        if (!bundles.containsKey(key)) {
            synchronized (bundles) {
                this.bundles.put(key, this.createComposite(bundleName, locale));
            }
        }
        return bundles.get(key);
    }

    public void clearCache() {
        this.bundles.clear();
    }

    /**
	 * @param locale The locale for which a composite bundle is needed
	 * @return A composite bundle for this locale
	 */
    protected synchronized ResourceBundle createComposite(String bundleName, Locale locale) {
        List<ResourceBundle> bundles = new ArrayList<ResourceBundle>();
        for (Locale current = locale; current != null; current = this.getNextLessRefined(current)) {
            String prefix = this.getKey(bundleName, current);
            ResourceBundle bundle = this.loadBundle(prefix);
            if (bundle != null) bundles.add(bundle);
        }
        ResourceBundle defaultBundle = this.loadBundle(this.getKey(bundleName, null));
        if (defaultBundle != null) bundles.add(defaultBundle);
        if (bundles.size() == 0) {
            return ResourceBundle.getBundle(bundleName, locale, Thread.currentThread().getContextClassLoader());
        }
        return new DefaultBundle(locale, bundles.toArray(new ResourceBundle[bundles.size()]));
    }

    /**
	 * @param prefix The prefix for the bundle
	 * @return The bundle associated with this prefix, or null if none could be located
	 */
    protected ResourceBundle loadBundle(String prefix) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(prefix + ".properties");
        if (url != null) {
            try {
                return new PropertyResourceBundle(url.openStream());
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }
        return null;
    }

    /**
	 * @param locale The current locale
	 * @return The next less refined locale or null if no more local's can be calculated
	 */
    protected Locale getNextLessRefined(Locale locale) {
        if (!"".equals(locale.getVariant())) return new Locale(locale.getLanguage(), locale.getCountry());
        if (!"".equals(locale.getCountry())) return new Locale(locale.getLanguage());
        Locale[] defaults = this.getDefaultLocales();
        if (!locale.equals(defaults[2])) {
            if (!locale.equals(locale != defaults[1])) {
                if (!locale.equals(locale != defaults[0])) {
                    return defaults[0];
                } else {
                    return defaults[1];
                }
            } else {
                return defaults[2];
            }
        }
        return null;
    }

    /**
	 * @return The current default locales for the current context
	 */
    protected Locale[] getDefaultLocales() {
        Locale defaultLocale = LocaleContext.getCurrentLocale();
        return new Locale[] { defaultLocale, ("".equals(defaultLocale.getVariant()) ? defaultLocale : new Locale(defaultLocale.getLanguage(), defaultLocale.getCountry())), ("".equals(defaultLocale.getCountry()) ? defaultLocale : new Locale(defaultLocale.getLanguage())) };
    }

    /**
	 * @param locale The locale for which a key is needed
	 * @return A locale unique key for referencing this locale
	 */
    protected String getKey(String bundleName, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(bundleName);
        if (locale != null) {
            if (!"".equals(locale.getLanguage())) sb.append("_").append(locale.getLanguage());
            if (!"".equals(locale.getCountry())) sb.append("_").append(locale.getCountry());
            if (!"".equals(locale.getVariant())) sb.append("_").append(locale.getVariant());
        }
        return sb.toString();
    }
}
