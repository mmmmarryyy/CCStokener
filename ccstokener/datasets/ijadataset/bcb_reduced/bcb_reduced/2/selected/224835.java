package net.hussnain.io.im;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;
import net.hussnain.io.im.*;

/**
 * interface implementation for information about the input method for loading. 
 *
 * Input mapping for Urdu will be created automatically at start.
 * To add other mappings for custom languages defined in *.rid-files, copy rabt mappings
 * for keyboard to folder with the name \".rabtPad\mappings\". The folder \".rabtPad\" should
 * exist in your home folder.
 *
 */
public class RabtInputMethodDescriptor implements java.awt.im.spi.InputMethodDescriptor {

    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("RabtPad");

    private static String urduLocalePath = "/net/hussnain/io/im/urdu_rabt.rid.xml";

    private static String urduLocalePathCrulp = "/net/hussnain/io/im/urdu_crulp.rid.xml";

    private static String programDirectory = ".rabtPad";

    private static String mappingDirectory = "mappings";

    java.util.Hashtable<String, Locale> locales;

    java.util.Hashtable<String, Properties> localesProps;

    RabtInputMethod inputMethod;

    public RabtInputMethodDescriptor() {
        locales = new java.util.Hashtable<String, Locale>();
        localesProps = new java.util.Hashtable<String, Properties>();
        addLocales();
    }

    /**
     * should return all dynamic locales.
     * @see java.awt.im.spi.InputMethodDescriptor#getAvailableLocales
     */
    public Locale[] getAvailableLocales() {
        return (Locale[]) (locales.values().toArray(new Locale[0]));
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#hasDynamicLocaleList
     */
    public boolean hasDynamicLocaleList() {
        return true;
    }

    /**
     * should return descr name for a specific dynamic locale
     * @see java.awt.im.spi.InputMethodDescriptor#getInputMethodDisplayName
     */
    public synchronized String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage) {
        return "RabtPad Input Methods";
    }

    /**
     * @see java.awt.im.spi.InputMethodDescriptor#getInputMethodIcon
     * @param inputLocale
     * @return
     */
    public java.awt.Image getInputMethodIcon(Locale inputLocale) {
        return null;
    }

    /**
     * initialize with locales
     * @see java.awt.im.spi.InputMethodDescriptor#createInputMethod
     */
    public java.awt.im.spi.InputMethod createInputMethod() throws Exception {
        if (inputMethod == null) inputMethod = new RabtInputMethod(locales, localesProps);
        return inputMethod;
    }

    /**
     * @return string representation of this descriptor
     */
    public String toString() {
        Locale loc[] = getAvailableLocales();
        String locnames = null;
        for (int i = 0; i < loc.length; i++) {
            if (locnames == null) {
                locnames = loc[i].toString();
            } else {
                locnames += "," + loc[i];
            }
        }
        return getClass().getName() + "[" + "locales=" + locnames + ",localelist=" + (hasDynamicLocaleList() ? "dynamic" : "static") + "]";
    }

    /**
     * Called once to update locale information for Rabt Input Methods.
     * First from net/hussnain/io/im/default_input.rid the file is read to configure
     * locale and keyboard mapping information for Urdu.
     *
     * More configuration files for configuration with the extension *.rid are
     * read and processed from \".rabtPad/mappings\", which should exist in your
     * home folder.
     * For each file a new locale and keyboard mapping is added to the
     * Rabt Input Methods.
     */
    private void addLocales() {
        java.net.URL fileUrl = getClass().getResource(urduLocalePath);
        addLocale(fileUrl);
        String homeFolderPath = System.getProperty("user.home");
        File homeFolder = new File(homeFolderPath);
        File refRabtFolder = new File(homeFolder, programDirectory);
        if (refRabtFolder.exists()) {
            File refMapsFolder = new File(refRabtFolder, mappingDirectory);
            if (refMapsFolder.exists()) {
                net.hussnain.io.file.RabtFileFilter filter = new net.hussnain.io.file.RabtFileFilter("Rapt InputMaps");
                filter.addFileExtention("rid.xml", "rabt input method description");
                File[] mapDefs = refMapsFolder.listFiles(filter);
                for (int i = 0; i < mapDefs.length; i++) {
                    File currFile = mapDefs[i];
                    if (currFile.isFile()) try {
                        addLocale((mapDefs[i].toURI().toURL()));
                    } catch (java.net.MalformedURLException exp) {
                        logger.warning("error loading locale from file " + mapDefs[i].getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Reads mapping information for keyboard from a file and adds a new locale
     * to the Rabt Input Method.
     * @param p_file file for locale informations and keyboard mapping
     */
    public void addLocale(java.net.URL p_url) {
        logger.info("adding locale from file " + p_url.toString());
        Properties localeProp = new java.util.Properties();
        try {
            InputStream in = new BufferedInputStream(p_url.openStream());
            localeProp.loadFromXML(in);
        } catch (java.io.IOException exp) {
            logger.info(exp.getMessage());
            return;
        }
        String localeCode = localeProp.getProperty("LocaleLanguageCode");
        String localeCountry = localeProp.getProperty("LocaleCountry");
        String localeVariant = localeProp.getProperty("LocaleVariant");
        Locale newLocale = null;
        if ((localeCode != null) && (localeCountry != null) && (localeVariant != null)) {
            newLocale = new Locale(localeCode, localeCountry, localeVariant);
        } else {
            if ((localeCode != null) && (localeCountry != null)) {
                newLocale = new Locale(localeCode, localeCountry);
            } else {
                if ((localeCode != null)) {
                    newLocale = new Locale(localeCode);
                }
            }
        }
        locales.put(localeCode, newLocale);
        localesProps.put(localeCode, localeProp);
        logger.info("Locale added is  " + newLocale.toString());
    }
}
