package seismosurfer.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import seismosurfer.util.SeismoException;

/**
 * A utility that handles the client-side caching 
 * for the some layers` data, i.e. shape files.
 * 
 */
public class LayerUtil {

    private static final String CACHE_DIR = "seismo";

    private static final String SEP = System.getProperty("file.separator");

    private static final String SHP_EXT = ".shp";

    private static final String SSX_EXT = ".ssx";

    public LayerUtil() {
    }

    /**
     * Copies shape files to a local directory
     * after reading them from the server
     * if they don`t exist already and returns 
     * the directory path.
     * 
     * @param shapePath the path to the jar file on the server
     * @return the local directory path
     */
    public static String loadShapeFile(String shapePath) {
        String uh = System.getProperty("user.home");
        String path = uh + SEP + CACHE_DIR + SEP + shapePath;
        System.out.println(path);
        String dirPath = getDirPath(path);
        File dir = new File(dirPath);
        System.out.println("Dir :" + dir.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File shp = new File(path + SHP_EXT);
        System.out.println("SHP :" + shp.toString());
        if (!shp.exists()) {
            makeFile(shp, shapePath);
        }
        File ssx = new File(path + SSX_EXT);
        System.out.println("SSX :" + ssx.toString());
        if (!ssx.exists()) {
            makeFile(ssx, shapePath);
        }
        return path;
    }

    /**
     * Copies a jar entry from the server to a local file
     * for caching.
     * 
     * @param file the local file
     * @param shapePath the path to the jar file on the server
     * 
     */
    protected static void makeFile(File file, String shapePath) {
        try {
            file.createNewFile();
            BufferedOutputStream localFile = new BufferedOutputStream(new FileOutputStream(file));
            URL url = getJarFileURL(shapePath, file.getName());
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            int fileSize = jarConnection.getContentLength();
            byte b[] = new byte[fileSize];
            BufferedInputStream jarFile = new BufferedInputStream(jarConnection.getInputStream());
            while (jarFile.read(b) != -1) {
                localFile.write(b);
            }
            localFile.close();
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * Constructs a url to a jar file entry.
     * 
     * @param jarPath the path to the jar file
     * @param entryName the name of an entry of the jar file
     * @return a url to a jar file entry
     */
    protected static URL getJarFileURL(String jarPath, String entryName) {
        URL url = null;
        try {
            url = new URL("jar:" + URLUtil.getAppletURL() + jarPath + ".jar!/" + entryName);
            System.out.println("JAR URL :" + url.toString());
            return url;
        } catch (MalformedURLException e) {
            throw new SeismoException(e);
        }
    }

    /**
     * Given a path returns its the directory path.
     * 
     * @param path the file path
     * @return the directory path
     */
    protected static String getDirPath(String path) {
        int sep = path.lastIndexOf(SEP);
        return path.substring(0, sep);
    }
}
