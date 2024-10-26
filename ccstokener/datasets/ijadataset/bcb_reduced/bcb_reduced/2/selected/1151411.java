package javax.help;

import java.net.URL;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.RuleBasedCollator;
import java.awt.Component;
import java.awt.IllegalComponentStateException;

/**
 * Provides a number of utility functions:
 *
 * Support for Beans, mapping from a Bean class to its HelpSet and to
 * its ID.
 * Support for finding localized resources.
 * Support for getting the default Query Engine
 *
 * This class has no public constructor.
 *
 * @author Eduardo Pelegri-Llopart
 * @author Roger D. Brinkley
 * @version	1.56	10/30/06
 */
public class HelpUtilities {

    /**
     * Given the class for a bean, get its HelpSet.
     * Returns the helpSetName property of the BeanDescriptor if defined.
     * Otherwise it returns a name as follows:
     * If the class is in the unnamed package, it returns <em>beanClassName</em>Help.hs.
     * Otherwise if it's in the form <em>package.ClassName</em> it returns
     * <em>package</em>/Help.hs after replacing "." with "/" in <em>package</em>.
     *
     * @param beanClass The Class
     * @return A String with the name of the HelpSet
     */
    public static String getHelpSetNameFromBean(Class beanClass) {
        String helpSetName;
        try {
            BeanInfo bi = Introspector.getBeanInfo(beanClass);
            helpSetName = (String) bi.getBeanDescriptor().getValue("helpSetName");
        } catch (Exception ex) {
            helpSetName = null;
        }
        if (helpSetName == null) {
            String className = beanClass.getName();
            int index = className.lastIndexOf(".");
            if (index == -1) {
                helpSetName = className + "Help.hs";
            } else {
                String packageName = className.substring(0, index);
                helpSetName = packageName.replace('.', '/') + "/Help.hs";
            }
        }
        return helpSetName;
    }

    /**
     * Given the class for a bean, get its ID string.
     * Returns the helpID property of the BeanDescriptor if defined,
     * otherwise it returns <em>beanName</em>.topID.
     *
     * @param beanClass the Class
     * @return A String with the ID to use
     */
    public static String getIDStringFromBean(Class beanClass) {
        String helpID;
        try {
            BeanInfo bi = Introspector.getBeanInfo(beanClass);
            helpID = (String) bi.getBeanDescriptor().getValue("helpID");
        } catch (Exception ex) {
            helpID = null;
        }
        if (helpID == null) {
            String className = beanClass.getName();
            helpID = className + ".topID";
        }
        return helpID;
    }

    /**
     * Default for the search engine
     */
    public static String getDefaultQueryEngine() {
        return "com.sun.java.help.search.DefaultSearchEngine";
    }

    /**
     * Locate a resource relative to a given classloader CL.
     * The name of the resource is composed by using FRONT,
     * adding _LANG _COUNTRY _VARIANT (with the usual rules)
     * and ending with BACK, which will usually be an extension
     * like ".hs" for a HelpSet, or ".class" for a class
     *
     * This method is a convenience method for getLocalizedResource() with
     * a tryRead parameter set to false.
     *
     * This functionality should likely be exposed as part of JDK1.2
     * @param cl The ClassLoader to get the resource from. If cl is null the default 
     * ClassLoader is used.
     * @returns the URL to the localized resource or null if not found.
     */
    public static URL getLocalizedResource(ClassLoader cl, String front, String back, Locale locale) {
        return getLocalizedResource(cl, front, back, locale, false);
    }

    /**
     * Locate a resource relative to a given classloader CL.
     * The name of the resource is composed by using FRONT,
     * adding _LANG _COUNTRY _VARIANT (with the usual rules)
     * and ending with BACK, which is usually an extension
     * like ".hs" for a HelpSet, or ".class" for a class
     *
     * This version accepts an explicit argument to work around some browser bugs.
     * 
     * This functionality should likely be exposed as part of JDK1.2
     * @param cl The ClassLoader to get the resource from. If cl is null the default
     * ClassLoader is used.
     * @returns the URL to the localized resource or null if not found.
     */
    public static URL getLocalizedResource(ClassLoader cl, String front, String back, Locale locale, boolean tryRead) {
        URL url;
        for (Enumeration tails = getCandidates(locale); tails.hasMoreElements(); ) {
            String tail = (String) tails.nextElement();
            String name = (new StringBuffer(front)).append(tail).append(back).toString();
            if (cl == null) {
                url = ClassLoader.getSystemResource(name);
            } else {
                url = cl.getResource(name);
            }
            if (url != null) {
                if (tryRead) {
                    try {
                        InputStream is = url.openConnection().getInputStream();
                        if (is != null) {
                            int i = is.read();
                            is.close();
                            if (i != -1) {
                                return url;
                            }
                        }
                    } catch (Throwable t) {
                    }
                } else {
                    return url;
                }
            }
        }
        return null;
    }

    private static Hashtable tailsPerLocales = new Hashtable();

    /**
     * This returns an enumeration of String tails. 
     * 
     * The core functionality on which getLocalizedResource is based.
     *
     * The suffixes are based on (1) the desired locale and (2) the default locale
     * in the following order from lower-level (more specific) to parent-level
     * (less specific):
     * <p>  "_" + language1 + "_" + country1 + "_" + variant1
     * <BR> "_" + language1 + "_" + country1
     * <BR> "_" + language1
     * <BR> ""
     * <BR> "_" + language2 + "_" + country2 + "_" + variant2
     * <BR> "_" + language2 + "_" + country2
     * <BR> "_" + language2
     *
     * The enumeration is of StringBuffer.
     * We pay some attention to efficiency in case a method like this is promoted,
     * hence we cache per locale.
     */
    public static synchronized Enumeration getCandidates(Locale locale) {
        Vector tails;
        LocalePair pair = new LocalePair(locale, Locale.getDefault());
        tails = (Vector) tailsPerLocales.get(pair);
        if (tails != null) {
            debug("getCandidates - cached copy");
            return tails.elements();
        }
        String lname1 = locale.toString();
        StringBuffer name1 = new StringBuffer("_").append(lname1);
        if (lname1 == null) {
            name1.setLength(0);
        }
        tails = new Vector();
        while (name1.length() != 0) {
            debug("  adding ", name1);
            String s = name1.toString();
            tails.addElement(s);
            int lastUnder = s.lastIndexOf('_');
            if (lastUnder != -1) {
                name1.setLength(lastUnder);
            }
        }
        debug("  addign -- null -- ");
        tails.addElement("");
        if (locale != Locale.getDefault()) {
            String lname2 = Locale.getDefault().toString();
            StringBuffer name2 = new StringBuffer("_").append(lname2);
            if (lname2 == null) {
                name2.setLength(0);
            }
            while (name2.length() != 0) {
                debug("  adding ", name2);
                String s = name2.toString();
                tails.addElement(s);
                int lastUnder = s.lastIndexOf('_');
                if (lastUnder != -1) {
                    name2.setLength(lastUnder);
                }
            }
        }
        tailsPerLocales.put(pair, tails);
        debug("tails is == ", tails);
        return tails.elements();
    }

    /**
     * Auxiliary class used in dealing with locale.
     */
    static class LocalePair {

        LocalePair(Locale locale1, Locale locale2) {
            this.locale1 = locale1;
            this.locale2 = locale2;
        }

        public int hashCode() {
            return locale1.hashCode() + locale2.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof LocalePair)) {
                return false;
            } else {
                LocalePair p = (LocalePair) obj;
                return locale1.equals(p.locale1) && locale2.equals(p.locale2);
            }
        }

        Locale locale1;

        Locale locale2;
    }

    /**
     * Get a localized Error String
     */
    private static Hashtable bundles;

    private static ResourceBundle lastBundle = null;

    private static Locale lastLocale = null;

    /**
     * Gets the locale of a component. If the component is null
     * it returns the defaultLocale. If the call to component.getLocale
     * returns an IllegalComponentStateException, the defaultLocale is
     * returned.
     */
    public static Locale getLocale(Component c) {
        if (c == null) {
            return Locale.getDefault();
        }
        try {
            return c.getLocale();
        } catch (IllegalComponentStateException ex) {
            return Locale.getDefault();
        }
    }

    private static synchronized ResourceBundle getBundle(Locale l) {
        if (lastLocale == l) {
            return lastBundle;
        }
        if (bundles == null) {
            bundles = new Hashtable();
        }
        ResourceBundle back = (ResourceBundle) bundles.get(l);
        if (back == null) {
            try {
                back = ResourceBundle.getBundle("javax.help.resources.Constants", l);
            } catch (MissingResourceException ex) {
                throw new Error("Fatal: Resource for javahelp is missing");
            }
            bundles.put(l, back);
        }
        lastBundle = back;
        lastLocale = l;
        return back;
    }

    /**
     * Get the Text message for the default locale.
     *
     * The getString version does not involve a format.
     */
    public static String getString(String key) {
        return getString(Locale.getDefault(), key);
    }

    public static String getText(String key) {
        return getText(Locale.getDefault(), key, null, null);
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     */
    public static String getText(String key, String s1) {
        return getText(Locale.getDefault(), key, s1, null);
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     * @param s2 The first parameter of a string. A null is valid for s2.
     */
    public static String getText(String key, String s1, String s2) {
        return getText(Locale.getDefault(), key, s1, s2);
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     * @param s2 The first parameter of a string. A null is valid for s2.
     * @param s3 The first parameter of a string. A null is valid for s3.
     */
    public static String getText(String key, String s1, String s2, String s3) {
        return getText(Locale.getDefault(), key, s1, s2, s3);
    }

    /**
     * Versions with an explicit locale.
*/
    public static String getString(Locale l, String key) {
        ResourceBundle bundle = getBundle(l);
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            throw new Error("Fatal: Localization data for JavaHelp is broken.  Missing " + key + " key.");
        }
    }

    public static String[] getStringArray(Locale l, String key) {
        ResourceBundle bundle = getBundle(l);
        try {
            return bundle.getStringArray(key);
        } catch (MissingResourceException ex) {
            throw new Error("Fatal: Localization data for JavaHelp is broken.  Missing " + key + " key.");
        }
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     * @param s2 The first parameter of a string. A null is valid for s2.
     * @param s3 The first parameter of a string. A null is valid for s3.
     */
    public static String getText(Locale l, String key) {
        return getText(l, key, null, null, null);
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     */
    public static String getText(Locale l, String key, String s1) {
        return getText(l, key, s1, null, null);
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     * @param s2 The first parameter of a string. A null is valid for s2.
     */
    public static String getText(Locale l, String key, String s1, String s2) {
        return getText(l, key, s1, s2, null);
    }

    /**
     * @param s1 The first parameter of a string. A null is valid for s1.
     * @param s2 The first parameter of a string. A null is valid for s2.
     * @param s3 The first parameter of a string. A null is valid for s3.
     */
    public static String getText(Locale l, String key, String s1, String s2, String s3) {
        ResourceBundle bundle = getBundle(l);
        if (s1 == null) {
            s1 = "null";
        }
        if (s2 == null) {
            s2 = "null";
        }
        if (s3 == null) {
            s3 = "null";
        }
        try {
            String fmt = bundle.getString(key);
            String[] args = { s1, s2, s3 };
            MessageFormat format = new MessageFormat(fmt);
            try {
                format.setLocale(l);
            } catch (NullPointerException ee) {
            }
            return format.format(args);
        } catch (MissingResourceException ex) {
            throw new Error("Fatal: Localization data for JavaHelp is broken.  Missing " + key + " key.");
        }
    }

    /**
     * Convenient method for creating a locale from a <tt>lang</tt> string.
     * Takes the <tt>lang</tt> string in the form of "language_country_variant"
     * or "language-country-variant" and 
     * parses the string and creates an appropriate locale.
     * @param lang A String representation of a locale, with the language, 
     * country and variant separated by underbars. Language is always lower
     * case, and country is always upper case. If the language is missing the
     * String begins with an underbar. If both language and country fields are
     * missing, a null Locale is returned. If lang is null a null Locale is 
     * returned
     * @returns The locale based on the string or null if the Locale could
     * not be constructed or lang was null.
     */
    public static Locale localeFromLang(String lang) {
        String language;
        String country;
        String variant = null;
        Locale newlocale = null;
        if (lang == null) {
            return newlocale;
        }
        int lpt = lang.indexOf("_");
        int lpt2 = lang.indexOf("-");
        if (lpt == -1 && lpt2 == -1) {
            language = lang;
            country = "";
            newlocale = new Locale(language, country);
        } else {
            if (lpt == -1 && lpt2 != -1) {
                lpt = lpt2;
            }
            language = lang.substring(0, lpt);
            int cpt = lang.indexOf("_", lpt + 1);
            int cpt2 = lang.indexOf("-", lpt + 1);
            if (cpt == -1 && cpt2 == -1) {
                country = lang.substring(lpt + 1);
                newlocale = new Locale(language, country);
            } else {
                if (cpt == -1 && cpt2 != -1) {
                    cpt = cpt2;
                }
                country = lang.substring(lpt + 1, cpt);
                variant = lang.substring(cpt + 1);
                newlocale = new Locale(language, country, variant);
            }
        }
        return newlocale;
    }

    /**
     * Returns information about whether a string is 
     * contained in another string. Compares the character data stored in two 
     * different strings based on the collation rules. 
     */
    public static boolean isStringInString(RuleBasedCollator rbc, String source, String target) {
        debug("isStringInString source=" + source + " targe =" + target);
        if (source == null || target == null) {
            return false;
        }
        if (source.length() == 0 && target.length() == 0) {
            return true;
        }
        int strengthResult = Collator.IDENTICAL;
        CollationElementIterator sourceCursor, targetCursor;
        boolean isFrenchSec = false;
        rbc.setDecomposition(Collator.FULL_DECOMPOSITION);
        String rules = rbc.getRules();
        if (rules.startsWith("@")) {
            isFrenchSec = true;
        }
        sourceCursor = rbc.getCollationElementIterator(source);
        targetCursor = rbc.getCollationElementIterator(target);
        int sOrder = 0, tOrder = 0;
        int pSOrder, pTOrder;
        short secSOrder, secTOrder;
        short terSOrder, terTOrder;
        boolean gets = true, gett = true;
        int toffset = 0;
        boolean initialCheckSecTer, checkSecTer, checkTertiary;
        startSearchForFirstMatch: while (true) {
            debug("while(true) toffset=" + toffset);
            try {
                sourceCursor.setOffset(0);
            } catch (NoSuchMethodError ex3) {
            }
            sOrder = sourceCursor.next();
            try {
                targetCursor.setOffset(toffset);
            } catch (NoSuchMethodError ex4) {
            } catch (Exception e) {
                return false;
            }
            tOrder = targetCursor.next();
            if (tOrder == CollationElementIterator.NULLORDER) {
                break;
            }
            debug("sOrder=" + sOrder + " tOrder=" + tOrder);
            while (tOrder != CollationElementIterator.NULLORDER) {
                if (sOrder == tOrder) {
                    try {
                        toffset = targetCursor.getOffset();
                    } catch (NoSuchMethodError ex) {
                    }
                    break;
                }
                pSOrder = CollationElementIterator.primaryOrder(sOrder);
                pTOrder = CollationElementIterator.primaryOrder(tOrder);
                if (pSOrder == pTOrder) {
                    try {
                        toffset = targetCursor.getOffset();
                    } catch (NoSuchMethodError ex2) {
                    }
                    break;
                }
                tOrder = targetCursor.next();
                debug("next tOrder=" + tOrder);
            }
            if (tOrder == CollationElementIterator.NULLORDER) {
                return false;
            }
            gets = false;
            gett = false;
            initialCheckSecTer = rbc.getStrength() >= Collator.SECONDARY;
            checkSecTer = initialCheckSecTer;
            checkTertiary = rbc.getStrength() >= Collator.TERTIARY;
            while (true) {
                if (gets) {
                    sOrder = sourceCursor.next();
                } else {
                    gets = true;
                }
                if (gett) {
                    tOrder = targetCursor.next();
                } else {
                    gett = true;
                }
                if ((sOrder == CollationElementIterator.NULLORDER) || (tOrder == CollationElementIterator.NULLORDER)) {
                    debug("One string at end");
                    break;
                }
                pSOrder = CollationElementIterator.primaryOrder(sOrder);
                pTOrder = CollationElementIterator.primaryOrder(tOrder);
                if (sOrder == tOrder) {
                    if (isFrenchSec && pSOrder != 0) {
                        if (!checkSecTer) {
                            checkSecTer = initialCheckSecTer;
                            checkTertiary = false;
                        }
                    }
                    debug("No diff at this positon continue");
                    continue;
                }
                if (pSOrder != pTOrder) {
                    if (sOrder == 0) {
                        gett = false;
                        continue;
                    }
                    if (tOrder == 0) {
                        gets = false;
                        continue;
                    }
                    if (pSOrder == 0) {
                        if (checkSecTer) {
                            targetCursor.next();
                            toffset = targetCursor.getOffset();
                            debug("Strength is secondary pSOrder === 0");
                            continue startSearchForFirstMatch;
                        }
                        gett = false;
                    } else if (pTOrder == 0) {
                        if (checkSecTer) {
                            targetCursor.next();
                            toffset = targetCursor.getOffset();
                            debug("Strength is secondary - pTOrder == 0");
                            continue startSearchForFirstMatch;
                        }
                        gets = false;
                    } else {
                        targetCursor.next();
                        toffset = targetCursor.getOffset();
                        debug("Order are ignorable");
                        continue startSearchForFirstMatch;
                    }
                } else {
                    if (checkSecTer) {
                        secSOrder = CollationElementIterator.secondaryOrder(sOrder);
                        secTOrder = CollationElementIterator.secondaryOrder(tOrder);
                        if (secSOrder != secTOrder) {
                            targetCursor.next();
                            toffset = targetCursor.getOffset();
                            debug("Secondary Difference");
                            continue startSearchForFirstMatch;
                        } else {
                            if (checkTertiary) {
                                terSOrder = CollationElementIterator.tertiaryOrder(sOrder);
                                terTOrder = CollationElementIterator.tertiaryOrder(tOrder);
                                if (terSOrder != terTOrder) {
                                    targetCursor.next();
                                    toffset = targetCursor.getOffset();
                                    debug("Tertiary difference");
                                    continue startSearchForFirstMatch;
                                }
                            }
                        }
                    }
                }
            }
            if (sOrder != CollationElementIterator.NULLORDER) {
                do {
                    if (CollationElementIterator.primaryOrder(sOrder) != 0) {
                        targetCursor.next();
                        toffset = targetCursor.getOffset();
                        debug("Additional non-ignborable base character in source string - source is greater");
                        continue startSearchForFirstMatch;
                    } else if (CollationElementIterator.secondaryOrder(sOrder) != 0) {
                        if (checkSecTer) {
                            targetCursor.next();
                            toffset = targetCursor.getOffset();
                            debug("Additional secondary elements source is greater");
                            continue startSearchForFirstMatch;
                        }
                    }
                } while ((sOrder = sourceCursor.next()) != CollationElementIterator.NULLORDER);
            }
            return true;
        }
        return false;
    }

    /**
     * Debug support
     */
    private static final boolean debug = false;

    private static void debug(Object msg1, Object msg2, Object msg3) {
        if (debug) {
            System.err.println("HelpUtilities: " + msg1 + msg2 + msg3);
        }
    }

    private static void debug(Object msg1) {
        debug(msg1, "", "");
    }

    private static void debug(Object msg1, Object msg2) {
        debug(msg1, msg2, "");
    }
}
