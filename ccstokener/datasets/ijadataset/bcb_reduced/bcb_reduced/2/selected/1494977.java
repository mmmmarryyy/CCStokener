package net.sf.vat4net.io;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 * Implements preferences as a singleton to be used throughout the system.
 * @author $Author: tom77 $
 * @version $Revision: 1.4 $
 * 
 * @see java.util.Properties
 */
public class Preferences {

    private static Preferences instance;

    private Properties prefs;

    public static final String PREFERENCES_FILE = "preferences.properties";

    static {
        try {
            instance = new Preferences(FileLoaderUtil.createURL(PREFERENCES_FILE));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not load preferences!\nGoing to close VAT4Net", "Could not load preferences!", JOptionPane.ERROR_MESSAGE);
            System.out.println((new File(PREFERENCES_FILE)).getAbsolutePath());
            System.exit(0);
        }
    }

    private Preferences(URL url) throws IOException {
        prefs = new Properties();
        if (url != null && prefs != null) {
            InputStream inStream = url.openStream();
            if (inStream == null) {
                throw new IOException("open stream from URL (" + url.toString() + ")");
            } else {
                prefs.load(inStream);
            }
        }
    }

    /**
     * Resets the preferences and initializes them from the properties file 
     * at the given URL.
     * Note: All current preferences are discarded. 
     * @param url the location of the properties file to load the preferences from
     * @throws IOException if the preferences could not be loaded
     */
    public static void reset(URL url) throws IOException {
        instance = new Preferences(url);
    }

    /**
     * Updates preferences according to parameters passed to the applet.
     * New values from the parameters update (override) existing ones.
     * @param applet
     * @return returns the number of successfully updated preference values
     */
    public static int update(Applet applet) {
        int updateCount = 0;
        ArrayList paramsToAdd = new ArrayList();
        Enumeration pNames = instance.prefs.propertyNames();
        while (pNames.hasMoreElements()) {
            String pName = (String) pNames.nextElement();
            String param = applet.getParameter(pName);
            if (param != null && param.length() > 0) {
                paramsToAdd.add(pName);
            }
        }
        for (Iterator it = paramsToAdd.iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            String value = applet.getParameter(name);
            put(name, value);
            System.out.println("update: " + name + "=" + value);
            updateCount++;
        }
        return updateCount;
    }

    private static String get(String key) {
        String s = instance.prefs.getProperty(key);
        if (s == null) {
            System.err.println("no value found for key " + key);
        }
        return s;
    }

    private static String put(String key, String value) {
        return (String) instance.prefs.setProperty(key, value);
    }

    private static String createKey(Object base, String key) {
        return base.getClass().getName() + "." + key;
    }

    public static boolean exists(String key) {
        return get(key) != null;
    }

    public static boolean exists(Object base, String key) {
        return exists(createKey(base, key));
    }

    public static boolean getBoolean(String key, boolean def) {
        String tmp = get(key);
        return (tmp != null) ? Boolean.valueOf(tmp).booleanValue() : def;
    }

    public static boolean getBoolean(Object base, String key, boolean def) {
        return getBoolean(createKey(base, key), def);
    }

    public static Object putBoolean(String key, boolean value) {
        return new Boolean(put(key, Boolean.toString(value)));
    }

    public static Object putBoolean(Object base, String key, boolean value) {
        return putBoolean(createKey(base, key), value);
    }

    public static String getString(String key, String def) {
        String tmp = get(key);
        return (tmp != null) ? tmp : def;
    }

    public static String getString(Object base, String key, String def) {
        return getString(createKey(base, key), def);
    }

    public static Object putString(String key, String value) {
        return put(key, value);
    }

    public static Object putString(Object base, String key, String value) {
        return putString(createKey(base, key), value);
    }

    public static int getInt(String key, int def) {
        String tmp = get(key);
        return (tmp != null) ? Integer.parseInt(tmp) : def;
    }

    public static int getInt(Object base, String key, int def) {
        return getInt(createKey(base, key), def);
    }

    public static Object putInt(String key, int value) {
        return new Integer(put(key, Integer.toString(value)));
    }

    public static Object putInt(Object base, String key, int value) {
        return putInt(createKey(base, key), value);
    }

    public static double getDouble(String key, double def) {
        String tmp = get(key);
        return (tmp != null) ? Double.parseDouble(tmp) : def;
    }

    public static double getDouble(Object base, String key, double def) {
        return getDouble(createKey(base, key), def);
    }

    public static Object putDouble(String key, double value) {
        return new Double(put(key, Double.toString(value)));
    }

    public static Object putDouble(Object base, String key, double value) {
        return putDouble(createKey(base, key), value);
    }
}
