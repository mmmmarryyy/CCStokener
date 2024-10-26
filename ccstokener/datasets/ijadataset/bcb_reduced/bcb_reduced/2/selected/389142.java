package de.sooja.server.web.tomcat;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Class copied from org.eclipse.osgi.framework.internal.defaultadaptor
 */
public class DevClassPathHelper {

    protected static boolean inDevelopmentMode = false;

    protected static String[] devDefaultClasspath;

    protected static Properties devProperties = null;

    static {
        String osgiDev = System.getProperty("osgi.dev");
        if (osgiDev != null) {
            try {
                inDevelopmentMode = true;
                URL location = new URL(osgiDev);
                devProperties = load(location);
                if (devProperties != null) devDefaultClasspath = getArrayFromList(devProperties.getProperty("*"));
            } catch (MalformedURLException e) {
                devDefaultClasspath = getArrayFromList(osgiDev);
            }
        }
    }

    public static String[] getDevClassPath(String id) {
        String[] result = null;
        if (id != null && devProperties != null) {
            String entry = devProperties.getProperty(id);
            if (entry != null) result = getArrayFromList(entry);
        }
        if (result == null) result = devDefaultClasspath;
        return result;
    }

    /**
   * Returns the result of converting a list of comma-separated tokens into an
   * array
   * 
   * @return the array of string tokens
   * @param prop
   *            the initial comma-separated string
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
                is.close();
            }
        } catch (IOException e) {
        }
        return props;
    }
}
