package org.servingMathematics.mqat.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.*;
import org.servingMathematics.mqat.MqatMain;
import org.servingMathematics.mqat.exceptions.ExtendedException;
import org.servingMathematics.mqat.exceptions.ProgramLogicException;

/**
 * Class for managing localization and internationalization
 *
 * @author Jakub Kahovec
 * @version 0.1
 */
public class ResourceManager {

    /** The path to xml files */
    public static final String RES_XML = "resources/xml/";

    /** The path to icons */
    public static final String RES_ICONS = "resources/icons/";

    /** The path to localizatin messages */
    public static final String RES_MESSAGES = "org/servingMathematics/mqat/resources/messages/";

    private static Locale currentLocale;

    private static ResourceBundle messages;

    static {
        setLocale("", "");
    }

    private ResourceManager() {
    }

    /**
     * Sets the locale for resource manager
     * 
     * @param language
     * @param country
     */
    public static void setLocale(String language, String country) {
        currentLocale = new Locale(language, country);
        try {
            messages = PropertyResourceBundle.getBundle(RES_MESSAGES + "messages", currentLocale, MqatMain.class.getClassLoader());
        } catch (MissingResourceException ex) {
            ExceptionHandler.handle(new ProgramLogicException("Message boundle for specified locale " + language + "_" + country + " hasn't been found, is used default messsage boundle", ex, ExtendedException.ERROR));
            messages = PropertyResourceBundle.getBundle(RES_MESSAGES + "/messages");
        }
    }

    /**
     * Returns localized string for a specified key
     *
     * @param key  Key of string to locate.
     *
     * @return  Localized text of key if key's been found, otherwise key
     */
    public static String getString(String key) {
        try {
            return messages.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    /**
     * Returns localized and parametrized string for a specified key
     *
     * @param key  Key of string to locate.
     * @param params Parametres witch are replaces in origin string
     *
     * @return  Localized text of key if key's been found, otherwise the key
     */
    public static String getParamString(String key, String[] params) {
        String localizedString = "";
        try {
            localizedString = messages.getString(key);
            for (int i = 0; i < params.length; i++) {
                localizedString = localizedString.replaceFirst("[{]param_" + (i + 1) + "[}]", params[i]);
            }
        } catch (MissingResourceException ex) {
            return key;
        }
        return localizedString;
    }

    /**
     * Returns the localized mnemonic key for a specified key
     *
     * @param key  Key of string to locate.
     *
     * @return    Localized mnemonic of key if key's been found, otherwise empty
     * string
     */
    public static char getMnemonic(String key) {
        String localizedMnemonic = "\r";
        try {
            localizedMnemonic = messages.getString(key + "_SC");
            StringTokenizer st = new StringTokenizer(localizedMnemonic, ",");
            if (st.hasMoreTokens()) {
                localizedMnemonic = st.nextToken().trim();
            }
        } catch (MissingResourceException ex) {
        }
        return localizedMnemonic.charAt(0);
    }

    /**
     * Returns the localized mnemonic key for a specified key
     *
     * @param key  Key of string to locate.
     *
     * @return  Localized mnemonic of key if key's been found, otherwise 0
     */
    public static int getMnemonicKey(String key) {
        return Character.toUpperCase(getMnemonic(key));
    }

    /**
     * Returns the localized accelerator key for a specified key
     *
     * @param key  Key of string to locate.
     *
     * @return  Localized acclerator of key if key's been found, otherwise null.
     */
    public static KeyStroke getAccelerator(String key) {
        String localizedAccelerator = null;
        try {
            localizedAccelerator = messages.getString(key + "_SC");
            StringTokenizer st = new StringTokenizer(localizedAccelerator, ",");
            if (st.hasMoreTokens()) {
                st.nextToken();
                if (st.hasMoreTokens()) {
                    localizedAccelerator = st.nextToken().trim();
                    String typedKey = localizedAccelerator.substring(localizedAccelerator.length() - 1);
                    localizedAccelerator = localizedAccelerator.substring(0, localizedAccelerator.length() - 1) + "pressed " + typedKey.toUpperCase();
                }
            }
        } catch (Exception ex) {
        }
        return KeyStroke.getKeyStroke(localizedAccelerator);
    }

    /**
     * Returns the localized tooltip for a specified key
     *
     * @param key  Key of string to locate.
     *
     * @return  Localized acclerator of key if key's been found, otherwise null.
     */
    public static String getToolTip(String key) {
        try {
            return messages.getString(key + "_TT");
        } catch (MissingResourceException ex) {
            return null;
        }
    }

    /**
     * Localizes specified abstract button based item
     *
     * @param key String
     * @param item AbstractButton
     */
    public static void localizeItem(String key, AbstractButton item) {
        item.setText(getString(key));
        item.setMnemonic(getMnemonic(key));
        item.setToolTipText(getToolTip(key));
        item.setIcon(getIcon(key));
    }

    /**
     * Localizes specified abstract button based item
     *
     * @param key String
     * @param item JLabel
     */
    public static void localizeItem(String key, JLabel item) {
        item.setText(getString(key));
        item.setDisplayedMnemonic(getMnemonic(key));
        item.setToolTipText(getToolTip(key));
        item.setIcon(getIcon(key));
    }

    /**
     * Localizes specified abstract button based item
     *
     * @param key String
     * @param  item JMenuItem
     */
    public static void localizeMenuItem(String key, JMenuItem item) {
        item.setText(getString(key));
        item.setMnemonic(getMnemonic(key));
        item.setAccelerator(getAccelerator(key));
        item.setToolTipText(getToolTip(key));
    }

    /**
     * Returns an icon from resources
     *
     * @param key 
     * @return Icon
     */
    public static Icon getIcon(String key) {
        String iconName;
        try {
            iconName = messages.getString(key + "_IC");
        } catch (MissingResourceException ex) {
            return null;
        }
        return getFileAsImageIcon(iconName, RES_ICONS);
    }

    /**
     * Returns the file from resources as a stream
     *
     * @param fileName String
     * @param resourcePath String
     * @return InputStream
     */
    public static InputStream getFileAsStream(String fileName, String resourcePath) {
        String correctResPath = resourcePath;
        if (resourcePath.length() > 0 && resourcePath.lastIndexOf('/') == -1) {
            correctResPath += "/";
        }
        return MqatMain.class.getResourceAsStream(correctResPath + fileName);
    }

    /**
     * Returns the file from resources as an image icon.
     *
     * @param fileName String
     * @param resourcePath String
     * @return InputStream
     */
    public static Icon getFileAsImageIcon(String fileName, String resourcePath) {
        BufferedInputStream iconIn;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        int i;
        iconIn = new BufferedInputStream(MqatMain.class.getResourceAsStream(resourcePath + fileName));
        try {
            while ((i = iconIn.read()) > -1) {
                byteOut.write(i);
            }
            return new ImageIcon(byteOut.toByteArray());
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Returns the URI of the specified resource path
     * 
     * @param fileName 
     * @param resourcePath String
     * @return URI
     */
    public static String getFileURI(String fileName, String resourcePath) {
        try {
            return new URI(MqatMain.class.getResource(resourcePath + "/" + fileName).toString()).toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Returns the array of uri from a resources specified by 
     * <code>resourceName</code> which are filter by the <code>regEx<code>
     * 
     * @param resourceName
     * @param regExFilter
     * @param firstNoEmptyMatched
     * @return the list of uri from the specified resource name.
     */
    public static String[] getURLListFromResource(String resourceName, String regExFilter, boolean firstNoEmptyMatched) {
        String[] urlArray;
        Vector<String> urlVector = new Vector<String>();
        try {
            ClassLoader classLoader = MqatMain.class.getClassLoader();
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            Enumeration e = urlClassLoader.findResources(resourceName);
            for (; e.hasMoreElements(); ) {
                URL url = (URL) e.nextElement();
                if ("file".equals(url.getProtocol())) {
                    File file = new File(url.getPath());
                    File[] fileList = file.listFiles();
                    if (fileList != null) {
                        for (int i = 0; i < fileList.length; i++) {
                            String urlStr = fileList[i].toURL().toString();
                            if (urlStr.matches(regExFilter)) {
                                urlVector.add(urlStr);
                            }
                        }
                    }
                } else if ("jar".equals(url.getProtocol())) {
                    JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarConnection.getJarFile();
                    Enumeration jarEntries = jarFile.entries();
                    for (; jarEntries.hasMoreElements(); ) {
                        JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
                        if (!jarEntry.isDirectory()) {
                            String urlStr = url.toString().substring(0, url.toString().lastIndexOf('!') + 1);
                            urlStr += "/" + jarEntry;
                            if (urlStr.matches(regExFilter)) {
                                urlVector.add(urlStr);
                            }
                        }
                    }
                }
                if (!urlVector.isEmpty() && firstNoEmptyMatched) {
                    break;
                }
            }
        } catch (Exception ex) {
            ExceptionHandler.handle(ex, ExceptionHandler.NO_VISUAL);
        }
        urlArray = urlVector.toArray(new String[urlVector.size()]);
        return urlArray;
    }

    /**
     * TODO Desctription
     * 
     * @param dirURL
     * @param regExFilter
     * @return Vector
     */
    public static Vector getFileEntriesInDir(URL dirURL, String regExFilter) {
        Vector<URL> urlVector = new Vector<URL>();
        File file = new File(dirURL.getPath());
        File[] fileList = file.listFiles();
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                String urlStr;
                try {
                    urlStr = fileList[i].toURL().toString();
                    if (fileList[i].isDirectory()) {
                        Vector files = getFileEntriesInDir(new URL(urlStr), regExFilter);
                        for (int j = 0; j < files.size(); j++) {
                            urlVector.add(((File) files.get(j)).toURL());
                        }
                    } else if (urlStr.matches(regExFilter)) {
                        urlVector.add(fileList[i].toURL());
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        return urlVector;
    }

    /**
     * TODO Description
     * 
     * @param jarURL
     * @param regExFilter
     * @return Vector
     */
    public static Vector<URL> getJarEntriesInJar(URL jarURL, String regExFilter) {
        Vector<URL> urlVector = new Vector<URL>();
        try {
            JarURLConnection jarConnection = (JarURLConnection) jarURL.openConnection();
            JarFile jarFile;
            jarFile = jarConnection.getJarFile();
            Enumeration e = jarFile.entries();
            for (; e.hasMoreElements(); ) {
                JarEntry jarEntry = (JarEntry) e.nextElement();
                String urlStr = jarURL.toString().substring(0, jarURL.toString().lastIndexOf('!') + 1);
                urlStr += "/" + jarEntry;
                if (urlStr.matches(regExFilter)) {
                    urlVector.add(new URL(urlStr));
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return urlVector;
    }
}
