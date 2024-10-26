package civquest.init;

import civquest.parser.ruleset.Registry;
import civquest.swing.ImageSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** LoadingSource-implementation for loading CivQuest from a jar-file.
 *
 *  NOTE: You can translate "/" inside a comment into 
 *        getDirectorySeparator()...
 */
class JarLoadingSource extends LoadingSource {

    URL baseURL = null;

    public JarLoadingSource() {
        Class classObject = null;
        try {
            classObject = Class.forName("civquest.init.JarLoadingSource");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Something strange happened: CivQuest ");
            System.err.println("       can't find the class this function is located in!!!");
            System.err.println("=====> CivQuest will abort NOW!!!");
            System.exit(-1);
        }
        ClassLoader classLoader = classObject.getClassLoader();
        try {
            URL civquestURL = classLoader.getResource("civquest");
            civquestURL = completeURLWithDirSeparator(civquestURL);
            baseURL = resolve(civquestURL, "..");
        } catch (MalformedURLException e) {
            System.err.println("Error: JarLoadingSource is unable to determine " + "baseURL due to an incorrect syntax!!!");
            System.err.println("====> CivQuest will abort NOW!!!");
            System.exit(-1);
        }
    }

    /** Returns a reference to a (newly constructed) top-level-registry-object.
	 * @return reference to a top-level registry
	 */
    public Registry constructRegistry(String theme) {
        try {
            if (!theme.endsWith(getDirectorySeparator())) {
                theme += getDirectorySeparator();
            }
            return new Registry(resolve(baseURL, "config" + getDirectorySeparator() + theme));
        } catch (MalformedURLException e) {
            System.err.println("JarLoadingSource.constructRegistry says: " + e);
            System.err.println("====> Registry cannot be constructed and CivQuest will abort NOW!!!");
            System.exit(-1);
            return null;
        }
    }

    /** Returns a reference to a (newly constructed) top-level-registry-object.
	 * @return reference to a top-level registry
	 */
    public ImageSet constructImageSet(String theme) {
        try {
            if (!theme.endsWith(getDirectorySeparator())) {
                theme += getDirectorySeparator();
            }
            return new ImageSet(resolve(baseURL, "images" + getDirectorySeparator() + theme));
        } catch (MalformedURLException e) {
            System.err.println("JarLoadingSource.constructImageSet says: " + e);
            System.err.println("====> Image cannot be loaded and CivQuest will abort NOW!!!");
            System.exit(-1);
            return null;
        }
    }

    public String getDirectorySeparator() {
        return "/";
    }

    public URL resolve(URL base, String suffix) throws MalformedURLException {
        String baseString = base.toString();
        if (baseString.startsWith("jar:file:")) {
            baseString = baseString.replaceFirst("jar:", "");
            URL rawRetURL = new URL(new URL(baseString), suffix);
            String rawRetURLString = rawRetURL.toString();
            String retURLString = rawRetURLString.replaceFirst("file:", "jar:file:");
            return new URL(retURLString);
        } else {
            return new URL(base, suffix);
        }
    }

    public String[] getDirectoryContents(URL url) throws IOException {
        JarURLConnection jcon = (JarURLConnection) url.openConnection();
        String jarDir = jcon.getJarEntry().getName();
        JarFile jarFile = jcon.getJarFile();
        Enumeration entries = jarFile.entries();
        List<String> entryStrings = new ArrayList<String>();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(jarDir) && name.length() > jarDir.length()) {
                entryStrings.add(name.replaceFirst(jarDir, ""));
            }
        }
        String[] retValue = new String[entryStrings.size()];
        for (int n = 0; n < retValue.length; n++) {
            retValue[n] = (String) (entryStrings.get(n));
        }
        return retValue;
    }

    public boolean doesPathExist(URL url) {
        try {
            JarURLConnection jcon = (JarURLConnection) url.openConnection();
            JarFile jarFile = jcon.getJarFile();
            String urlString = url.toString();
            String entryName = urlString.replaceFirst(".*?jar!/", "");
            return (jarFile.getEntry(entryName) != null);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            System.err.println("JarLoadingSource.doesPathExist(URL) says: " + e);
            System.err.println("====> Aborting CivQuest....");
            System.exit(-1);
            return false;
        }
    }
}
