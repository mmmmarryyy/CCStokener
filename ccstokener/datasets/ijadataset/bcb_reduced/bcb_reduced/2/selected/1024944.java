package net.sf.ajio.ihm.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * AJIO
 *
 * @author Olivier CHABROL olivierchabrol@users.sourceforge.net
 * @copyright (C)2004 Olivier CHABROL
 * AJIO
 */
public class Language {

    private static Language _instance = null;

    private String _language = null;

    private static Logger _log = Logger.getLogger(Language.class.getName());

    private Properties _properties = null;

    private Language(String language) {
        _language = language;
        loadLanguage();
    }

    public static Language getInstance(String language) {
        if (_instance == null) _instance = new Language(language);
        return _instance;
    }

    public static Language getInstance() {
        if (_instance == null) _instance = new Language("fr_FR");
        return _instance;
    }

    private void loadLanguage() {
        URL url = getClass().getClassLoader().getResource("language/" + _language + ".properties");
        _properties = new Properties();
        _log.debug("Url : " + url);
        try {
            _properties.load(url.openStream());
        } catch (IOException e) {
            _log.error("Impossible de charger " + _language + ".properties");
        }
    }

    public String getLabel(String keyLanguage) {
        return _properties.getProperty(keyLanguage);
    }
}
