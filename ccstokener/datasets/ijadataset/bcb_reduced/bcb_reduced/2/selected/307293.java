package org.opencdspowered.opencds.translatortool.lang;

import java.io.*;
import java.util.*;
import java.io.FileInputStream;

/**
 * The Language class is used to specify one language, in combination with a
 *  file.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class Language {

    private Locale m_Locale;

    private File m_File;

    private String m_StringFile;

    private String m_Code;

    private String m_Folder;

    private Properties m_Properties;

    private String m_Language;

    private String m_Country;

    /**
     * The Language constructor.
     *
     * @param   language    The language of the file.
     * @param   country     The country of the file.
     * @param   file        The file itself.
    */
    public Language(String language, String country, File file, String folder) {
        m_Language = language;
        m_Country = country;
        m_Locale = new Locale(language, country);
        m_File = file;
        m_Code = language + "_" + country;
        m_Folder = folder;
        m_Properties = new Properties();
        initKeys();
    }

    /**
     * The Language constructor.
     *
     * @param   language    The language of the file.
     * @param   country     The country of the file.
     * @param   file        The file itself.
    */
    public Language(String language, String country, String file) {
        m_Language = language;
        m_Country = country;
        m_Locale = new Locale(language, country);
        m_File = null;
        m_StringFile = file;
        m_Code = language + "_" + country;
        m_Properties = new Properties();
        m_Folder = null;
        initKeys();
    }

    /**
     * Initializes the keys found inside the file.
    */
    private void initKeys() {
        try {
            if (m_Folder != null) {
                m_Properties.load(new FileInputStream(m_Folder + "/messages_" + getLanguage() + "_" + getCountry() + ".properties"));
            } else {
                java.net.URL url = getClass().getResource(m_StringFile);
                m_Properties.load(url.openStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the language of this file.
     *
     * @return  A string with the language.
    */
    public String getLanguage() {
        return m_Language;
    }

    /**
     * Get the country of the file.
     *
     * @return  A string with the country.
    */
    public String getCountry() {
        return m_Country;
    }

    /**
     * Get the country/language combination code.
     *
     * @return  The code. Syntax: <i>en_US</i>
    */
    public String getCode() {
        return m_Code;
    }

    /**
     * Get the languages display name.
     *
     * @return  A string with the display name.
    */
    public String getDisplayLanguage() {
        return m_Locale.getDisplayLanguage();
    }

    /**
     * Get the locales displayname.
     *
     * @return  A string with the display name.
    */
    public String getDisplayName() {
        return m_Locale.getDisplayName();
    }

    /**
     * Get the country's display name
     *
     * @return  A string with the country's display name.
    */
    public String getDisplayCountry() {
        return m_Locale.getDisplayCountry();
    }

    /**
     * Get localised message.
     *
     * @param   key The key to use.
     * @return  A string with the localised message.
    */
    public String getLocalisedMessage(String key) {
        return m_Properties.getProperty(key);
    }

    /**
     * Get localised object.
     *
     * @param   key The key to use.
     * @return  A Object with the localised message.
    */
    public Object getLocalisedObject(String key) {
        return m_Properties.getProperty(key);
    }

    /**
     * Get all the entries.
     * 
     * @return  Iterator with all the entries.
    */
    public Set<Map.Entry<Object, Object>> getAllEntries() {
        return m_Properties.entrySet();
    }

    /**
     * Get the locale.
     * 
     * @return  The locale.
    */
    public Locale getLocale() {
        return m_Locale;
    }
}
