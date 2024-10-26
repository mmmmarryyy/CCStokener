package gnujatella.utils;

import java.util.*;
import java.io.*;
import java.net.*;

public class CfgRes {

    private static final String appname = "gnujatella";

    private static CfgRes instance = null;

    private Properties properties = new Properties();

    private Properties defaultProperties = new Properties();

    private String configfile;

    private CfgRes() {
        String sep = System.getProperty("file.separator");
        String home = System.getProperty("user.home");
        defaultProperties = getDefaultProperties();
        properties = defaultProperties;
        String name = properties.getProperty("application.shortname", "an_application");
        configfile = home + sep + name + ".conf";
        Logger.defaultLog("CfgRes", Logger.LOG_INFO, "using config file: '" + configfile + "'");
        try {
            getPropertiesFromURL(properties, new URL("file:/" + configfile));
        } catch (MalformedURLException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "tried to open 'file:/" + configfile + "',but failed....");
        }
    }

    /**
         * Returns the current instance of the CfgRes.
         */
    public static CfgRes getInstance() {
        if (instance == null) instance = new CfgRes();
        return instance;
    }

    /**
         * Gets the string from a default property.
         */
    public String getDefaultString(String key) {
        return defaultProperties.getProperty(key);
    }

    /**
         * Gets the string-tokens from a default property.
         */
    public Vector getDefaultStringTokens(String key) {
        StringTokenizer st = new StringTokenizer(defaultProperties.getProperty(key));
        Vector tokvec = new Vector(st.countTokens());
        while (st.hasMoreTokens()) tokvec.add(st.nextToken());
        return tokvec;
    }

    /**
         * Gets the integer from a default property.
         */
    public int getDefaultInt(String key) {
        String value = defaultProperties.getProperty(key);
        return Integer.valueOf(value).intValue();
    }

    /**
         * Gets the string from an active property.
         */
    public String getString(String key) {
        return properties.getProperty(key);
    }

    /**
         * Gets the string-tokens from an active property.
         */
    public Vector getStringTokens(String key) {
        StringTokenizer st = new StringTokenizer(properties.getProperty(key));
        Vector tokvec = new Vector(st.countTokens());
        while (st.hasMoreTokens()) tokvec.add(st.nextToken());
        return tokvec;
    }

    /**
         * Gets the integer from an active property.
         */
    public int getInt(String key) {
        String value = properties.getProperty(key);
        return Integer.valueOf(value).intValue();
    }

    /**
         * Sets the value of an active property.
         */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
         * Sets the value of an active property.
         */
    public void setProperty(String key, int value) {
        properties.put(key, String.valueOf(value));
    }

    /**
         * Loads the default properties and returns the
         * properties object.
         */
    public Properties getDefaultProperties() {
        return createPropertiesFromURL(ResUtil.getURL("resources/default.conf"));
    }

    /**
         * Loads the properties from an url and returns the
         * properties object.
         */
    public Properties createPropertiesFromURL(URL url) {
        Properties prop = new Properties();
        getPropertiesFromURL(prop, url);
        return prop;
    }

    /**
         * Loads properties from a file and stores them in the Properties object.
         */
    public void getPropertiesFromURL(Properties prop, URL url) {
        InputStream in;
        try {
            in = url.openStream();
        } catch (IOException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "Could not open properties file '" + url + "'!");
            return;
        }
        try {
            prop.load(in);
        } catch (IOException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "Could not read from properties file '" + url + "'!");
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "Could not close properties file '" + url + "'!");
            return;
        }
    }

    public Properties diffCurDef() {
        return diffProperties(getDefaultProperties(), properties);
    }

    public Properties diffProperties(Properties orig, Properties diff) {
        Properties diffProp = new Properties();
        Iterator iterator = diff.keySet().iterator();
        String property;
        while (iterator.hasNext()) {
            property = (String) (iterator.next());
            if (!orig.getProperty(property).equals(diff.getProperty(property))) diffProp.put(property, diff.getProperty(property));
        }
        return diffProp;
    }

    public void save() {
        savePropertiesToFile(diffCurDef(), configfile);
    }

    public void savePropertiesToFile(Properties prop, String file) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "Could not open '" + file + "' for writing!");
            return;
        }
        try {
            prop.store(out, "[------------]");
        } catch (IOException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "Could not write to file '" + file + "'!");
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
            Logger.defaultLog("CfgRes", Logger.LOG_ERROR, "Couldn't  close file '" + file + "'!");
            return;
        }
    }
}
