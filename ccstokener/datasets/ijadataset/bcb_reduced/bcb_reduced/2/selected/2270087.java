package org.ws4d.osgi.skeletonGenerator.misc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Vector;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.ws4d.java.xml.QualifiedName;

public class Util {

    /**
	   * Install a bundle from a resource file.
	   *
	   * @param bc context owning both resources and to install bundle from
	   * @param resource resource name of bundle jar file
	   * @return the installed bundle
	   * @throws BundleException if no such resource is found or if
	   *                         installation fails.
	   */
    public static Bundle installBundle(BundleContext bc, String resource) throws BundleException {
        try {
            System.out.println("installBundle(" + resource + ")");
            URL url = bc.getBundle().getResource(resource);
            if (url == null) {
                throw new BundleException("No resource " + resource);
            }
            InputStream in = url.openStream();
            if (in == null) {
                throw new BundleException("No resource " + resource);
            }
            return bc.installBundle("internal:" + resource, in);
        } catch (IOException e) {
            throw new BundleException("Failed to get input stream for " + resource + ": " + e);
        }
    }

    /**
	 * Replaces an alphabet of forbidden chars in a given string with a specified char
	 * 
	 * @param string where to replace the chars
	 * @param forbidden the alphabet with forbidden chars
	 * @param replace the char which will replace the forbidden chars
	 * @return the updated string
	 */
    public static String replaceChars(String string, char[] forbidden, char replace) {
        for (int i = 0; i < forbidden.length; i++) {
            string = string.replace(forbidden[i], replace);
        }
        return string;
    }

    /**
	 * Removes an alphabet of forbidden chars in a given string
	 * 
	 * @param string where to remove the chars
	 * @param forbidden an alphabet of forbidden chars
	 * @return the updated string
	 */
    public static String removeChars(String string, char[] forbidden) {
        int a = 0;
        String part = null;
        for (int i = 0; i < forbidden.length; i++) {
            while (a > -1) {
                a = string.indexOf(forbidden[i]);
                if (a == 0) {
                    string = string.substring(1);
                } else if (a > 0) {
                    part = string.substring(0, a);
                    string = part + string.substring(a + 1);
                }
            }
            a = 0;
        }
        return string;
    }

    /**
	 * Concatenates two class arrays.
	 * 
	 * @param firstPart
	 * @param secondPart
	 * @return the concatenated array
	 */
    public static synchronized Class[] concatenateClassArrays(Class[] firstPart, Class[] secondPart) {
        if (firstPart == null && secondPart == null) return new Class[0]; else if (firstPart == null) return secondPart; else if (secondPart == null) return firstPart; else {
            int fpl = firstPart.length;
            int spl = secondPart.length;
            Class[] ret = new Class[fpl + spl];
            System.arraycopy(firstPart, 0, ret, 0, fpl);
            System.arraycopy(secondPart, 0, ret, fpl, spl);
            return ret;
        }
    }

    /**
	 * Concatenates two method arrays
	 * 
	 * @param firstPart
	 * @param secondPart
	 * @return the concatenated Array
	 */
    public static synchronized Method[] concatenateMethodArrays(Method[] firstPart, Method[] secondPart) {
        if (firstPart == null && secondPart == null) return new Method[0]; else if (firstPart == null) return secondPart; else if (secondPart == null) return firstPart; else {
            int fpl = firstPart.length;
            int spl = secondPart.length;
            Method[] ret = new Method[fpl + spl];
            System.arraycopy(firstPart, 0, ret, 0, fpl);
            System.arraycopy(secondPart, 0, ret, fpl, spl);
            return ret;
        }
    }

    /**
	 * Checks if a string contains a pattern. Algortithem of Knuth, Morris and Pratt.
	 *  
	 * @param string 
	 * @param pattern 
	 * @return
	 */
    public static boolean stringContainsString(String string, String pattern) {
        int d[] = new int[pattern.length()];
        int i = 0, j = 0, k = 0;
        d[0] = -1;
        while (j < pattern.length() - 1) {
            while ((k >= 0) && (pattern.charAt(j) != pattern.charAt(k))) {
                k = d[k];
            }
            j++;
            k++;
            if (pattern.charAt(j) == pattern.charAt(k)) {
                d[j] = d[k];
            } else {
                d[j] = k;
            }
        }
        i = 0;
        j = 0;
        k = 0;
        while ((j < pattern.length()) && (i < string.length())) {
            while (k <= i) {
                k++;
            }
            while ((j >= 0) && (string.charAt(i) != pattern.charAt(j))) {
                j = d[j];
            }
            i++;
            j++;
        }
        if (j == pattern.length()) {
            return true;
        } else {
            return false;
        }
    }

    public static Vector[] getVectorDiff(Vector oldVector, Vector newVector) {
        if (oldVector == null || oldVector.size() == 0) return new Vector[] { newVector, new Vector() };
        Vector addedElements = new Vector();
        Vector removedElements = new Vector();
        for (Iterator iterator = newVector.iterator(); iterator.hasNext(); ) {
            Object element = (Object) iterator.next();
            if (!oldVector.contains(element)) addedElements.add(element);
        }
        for (Iterator iterator = oldVector.iterator(); iterator.hasNext(); ) {
            Object element = (Object) iterator.next();
            if (!newVector.contains(element)) removedElements.add(element);
        }
        return new Vector[] { addedElements, removedElements };
    }

    public static String getBundleFromWhitelistEntry(String entry) {
        String bundle = entry.substring(0, entry.lastIndexOf('/'));
        return bundle;
    }

    public static String getServiceFromWhitelistEntry(String entry) {
        String service = entry.substring(entry.lastIndexOf('/') + 1);
        return service;
    }

    public static String showErrorlogstamp() {
        Calendar cal = Calendar.getInstance();
        int tmp = cal.get(Calendar.HOUR_OF_DAY);
        String h = (tmp < 10 ? "0" : "") + String.valueOf(tmp);
        tmp = cal.get(Calendar.MINUTE);
        String m = (tmp < 10 ? "0" : "") + String.valueOf(tmp);
        tmp = cal.get(Calendar.SECOND);
        String s = (tmp < 10 ? "0" : "") + String.valueOf(tmp);
        tmp = cal.get(Calendar.MILLISECOND);
        String ms = (tmp < 10 ? "00" : (tmp < 100 ? "0" : "")) + String.valueOf(tmp);
        return "[" + h + ":" + m + ":" + s + "." + ms + "]";
    }

    public static String getPortTypeWithoutConfig(QualifiedName qn) {
        String portType = qn.getLocalPart();
        if (portType.endsWith(")")) {
            portType = (String) portType.substring(0, portType.indexOf("("));
        }
        return portType;
    }

    public static String setLoggingClass(Object thisInstance) {
        return thisInstance.getClass().getName().substring(thisInstance.getClass().getName().lastIndexOf(".") + 1);
    }
}
