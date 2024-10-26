package com.duroty.config;

import java.net.URL;
import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.1 $
 */
public class Configuration {

    /**
     * DOCUMENT ME!
     */
    public static final String COOKIE_LANGUAGE = "COOKIE_LANGUAGE";

    /**
     * DOCUMENT ME!
     */
    public static final String COOKIE_MAX_AGE = "COOKIE_MAX_AGE";

    /**
     * DOCUMENT ME!
     */
    public static final String COOKIES_TIME_TO_LIVE_REMEMBER_MY_PASSWORD = "COOKIES_TIME_TO_LIVE_REMEMBER_MY_PASSWORD";

    /**
     * DOCUMENT ME!
     */
    public static final String AVAILABLE_LANGUAGE = "AVAILABLE_LANGUAGE";

    /**
     * DOCUMENT ME!
     */
    public static final String AUTO_LOCALE = "AUTO_LOCALE";

    /**
     * DOCUMENT ME!
     */
    public static final String DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE";

    /**
     * DOCUMENT ME!
     */
    public static final String JNDI_INITIAL_CONTEXT_FACTORY = "JNDI_INITIAL_CONTEXT_FACTORY";

    /**
     * DOCUMENT ME!
     */
    public static final String JNDI_URL_PKG_PREFIXES = "JNDI_URL_PKG_PREFIXES";

    /**
     * DOCUMENT ME!
     */
    public static final String JNDI_PROVIDER_URL = "JNDI_PROVIDER_URL";

    /**
     * DOCUMENT ME!
     */
    public static final String SECURITY_PROTOCOL = "SECURITY_PROTOCOL";

    /**
     * DOCUMENT ME!
     */
    public static final String INDEX_URI = "INDEX_URI";

    /**
     * DOCUMENT ME!
     */
    public static final String LOCAL_WEB_SERVER = "LOCAL_WEB_SERVER";

    /**
     * DOCUMENT ME!
     */
    public static final String MAX_FILE_UPLOAD = "MAX_FILE_UPLOAD";

    /**
     * DOCUMENT ME!
     */
    public static Properties properties = new Properties();

    /**
     * Creates a new Configuration object.
     */
    private Configuration() {
    }

    /**
     * DOCUMENT ME!
     */
    public static void init() {
        try {
            URL url = Configuration.class.getResource("/constants.properties");
            properties.load(url.openConnection().getInputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
