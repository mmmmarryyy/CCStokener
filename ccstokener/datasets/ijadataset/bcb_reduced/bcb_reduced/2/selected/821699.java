package universe.common;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

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
    *       Check if the UniverseData.jar (or whatever it is named) is
    *       available (on the classpath).
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
    *       Checks the presence of the UniverseData.jar file, and, if not
    *       present, informs the user and exits.
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
    *       Given a resource name, extract a URL for the given resource.
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
    *       get an ImageIcon
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
    *       gets an InputStream to the named resource
    *
    ********************************************************************/
    public static InputStream getInputStream(String name) throws java.io.IOException {
        URL url = getURL(name);
        if (url != null) {
            return url.openStream();
        }
        return null;
    }

    /********************************************************************
    *
    *       gets an InputStreamReader to the named resource.
    *
    ********************************************************************/
    public static InputStreamReader getInputStreamReader(String name) throws java.io.IOException {
        URL url = getURL(name);
        if (url != null) {
            return new InputStreamReader(url.openStream());
        }
        return null;
    }
}
