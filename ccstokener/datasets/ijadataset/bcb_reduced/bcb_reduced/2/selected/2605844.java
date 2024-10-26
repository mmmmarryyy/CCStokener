package wotlas.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;

/** Various useful tools...
 *
 * @author Aldiss
 */
public class Tools {

    /** Waits ms milliseconds with a very low CPU use.
     *
     * @param ms number of milliseconds to wait.
     */
    public static void waitTime(long ms) {
        Object o = new Object();
        synchronized (o) {
            try {
                o.wait(ms);
            } catch (InterruptedException e) {
            }
        }
    }

    /** Is the Java version higher than the "min_required_version" string ?
     *  If it's not the case we return false and signal an ERROR to the Debug utility.
     *
     * @param min_required_version the minimum version number acceptable for this JVM
     *        ("1.2.2" for example).
     * @return true if the JVM version is higher, false otherwise.
     */
    public static boolean javaVersionHigherThan(String min_required_version) {
        String version = System.getProperty("java.version");
        if (version == null) {
            Debug.signal(Debug.ERROR, null, "Could not obtain JVM version...");
            return false;
        }
        if (version.compareTo(min_required_version) < 0) {
            Debug.signal(Debug.ERROR, null, "Your Java version is " + version + ". The minimum required version is " + min_required_version + " !");
            return false;
        }
        return true;
    }

    /** To get a date formated in a lexical way ( year-month-day).
     *  Example: "2001-09-25". Note that we write "09" instead of "9".
     *
     * @return date
     */
    public static String getLexicalDate() {
        Calendar rightNow = Calendar.getInstance();
        String year = "" + rightNow.get(Calendar.YEAR);
        String month = null;
        String day = null;
        if (rightNow.get(Calendar.MONTH) <= 9) {
            month = "0" + (rightNow.get(Calendar.MONTH) + 1);
        } else {
            month = "" + (rightNow.get(Calendar.MONTH) + 1);
        }
        if (rightNow.get(Calendar.DAY_OF_MONTH) <= 9) {
            day = "0" + rightNow.get(Calendar.DAY_OF_MONTH);
        } else {
            day = "" + rightNow.get(Calendar.DAY_OF_MONTH);
        }
        return year + "-" + month + "-" + day;
    }

    /** To get a date formated in a lexical way ( year-month-day).
     *  Example: "2001-09-25". Note that we write "09" instead of "9".
     *
     * @param currentTime the date to convert
     * @return date
     */
    public static String getLexicalDate(Calendar currentTime) {
        String year = "" + currentTime.get(Calendar.YEAR);
        String month = null;
        String day = null;
        if (currentTime.get(Calendar.MONTH) <= 9) {
            month = "0" + (currentTime.get(Calendar.MONTH) + 1);
        } else {
            month = "" + (currentTime.get(Calendar.MONTH) + 1);
        }
        if (currentTime.get(Calendar.DAY_OF_MONTH) <= 9) {
            day = "0" + currentTime.get(Calendar.DAY_OF_MONTH);
        } else {
            day = "" + currentTime.get(Calendar.DAY_OF_MONTH);
        }
        return year + "-" + month + "-" + day;
    }

    /** To get the time in pre-formated way.
     *  Example: "10h-05m-03s". Note that we write "03" instead of "3".
     *
     * @return time
     */
    public static String getLexicalTime() {
        Calendar rightNow = Calendar.getInstance();
        String hour = null;
        String min = null;
        String sec = null;
        if (rightNow.get(Calendar.HOUR_OF_DAY) <= 9) {
            hour = "0" + rightNow.get(Calendar.HOUR_OF_DAY);
        } else {
            hour = "" + rightNow.get(Calendar.HOUR_OF_DAY);
        }
        if (rightNow.get(Calendar.MINUTE) <= 9) {
            min = "0" + rightNow.get(Calendar.MINUTE);
        } else {
            min = "" + rightNow.get(Calendar.MINUTE);
        }
        if (rightNow.get(Calendar.SECOND) <= 9) {
            sec = "0" + rightNow.get(Calendar.SECOND);
        } else {
            sec = "" + rightNow.get(Calendar.SECOND);
        }
        return hour + "h-" + min + "m-" + sec + "s";
    }

    /** To get an instance of an object from its class name. We assume that the
     *  object has an empty constructor.
     *
     *  @param className a string representing the class name of the filter
     *  @return an instance of the object, null if we cannot get an instance.
     */
    public static Object getInstance(String className) {
        try {
            Class myClass = Class.forName(className);
            return myClass.newInstance();
        } catch (Exception ex) {
            Debug.signal(Debug.ERROR, null, "Failed to create new instance of " + className + ", " + ex);
            return null;
        }
    }

    /** To display a GUI Debug Message.
     */
    public static void displayDebugMessage(String title, String msg) {
        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    /** To get a System property. If the property is not found we return an empty String.
     *
     *  @param key property key
     *  @return systemp property.
     */
    public static String getSystemProp(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            return "";
        }
        return value;
    }

    /** To tell if we are on a Windows or Unix System. This can be used if shell scripts are
     *  needed.
     */
    public static boolean isWindowsOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") < 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns a new string where the 'newStr' string has replaced all 'find' patterns.
     * @param in String to edit
     * @param find string to match
     * @param newStr string to substitude for find
     */
    public static String subString(String in, String find, String newStr) {
        StringBuffer buf = new StringBuffer("");
        int cur = 0, nxt = 0;
        while (cur < in.length() && ((nxt = in.indexOf(find, cur)) >= 0)) {
            buf.append(in.substring(cur, nxt));
            buf.append(newStr);
            cur = nxt + find.length();
        }
        if (cur < in.length()) {
            buf.append(in.substring(cur, in.length()));
        }
        return buf.toString();
    }

    /** To create a random key of 'nbChars' chars.
     *  ( I smile because I'm sure there will be someone one day seeking this code
     *    to find the key's logic... well, as you see, they keys are generated very
     *    simply. ).
     * @return a string containing a random key of nbChars
     */
    public static String keyGenerator(int nbChars, int seed) {
        StringBuffer buf = new StringBuffer("");
        Random r = new Random(System.currentTimeMillis() * seed);
        for (int i = 0; i < nbChars; i++) {
            if (r.nextInt(2) == 1) {
                buf.append((char) ('A' + r.nextInt(26)));
            } else {
                buf.append((char) ('0' + r.nextInt(10)));
            }
        }
        return buf.toString();
    }

    /**
     *  <p> Search for classes that implement a given interface. You can specify in which
     *  packages to search. This method works EVEN if the classes are nested in a
     *  JAR or ZIP files (as long as the JAR/ZIP file is specified in the classpath)</p>
     *
     *  <p> Note that this does not make sense for all class loaders.  In cases
     *  where it doesn't make sense, the return value will be null. </p>
     *
     *  <p> This can also be a very slow method if the classpath is long. Here is an example
     *  of use :
     *  </p>
     *  <pre>
     *     String packages[] = { "wotlas.server.chat", "wotlas.server.chat.extra" };
     *     Class chatCommands[] = Tools.getImplementorsOf( "wotlas.server.chat.ChatCommand", packages );
     *
     *   will return the chat commands classes found in the two specified packages.
     *
     *   Other example :
     *
     *     Class chatCommands[] = Tools.getImplementorsOf( "wotlas.server.chat.ChatCommand", null );
     *
     *   will search everywhere for the commands (using the classpath).
     *  </pre>
     *
     *  IMPORTANT : we assume the classpath contains a least a "." if you want to search among
     *              the local files. If your project only contains JAR just enter them in your
     *              classpath, the "." is not needed.
     *
     *  I want to thank the AliceBot project from which I took some part of the following code.
     *
     *  @param interfaceName the fully-qualified name of the interface whose implementations are wanted
     *                       such as wotlas.server.chat.ChatCommand
     *  @param packages package names where to perform the search, if you want to search
     *                 everywhere just give a null value or new String[0].
     *  @return the found classes that implement the given interface.
     *  @exception ClassNotFoundException if the class of the interface
     *  @exception SecurityException if we have no access to local files.
     */
    public static Class[] getImplementorsOf(String interfaceName, String packages[]) throws ClassNotFoundException, SecurityException {
        StringTokenizer tokenizer = new StringTokenizer(System.getProperty("java.class.path", "."), System.getProperty("path.separator", ";"));
        String packageNames[] = null;
        if (packages != null && packages.length != 0) {
            packageNames = new String[packages.length];
            for (int i = 0; i < packageNames.length; i++) {
                packageNames[i] = Tools.subString(packages[i], ".", File.separator);
            }
        }
        Class theInterface = Class.forName(interfaceName);
        HashSet<Class> results = new HashSet<Class>();
        while (tokenizer.hasMoreTokens()) {
            String directory = tokenizer.nextToken();
            HashSet<String> list = new HashSet<String>();
            if (directory.endsWith(".jar") || directory.endsWith(".zip")) {
                Enumeration entries = null;
                if (directory.endsWith(".jar")) {
                    JarFile jar = null;
                    try {
                        jar = new JarFile(directory);
                    } catch (IOException e) {
                        Debug.signal(Debug.ERROR, null, "Classpath contains invalid entry: " + directory);
                        continue;
                    }
                    entries = jar.entries();
                } else {
                    ZipFile zip = null;
                    try {
                        zip = new ZipFile(directory);
                    } catch (IOException e) {
                        Debug.signal(Debug.ERROR, null, "Classpath contains invalid entry: " + directory);
                        continue;
                    }
                    entries = zip.entries();
                }
                if (entries != null) {
                    while (entries.hasMoreElements()) {
                        String entry = ((ZipEntry) entries.nextElement()).getName();
                        if (!entry.endsWith(".class")) {
                            continue;
                        }
                        entry = entry.substring(0, entry.lastIndexOf(".class"));
                        entry = entry.replace('/', '.');
                        if (packageNames != null) {
                            for (int i = 0; i < packages.length; i++) {
                                if (entry.startsWith(packages[i])) {
                                    list.add(entry);
                                    break;
                                }
                            }
                        } else {
                            list.add(entry);
                        }
                    }
                }
            } else {
                if (!directory.equals(".")) {
                    boolean found = false;
                    for (int p = 0; p < packageNames.length; p++) {
                        File packageFiles[] = new File(directory, packageNames[p]).listFiles();
                        if (packageFiles == null || packageFiles.length == 0) {
                            Debug.signal(Debug.WARNING, null, "Empty Package : " + directory + " ; " + packages[p]);
                            continue;
                        }
                        for (int i = 0; i < packageFiles.length; i++) {
                            String entry = packageFiles[i].getPath();
                            if (entry.endsWith(".class")) {
                                found = true;
                                entry = Tools.subString(entry, File.separator, ".");
                                entry = entry.substring(directory.length() + 1, entry.lastIndexOf(".class"));
                                list.add(entry);
                            }
                        }
                    }
                    if (!found) {
                        File files[] = new File(directory).listFiles();
                        if (files != null) {
                            for (int index = 0; index < files.length; index++) {
                                String entry = files[index].getPath();
                                if (entry.endsWith(".class")) {
                                    entry = Tools.subString(entry, File.separator, ".");
                                    entry = entry.substring(0, entry.lastIndexOf(".class"));
                                    list.add(entry);
                                }
                            }
                        }
                    }
                } else if (packageNames != null) {
                    for (int p = 0; p < packageNames.length; p++) {
                        File packageFiles[] = new File(packageNames[p]).listFiles();
                        if (packageFiles == null || packageFiles.length == 0) {
                            Debug.signal(Debug.WARNING, null, "Empty Package : " + packages[p]);
                            continue;
                        }
                        for (int i = 0; i < packageFiles.length; i++) {
                            String entry = packageFiles[i].getPath();
                            if (entry.endsWith(".class")) {
                                entry = Tools.subString(entry, File.separator, ".");
                                entry = entry.substring(0, entry.lastIndexOf(".class"));
                                list.add(entry);
                            }
                        }
                    }
                }
            }
            if (list.size() == 0) {
                String[] entries;
                String name;
                for (int i = 0; i < packages.length; i++) {
                    name = "/" + packages[i].replace('.', '/') + "/classes.lst";
                    entries = Tools.listFilesInJar(name, packages[i].replace('.', '/'), "class");
                    if (entries == null || entries.length == 0) {
                        Debug.signal(Debug.WARNING, null, "Trying to find : " + name + " -> classes not found ");
                        continue;
                    }
                    Debug.signal(Debug.WARNING, null, "Trying to find : " + name + " -> found " + entries.length + ".class");
                    for (int j = 0; j < entries.length; j++) {
                        name = entries[j].substring(1, entries[j].lastIndexOf(".class"));
                        name = name.replace('/', '.');
                        list.add(name);
                    }
                }
            }
            if (list.size() == 0) {
                continue;
            }
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Class candidate = null;
                String className = (String) it.next();
                if (className.equals(interfaceName)) {
                    continue;
                }
                try {
                    candidate = Class.forName(className);
                } catch (Exception e) {
                    Debug.signal(Debug.WARNING, null, "Failed to find class : " + className + " Msg: " + e);
                    continue;
                }
                if (theInterface.isAssignableFrom(candidate)) {
                    results.add(candidate);
                }
            }
        }
        Class toReturn[] = {};
        return (Class[]) results.toArray(toReturn);
    }

    /** Returns true if we have the given jar name in our classpath.
     * @param jarName jar file name, such as "wotlas.jar"
     * @return true if the JAR is in the classpath, false if not
     */
    public static boolean hasJar(String jarName) {
        jarName = jarName.toLowerCase();
        int index = jarName.lastIndexOf("/");
        if (index >= 0) {
            jarName = jarName.substring(index + 1, jarName.length());
        }
        StringTokenizer tokenizer = new StringTokenizer(System.getProperty("java.class.path", "."), System.getProperty("path.separator", ";"));
        while (tokenizer.hasMoreTokens()) {
            String directory = tokenizer.nextToken().toLowerCase();
            if (directory.endsWith(jarName)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Returns all the files that are in the given jar, have in the specified path and
     *  have the given extension. IF you don't specify an extension (ext==null)
     *  we search for directories not files. In any case we don't recurse between
     *  eventual sub-directories.
     *
     *  @param resourcesLstName name of resources file that give us the complete list of the resources that we are seeking.
     *  @param dirPath path to search "base/gui/chat" for example.
     *  @param ext extension of the files to list : ".gif" for example.
     *  @return the paths of the files found, an empty array otherwise
     */
    public static String[] listFilesInJar(String resourcesLstName, String dirPath, String ext) {
        try {
            dirPath = Tools.subString(dirPath, "\\", "/");
            if (!dirPath.endsWith("/")) {
                dirPath = dirPath + "/";
            }
            if (dirPath.startsWith("/")) {
                dirPath = dirPath.substring(1, dirPath.length());
            }
            URL url = ResourceLookup.getClassResourceUrl(Tools.class, resourcesLstName);
            if (url == null) {
                String msg = "File not found " + resourcesLstName;
                Debug.signal(Debug.ERROR, null, msg);
                return new String[0];
            }
            InputStream is = url.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String name = in.readLine();
            HashSet<String> list = new HashSet<String>(10);
            while (name != null) {
                name = in.readLine();
                if (name == null) {
                    continue;
                }
                if (ext != null && !name.endsWith(ext)) {
                    continue;
                }
                if (name.indexOf('.') == -1 && !name.endsWith("/")) {
                    name = name + "/";
                }
                int index = name.indexOf(dirPath);
                if (index < 0) {
                    continue;
                }
                index += dirPath.length();
                if (index >= name.length() - 1) {
                    continue;
                }
                index = name.indexOf("/", index);
                if (ext != null && (name.endsWith("/") || index >= 0)) {
                    continue;
                } else if (ext == null && (index < 0 || index < name.length() - 1)) {
                    continue;
                }
                list.add("/" + name);
            }
            is.close();
            String[] toReturn = {};
            return list.toArray(toReturn);
        } catch (IOException ioe) {
            String msg = "Error reading file " + resourcesLstName + " caused by " + ioe;
            Debug.signal(Debug.ERROR, null, msg);
            return new String[0];
        }
    }
}
