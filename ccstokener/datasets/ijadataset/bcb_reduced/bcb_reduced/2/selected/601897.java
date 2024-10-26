package freets.tools;

import java.util.*;
import java.util.jar.*;
import java.lang.*;
import java.lang.reflect.*;
import java.net.*;
import java.io.*;
import freets.data.settings.Settings;

/**
 * This class contains some static methods for retrieving and handling JAR-files
 * and JAR-entries.
 *
 * @author T. F�rster
 * @version $id$
 */
public final class JarLoader {

    /**
     * The constructor is empty.
     */
    private JarLoader() {
    }

    /**
     * Downloads a JAR-file form the intranet/internet to the ~freets-home/lib/
     * directory on the local disk.
     * @param url the URL of the JAR file to download
     * @return the global pathname of the JAR file on the local system
     */
    public static String downloadJar(URL url) throws IOException {
        String localFile = null;
        char[] buf = new char[4096];
        int num;
        localFile = Settings.getFreeTsUserPath() + "lib" + Settings.SLASH + getURLFileName(url);
        DebugDialog.print("Downloading jar-file " + url + " to " + localFile + ".", 4);
        InputStreamReader in = new InputStreamReader(url.openStream());
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(localFile));
        do {
            num = in.read(buf, 0, 4096);
            if (num > 0) {
                out.write(buf, 0, num);
            }
        } while (num > 0);
        in.close();
        out.close();
        return localFile;
    }

    /**
     * Get the manifest of a JAR-file.
     * @param pathname the pathname of the file on the harddisk
     * @return the manifest of the JAR-file, or <i>null</i> if an error occurred.
     */
    public static Manifest getManifest(String pathname) {
        Manifest manifest = null;
        try {
            InputStream in = new FileInputStream(pathname);
            manifest = new JarInputStream(in).getManifest();
            in.close();
        } catch (IOException ioex) {
            System.out.println("->" + ioex);
        } finally {
            return manifest;
        }
    }

    /**
     * Unpacks a single file from a JAR-file to the local disk.
     * @param jarFile the JAR-file the file is located in
     * @param entry the JarEntry to be unpacked
     */
    public static void unpackFile(JarFile jarFile, JarEntry entry) throws IOException {
        File localFile = new File(Settings.getBaseDir() + "/" + entry.getName());
        int num;
        if (entry.isDirectory()) {
            localFile.mkdirs();
            DebugDialog.print("Making directory: " + localFile.getName(), 5);
        } else {
            if (localFile.exists()) {
                DebugDialog.print("Local file already exists: " + localFile.getName(), 5);
                return;
            }
            DebugDialog.print("Unpacking file " + localFile.getName() + " from " + jarFile.getName(), 5);
            ensureDirectoryExists(localFile);
            char[] buf = new char[4096];
            InputStreamReader in = new InputStreamReader(jarFile.getInputStream(entry));
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(localFile));
            do {
                num = in.read(buf, 0, 4096);
                if (num > 0) {
                    out.write(buf, 0, num);
                }
            } while (num > 0);
            in.close();
            out.close();
        }
    }

    /**
     * Ensures that the directory of a file exists. If it does not exist, if will be created.
     * @param file the file which directory shall be created if necessary
     */
    public static void ensureDirectoryExists(File file) throws IOException {
        if (file.isDirectory()) {
            file.mkdirs();
        } else {
            String p = file.getPath();
            int i = p.lastIndexOf("/");
            File f2 = new File(p.substring(0, i - 1));
            DebugDialog.print("MAKING DIRS: " + p.substring(0, i - 1), 5);
            f2.mkdirs();
        }
    }

    /**
     * Returns the filename of a URL.<p>
     * Unfortunately the <code>java.net.URL</code> class does not provide such a method.
     * @param url an URL
     * @return the filename
     */
    public static String getURLFileName(URL url) {
        String s = url.getFile();
        return s.substring(1 + s.lastIndexOf("/"));
    }
}
