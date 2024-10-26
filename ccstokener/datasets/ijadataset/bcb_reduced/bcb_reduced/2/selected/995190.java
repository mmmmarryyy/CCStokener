package com.wigball.ledgeredger.util;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * Support for internationalisation
 * 
 * @author $Author: lazydays $
 * @version $Revision: 14 $
 * 
 * $Date: 2007-03-11 09:59:14 -0400 (Sun, 11 Mar 2007) $
 */
public class I18N {

    /**
	 * 
	 */
    public static final Locale DEFAULT_LOCALE = Locale.GERMAN;

    /**
	 * 
	 */
    private static final Properties PROPS;

    static {
        PROPS = new Properties();
    }

    /**
	 * 
	 * @param lng The locale to load properties for
	 */
    public static void init(Locale lng) {
        try {
            Locale toLoad = lng != null ? lng : DEFAULT_LOCALE;
            URL url = ClassLoader.getSystemResource("locales/" + toLoad.getISO3Language() + ".properties");
            if (url == null) {
                url = ClassLoader.getSystemResource("locales/" + DEFAULT_LOCALE.getISO3Language() + ".properties");
            }
            PROPS.clear();
            PROPS.load(url.openStream());
        } catch (IOException ioe) {
            try {
                URL url = ClassLoader.getSystemResource("locales/" + DEFAULT_LOCALE.getISO3Language() + ".properties");
                PROPS.clear();
                PROPS.load(url.openStream());
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(99);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(99);
        }
    }

    /**
	 * 
	 * @param key
	 * @return
	 */
    public static String getString(String key) {
        String result = PROPS.getProperty(key);
        return result == null ? key + ": N/A" : result;
    }

    /**
	 * 
	 * @param key
	 * @param param
	 * @return
	 */
    public static String getString(String key, Object[] param) {
        String result = PROPS.getProperty(key);
        if (result == null || param == null) {
            return key + ": N/A";
        }
        String[] splitted = result.split("\\{[0-9]\\}");
        StringBuffer resultString = new StringBuffer();
        for (int i = 0; i < splitted.length; i++) {
            resultString.append(splitted[i]);
            if (i < param.length) {
                resultString.append(param[i].toString());
            }
        }
        return resultString.toString();
    }
}
