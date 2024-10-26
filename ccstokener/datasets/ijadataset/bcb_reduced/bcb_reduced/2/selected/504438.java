package mipt.crec.lab.gui;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import mipt.common.Const;
import mipt.common.Utils;

/**
 * By default loads from *.properties file (in "name=value" format) but the max-args constructor
 *  can load to *.xml (xml format also described in java.util.Properties).
 * Note: Does not treat \t characters as white space (may be rewritted by using BundleCoverter.trim()).
 * Note: It does not support locale variants (but support _en_US), properties files are assumed to be UTF-16.
 * TO DO?: determine the encoding of properties file by the last part of its name (locale).
 * There are not many imporvements from java.util.PropertyResourceBundle:
 *  a) encoding&format functionality in constructor;
 *  b) support of '\n' in String values (impossible in standard *.properties format): "\\n" is converted to '\n'.
 *  c) String[] values support by "[[" separator (extended *.properties format but not *.xml!).
 * Note: if String[] support is switched off, there could be some improvements (due to usage of
 *  Properties.load(Reader) method) but this this is possible as of Java 1.6 only 
 * @author Evdokimov
 */
public class PropertiesResourceBundle extends ResourceBundle {

    protected HashMap lookup;

    /**
	 * @param shortPath - '/'-separated file name without extension and locale;
	 *  *.properties is assumed, String[] value is possible and first trial is from file
	 */
    public PropertiesResourceBundle(String shortPath, Locale locale) {
        this(shortPath, locale, true, false, true);
    }

    /**
	 * @param shortPath - '/'-separated file name without extension and locale
	 * Note that supportArrayValues does not matter (assumed to be false) if fromXML=true (yet?) 
	 */
    public PropertiesResourceBundle(String shortPath, Locale locale, boolean supportArrayValues, boolean fromXML, boolean fromFileFirst) throws MissingResourceException {
        InputStream stream;
        String path, ext = fromXML ? ".xml" : ".properties";
        String p1 = shortPath + "_" + locale.getLanguage() + "_" + locale.getCountry();
        path = p1;
        do {
            path = p1 + ext;
            stream = getResourceAsStream(path, fromFileFirst);
            int j = p1.lastIndexOf('_');
            if (j < 0 || p1.length() == shortPath.length()) throwMissingResourceException(null, path); else p1 = p1.substring(0, j);
        } while (stream == null);
        if (fromXML) {
            Properties p = new Properties();
            try {
                p.loadFromXML(stream);
            } catch (IOException e) {
                throwMissingResourceException(e, path);
            }
            lookup = new HashMap(p);
        } else {
            Reader r = null;
            try {
                r = new InputStreamReader(stream, "UTF-16");
                if (supportArrayValues) throw new Throwable();
                Properties p = new Properties();
                p.getClass().getMethod("load", new Class[] { Reader.class }).invoke(p, r);
                lookup = new HashMap(p);
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) e = ((InvocationTargetException) e).getTargetException();
                if (e instanceof IOException || r == null) throwMissingResourceException(e, path);
                lookup = new HashMap();
                try {
                    load(lookup, r);
                } catch (IOException e1) {
                    throwMissingResourceException(e1, path);
                }
            }
        }
    }

    protected final void throwMissingResourceException(Throwable e, String path) throws MissingResourceException {
        throw new MissingResourceException("Can't find bundle " + path + (e == null ? "" : ": " + e.getMessage()), path, "");
    }

    /**
	 * Fastens loading as PropertyResourceBundle does (but with another loading technology).
	 * Tries getting from File even if fromFile==false but Utils.getResource() has failed.
	 * And rries getting as resource even if fromFile==true but FileInputStream has failed.
	 */
    public static InputStream getResourceAsStream(final String path, final boolean fromFileFirst) {
        return (InputStream) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                try {
                    if (fromFileFirst) try {
                        return new FileInputStream(Const.userDir + Const.fileSep + path);
                    } catch (IOException e) {
                        return Utils.getResourceAsStream(path, PropertiesResourceBundle.class);
                    } else {
                        URL url = Utils.getResource(path, PropertiesResourceBundle.class);
                        if (url != null) return url.openStream(); else return new FileInputStream(Const.userDir + Const.fileSep + path);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    /**
	 * Is equivalent of Properties.load(Reader) appered in Java 1.6 
	 */
    public void load(Map map, Reader r) throws IOException {
        BufferedReader br = new BufferedReader(r);
        try {
            while (br.ready()) {
                String line = br.readLine();
                if (line == null) break;
                processLine(map, line);
            }
        } finally {
            br.close();
        }
    }

    protected void processLine(Map map, String line) throws IOException {
        String comment = "#";
        if (line.startsWith(comment)) return;
        int j = line.indexOf('=');
        if (j >= 0) {
            String key = line.substring(0, j).trim();
            if (key.startsWith(comment)) return;
            line = line.substring(j + 1);
            if (!line.startsWith("[[")) map.put(key, correctValue(line)); else {
                String[] array = line.split("\\[\\["), a = new String[array.length - 1];
                for (int i = 1; i < array.length; i++) a[i - 1] = correctValue(array[i]);
                map.put(key, a);
            }
        } else if (line.trim().length() > 0) {
            throw new IOException("Properties format error in " + line);
        }
    }

    /**
	 * This implementation only converts "\\n" to '\n'.
	 */
    protected String correctValue(String value) {
        return value.replace("\\n", "\n");
    }

    /**
	 * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
	 */
    protected final Object handleGetObject(String key) {
        if (key == null) throw new NullPointerException("ResourceBundle key can't be null");
        return lookup.get(key);
    }

    /**
	 * @see java.util.ResourceBundle#getKeys()
	 */
    public final Enumeration<String> getKeys() {
        ResourceBundle parent = this.parent;
        return new ResourceBundleEnumeration(lookup.keySet(), (parent != null) ? parent.getKeys() : null);
    }

    /**
	 * This class is totally copied from java.util.ResourceBundleEnumeration
	 */
    class ResourceBundleEnumeration implements Enumeration<String> {

        Set<String> set;

        Iterator<String> iterator;

        Enumeration<String> enumeration;

        /**
	     * Constructs a resource bundle enumeration.
	     * @param set an set providing some elements of the enumeration
	     * @param enumeration an enumeration providing more elements of the enumeration.
	     *        enumeration may be null.
	     */
        ResourceBundleEnumeration(Set<String> set, Enumeration<String> enumeration) {
            this.set = set;
            this.iterator = set.iterator();
            this.enumeration = enumeration;
        }

        String next = null;

        public boolean hasMoreElements() {
            if (next == null) {
                if (iterator.hasNext()) {
                    next = iterator.next();
                } else if (enumeration != null) {
                    while (next == null && enumeration.hasMoreElements()) {
                        next = enumeration.nextElement();
                        if (set.contains(next)) {
                            next = null;
                        }
                    }
                }
            }
            return next != null;
        }

        public String nextElement() {
            if (hasMoreElements()) {
                String result = next;
                next = null;
                return result;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
