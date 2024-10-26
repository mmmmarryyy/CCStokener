package wsl.fw.util;

import java.util.Properties;
import java.util.Enumeration;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import wsl.fw.resource.ResId;

public class Config extends Properties {

    public static final ResId DEBUG_PROPERTIES = new ResId("Config.debug.Properties"), DEBUG_ADDING = new ResId("Config.debug.Edding"), ERR_RESOURCE1 = new ResId("Config.error.Resource1"), ERR_RESOURCE2 = new ResId("Config.error.Resource2"), ERR_ADDING = new ResId("Config.error.Adding"), WARNING_CONTEXT = new ResId("Config.warning.Context");

    public static final String RESOURCE_PREFIX = "resource://";

    public static final String DEFAULT_CONFIG_FILE = RESOURCE_PREFIX + "wsl/config/default.conf";

    private static Config s_singleton = null;

    /**
	 * Static accessor function to get the singleton Config object. If none
	 * exists then one is created using the default config file.
	 * If your application requires a specific conf file and does NOT want to
	 * load the default conf file then you must call setSingleton () before
	 * calling getSingleton () or any other Config functions.
	 * @return the Config singleton.
	 */
    public static synchronized Config getSingleton() {
        if (s_singleton == null) {
            s_singleton = new Config();
            s_singleton.init(true, true);
        }
        return s_singleton;
    }

    /**
	 * Static accessor to get a property from the singleton.
	 * @param key, the name of the property to get.
	 * @return a string containing the named property, or null if not found.
	 */
    public static String getProp(String key) {
        return getSingleton().getProperty(key);
    }

    /**
	 * Static accessor to get an int property from the singleton.
	 * @param key, the name of the property to get.
	 * @param defaultValue, the default value to use if the property does not
	 *   exist or cannot be parsed to an int.
	 * @return the integer value of the property or the default if not found.
	 */
    public static int getProp(String key, int defaultValue) {
        return getSingleton().getProperty(key, defaultValue);
    }

    /**
	 * Static function to set the singleton Config object to one that is loaded
	 * from the specified url rather than using the default conf file.
	 * Use this before any other Config calls if you DO NOT want to use the
	 * default conf at all. If you want to use the defualt conf and add
	 * additional conf files you can just get the default singleton and then
	 * call addConfig to add additional conf files.
	 *
	 * @param confFileUrl, a string defining the url of the desired conf file,
	 *   usually loaded as a classpath relative resource i.e.
	 *   (RESOURCE_PREFIX + wsl/config/appname.conf).
	 * @param useSystemProperties, if false the system properties are NOT
	 *   included and any config files specified in the system properties will
	 *   not be read.
	 */
    public static synchronized void setSingleton(String confFileUrl, boolean useSystemProperties) {
        s_singleton = new Config();
        s_singleton.init(useSystemProperties, false);
        s_singleton.addConfig(confFileUrl);
    }

    /**
	 * Static accessor to get a property from the singleton.
	 * @param key, the name of the property to get.
	 * @return a string containing the named property, or null if not found.
	 */
    public static String getProp(String key, String def) {
        return getSingleton().getProperty(key, def);
    }

    /**
	 * Default constructor, should call init () after construction to load the
	 * system properties, default config file, any config files specified in the
	 * system properties and all nested config files.
	 */
    protected Config() {
    }

    /**
	 * Init the config, optionally load the default config file and system
	 * properties any nested config files.
	 * @param useSystemProperties, if false the system properties are NOT
	 *   included and any config files specified in the system properties will
	 *   not be read.
	 * @param useDefaultConfigFile, if false the default config file will NOT
	 *   be read and any config files specified in the default config file
	 *   will not be read.
	 */
    public void init(boolean useSystemProperties, boolean useDefaultConfigFile) {
        if (useDefaultConfigFile) addConfig(DEFAULT_CONFIG_FILE);
        if (useSystemProperties) {
            Log.debug(DEBUG_PROPERTIES.getText());
            addConfig(System.getProperties());
        }
    }

    /**
	 * Get an int property from the config.
	 * @param key, the name of the property to get.
	 * @param defaultValue, the default value to use if the property does not
	 *   exist or cannot be parsed to an int.
	 * @return the integer value of the property or the defaultif not found.
	 */
    public int getProperty(String key, int defaultValue) {
        String sValue = getProperty(key);
        int iValue = defaultValue;
        if (!Util.isEmpty(sValue)) try {
            iValue = Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
        }
        return iValue;
    }

    /**
	 * Get the set of sub-key property names (using structured names) that begin
	 * with a certain prefix. if full is false only the sub-key part (excluding
	 * the prefix) will be returned. If leaf is true only top level sub-keys
	 * (i.e. those that have no further dots) will be returned. E.g. if the
	 * Config contains the following properties: wsl.one.aaa, wsl.one.aaa.zzz,
	 * wsl.two.aaa and wsl.two.aaa.zzz then getSubkeys ("wsl.one", false, false)
	 * would return aaa, aaa.zzz and getSubkeys ("wsl.one", true, true) would
	 * return wsl.one.aaa.
	 * @param keyPrefix, only subkeys that begin with this prefix (i.e. the
	 *   prefix and a dot) are returned. If null return all keys/leaf keys.
	 * @param leaf, if true only return leaf subkeys (those with no more dots).
	 * @param full, if true the full key (not just the subkey without the
	 *   prefix) is returned.
	 * @return an array of strings containing the full or partial subkeys.
	 *   May be empty.
	 */
    public String[] getSubkeys(String keyPrefix, boolean leaf, boolean full) {
        if (keyPrefix != null) keyPrefix += CKfw.DOT;
        ArrayList subkeys = new ArrayList();
        synchronized (this) {
            Enumeration nameEnum = propertyNames();
            while (nameEnum.hasMoreElements()) {
                String key = (String) nameEnum.nextElement();
                if (keyPrefix == null || key.startsWith(keyPrefix)) {
                    String part = key.substring(keyPrefix.length());
                    if (!leaf || (part.indexOf(CKfw.DOT) == -1)) subkeys.add((full) ? key : part);
                }
            }
        }
        return (String[]) subkeys.toArray(new String[0]);
    }

    /**
	 * Add configuration data from the supplied Properties and recursively add
	 * any nested configuration file entries.
	 * @param props, the properties to add.
	 */
    public void addConfig(Properties props) {
        Util.argCheckNull(props);
        Enumeration nameEnum = props.propertyNames();
        Enumeration lastEnum = props.propertyNames();
        synchronized (this) {
            while (nameEnum.hasMoreElements()) {
                String propName = (String) nameEnum.nextElement();
                String propValue = props.getProperty(propName);
                setProperty(propName, propValue);
                if (propName.startsWith(CKfw.CONFIG_NAME_PREFIX)) addConfig(propValue);
            }
            while (lastEnum.hasMoreElements()) {
                String propName = (String) lastEnum.nextElement();
                String propValue = props.getProperty(propName);
                if (propName.startsWith(CKfw.DOLAST_CONFIG_NAME_PREFIX)) addConfig(propValue, false);
            }
        }
        Log.init();
    }

    /**
	 * Add configuration data from a file specified by URL and recursively add
	 * any nested configuration file entries.
	 * @param configURL, the file as a URL. This may be a normal URL using
	 *   standard accepted protocols (e.g. http://server/dir/file,
	 *   file://c:/dir/file) or a resource which is loaded from file or jar
	 *   relative to the classpath (resource://subdir/file).
	 * @param logIfMissing, if true nested config files that cannot be found
	 *   will be logged.
	 */
    public void addConfig(String configURL, boolean logIfMissing) {
        Log.debug(DEBUG_ADDING.getText() + " " + configURL);
        try {
            URL url = null;
            if (configURL.startsWith(RESOURCE_PREFIX)) {
                String resourceName = configURL.substring(RESOURCE_PREFIX.length());
                url = getClass().getClassLoader().getResource(resourceName);
                if (url == null) {
                    if (logIfMissing) Log.error(ERR_RESOURCE1.getText() + " [" + resourceName + "] " + ERR_RESOURCE2.getText());
                    return;
                }
            } else url = new URL(configURL);
            InputStream is = url.openStream();
            Properties configProperties = new Properties();
            configProperties.load(is);
            is.close();
            addConfig(configProperties);
        } catch (Exception e) {
            Log.error(ERR_ADDING.getText() + " [" + configURL + "] : ", e);
        }
    }

    /**
	 * As above but defaulting to logIfMissing enabled
	 */
    public void addConfig(String configURL) {
        addConfig(configURL, true);
    }

    /**
	 * Add a set of properties defined by a context entry to this Config.
	 * @param context, the name of the context, may not be null or empty.
	 * @throws IllegalArgumentException if context is invalid.
	 */
    public void addContext(String context) {
        Util.argCheckEmpty(context);
        String contextUrl = getProperty(CKfw.CONTEXT_PREFIX + context);
        if (contextUrl != null) addConfig(contextUrl); else Log.warning("Config.addContext: contex [" + CKfw.CONTEXT_PREFIX + context + "] " + WARNING_CONTEXT.getText());
    }

    /**
	 * Add a set of properties defined by a context entry to this Config.
	 * @param args, command line arguments which are parsed for the -context
	 *   flag which is used to set the context. May not be null.
	 * @param defaultContext, the name of the default context to use if there is
	 *   no command line context, may be null or empty.
	 */
    public void addContext(String args[], String defaultContext) {
        String context = Util.getArg(args, "-context", defaultContext);
        if (context != null && context.length() > 0) addContext(context);
    }

    /**
	 * Get the help for the -context argument.
	 */
    public static String getHelp() {
        return "Optionally you may use the -context <context name> argument to\n" + "override the default context. Context names are defined in the\n" + "configuration files.";
    }
}
