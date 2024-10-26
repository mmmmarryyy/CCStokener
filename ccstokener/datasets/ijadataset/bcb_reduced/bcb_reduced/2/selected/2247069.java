package com.vgkk.hula.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.io.*;
import java.net.URL;

/**
 * This class is roughly analogous to a ResourceBundle but allows for property
 * files to use encodings other than ASCII.  
 * 
 * Note: As of Java 5 this is possible using regular ResourceBundles and
 * property files but only if XML is used.
 * 
 * Important API Change: As of the alpha3 release the properties localizer
 * assumes UTF8 encoding.  It is no longer possible to specify different
 * encodings.
 * 
 * @author Tim Romero
 * @author Daniel Leuck
 */
public class PropertiesLocalizer {

    private static final String LOCALE_FILE_URL_SUFFIX = ".hprops";

    private static PropertiesLocalizer instance;

    private HashMap<Locale, Map<String, String>> localeToMap = new HashMap<Locale, Map<String, String>>();

    /**
     * Returns the instance of this class.
     * @return the instance of this class
     * @author (2004-Oct-22) Tim Romero CR: Velin Doycinov
     */
    public static PropertiesLocalizer getInstance() {
        if (instance == null) {
            instance = new PropertiesLocalizer();
        }
        return instance;
    }

    /**
	 * Private constructor.
	 */
    private PropertiesLocalizer() {
    }

    /**
     * Adds a locale to the localizaer.  Properties for the given locale are
     * loaded from the classpath (default package) by searching for 
     * files in the format xx.properties (top level) or xx_XX.properties
     * for locale specific language variants.  All files must use UTF8 encoding.
     * 
     * @param the locale to load
     * @throws IOException if the reource file cannot be loaded
     * @author (2005-Jul-08) Daniel Leuck CR: ??
     */
    private void addLocale(Locale locale) throws MissingResourceException {
        locale = stripVariant(locale);
        Map<String, String> props = localeToMap.get(locale);
        if (props == null) props = new HashMap<String, String>();
        String urlName = locale.toString() + LOCALE_FILE_URL_SUFFIX;
        URL url = ClassLoader.getSystemClassLoader().getResource(urlName);
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(urlName);
            if (url == null) {
                throw new MissingResourceException("Cannot locate hprops for " + "locale " + locale, "PropertiesLocalizer", "HProperties: for " + locale);
            }
        }
        BufferedReader input = null;
        try {
            input = new BufferedReader(new InputStreamReader((InputStream) url.openStream(), "UTF8"));
            String str;
            while ((str = input.readLine()) != null) {
                str = str.trim();
                if (!str.startsWith("#") && !str.startsWith("!")) {
                    int index = -1;
                    for (int i = 0; i < str.length(); i++) {
                        char c = str.charAt(i);
                        if (c == ':' || c == '=') {
                            index = i;
                            break;
                        }
                    }
                    if (index > 0) {
                        String key = str.substring(0, index).trim();
                        String value = str.substring(index + 1);
                        props.put(key, value);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new MissingResourceException(ioe.getMessage(), "PropertiesLocalizer", "HProperties: for " + locale.toString());
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {
            }
        }
        localeToMap.put(locale, props);
    }

    /**
     * Strips the variant portion off a local if it exists.  Variants are
     * poorly defined in the Locale class.  They sometimes indicate encodings
     * and sometimes indicate other things.  We ignore this portion and specify
     * the ecoding separately.
     * 
     * @param The locale to strip
     * @return The new locale with no variant portion  
     * 
     * @author (2005-Jul-08) Daniel Leuck CR: Tim Romero
     */
    private Locale stripVariant(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        return (country.length() == 0) ? new Locale(language) : new Locale(language, country);
    }

    /**
     * Returns the map of all localized Strings in the the specified locale or 
     * null if the specified locale does not exisit or has not been initalized.
     * 
     * If properties for the given locale are no in the cache, this method will
     * attempt to auto-load them assuming UTF8 encoding.
     * 
     * Note: MissingResourceExceptions are suppressed in this method.  If any
     * errors occur the method returns null.  We do this because autoloading
     * should be transparent and also so client methods can check cascading
     * properties (Language_Country -> Language)
     * 
     * @param locale the locale key for these properties
     * @return tha map of all localized strings in the specified locale or null if
     * the specified locale does not exisit or has not been initalized
     * @author (2005-Jul-08) Daniel Leuck CR: ??
     */
    public Map<String, String> getProperties(Locale locale) {
        Map<String, String> props = localeToMap.get(stripVariant(locale));
        if (props == null) {
            try {
                addLocale(locale);
                props = localeToMap.get(stripVariant(locale));
            } catch (MissingResourceException mre) {
            }
        }
        return props;
    }

    /**
     * Return the localized value of the specifed key in the given locale.  If
     * the key is not available and the locale specifies a country, the country
     * portion is stripped and the language is searched by itself.  If the key
     * still cannot be located null is returned.  In many cases the application
     * will want to substitute a string in the applications default locale if
     * no locale specific string exists.
     * 
     * Note: If the locale specifies a country, this method handles both the
     * case where no properties file exists for the country (but does for the
     * language) and the case where the country specific file exists but does
     * not contain the key.
     * 
     * @param language the language of the localization.
     * @param key the key to be localized.
     * @return the localized value of the specifed key in the given locale or
     *         null if no local or a parent local does not exist
     * @author (2005-Jul-08) Daniel Leuck CR: ??
     */
    public String getValue(Locale locale, String key) {
        locale = stripVariant(locale);
        Map<String, String> props = getProperties(locale);
        if (locale.getCountry().length() == 0) return (props == null) ? null : props.get(key);
        if (props == null) {
            props = getProperties(new Locale(locale.getLanguage()));
            return (props == null) ? null : props.get(key);
        }
        String value = props.get(key);
        if (value != null) return value;
        if (value == null) props = getProperties(new Locale(locale.getLanguage()));
        return (props == null) ? null : props.get(key);
    }
}
