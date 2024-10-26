package de.fhg.igd.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import de.fhg.igd.io.Pipe;

/**
 * This class provides methods to return the URL, the source, and
 * the file input stream of a class identified by a given class name.
 *
 * These methods are available for both the system domain, with its 
 * three possible class sources (compare Java System Properties
 * <code>sun.boot.class.path</code>, <code>java.ext.dirs</code>,
 * and <code>java.class.path</code>), and for a user domain, which
 * can be initialized with a user class path, a user class resource
 * and user class URLs. 
 *
 * Thereby, the user obtains valuable helper methods, which might
 * be used when implementing its own user level class loader.
 *
 * Java class paths and class URL are internally handled in a 
 * canonicalized format, which is compatible to the one used by
 * Java's class loaders. Thereby, this class is able to use
 * <code>ClassLoader</code> and <code>URLClassLoader</code>
 * to benefit by their optimized native implementations as far as 
 * possible.  
 *
 * Further, it is possible to request a sorted set of available 
 * classes according to a chosen set of system and/or user class sources.
 * Since the chosen sources are recursively inspected, and because
 * of not using native implementation for doing that, these methods 
 * might process very slow in contrast to those described above.
 * 
 * @author Jan Peters
 * @version "$Id: ClassSource.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class ClassSource {

    /**
     * The list of valid command line options for the main method.
     */
    protected static final String options_ = "help:!,verbose:!,debug:!,hex:!,resource:!,search:s," + "userclasspath:s,userclassresource:s,userclassurls:U[";

    /**
     * The file extension of zip files.
     */
    public static final String ZIP_EXTENSION = ".zip";

    /**
     * The file extension of jar files.
     */
    public static final String JAR_EXTENSION = ".jar";

    /**
     * The file extension of class files.
     */
    public static final String CLASS_EXTENSION = ".class";

    /**
     * The protocol identifier for resource URLs
     */
    public static final String RESOURCE_URL_PROTOCOL = "resource";

    /**
     * The protocol identifier for archive URLs
     */
    public static final String ARCHIVE_URL_PROTOCOL = "jar";

    /**
     * The protocol identifier for file URLs
     */
    public static final String FILE_URL_PROTOCOL = "file";

    /**
     * <code>SOURCE_NOT_FOUND</code> is element of a set of flags 
     * given as bit string describe the source of a class:
     *
     * If this flag is set the class could not be found within the 
     * system or user class sources.
     */
    public static int SOURCE_NOT_FOUND = 0x00000000;

    /**
     * <code>SOURCE_ALL</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * This flag describes a class located in any of the system 
     * or user sources.
     */
    public static int SOURCE_ALL = 0x00ffffff;

    /**
     * <code>SOURCE_SYSTEM</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_SYSTEM == SOURCE_SYSTEM
     * </pre>
     *
     * describes a class located in one of the system sources marked by
     * <code>SOURCE_BOOT_CLASSPATH</code>, 
     * <code>SOURCE_SYSTEM_JARFILES</code>, 
     * or <code>SOURCE_SYSTEM_CLASSPATH</code>. 
     */
    public static int SOURCE_SYSTEM = 0x00010000;

    /**
     * <code>SOURCE_USER</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with
     *
     * <pre>
     * classSource & SOURCE_USER == SOURCE_USER
     * </pre>
     *
     * describes a class located in one of the system sources marked by
     * <code>SOURCE_USER_LOCALCLASS</code>,
     * <code>SOURCE_USER_CLASSRESORUCE</code>, 
     * or <code>SOURCE_USER_REMOTE</code>.
     */
    public static int SOURCE_USER = 0x00020000;

    /**
     * <code>SOURCE_BOOT_CLASSPATH</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_BOOT_CLASSPATH == SOURCE_BOOT_CLASSPATH
     * </pre>
     *
     * describes a class located in one of the boot class paths of the
     * JavaVM (compare Java System Property: <code>sun.boot.class.path</code>).
     */
    public static int SOURCE_BOOT_CLASSPATH = 0x00010001;

    /**
     * <code>SOURCE_SYSTEM_JARFILE</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_SYSTEM_JARFILE == SOURCE_SYSTEM_JARFILE
     * </pre>
     *
     * describes a class located as JAR/ZIP-file entry in one of 
     * java extension directories of the JavaVM (compare Java System 
     * Property: <code>java.ext.dirs</code>).
     */
    public static int SOURCE_SYSTEM_JARFILE = 0x00010002;

    /**
     * <code>SOURCE_SYSTEM_CLASSPATH</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_SYSTEM_CLASSPATH == SOURCE_SYSTEM_CLASSPATH
     * </pre>
     *
     * describes a class located in one of the system class paths of the
     * JavaVM (compare Java System Property: <code>java.class.path</code>).
     */
    public static int SOURCE_SYSTEM_CLASSPATH = 0x00010004;

    /**
     * <code>SOURCE_USER_LOCALCLASS</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_USER_LOCALCLASS == SOURCE_USER_LOCALCLASS
     * </pre>
     *
     * describes a class located within the local file system in one
     * of the given user class paths or class URLs (compare 
     * <code>setUserClassPath()</code>, <code>setUserClassPaths()</code>,
     * and <code>setUserClassUrls()</code>).
     */
    public static int SOURCE_USER_LOCALCLASS = 0x00020008;

    /**
     * <code>SOURCE_USER_CLASSRESOURCE</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_USER_CLASSRESOURCE == SOURCE_USER_CLASSRESOURCE
     * </pre>
     *
     * describes a class located within the given user class resource
     * (compare <code>setUserClassResource()</code>).
     */
    public static int SOURCE_USER_CLASSRESOURCE = 0x00020010;

    /**
     * <code>SOURCE_USER_REMOTECLASS</code> is element of a set of flags 
     * given as bit string, which describe the source of a class:
     *
     * A class source with 
     *
     * <pre>
     * classSource & SOURCE_USER_REMOTECLASS == SOURCE_USER_REMOTECLASS
     * </pre>
     *
     * describes a class located on a remote host below one of the given
     * user class URLs (compare <code>setUserClassUrls()</code>).
     */
    public static int SOURCE_USER_REMOTECLASS = 0x00020020;

    /**
     * This String array contains the different paths from the 
     * Java System Property <code>sun.boot.class.path</code>.
     */
    private static String[] bootClassPaths_;

    /**
     * This String array contains the different paths from the 
     * Java System Property <code>java.ext.dirs</code>.
     */
    private static String[] systemJarFiles_;

    /**
     * This String array contains the different paths from the 
     * Java System Property <code>java.class.path</code>.
     */
    private static String[] systemClassPaths_;

    /**
     * This String array contains the different paths given
     * through <code>setUserClassPath()</code> or
     * <code>setUserClassPaths()</code>.
     */
    private String[] userClassPaths_;

    /**
     * This URL array contains the different paths within 
     * <code>userClassPaths_</code> as corresponding 
     * <code>file:</code>-URLs.
     */
    private URL[] userClassPathUrls_;

    /**
     * This resource contains the classes given through
     * <code>setUserClassResource_</code>.
     */
    private Resource userClassResource_;

    /**
     * This URL array contains the different URLs given
     * throudh <code>setUserClassUrls()</code>.
     */
    private URL[] userClassUrls_;

    /** 
     * This instance of a <code>DummyClassLoader</code>
     * is used to initialize the <code>URLClassLoader</Code>.
     * This ensures that no parent class loader is invoked.
     */
    private static DummyClassLoader dummyClassLoader_;

    /**
     * The static class initializer parses the Java System Properties
     * and sets the variables <code>bootClassPaths_</code>,
     * <code>systemJarFiles_</code>, and <code>systemClassPaths_</code>.
     */
    static {
        StringTokenizer st;
        ArrayList paths;
        String systemClassPaths;
        String bootClassPaths;
        String systemExtDirs;
        String path;
        File[] files;
        File dir;
        int n;
        bootClassPaths = System.getProperty("sun.boot.class.path");
        if (bootClassPaths != null) {
            st = new StringTokenizer(bootClassPaths, File.pathSeparator);
            paths = new ArrayList();
            while (st.hasMoreTokens()) {
                path = canonicalClassPath(st.nextToken());
                paths.add(path);
            }
            bootClassPaths_ = (String[]) paths.toArray(new String[0]);
        } else {
            bootClassPaths_ = new String[0];
        }
        systemExtDirs = System.getProperty("java.ext.dirs");
        if (systemExtDirs != null) {
            st = new StringTokenizer(systemExtDirs, File.pathSeparator);
            paths = new ArrayList();
            while (st.hasMoreTokens()) {
                dir = new File(st.nextToken());
                files = dir.listFiles();
                for (n = 0; files != null && n < files.length; n++) {
                    if (checkFileExtension(files[n].getName(), ZIP_EXTENSION) || checkFileExtension(files[n].getName(), JAR_EXTENSION)) {
                        path = canonicalClassPath(files[n].getPath());
                        paths.add(path);
                    }
                }
            }
            systemJarFiles_ = (String[]) paths.toArray(new String[0]);
        } else {
            systemJarFiles_ = new String[0];
        }
        systemClassPaths = System.getProperty("java.class.path");
        if (systemClassPaths != null) {
            st = new StringTokenizer(systemClassPaths, File.pathSeparator);
            paths = new ArrayList();
            while (st.hasMoreTokens()) {
                path = canonicalClassPath(st.nextToken());
                paths.add(path);
            }
            systemClassPaths_ = (String[]) paths.toArray(new String[0]);
        } else {
            systemClassPaths_ = new String[0];
        }
        dummyClassLoader_ = new DummyClassLoader();
    }

    /**
     * Returns the canonic form of the given <code>path</code>.
     * 
     * This methods makes use of <code>File#getCanonicalPath()</code>, 
     * if possible. Further all contained <code>File.separatorChar</code> 
     * characters are replaced by slashes ('/'), and a leading slash ('/')
     * is appended, if not existent.
     *
     * Examples:
     * <ul>
     * <li> The path <code>/user/home/tmp/../classes</code> on a UNIX-like
     * system will be transformed into <code>/user/home/classes</code>.
     * <li> The path <code>D:\\user\\tmp\\..\\classes/</code> on a Windows
     * system will be transformed into <code>/D:/user/classes/</code>.
     * </ul>
     *
     * This canonic path form is still compatible to and can be used by 
     * Java's <code>File</code> class to identify paths and files.
     *
     * @param path The path to transform.
     * @return the canonic form of the given path.
     */
    public static String canonicalClassPath(String path) {
        String canonicalPath;
        File file;
        canonicalPath = path;
        try {
            file = new File(canonicalPath);
            canonicalPath = file.getCanonicalPath();
        } catch (Throwable t) {
        }
        canonicalPath = canonicalPath.replace(File.separatorChar, '/');
        if (!canonicalPath.startsWith("/")) {
            canonicalPath = "/" + canonicalPath;
        }
        return canonicalPath;
    }

    /**
     * Returns the canonic form of the given <code>path</code>
     * as <code>file:</code> URL.
     * 
     * This method makes use of <code>canonicalClassPath()</code>.
     *
     * Further, to all paths, which do not end with 
     * <code>JAR_EXTENSION</code> or <code>ZIP_EXTENSION</code>
     * (ignoring the case), an ending slash ('/') is appended. 
     * This ensures compatibility to class URL needed by Java's 
     * <code>URLClassLoader</code>.
     *
     * @param path The path to transform.
     * @return the canonic form of the given path as URL.
     */
    public static URL canonicalClassUrl(String path) {
        String canonicalPath;
        URL canonicalUrl;
        canonicalPath = canonicalClassPath(path);
        if (!canonicalPath.endsWith("/")) {
            if (!checkFileExtension(canonicalPath, JAR_EXTENSION) && !checkFileExtension(canonicalPath, ZIP_EXTENSION)) {
                canonicalPath = canonicalPath.concat("/");
            }
        }
        try {
            canonicalUrl = new URL(FILE_URL_PROTOCOL, null, canonicalPath);
            return canonicalUrl;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns the canonic form of the given <code>url</code>.
     *
     * If the given URL is a <code>file:</code>-URL denoting a
     * local path, the path part of the URL (compare 
     * <code>URL#getFile()</code>) is transformed through 
     * <code>canoncialClassPath</code>.
     *
     * Besides, to all URLs, which do not end with
     * <code>JAR_EXTENSION</code> or <code>ZIP_EXTENSION</code>
     * (ignoring the case), an ending slash ('/') is appended. 
     * This ensures compatibility to class URL needed by Java's 
     * <code>URLClassLoader</code>.
     *
     * @param url The URL to transform.
     * @return the canonic form of the given URL.
     */
    public static URL canonicalClassUrl(URL url) {
        String path;
        URL canonicalUrl;
        canonicalUrl = url;
        if (url.getProtocol().equalsIgnoreCase(FILE_URL_PROTOCOL)) {
            canonicalUrl = canonicalClassUrl(url.getFile());
        } else {
            path = url.toString();
            path = path.toLowerCase();
            if (!path.endsWith("/")) {
                if (!path.endsWith(JAR_EXTENSION) && !path.endsWith(ZIP_EXTENSION)) {
                    try {
                        canonicalUrl = new URL(url.toString().concat("/"));
                    } catch (Throwable t) {
                    }
                }
            }
        }
        return canonicalUrl;
    }

    /**
     * Checks if the given <code>file</code> has the given
     * <code>extension</code>. 
     *
     * This method ignores the case of file and extension.
     * If the given file or extension is <code>null</code>,
     * <code>false</code> is returned.
     *
     * @param file The file to check.
     * @param extension The extension to compare with.
     * @return <code>true</code>, iff the given file ends with
     *   the given extension.
     */
    public static boolean checkFileExtension(File file, String extension) {
        if (file == null) {
            return false;
        }
        return checkFileExtension(file.getPath(), extension);
    }

    /**
     * Checks if the given <code>file</code> has the given
     * <code>extension</code>. 
     *
     * This method ignores the case of file and extension.
     * If the given file or extension is <code>null</code>,
     * <code>false</code> is returned.
     *
     * @param file The file to check.
     * @param extension The extension to compare with.
     * @return <code>true</code>, iff the given file ends with
     *   the given extension.
     */
    public static boolean checkFileExtension(String file, String extension) {
        if (file == null || extension == null) {
            return false;
        }
        return file.toLowerCase().endsWith(extension.toLowerCase());
    }

    /**
     * Transformes the given <code>className</code> into 
     * a corresponding file name denoting the class within
     * the directory of a class path.
     *
     * E.g. the class name <code>de.fhg.igd.util.ClassSource</code>
     * is transformed to <code>de/fhg/igd/util/ClassSource.class</code>.
     * 
     * If the given class name is <code>null</code>, then 
     * <code>null</code> is returned.
     * 
     * @param className the fully qualified class name to transform,
     *   including package prefix with dots ('.') as separators.
     * @return the corresponding file name.
     */
    public static String transformClassToFile(String className) {
        String classFile;
        if (className == null) {
            return null;
        }
        classFile = className.trim();
        classFile = classFile.replace('.', '/').concat(CLASS_EXTENSION);
        return classFile;
    }

    /**
     * Transforms the given <code>classFile</code> as 
     * file name denoting a class within the directory
     * of a class path intothe corresponding class name.
     * 
     * E.g. the class name <code>de/fhg/igd/util/ClassSource.class</code>
     * is transformed to <code>de.fhg.igd.util.ClassSource</code>.
     *
     * If the given class file does not end with the
     * <code>CLASS_EXTENSION</code>, <code>null</code> is returned.
     * 
     * @param classFile the class file to transform.
     * @return the corresponding class.
     */
    public static String transformFileToClass(String classFile) {
        String className;
        if (checkFileExtension(classFile, CLASS_EXTENSION)) {
            className = classFile.substring(0, classFile.length() - CLASS_EXTENSION.length());
            className = className.replace('/', '.');
            return className;
        } else {
            return null;
        }
    }

    /**
     * Returns a sorted set of resource file names found within the
     * given <code>path</code>, whereas the given <code>prefix</code>
     * is added to found resources.
     *
     * (1) If the given <code>path</code> denotes a directory, this 
     *     method recursively calls itself to descend into every single 
     *     subdirectory. The flag <code>inspectJars</code> is
     *     automatically disabled with the first level of recursion.
     * 
     * (2) If the given <code>path</code> denotes a ZIP or JAR archive, 
     *     and <code>inpectJars</code> is set, this method inspects the 
     *     archive for resource files.
     * 
     * (3) If the given <code>path</code> denotes a file this file
     *     is added to the set.
     *
     * Set <code>prefix</code> to <code>null</code> to indicate the first
     * call of this method with a given class path (This ensures, that the 
     * class path name itself is not used as prefix).
     *
     * @param prefix The prefix to be added to resource file names. 
     * @param path A class path to search for resource files.
     * @param onlyClasses If <code>true</code> only resource files are 
     *   returned, which denote a class file with the appropriate file 
     *   extension.
     * @param inspectJars If <code>true</code> and the path denotes
     *   a ZIP or JAR archive, this archive is inspected for classes.
     * @return A sorted set of resource file names beginning with 
     *   the given prefix.
     */
    protected static SortedSet getResourceNames0(String prefix, File path, boolean onlyClasses, boolean inspectJars) {
        ZipInputStream zipIn;
        ZipEntry zipEntry;
        SortedSet resources;
        SortedSet set;
        String file;
        File[] files;
        int pos;
        int i;
        resources = new TreeSet();
        zipIn = null;
        if (path.isDirectory()) {
            try {
                files = path.listFiles();
                if (prefix != null) {
                    prefix = prefix + path.getName() + "/";
                } else {
                    prefix = "";
                }
                for (i = 0; i < files.length; i++) {
                    set = getResourceNames0(prefix, files[i], onlyClasses, false);
                    resources.addAll(set);
                }
            } catch (Throwable t) {
            }
            return resources;
        }
        file = path.getName();
        if (prefix == null) {
            prefix = "";
        }
        if (inspectJars && (checkFileExtension(file, ZIP_EXTENSION) || checkFileExtension(file, JAR_EXTENSION))) {
            try {
                zipIn = new ZipInputStream(new FileInputStream(path));
                while ((zipEntry = zipIn.getNextEntry()) != null) {
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    file = zipEntry.getName();
                    if (onlyClasses) {
                        if (checkFileExtension(file, CLASS_EXTENSION)) {
                            resources.add(file);
                        }
                    } else {
                        resources.add(file);
                    }
                }
            } catch (Throwable t) {
            } finally {
                try {
                    zipIn.close();
                } catch (Exception e) {
                }
            }
        } else {
            if (onlyClasses) {
                if (checkFileExtension(file, CLASS_EXTENSION)) {
                    resources.add(prefix + file);
                }
            } else {
                resources.add(prefix + file);
            }
        }
        return resources;
    }

    /** 
     * Returns a sorted set of resource file names found within 
     * the given class <code>path</code>, including the relative
     * path within the class path as prefix.
     *
     * If the class path denotes a ZIP or JAR archive, this
     * archive is automatically inspected for resource files.
     *
     * @param path A class path to search for resource files.
     * @param onlyClasses If <code>true</code> only resource files are 
     *   returned, which denote a class file with the appropriate file 
     *   extension.
     * @return A sorted set of resource file names.
     */
    public static SortedSet getResourceNames(File path, boolean onlyClasses) {
        return getResourceNames0(null, path, onlyClasses, true);
    }

    /**
     * Returns <code>true</code>, if the given <code>source1</code>
     * is of type <code>source2</code>. Otherwise, <code>false</code>
     * is returned.
     *
     * @param source1 the source to check.
     * @param source2 the source type to compare with.
     * @return <code>true</code>, iff the given <code>source1</code>
     * is of type <code>source2</code>.
     */
    public static boolean checkSource(int source, int sourcemask) {
        if (sourcemask == SOURCE_NOT_FOUND) {
            return (source == SOURCE_NOT_FOUND);
        }
        if (sourcemask == SOURCE_ALL) {
            return (source != SOURCE_NOT_FOUND);
        }
        return ((source & sourcemask) == sourcemask);
    }

    /**
     * Returns the URL of the system class with the given 
     * <code>className</code>, or <code>null</code> if the
     * given class is not found.
     *
     * This method makes use of the static method 
     * <code>ClassLoader.getSystemResource</code>, and thereby
     * returns the URL of the class file corresponding
     * to the given class name.
     *
     * The class file is searched in the following order within 
     * Java's boot class path, the class archives in Java's extension 
     * directories, and the current system class path 
     * (compare Java's System Properties <code>sun.boot.class.path</code>, 
     * <code>java.ext.dirs</code>, and <code>java.class.path</code>).
     *
     * @param className the fully qualified system class,
     *   including package prefix with dots ('.') as separators.
     * @return the corresponding URL of the class file.
     */
    public static URL systemClassUrl(String className) {
        return systemResourceUrl(transformClassToFile(className));
    }

    /** 
     * Returns the URL of the system resource with the given 
     * <code>resourceFile</code> name, or <code>null</code> if the
     * given resource file is not found.
     *
     * This method makes use of the static method 
     * <code>ClassLoader.getSystemResource</code>, and thereby
     * returns the URL of the resource file corresponding
     * to the given name.
     *
     * The resource file is searched in the following order within 
     * Java's boot class path, the class archives in Java's extension 
     * directories, and the current system class path 
     * (compare Java's System Properties <code>sun.boot.class.path</code>, 
     * <code>java.ext.dirs</code>, and <code>java.class.path</code>).
     *
     * @param resourceFile The name of the system resource, given as 
     *   relative resource file path with slashes ('/') as sesparators.
     * @return the corresponding URL of the resource file.
     */
    public static URL systemResourceUrl(String resourceFile) {
        if (resourceFile == null) {
            return null;
        }
        return ClassLoader.getSystemResource(resourceFile);
    }

    /**
     * Returns the source of the system class with the given 
     * <code>className</code>. 
     * 
     * Depending on the corresponding class URL (see 
     * <code>systemClassUrl()</code>, its source is derived
     * by comparing this URL with the different possible
     * system class sources in the following order:
     *
     * (1) Return <code>SOURCE_BOOT_CLASSPATH</code>, if the class 
     *     can be found within Java's boot class path.
     *
     * (2) Return <code>SOURCE_SYSTEM_JARFILES</code>, if the class 
     *     can be found within an archive in Java's extension 
     *     directories.
     *
     * (3) Return <code>SOURCE_SYSTEM_CLASSPATH</code>, if the class 
     *     can be found within Java's system class path.
     *
     * If the class cannot be found within these three system sources,
     * this method returns <code>SOURCE_NOT_FOUND</code>.
     *
     * @param className the fully qualified system class,
     *   including package prefix with dots ('.') as separators.
     * @return the corresponding source of the class file.
     */
    public static int systemClassSource(String className) {
        return systemResourceSource(transformClassToFile(className));
    }

    /** 
     * Returns the source of the system resource file with the given 
     * <code>resourceFile</code> name. 
     * 
     * Depending on the corresponding resource URL (see 
     * <code>systemResourceUrl()</code>, its source is derived
     * by comparing this URL with the different possible
     * system resource sources in the following order:
     *
     * (1) Return <code>SOURCE_BOOT_CLASSPATH</code>, if the resource
     *     can be found within Java's boot class path.
     *
     * (2) Return <code>SOURCE_SYSTEM_JARFILES</code>, if the resource
     *     can be found within an archive in Java's extension 
     *     directories.
     *
     * (3) Return <code>SOURCE_SYSTEM_CLASSPATH</code>, if the resource
     *     can be found within Java's system class path.
     *
     * If the resource file cannot be found within these three system 
     * sources, this method returns <code>SOURCE_NOT_FOUND</code>.
     *
     * @param resourceFile The name of the system resource, given as 
     *   relative resource file path with slashes ('/') as sesparators.
     * @return the corresponding source of the resource file.
     */
    public static int systemResourceSource(String resourceFile) {
        String url;
        URL resourceUrl;
        int resourceFileLen;
        int pos;
        int i;
        resourceUrl = systemResourceUrl(resourceFile);
        if (resourceUrl == null) {
            return SOURCE_NOT_FOUND;
        }
        url = resourceUrl.toString();
        resourceFileLen = resourceFile.length();
        if (url.startsWith(ARCHIVE_URL_PROTOCOL + ":")) {
            url = url.substring(ARCHIVE_URL_PROTOCOL.length() + 1);
        }
        if (url.startsWith(FILE_URL_PROTOCOL + ":")) {
            url = url.substring(FILE_URL_PROTOCOL.length() + 1);
        }
        if (url.startsWith("//")) {
            url = url.substring(2);
        }
        pos = url.indexOf("!");
        if (pos != -1) {
            url = url.substring(0, pos);
        } else {
            url = url.substring(0, url.length() - (resourceFileLen + 1));
        }
        for (i = 0; i < bootClassPaths_.length; i++) {
            if (url.equals(bootClassPaths_[i])) {
                return SOURCE_BOOT_CLASSPATH;
            }
        }
        for (i = 0; i < systemJarFiles_.length; i++) {
            if (url.equals(systemJarFiles_[i])) {
                return SOURCE_SYSTEM_JARFILE;
            }
        }
        for (i = 0; i < systemClassPaths_.length; i++) {
            if (url.equals(systemClassPaths_[i])) {
                return SOURCE_SYSTEM_CLASSPATH;
            }
        }
        return SOURCE_NOT_FOUND;
    }

    /**
     * Returns the <code>InputStream</code> of the class file corresponding
     * to the system class with the given <code>className</code>, or 
     * <code>null</code> if the given class is not found.
     * 
     * This method makes use of <code>systemClassUrl()</code>, and 
     * subsequently return the input stream according to the class URL.
     *
     * @param className the fully qualified system class,
     *   including package prefix with dots ('.') as separators.
     * @return the input stream of the class file.
     */
    public static InputStream systemClassInputStream(String className) {
        return systemResourceInputStream(transformClassToFile(className));
    }

    /**
     * Returns the <code>InputStream</code> of the resource file 
     * corresponding to the system resource with the given 
     * <code>resourceFile</code> name, or <code>null</code> if the 
     * given resource file is not found.
     * 
     * This method makes use of <code>systemResourceUrl()</code>, and 
     * subsequently return the input stream according to the resoucre URL.
     *
     * @param resourceFile The name of the system resource, given as 
     *   relative resource file path with slashes ('/') as sesparators.
     * @return the input stream of the resource file.
     */
    public static InputStream systemResourceInputStream(String resourceFile) {
        ZipInputStream zipIn;
        ZipEntry zipEntry;
        String zipFile;
        String url;
        URL resourceUrl;
        int pos;
        resourceUrl = systemResourceUrl(resourceFile);
        if (resourceUrl == null) {
            return null;
        }
        if (!resourceUrl.getProtocol().equalsIgnoreCase(ARCHIVE_URL_PROTOCOL)) {
            try {
                return resourceUrl.openStream();
            } catch (Throwable t) {
                return null;
            }
        }
        url = resourceUrl.toString().substring(4);
        pos = url.indexOf("!");
        if (pos == -1) {
            return null;
        }
        zipFile = url.substring(pos + 1);
        url = url.substring(0, pos);
        if (zipFile.startsWith("/")) {
            zipFile = zipFile.substring(1);
        }
        try {
            resourceUrl = new URL(url);
        } catch (Throwable t) {
            return null;
        }
        try {
            zipIn = new ZipInputStream(resourceUrl.openStream());
        } catch (Throwable t) {
            return null;
        }
        try {
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                if (zipEntry.getName().equals(zipFile)) {
                    return zipIn;
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    /**
     * Returns a sorted set containing the fully qualified names of all
     * system classes available within the given <code>classSource</code>s.
     *
     * The class source is specified as binary OR ('|') concatenation of
     * any of the three system class sources:
     * 
     * <code>SOURCE_BOOT_CLASSPATH</code>,
     * <code>SOURCE_SYSTEM_JARFILES</code>, 
     * and <code>SOURCE_SYSTEM_CLASSPATH</code>.
     * 
     * Since this method recursively inspects all given system class sources
     * it may take some seconds before the method returns.
     *
     * @param classSource The class sources to inspect.
     * @return the sorted set of fully qualified class names.
     */
    public SortedSet getSystemClassNames(int classSource) {
        SortedSet resources;
        SortedSet classes;
        Iterator it;
        String resource;
        resources = getSystemResourceNames0(classSource, true);
        if (resources == null) {
            return null;
        }
        classes = new TreeSet();
        for (it = resources.iterator(); it.hasNext(); ) {
            resource = (String) it.next();
            classes.add(transformFileToClass(resource));
        }
        return classes;
    }

    /** 
     * Returns a sorted set containing the fully qualified resource 
     * files names of all system resources available within the given 
     * <code>resourceSource</code>s.
     *
     * The resource source is specified as binary OR ('|') concatenation of
     * any of the three system resource sources:
     * 
     * <code>SOURCE_BOOT_CLASSPATH</code>,
     * <code>SOURCE_SYSTEM_JARFILES</code>, 
     * and <code>SOURCE_SYSTEM_CLASSPATH</code>.
     * 
     * Since this method recursively inspects all given system resource sources
     * it may take some seconds before the method returns.
     *
     * @param classSource The resource sources to inspect.
     * @return the sorted set of fully qualified resource file names.
     */
    public SortedSet getSystemResourceNames(int resourceSource) {
        return getSystemResourceNames0(resourceSource, false);
    }

    /**
     * Returns a sorted set containing the fully qualified resource
     * names of all system resources available within the given 
     * <code>resourceSource</code>s.
     *
     * The resource source is specified as binary OR ('|') concatenation of
     * any of the three system resource sources:
     * 
     * <code>SOURCE_BOOT_CLASSPATH</code>,
     * <code>SOURCE_SYSTEM_JARFILES</code>, 
     * and <code>SOURCE_SYSTEM_CLASSPATH</code>.
     * 
     * Since this method recursively inspects all given system resource sources
     * it may take some seconds before the method returns.
     *
     * @param onlyClasses If <code>true</code> only resource files are 
     *   returned, which denote a class file with the appropriate file 
     *   extension.
     * @param resourceSource The resouce sources to inspect.
     * @return the sorted set of fully qualified resource file names.
     */
    protected SortedSet getSystemResourceNames0(int resourceSource, boolean onlyClasses) {
        SortedSet resources;
        SortedSet set;
        String file;
        String name;
        int i;
        resources = new TreeSet();
        if ((resourceSource & SOURCE_BOOT_CLASSPATH) == SOURCE_BOOT_CLASSPATH) {
            for (i = 0; i < bootClassPaths_.length; i++) {
                set = getResourceNames(new File(bootClassPaths_[i]), onlyClasses);
                resources.addAll(set);
            }
        }
        if ((resourceSource & SOURCE_SYSTEM_JARFILE) == SOURCE_SYSTEM_JARFILE) {
            for (i = 0; i < systemJarFiles_.length; i++) {
                set = getResourceNames(new File(systemJarFiles_[i]), onlyClasses);
                resources.addAll(set);
            }
        }
        if ((resourceSource & SOURCE_SYSTEM_CLASSPATH) == SOURCE_SYSTEM_CLASSPATH) {
            for (i = 0; i < systemClassPaths_.length; i++) {
                set = getResourceNames(new File(systemClassPaths_[i]), onlyClasses);
                resources.addAll(set);
            }
        }
        return resources;
    }

    /**
     * Default constructor to create a new <code>ClassSource</code> instance.
     * 
     * Neither user class path, user class URL, nor user class resource is set.
     */
    public ClassSource() {
        this((String) null, null, null);
    }

    /**
     * Creates a new <code>ClassSource</code> instance and sets the 
     * given user class paths, user class URLs and user class resource.
     *
     * Single parameters may be <code>null</code>, if the corresponding
     * class variables should not be initialized, yet.
     *
     * @param classpath The user class paths as concatenation of single class 
     *   paths separated by the current <code>File.separator</code>.
     * @param resource The user class resource.
     * @param urls The user class URLs as array of single class URLs.
     */
    public ClassSource(String classpath, Resource resource, URL[] urls) {
        setUserClassPath(classpath);
        setUserClassResource(resource);
        setUserClassUrls(urls);
    }

    /**
     * Creates a new <code>ClassSource</code> instance and sets the 
     * given user class paths, user class URLs and user class resource.
     *
     * Single parameters may be <code>null</code>, if the corresponding
     * class variables should not be initialized, yet.
     *
     * @param classpaths The user class paths as array of single class paths.
     * @param resource The user class resource.
     * @param urls The user class URLs as array of single class URLs.
     */
    public ClassSource(String[] classpaths, Resource resource, URL[] urls) {
        setUserClassPaths(classpaths);
        setUserClassResource(resource);
        setUserClassUrls(urls);
    }

    /**
     * Sets the given user <code>classpath</code>s.
     *
     * This method overwrites previous settings of user class paths.
     * If <code>classpath</code> is <code>null</code>, previously
     * set user class paths are deleted.
     * 
     * @param classpath The user class paths as concatenation of single class 
     *   paths separated by the current <code>File.separator</code>.
     */
    public void setUserClassPath(String classpath) {
        StringTokenizer st;
        ArrayList paths;
        if (classpath == null) {
            userClassPaths_ = null;
            userClassPathUrls_ = null;
            return;
        }
        st = new StringTokenizer(classpath, File.pathSeparator);
        paths = new ArrayList();
        while (st.hasMoreTokens()) {
            paths.add(st.nextToken());
        }
        setUserClassPaths((String[]) paths.toArray(new String[0]));
    }

    /**
     * Sets the given user <code>classpaths</code>.
     *
     * This method overwrites previous settings of user class paths.
     * If <code>classpaths</code> is <code>null</code>, previously
     * set user class paths are deleted.
     * 
     * @param classpaths The user class paths as array of single class paths.
     */
    public void setUserClassPaths(String[] classpaths) {
        ArrayList paths;
        ArrayList urls;
        String path;
        URL url;
        int i;
        if (classpaths == null) {
            userClassPaths_ = null;
            userClassPathUrls_ = null;
            return;
        }
        paths = new ArrayList();
        urls = new ArrayList();
        for (i = 0; i < classpaths.length; i++) {
            path = canonicalClassPath(classpaths[i]);
            paths.add(path);
            url = canonicalClassUrl(path);
            if (url != null) {
                urls.add(url);
            }
        }
        userClassPaths_ = (String[]) paths.toArray(new String[0]);
        userClassPathUrls_ = (URL[]) urls.toArray(new URL[0]);
    }

    /**
     * Sets the given user class <code>resource</code>.
     *
     * This method overwrites previous settings of the user class resource.
     * If <code>resource</code> is <code>null</code>, a previously set user 
     * class resource is deleted.
     * 
     * @param resource The user class resource.
     */
    public void setUserClassResource(Resource resource) {
        userClassResource_ = resource;
    }

    /**
     * Sets the given user class <code>URL</code>s.
     *
     * This method overwrites previous settings of user class URLs.
     * If <code>urls</code> is <code>null</code>, previously set 
     * user class URLs are deleted.
     * 
     * @param urls The user class URLs as array of single class URLs.
     */
    public void setUserClassUrls(URL[] urls) {
        ArrayList ar;
        URL url;
        int i;
        if (urls == null) {
            userClassUrls_ = null;
            return;
        }
        ar = new ArrayList();
        for (i = 0; i < urls.length; i++) {
            url = canonicalClassUrl(urls[i]);
            if (url != null) {
                ar.add(url);
            }
        }
        userClassUrls_ = (URL[]) ar.toArray(new URL[0]);
    }

    /**
     * Returns the URL of the user class with the given 
     * <code>className</code> as <code>String</code>, or 
     * <code>null</code> if the given class is not found.
     *
     * This method makes use of the <code>URLClassLoader</code>
     * to locate classes denoted through the user class paths or 
     * the user class URLs. Further the user class resource is
     * inspected for a class file corresponding to the given
     * class name.
     *
     * The class file is searched in the following order within
     * the user class path, the user class resource, and the
     * user class URLs.
     *
     * If the class file is found within the user class resource
     * the URL's syntax if equal a file-URL, except the protocol 
     * is <code>resource:</code>.
     *
     * @param className the fully qualified user class,
     *   including package prefix with dots ('.') as separators.
     * @return the corresponding URL of the class file as <code>String</code>.
     */
    public String userClassUrl(String className) {
        return userResourceUrl(transformClassToFile(className));
    }

    /** 
     * Returns the URL of the user resource file with the given 
     * <code>resourceFile</code> name as <code>String</code>, or 
     * <code>null</code> if the given resouce file is not found.
     *
     * This method makes use of the <code>URLClassLoader</code>
     * to locate resources denoted through the user class paths or 
     * the user class URLs. Further the user class resource is
     * inspected for a resource file corresponding to the given
     * resource file name.
     *
     * The resource file is searched in the following order within
     * the user class path, the user class resource, and the
     * user class URLs.
     *
     * If the resource file is found within the user class resource
     * the URL's syntax is equal to a file-URL, except that the 
     * protocol is <code>resource:</code>.
     *
     * @param resourceFile The name of the system resource file, given as 
     *   relative resource file path with slashes ('/') as sesparators.
     * @return the corresponding URL of the resource file name 
     *   as <code>String</code>.
     */
    public String userResourceUrl(String resourceFile) {
        URLClassLoader ucl;
        InputStream rin;
        URL url;
        if (resourceFile == null) {
            return null;
        }
        if (userClassPathUrls_ != null) {
            ucl = new URLClassLoader(userClassPathUrls_, dummyClassLoader_);
            try {
                rin = ucl.getResourceAsStream(resourceFile);
                url = ucl.getResource(resourceFile);
                if (url != null) {
                    return url.toString();
                }
            } catch (Throwable t) {
            }
        }
        if (userClassResource_ != null) {
            if (userClassResource_.exists(resourceFile)) {
                return RESOURCE_URL_PROTOCOL + ":" + resourceFile;
            }
        }
        if (userClassUrls_ != null) {
            ucl = new URLClassLoader(userClassUrls_, dummyClassLoader_);
            try {
                rin = ucl.getResourceAsStream(resourceFile);
                url = ucl.getResource(resourceFile);
                if (url != null) {
                    return url.toString();
                }
            } catch (Throwable t) {
            }
        }
        return null;
    }

    /**
     * Returns the source of the user class with the given 
     * <code>className</code>. 
     * 
     * Depending on the corresponding class URL (see 
     * <code>userClassUrl()</code>, its source is derived
     * by comparing this URL with the different possible
     * user class sources in the following order:
     *
     * (1) Return <code>SOURCE_USER_LOCALCLASS</code>, if the class 
     *     can be found within the user class path or within a
     *     user class URL denoting a local class by using the 
     *     <code>file:</code> protocol.
     *
     * (2) Return <code>SOURCE_USER_CLASSRESOURCE</code>, if the class 
     *     can be found within the user class resource.
     *
     * (3) Return <code>SOURCE_USER_REMOTECLASS</code>, if the class 
     *     can be found within a user class URL, which is not 
     *     denoting a local class by using the <code>fie:</code>
     *     protocol.
     *
     * If the class cannot be found within these three user sources,
     * this method returns <code>SOURCE_NOT_FOUND</code>.
     *
     * @param className the fully qualified user class,
     *   including package prefix with dots ('.') as separators.
     * @return the corresponding source of the class file.
     */
    public int userClassSource(String className) {
        return userResourceSource(transformClassToFile(className));
    }

    /**
     * Returns the source of the user resource file with the given 
     * <code>resourceFile</code> name. 
     * 
     * Depending on the corresponding resource URL (see 
     * <code>userResourceUrl()</code>, its source is derived
     * by comparing this URL with the different possible
     * user resource sources in the following order:
     *
     * (1) Return <code>SOURCE_USER_LOCALCLASS</code>, if the resource 
     *     file can be found within the user class path or within a
     *     user class URL denoting a local resource by using the 
     *     <code>file:</code> protocol.
     *
     * (2) Return <code>SOURCE_USER_CLASSRESOURCE</code>, if the 
     *     resource file can be found within the user class resource.
     *
     * (3) Return <code>SOURCE_USER_REMOTECLASS</code>, if the resource
     *     file can be found within a user class URL, which is not 
     *     denoting a local resource by using the <code>fie:</code>
     *     protocol.
     *
     * If the resource file cannot be found within these three user 
     * sources, this method returns <code>SOURCE_NOT_FOUND</code>.
     *
     * @param resourceFile The name of the system resource, given as 
     *   relative resource file path with slashes ('/') as sesparators.
     * @return the corresponding source of the resource file.
     */
    public int userResourceSource(String resourceFile) {
        String url;
        url = userResourceUrl(resourceFile);
        if (url == null) {
            return SOURCE_NOT_FOUND;
        }
        if (url.startsWith(ARCHIVE_URL_PROTOCOL + ":")) {
            url = url.substring(ARCHIVE_URL_PROTOCOL.length() + 1);
        }
        if (url.startsWith(FILE_URL_PROTOCOL + ":")) {
            return SOURCE_USER_LOCALCLASS;
        }
        if (url.startsWith(RESOURCE_URL_PROTOCOL + ":")) {
            return SOURCE_USER_CLASSRESOURCE;
        }
        return SOURCE_USER_REMOTECLASS;
    }

    /**
     * Returns the <code>InputStream</code> of the class file corresponding
     * to the user class with the given <code>className</code>, or 
     * <code>null</code> if the given class is not found.
     * 
     * This method makes use of <code>userClassUrl()</code>, and 
     * subsequently returns the input stream according to the class URL.
     *
     * @param className the fully qualified user class,
     *   including package prefix with dots ('.') as separators.
     * @return the input stream of the class file.
     */
    public InputStream userClassInputStream(String className) {
        return userResourceInputStream(transformClassToFile(className));
    }

    /**
     * Returns the <code>InputStream</code> of the resource file corresponding
     * to the user resource with the given <code>resourceFile</code> name, or 
     * <code>null</code> if the given resource is not found.
     * 
     * This method makes use of <code>userResourceUrl()</code>, and 
     * subsequently returns the input stream according to the resource URL.
     *
     * @param resourceFile The name of the user resource file, given as 
     *   relative resource file path with slashes ('/') as sesparators.
     * @return the input stream of the resource file.
     */
    public InputStream userResourceInputStream(String resourceFile) {
        ZipInputStream zipIn;
        InputStream in;
        ZipEntry zipEntry;
        String zipFile;
        String url;
        URL resourceUrl;
        int pos;
        url = userResourceUrl(resourceFile);
        if (url == null) {
            return null;
        }
        if (url.startsWith(RESOURCE_URL_PROTOCOL + ":")) {
            try {
                in = userClassResource_.getInputStream(resourceFile);
                return in;
            } catch (Throwable t) {
                return null;
            }
        }
        try {
            resourceUrl = new URL(url);
        } catch (Throwable t) {
            return null;
        }
        if (!resourceUrl.getProtocol().equalsIgnoreCase(ARCHIVE_URL_PROTOCOL)) {
            try {
                return resourceUrl.openStream();
            } catch (Throwable t) {
                return null;
            }
        }
        url = resourceUrl.toString().substring(4);
        pos = url.indexOf("!");
        if (pos == -1) {
            return null;
        }
        zipFile = url.substring(pos + 1);
        url = url.substring(0, pos);
        if (zipFile.startsWith("/")) {
            zipFile = zipFile.substring(1);
        }
        try {
            resourceUrl = new URL(url);
        } catch (Throwable t) {
            return null;
        }
        try {
            zipIn = new ZipInputStream(resourceUrl.openStream());
        } catch (Throwable t) {
            return null;
        }
        try {
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                if (zipEntry.getName().equals(zipFile)) {
                    return zipIn;
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    /**
     * Returns a sorted set containing the fully qualified names of all
     * user classes available within the given <code>classSource</code>s.
     *
     * The class source is specified as binary OR ('|') concatenation of
     * any of the three user class sources: 
     * 
     * <code>SOURCE_USER_LOCALCLASS</code>,
     * <code>SOURCE_USER_CLASSRESOURCE</code>, 
     * and <code>SOURCE_USER_REMOTECLASS</code>.
     * 
     * In case of remote classes, it is only possible to inspect ZIP
     * or JAR archives denoted through a user class URL.
     * 
     * Since this method recursively inspects all given user class sources
     * it may take some seconds before the method returns.
     *
     * @param classSource The class sources to inspect.
     * @return the sorted set of of fully qualified class names.
     */
    public SortedSet getUserClassNames(int classSource) {
        SortedSet resources;
        SortedSet classes;
        Iterator it;
        String resource;
        resources = getUserResourceNames0(classSource, true);
        if (resources == null) {
            return null;
        }
        classes = new TreeSet();
        for (it = resources.iterator(); it.hasNext(); ) {
            resource = (String) it.next();
            classes.add(transformFileToClass(resource));
        }
        return classes;
    }

    /**
     * Returns a sorted set containing the fully qualified resource file 
     * names of all user resources available within the given 
     * <code>resourceSource</code>s.
     *
     * The resource source is specified as binary OR ('|') concatenation of
     * any of the three user resource sources: 
     * 
     * <code>SOURCE_USER_LOCALCLASS</code>,
     * <code>SOURCE_USER_CLASSRESOURCE</code>, 
     * and <code>SOURCE_USER_REMOTECLASS</code>.
     * 
     * In case of remote resource, it is only possible to inspect ZIP
     * or JAR archives denoted through a user resource URL.
     * 
     * Since this method recursively inspects all given user resource sources
     * it may take some seconds before the method returns.
     *
     * @param resourceSource The resource sources to inspect.
     * @return the sorted set of of fully qualified resource file names.
     */
    public SortedSet getUserResourceNames(int resourceSource) {
        return getUserResourceNames0(resourceSource, false);
    }

    /**
     * Returns a sorted set containing the fully qualified resource file 
     * names of all user resource files classes available within the 
     * given <code>resourceSource</code>s.
     *
     * The resource source is specified as binary OR ('|') concatenation of
     * any of the three user resources sources: 
     * 
     * <code>SOURCE_USER_LOCALCLASS</code>,
     * <code>SOURCE_USER_CLASSRESOURCE</code>, 
     * and <code>SOURCE_USER_REMOTECLASS</code>.
     * 
     * In case of remote resources, it is only possible to inspect ZIP
     * or JAR archives denoted through a user resource URL.
     * 
     * Since this method recursively inspects all given user resource sources
     * it may take some seconds before the method returns.
     *
     * @param resourceSource The resource sources to inspect.
     * @return the sorted set of of fully qualified resource file names.
     */
    public SortedSet getUserResourceNames0(int resourceSource, boolean onlyClasses) {
        ZipInputStream zipIn;
        SortedSet resources;
        SortedSet set;
        ZipEntry zipEntry;
        Iterator it;
        String name;
        URL url;
        int i;
        resources = new TreeSet();
        zipIn = null;
        if ((resourceSource & SOURCE_USER_LOCALCLASS) == SOURCE_USER_LOCALCLASS) {
            for (i = 0; userClassPaths_ != null && i < userClassPaths_.length; i++) {
                set = getResourceNames(new File(userClassPaths_[i]), onlyClasses);
                resources.addAll(set);
            }
            for (i = 0; userClassUrls_ != null && i < userClassUrls_.length; i++) {
                if (userClassUrls_[i].getProtocol().equalsIgnoreCase(FILE_URL_PROTOCOL)) {
                    set = getResourceNames(new File(userClassUrls_[i].getFile()), onlyClasses);
                    resources.addAll(set);
                }
            }
        }
        if ((resourceSource & SOURCE_USER_CLASSRESOURCE) == SOURCE_USER_CLASSRESOURCE) {
            try {
                for (it = userClassResource_.list().iterator(); it.hasNext(); ) {
                    name = (String) it.next();
                    if (onlyClasses) {
                        if (checkFileExtension(name, CLASS_EXTENSION)) {
                            resources.add(name);
                        }
                    } else {
                        resources.add(name);
                    }
                }
            } catch (Throwable t) {
            }
        }
        if ((resourceSource & SOURCE_USER_REMOTECLASS) == SOURCE_USER_REMOTECLASS) {
            for (i = 0; userClassUrls_ != null && i < userClassUrls_.length; i++) {
                url = userClassUrls_[i];
                if (!url.getProtocol().equalsIgnoreCase(FILE_URL_PROTOCOL) && (checkFileExtension(url.getFile(), JAR_EXTENSION) || checkFileExtension(url.getFile(), ZIP_EXTENSION))) {
                    try {
                        zipIn = new ZipInputStream(url.openStream());
                        while ((zipEntry = zipIn.getNextEntry()) != null) {
                            if (zipEntry.isDirectory()) {
                                continue;
                            }
                            name = zipEntry.getName();
                            if (onlyClasses) {
                                if (checkFileExtension(name, CLASS_EXTENSION)) {
                                    resources.add(name);
                                }
                            } else {
                                resources.add(name);
                            }
                        }
                    } catch (Throwable t) {
                    } finally {
                        try {
                            zipIn.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return resources;
    }

    /**
     * Return the hexadecimal representation of the given integer 
     * <code>hex</code> as string, using <code>n</code> as maximum 
     * string length to indent the string with <code>0</code>s up 
     * this given length.
     *
     * @param hex The integer to transform.
     * @param n The maximum string length.
     * @return the hexadecimal representation of <code>hex</code> as string.
     */
    protected static String indentHex(int hex, int n) {
        String result;
        String str;
        int mask;
        int i;
        if (n > 8) {
            n = 8;
        }
        for (mask = 1, i = 0; i < n; i++) {
            mask *= 0x10;
        }
        mask--;
        str = Integer.toHexString(hex & mask);
        for (result = "", i = str.length(); i < n; i++) {
            result += "0";
        }
        result += str;
        return result;
    }

    /**
     * Returns the hex dump of the given <code>data</code> as string.
     *
     * Each line containes the hex dump of 16 bytes of data in the
     * following syntax:
     * <pre>
     * aaaaaaaa xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx xx cccccccccccccccc
     * </pre>
     * with <code>aaaaaaaa</code> as offset of the first byte, 16 times 
     * <code>xx</code> as hexadecimal string representation of the single 
     * bytes, and 16 times <code>c</code> as character representation of 
     * the single bytes.
     * 
     * If the chararcter representation of a byte is not a letter or digit,
     * it is represented as dot ('.').
     *
     * @param data The data as byte array.
     * @return the data's hex dump.
     */
    protected static String hexDump(byte[] data) {
        StringBuffer charbuf;
        StringBuffer hexbuf;
        StringBuffer strbuf;
        String separator;
        String addr;
        String hex;
        int i;
        strbuf = new StringBuffer();
        separator = ".";
        charbuf = new StringBuffer();
        hexbuf = new StringBuffer();
        addr = "00000000";
        for (i = 0; i < data.length; i++) {
            if (i != 0 && (i % 16) == 0) {
                strbuf.append(addr);
                strbuf.append("   ");
                strbuf.append(hexbuf.toString());
                strbuf.append("  ");
                strbuf.append(charbuf.toString());
                strbuf.append("\n");
                charbuf = new StringBuffer();
                hexbuf = new StringBuffer();
                addr = indentHex(i, 8);
            }
            hexbuf.append(indentHex((int) data[i], 2) + " ");
            if (Character.isLetterOrDigit((char) data[i])) {
                charbuf.append((char) data[i]);
            } else {
                charbuf.append(separator);
            }
        }
        if (charbuf.length() > 0) {
            while ((i % 16) != 0) {
                hexbuf.append("   ");
                i++;
            }
            strbuf.append(addr);
            strbuf.append("   ");
            strbuf.append(hexbuf.toString());
            strbuf.append("  ");
            strbuf.append(charbuf.toString());
            strbuf.append("\n");
        }
        return strbuf.toString();
    }

    /**
     * Prints a help text about the command line option of the 
     * main method.
     */
    protected static void printHelp() {
        System.out.println("\n  USAGE: java " + ClassSource.class.getName() + " <options>\n");
        System.out.println("  Options:\n");
        System.out.println("   -help                               " + "Displays this help message");
        System.out.println();
        System.out.println("   -userclasspath <class_path>         " + "Set system dependent <user_class_path>\n" + "                                       " + "as user class path");
        System.out.println("   -userclassresource <file_resource>  " + "Set <file_resource> (type: ZIP or JAR)\n" + "                                       " + "as user class resource");
        System.out.println("   -userclassurls (<class_url> ' ')*   " + "Set the <user_url>s as user class URLs");
        System.out.println();
        System.out.println("   -resource                           " + "Flag to toggle between class names and\n" + "                                       " + "resource files for input and output");
        System.out.println("   -verbose                            " + "Displays all configured class paths/URLs");
        System.out.println("   -debug                              " + "Displays all available classes");
        System.out.println();
        System.out.println("   -search <name>                      " + "Search class <name> in the system and\n" + "                                       " + "given user sources");
        System.out.println("   -hex                                " + "Print hex dump of found class");
        System.out.println();
    }

    /**
     * The main method of this class, which parses the following 
     * command line paramters:
     *
     * <ul>
     * <li><code>-help</code> Displays a short help message.
     * <li><code>-verbose</code> Displays all configured class paths/URLs.
     * <li><code>-debug</code> Displays all available classes.
     * <li><code>-hex</code> Print hex dump of selected class.
     * <li><code>-class &lt;class_name&gt;</code> Search class 
     *   &lt;class_name&gt; in the system and given user sources.
     * <li><code>-userclasspath &lt;class_path&gt;</code> Set 
     *   &lt;user_class_path&gt; containing single class paths 
     *   concatenated with the current <code>File.separator</code>.
     * <li><code>-userclassresource &lt;file_resource&gt;</code> 
     *   Set &lt;file_resource&gt; (type: ZIP or JAR) as user
     *   class resource.
     * <li><code>-userclassurls (&lt;class_url&gt; ' ')*</code> Set 
     *   the &lt;user_url&gt;s as user class URLs.
     * </ul>
     *
     * @param argv The command line parameters.
     */
    public static void main(String[] argv) {
        ByteArrayOutputStream bos;
        de.fhg.igd.util.URL argUrl;
        StringTokenizer st;
        InputStream in;
        ClassSource cs;
        ArgsParser p;
        ArrayList ucu;
        ArrayList al;
        SortedSet sortedSet;
        Iterator it;
        Resource userClassResource;
        boolean resourceFlag;
        String urlStr;
        String sourceStr;
        String name;
        String ucp;
        String str;
        URL[] userClassUrls;
        URL url;
        int source;
        int i;
        try {
            cs = new ClassSource();
            p = new ArgsParser(options_);
            p.parse(argv);
            if (p.isDefined("help")) {
                printHelp();
                return;
            }
            if (p.isDefined("resource")) {
                resourceFlag = true;
            } else {
                resourceFlag = false;
            }
            if (p.isDefined("userclasspath")) {
                cs.setUserClassPath(p.stringValue("userclasspath"));
            }
            userClassResource = null;
            if (p.isDefined("userclassresource")) {
                try {
                    userClassResource = new MemoryResource();
                    Resources.unzip(new FileInputStream(p.stringValue("userclassresource")), userClassResource);
                    cs.setUserClassResource(userClassResource);
                } catch (Throwable t) {
                    userClassResource = null;
                }
            }
            if (p.isDefined("userclassurls")) {
                ucu = (ArrayList) p.value("userclassurls");
                al = new ArrayList();
                for (i = 0; i < ucu.size(); i++) {
                    try {
                        argUrl = (de.fhg.igd.util.URL) ucu.get(i);
                        if (argUrl.getProtocol().equalsIgnoreCase(FILE_URL_PROTOCOL)) {
                            url = new URL(FILE_URL_PROTOCOL, null, argUrl.getPath());
                        } else {
                            url = new URL(argUrl.toString());
                        }
                        al.add(url);
                    } catch (Throwable t) {
                    }
                }
                userClassUrls = (URL[]) al.toArray(new URL[0]);
            } else {
                userClassUrls = null;
            }
            cs.setUserClassUrls(userClassUrls);
            if (p.isDefined("verbose") || p.isDefined("debug")) {
                System.out.println();
                for (i = 0; i < ClassSource.bootClassPaths_.length; i++) {
                    System.out.println("[sun.boot.class.path] " + ClassSource.bootClassPaths_[i]);
                }
                System.out.println();
                for (i = 0; i < ClassSource.systemJarFiles_.length; i++) {
                    System.out.println("[java.ext.dirs] " + ClassSource.systemJarFiles_[i]);
                }
                System.out.println();
                for (i = 0; i < ClassSource.systemClassPaths_.length; i++) {
                    System.out.println("[java.class.path] " + ClassSource.systemClassPaths_[i]);
                }
                System.out.println();
                for (i = 0; cs.userClassPaths_ != null && i < cs.userClassPaths_.length; i++) {
                    System.out.println("[user.class.path] " + cs.userClassPaths_[i]);
                }
                System.out.println();
                if (userClassResource != null) {
                    System.out.println("[user.class.resource] " + p.stringValue("userclassresource"));
                }
                System.out.println();
                for (i = 0; cs.userClassUrls_ != null && i < cs.userClassUrls_.length; i++) {
                    System.out.println("[user.class.url] " + cs.userClassUrls_[i]);
                }
                System.out.println();
            }
            if (p.isDefined("debug")) {
                System.out.println();
                if (resourceFlag) {
                    System.out.println("List of all system resources:");
                    sortedSet = cs.getSystemResourceNames(SOURCE_ALL);
                } else {
                    System.out.println("List of all system classes:");
                    sortedSet = cs.getSystemClassNames(SOURCE_ALL);
                }
                for (it = sortedSet.iterator(); it.hasNext(); ) {
                    str = (String) it.next();
                    System.out.println("> " + str);
                }
                System.out.println();
                if (resourceFlag) {
                    System.out.println("List of all user resources:");
                    sortedSet = cs.getUserResourceNames(SOURCE_ALL);
                } else {
                    System.out.println("List of all user classes:");
                    sortedSet = cs.getUserClassNames(SOURCE_ALL);
                }
                for (it = sortedSet.iterator(); it.hasNext(); ) {
                    str = (String) it.next();
                    System.out.println("> " + str);
                }
                System.out.println();
            }
            if (p.isDefined("search")) {
                name = p.stringValue("search");
                if (resourceFlag) {
                    System.out.println("ResourceName   = " + name);
                    url = ClassSource.systemResourceUrl(name);
                    if (url == null) {
                        urlStr = cs.userResourceUrl(name);
                        System.out.println("ResourceUrl    = " + urlStr);
                    } else {
                        System.out.println("ResourceUrl    = " + url);
                    }
                    source = ClassSource.systemResourceSource(name);
                    source |= cs.userResourceSource(name);
                    System.out.print("ResourceSource = ");
                } else {
                    System.out.println("ClassName   = " + name);
                    url = ClassSource.systemClassUrl(name);
                    if (url == null) {
                        urlStr = cs.userClassUrl(name);
                        System.out.println("ClassUrl    = " + urlStr);
                    } else {
                        System.out.println("ClassUrl    = " + url);
                    }
                    source = ClassSource.systemClassSource(name);
                    source |= cs.userClassSource(name);
                    System.out.print("ClassSource = ");
                }
                sourceStr = "";
                if ((source & SOURCE_BOOT_CLASSPATH) == SOURCE_BOOT_CLASSPATH) {
                    sourceStr += " | <sun.boot.class.path>";
                }
                if ((source & SOURCE_SYSTEM_JARFILE) == SOURCE_SYSTEM_JARFILE) {
                    sourceStr += " | <java.ext.dirs>";
                }
                if ((source & SOURCE_SYSTEM_CLASSPATH) == SOURCE_SYSTEM_CLASSPATH) {
                    sourceStr += " | <java.class.path>";
                }
                if ((source & SOURCE_USER_LOCALCLASS) == SOURCE_USER_LOCALCLASS) {
                    sourceStr += " | <user.local.class>";
                }
                if ((source & SOURCE_USER_CLASSRESOURCE) == SOURCE_USER_CLASSRESOURCE) {
                    sourceStr += " | <user.class.resource>";
                }
                if ((source & SOURCE_USER_REMOTECLASS) == SOURCE_USER_REMOTECLASS) {
                    sourceStr += " | <user.remote.class>";
                }
                sourceStr = sourceStr.trim();
                if (sourceStr.length() != 0) {
                    sourceStr = sourceStr.substring(2);
                } else {
                    sourceStr = "<not_found>";
                }
                System.out.println(sourceStr);
                if (p.isDefined("hex")) {
                    if (source != SOURCE_NOT_FOUND) {
                        if (resourceFlag) {
                            System.out.println();
                            System.out.println("ResourceHexDump:\n");
                            in = ClassSource.systemResourceInputStream(name);
                            if (in == null) {
                                in = cs.userResourceInputStream(name);
                            }
                        } else {
                            System.out.println();
                            System.out.println("ClassHexDump:\n");
                            in = ClassSource.systemClassInputStream(name);
                            if (in == null) {
                                in = cs.userClassInputStream(name);
                            }
                        }
                        bos = new ByteArrayOutputStream();
                        Pipe.pipe(in, bos);
                        try {
                            in.close();
                        } catch (Throwable t) {
                        }
                        System.out.println(hexDump(bos.toByteArray()));
                    } else {
                        System.out.println();
                        System.out.println("HexDump: <none>\n");
                        System.out.println();
                    }
                }
            }
        } catch (Throwable t) {
            printHelp();
            t.printStackTrace();
        }
    }
}

/**
 * This class provides a class loader dummy, which finds no classes and 
 * explicitly does not ask any parent class loader to do its job.
 *
 * This class is used as parent to initialize the <code>URLClassLoader</code>, 
 * and thereby prevent this class loader to load any classes from URLs not
 * explicitly initialized with the <code>URLClassLoader</code>'s constructor.
 */
class DummyClassLoader extends ClassLoader {

    public DummyClassLoader() {
    }

    public DummyClassLoader(ClassLoader parent) {
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    public URL getResource(String name) {
        return null;
    }

    public InputStream getResourceAsStream(String name) {
        return null;
    }

    public static URL getSystemResource(String name) {
        return null;
    }

    public static Enumeration getSystemResources(String name) throws IOException {
        return null;
    }

    public static InputStream getSystemResourceAsStream(String name) {
        return null;
    }

    public static ClassLoader getSystemClassLoader() {
        return null;
    }

    public synchronized void setDefaultAssertionStatus(boolean enabled) {
    }

    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled) {
    }

    public synchronized void setClassAssertionStatus(String className, boolean enabled) {
    }

    public synchronized void clearAssertionStatus() {
    }
}
