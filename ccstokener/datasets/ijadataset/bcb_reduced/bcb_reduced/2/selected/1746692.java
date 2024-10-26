package com.wigball.mp3libexporter.util;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * hosts the language strings
 * 
 * @author $Author: lazydays $
 * @version $Revision: 4 $
 * 
 * $Date: 2007-03-01 16:42:14 -0500 (Thu, 01 Mar 2007) $
 *
 */
public class I18N {

    /**
	 * 
	 */
    private static final Properties PROPS;

    static {
        PROPS = new Properties();
    }

    /**
	 * 
	 * @param language
	 */
    public static void init(Locale language) throws IOException {
        URL url = ClassLoader.getSystemResource("locales/" + language.getISO3Language() + ".properties");
        if (url == null) {
            throw new IOException("Could not load resource locales/" + language.getISO3Language() + ".properties");
        }
        PROPS.clear();
        PROPS.load(url.openStream());
    }

    /**
	 * 
	 * @param key
	 * @return
	 */
    public static String getString(String key) {
        String result = PROPS.getProperty(key);
        if (result == null) {
            result = "N/A";
            System.out.println("There is no locale string for key " + key);
        }
        return result;
    }
}
