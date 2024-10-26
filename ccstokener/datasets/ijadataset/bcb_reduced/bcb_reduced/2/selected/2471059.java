package org.eclipse.osgi.framework.adaptor.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This class provides helper methods to support developement classpaths.
 * @since 3.1
 */
public final class DevClassPathHelper {

    private static boolean inDevelopmentMode = false;

    private static String[] devDefaultClasspath;

    private static Dictionary devProperties = null;

    static {
        String osgiDev = System.getProperty("osgi.dev");
        if (osgiDev != null) {
            try {
                inDevelopmentMode = true;
                URL location = new URL(osgiDev);
                devProperties = load(location);
                if (devProperties != null) devDefaultClasspath = getArrayFromList((String) devProperties.get("*"));
            } catch (MalformedURLException e) {
                devDefaultClasspath = getArrayFromList(osgiDev);
            }
        }
    }

    private static String[] getDevClassPath(String id, Dictionary properties, String[] defaultClasspath) {
        String[] result = null;
        if (id != null && properties != null) {
            String entry = (String) properties.get(id);
            if (entry != null) result = getArrayFromList(entry);
        }
        if (result == null) result = defaultClasspath;
        return result;
    }

    /**
	 * Returns a list of classpath elements for the specified bundle symbolic name.
	 * @param id a bundle symbolic name to get the development classpath for
	 * @param properties a Dictionary of properties to use or <code>null</code> if
	 * the default develoment classpath properties should be used
	 * @return a list of development classpath elements
	 */
    public static String[] getDevClassPath(String id, Dictionary properties) {
        if (properties == null) return getDevClassPath(id, devProperties, devDefaultClasspath);
        return getDevClassPath(id, properties, getArrayFromList((String) properties.get("*")));
    }

    /**
	 * Returns a list of classpath elements for the specified bundle symbolic name.
	 * @param id a bundle symbolic name to get the development classpath for
	 * @return a list of development classpath elements
	 */
    public static String[] getDevClassPath(String id) {
        return getDevClassPath(id, null);
    }

    /**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
    public static String[] getArrayFromList(String prop) {
        if (prop == null || prop.trim().equals("")) return new String[0];
        Vector list = new Vector();
        StringTokenizer tokens = new StringTokenizer(prop, ",");
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            if (!token.equals("")) list.addElement(token);
        }
        return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
    }

    /**
	 * Indicates the development mode.
	 * @return true if in development mode; false otherwise
	 */
    public static boolean inDevelopmentMode() {
        return inDevelopmentMode;
    }

    private static Properties load(URL url) {
        Properties props = new Properties();
        try {
            InputStream is = null;
            try {
                is = url.openStream();
                props.load(is);
            } finally {
                if (is != null) is.close();
            }
        } catch (IOException e) {
        }
        return props;
    }
}
