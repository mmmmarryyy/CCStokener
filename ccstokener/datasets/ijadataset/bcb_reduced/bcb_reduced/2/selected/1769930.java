package org.gjt.universe;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 * Encapsulates searching for resources (e.g. images, sounds,
 * custom data files) used by the game into a single facade,
 * so that callers need not know how or where these data are
 * stored.
 * <p>
 * Currently all resources are kept in a single jar file,
 * UniverseData.jar, which should be in the same directory
 * as the game's jar.  However, in the future a more complex
 * scheme may be employed (e.g. to support custom variations
 * of the game, localization, etc.).
 * <p>
 * Note that the game is unable to run without UniverseData.jar
 * and if it is not detected or is believed to be damaged, an
 * fatal error explaining this will be reported ot the user
 * and the application will exit.
 * <p>
 * Note: if additional resource files are desired (in addition
 * to UniverseData.jar, they can be automatically used just by
 * adding the file name to the <code>urlsToLoadFrom</code>
 * variable.
 * <p>
 * Some changes were made: file-handling methods will throw a
 * FileNotFoundException instead of null, to make error handling
 * easier (no need to test for null case).
 *
 *      @author Sean Starkey
 *      @author Zach DelProposto
 *      @author Allan Noordvyk
 **/
public class UniverseData {

    private static UniverseData singleton;

    private static URLClassLoader classLoader;

    private static final String CHECK_FILE = "data.check";

    static {
        singleton = new UniverseData();
        try {
            URL[] urlsToLoadFrom = new URL[] { new URL("file:UniverseData.jar"), new URL("http://prdownloads.sourceforge.net/universe/UniverseData.jar") };
            classLoader = new URLClassLoader(urlsToLoadFrom);
        } catch (MalformedURLException e) {
        }
    }

    private UniverseData() {
    }

    /********************************************************************
     *
     *      Check if the UniverseData.jar (or whatever it is named) is
     *      available (on the classpath).
     *
     ********************************************************************/
    public static boolean isDataPresent() {
        if (getURL(CHECK_FILE) != null) {
            return true;
        }
        return false;
    }

    /********************************************************************
     *
     *      Checks the presence of the UniverseData.jar file, and, if not
     *      present, informs the user and exits.
     *
     ********************************************************************/
    public static void checkDataPresent() {
        if (!isDataPresent()) {
            JOptionPane.showMessageDialog(null, "The Universe Data file, \"UniverseData.jar\", could not be\n" + "located. It should be placed in the same folder as the Universe.jar game.\n" + "Please check your set-up and try running Universe again.\n" + "\nUniverse will now exit.", "Universe: Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /********************************************************************
     *
     *      Given a resource name, extract a URL for the given resource.
     *
     ********************************************************************/
    public static URL getURL(String name) {
        if (classLoader == null) {
            return null;
        }
        return classLoader.getResource(name);
    }

    /********************************************************************
     *
     * Get an ImageIcon. Return null if an ImageIcon could not be found.
     *
     ********************************************************************/
    public static ImageIcon getImageIcon(String name) {
        URL url = getURL(name);
        if (url != null) {
            return new ImageIcon(url);
        }
        return null;
    }

    /********************************************************************
     *
     * Gets an InputStream to the named resource; throws an exception
     * if the resource could not be found.
     *
     ********************************************************************/
    public static InputStream getInputStream(String name) throws java.io.IOException {
        URL url = getURL(name);
        if (url != null) {
            return url.openStream();
        }
        throw new FileNotFoundException("UniverseData: Resource \"" + name + "\" not found.");
    }

    /********************************************************************
     *
     * Gets an InputStreamReader to the named resource.
     *
     ********************************************************************/
    public static InputStreamReader getInputStreamReader(String name) throws java.io.IOException {
        URL url = getURL(name);
        if (url != null) {
            return new InputStreamReader(url.openStream());
        }
        throw new FileNotFoundException("UniverseData: Resource \"" + name + "\" not found.");
    }

    /********************************************************************
     *
     * Gets an Image to the named resource. Returns null if an error
     * occurs.
     * <p>
     * We take the easy way out here....
     *
     ********************************************************************/
    public static Image getImage(String name) {
        URL url = getURL(name);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        return null;
    }

    /********************************************************************
     *
     * Returns a resource as a String. Note that this may not be the
     * best choice for large resources or where additional parsing is
     * required. If an IOException occurs, this method will return null.
     *
     ********************************************************************/
    public static String getText(String name) {
        BufferedReader br = null;
        StringBuffer sb = null;
        try {
            br = new BufferedReader(getInputStreamReader(name));
            sb = new StringBuffer(2048);
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            return sb.toString();
        } catch (IOException e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
