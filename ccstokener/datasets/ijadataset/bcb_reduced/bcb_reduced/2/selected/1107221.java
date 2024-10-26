package swifu.init;

import swifu.parser.ruleset.Registry;
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

/** LoadingSource-implementation for loading SwiFu from a jar-file.
 *
 *  NOTE: You can translate "/" inside a comment into 
 *        getDirectorySeparator()...
 */
class JarLoadingSource extends LoadingSource {

    URL baseURL = null;

    public JarLoadingSource() {
        Class classObject = null;
        try {
            classObject = Class.forName("swifu.init.JarLoadingSource");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Something strange happened: SwiFu ");
            System.err.println("       can't find the class this function is located in!!!");
            System.err.println("=====> SwiFu will abort NOW!!!");
            System.exit(-1);
        }
        ClassLoader classLoader = classObject.getClassLoader();
        try {
            URL swifuURL = classLoader.getResource("swifu");
            swifuURL = completeURLWithDirSeparator(swifuURL);
            baseURL = resolve(swifuURL, "..");
        } catch (MalformedURLException e) {
            System.err.println("Error: JarLoadingSource is unable to determine " + "baseURL due to an incorrect syntax!!!");
            System.err.println("====> SwiFu will abort NOW!!!");
            System.exit(-1);
        }
    }

    /** Returns a reference to a (newly constructed) top-level-registry-object.
	 * @return reference to a top-level registry
	 */
    public Registry constructRegistry() {
        return new Registry(baseURL, this);
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
        List entryStrings = new ArrayList();
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
            System.err.println("====> Aborting SwiFu....");
            System.exit(-1);
            return false;
        }
    }
}
