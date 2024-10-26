package wotlas.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;

/** Various useful tools to manipulate files...
 *
 * @author Aldiss
 */
public class FileTools {

    /**
     * Name of a directory to be ignored and used for CVS synchronization.
     */
    public static final String CVS_REPOSITORY_FILES = "CVS";

    /**
     * Extension filename used by resources list files.
     */
    public static final String EXT_RESOURCES_LIST_FILES = ".lst";

    /** Finds the file/directory having the highest (or lowest) name (lexical order) in the
     * specified directory. The file/directory must have the XXXsomethingYYY format, where
     * is XXX is the "beg" parameter and YYY the "end" parameter.<br><p>
     *
     *  For example if the String list[] contains :<br><p>
     *
     *  ah-ah.cfg <br>
     *  save-2001-09-01.cfg <br>
     *  save-2001-09-02.cfg <br>
     *  save-2001-09-03.cfg <br>
     *  zzz-zzz.dat <br><p>
     *
     *  the call findSave( list, "save-",".cfg", true) will return "save-2001-09-03.cfg".
     *  the call findSave( list, "save-",".cfg", false) will return "save-2001-09-01.cfg".
     *
     *  Note that in the example we paid attention to the file names : no "9" but "09" to keep
     *  a useful lexical order.
     *
     * @param list list of file names to inspect
     * @param beg  file name prefix.
     * @param end file name suffix.
     * @param latest latest or oldest save ?
     * @return file name with the highest or lowest lexical order and matching the specified format.
     */
    public static String findSave(String list[], String beg, String end, boolean latest) {
        if (list == null) return null;
        int index = -1;
        for (int i = 0; i < list.length; i++) {
            String name = list[i];
            int ind = list[i].lastIndexOf(File.separator);
            if (ind < 0) {
                ind = list[i].lastIndexOf("/");
                if (ind > 0) name = name.substring(ind + 1, name.length());
            } else name = name.substring(ind + File.separator.length(), name.length());
            if (!name.startsWith(beg) || !name.endsWith(end)) continue;
            if (index == -1) index = i; else {
                int compare = list[i].compareTo(list[index]);
                if ((latest && compare > 0) || (!latest && compare < 0)) index = i;
            }
        }
        if (index == -1) return null; else return list[index];
    }

    /** To load a simple properties file...
     *
     * @param inStream an input stream
     * @return properties file
     */
    public static Properties loadPropertiesFromStream(InputStream inStream) {
        try {
            BufferedInputStream is = new BufferedInputStream(inStream);
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (Exception e) {
            Debug.signal(Debug.WARNING, null, e);
            return null;
        }
    }

    /** To load a simple properties file...
     *
     * @param pathname properties complete filename
     * @return properties file
     */
    public static Properties loadPropertiesFile(String pathname) {
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(pathname));
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (Exception e) {
            Debug.signal(Debug.WARNING, null, e);
            return null;
        }
    }

    /** To save a simple properties file... For more complex use PREFER the
     *  use of the persistence library.
     *
     * @param props properties to save
     * @param pathname file name to save the properties to.
     * @param header header to put in the file.
     * @return true if success, false otherwise
     */
    public static boolean savePropertiesFile(Properties props, String pathname, String header) {
        try {
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(pathname));
            props.store(os, header);
            return true;
        } catch (Exception e) {
            Debug.signal(Debug.WARNING, null, e);
            return false;
        }
    }

    /** Saves the given string to a text file pointed by filename.
     *
     * @param filepath the complete filename (ex: /infres/pub/bob.txt )
     * @param text the string to save
     * @return true on success, false on failure
     */
    public static boolean saveTextToFile(String filename, String text) {
        try {
            BufferedWriter w_out = new BufferedWriter(new FileWriter(filename));
            int pos_start = 0, pos_end = 0;
            while ((pos_end = text.indexOf("\n", pos_start)) != -1) {
                w_out.write(text, pos_start, pos_end - pos_start);
                w_out.newLine();
                pos_start = pos_end + 1;
            }
            if (pos_start < text.length()) w_out.write(text, pos_start, text.length() - pos_start);
            w_out.flush();
            w_out.close();
        } catch (IOException e) {
            Debug.signal(Debug.ERROR, null, "Error: " + e);
            return false;
        }
        return true;
    }

    /** Loads the text file pointed by the filename.
     *
     * @param filename the complete filename (ex: /infres/pub/bob.txt )
     * @return a string representing the text file on success, null on failure
     */
    public static String loadTextFromFile(String filename) {
        StringBuffer text = new StringBuffer("");
        String tmp;
        boolean firstLine = true;
        try {
            BufferedReader r_in = new BufferedReader(new FileReader(filename));
            while ((tmp = r_in.readLine()) != null) {
                if (!firstLine) text.append("\n"); else firstLine = false;
                text.append(tmp);
            }
            r_in.close();
        } catch (IOException e) {
            Debug.signal(Debug.ERROR, null, "Error: " + e);
            return null;
        }
        return text.toString();
    }

    /** Loads the text from the stream.
     * @param is an inputstream
     * @return a string representing the text file on success, null on failure
     */
    public static String loadTextFromStream(InputStream is) {
        StringBuffer text = new StringBuffer("");
        String tmp;
        boolean firstLine = true;
        try {
            BufferedReader r_in = new BufferedReader(new InputStreamReader(is));
            while ((tmp = r_in.readLine()) != null) {
                if (!firstLine) text.append("\n"); else firstLine = false;
                text.append(tmp);
            }
            r_in.close();
        } catch (IOException e) {
            Debug.signal(Debug.ERROR, null, "Error: " + e);
            return null;
        }
        return text.toString();
    }

    /** To read a distant file represented by an URL.
     * @param urlName string representing an url
     * @return the loaded text file or null if an error occured.
     */
    public static String getTextFileFromURL(String urlName) {
        try {
            StringBuffer textFile = new StringBuffer("");
            String line = null;
            URL url = new URL(urlName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            while ((line = reader.readLine()) != null) textFile = textFile.append(line + "\n");
            reader.close();
            return textFile.toString();
        } catch (Exception e) {
            Debug.signal(Debug.ERROR, null, "Failed to open " + urlName + ", exception " + e);
            return null;
        }
    }

    /** To update a property file without modifying its format. We don't save the file
     *  disk.
     *
     *  @param propertyName the property name to search and replace
     *  @param newValue the new value for the property
     *  @param oldConfig previous config file
     *  @return new config file.
     */
    public static String updateProperty(String propertyName, String newValue, String oldConfig) {
        StringBuffer newConfig = new StringBuffer("");
        int pos = oldConfig.lastIndexOf(propertyName);
        if (pos < 0) return null;
        pos = oldConfig.indexOf("=", pos);
        if (pos < 0) return null;
        newConfig.append(oldConfig.substring(0, pos + 1));
        newConfig.append(newValue);
        pos = oldConfig.indexOf("\n", pos);
        if (pos > 0 && pos < oldConfig.length()) newConfig.append(oldConfig.substring(pos, oldConfig.length())); else newConfig.append("\n");
        return newConfig.toString();
    }

    /** To list the files from a disk's directory (only the files, not dirs...),  we filter
     *  the files that have the specified extension.
     *
     * @param dirName directory to use as root for our search
     * @param ext extension of the files to find (enter "" to get all the files)
     * @return files path name found ( on one level, we don't extend the search
     *         to sub-directories ). If no files are found we return an empty array.
     */
    public static String[] listFiles(String dirName, String ext) {
        File flist[] = new File(dirName).listFiles();
        if (flist == null) return new String[0];
        Vector list = new Vector(20);
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory() || !flist[i].getName().endsWith(ext)) continue;
            if (flist[i].getName().indexOf(CVS_REPOSITORY_FILES) != -1 || flist[i].getName().endsWith(EXT_RESOURCES_LIST_FILES)) continue;
            list.addElement(flist[i].getPath());
        }
        String toReturn[] = {};
        return (String[]) list.toArray(toReturn);
    }

    /** To list the sub-dirs from a disk's directory.
     *
     * @param dirName directory to use as root for our search
     * @return dirs name found ( on one level, we don't extend the search
     *         to sub-directories ). If no dirs are found we return an empty array.
     */
    public static String[] listDirs(String dirName) {
        File flist[] = new File(dirName).listFiles();
        if (flist == null) return new String[0];
        Vector list = new Vector(10);
        for (int i = 0; i < flist.length; i++) {
            if (!flist[i].isDirectory()) continue;
            if (flist[i].getName().indexOf(CVS_REPOSITORY_FILES) != -1) continue;
            if (flist[i].getPath().endsWith(File.separator)) list.addElement(flist[i].getPath()); else list.addElement(flist[i].getPath() + File.separator);
        }
        String toReturn[] = {};
        return (String[]) list.toArray(toReturn);
    }
}
