package ezinjector.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PackageUtil {

    private static final Logger logger = Logger.getLogger(PackageUtil.class.getName());

    /**
	 * Returns the classes in the given packages.
	 * @param pkgs the packages
	 * @return the classes
	 */
    public Collection<Class<?>> getClasses(Package... pkgs) {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (URL u : getJarFileResources(pkgs)) classes.addAll(getJarClasses(u));
        Map<File, Package> dirs = getDirectoryResources(pkgs);
        for (File f : dirs.keySet()) classes.addAll(getDirClasses(f, dirs.get(f).getName()));
        return classes;
    }

    /**
	 * Return the classes in the given directory and all sub-directories.
	 * @param dir the directory
	 * @param packageName the package name corresponding to the dir
	 * @return the classes
	 */
    public Collection<Class<?>> getDirClasses(File dir, String packageName) {
        logger.fine("Mapping directory: " + dir);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (String name : dir.list()) {
            if (getClassName(name) != null) {
                try {
                    String className = packageName + "." + getClassName(name);
                    classes.add(Class.forName(className));
                } catch (Throwable ex) {
                    logger.log(Level.FINE, "", ex);
                }
            } else {
                File subDir = new File(dir.getPath() + File.separator + name);
                if (subDir.isDirectory()) classes.addAll(getDirClasses(subDir, packageName + "." + name));
            }
        }
        return classes;
    }

    /**
	 * Returns the classes in the given jar file.
	 * @param jar the jar file
	 * @return the classes
	 */
    public Collection<Class<?>> getJarClasses(URL jar) {
        logger.fine("Getting classes in jar file: " + jar);
        Set<Class<?>> classes = new HashSet<Class<?>>();
        try {
            JarURLConnection conn = (JarURLConnection) jar.openConnection();
            Enumeration<JarEntry> entries = conn.getJarFile().entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (getClassName(entryName) != null) classes.add(Class.forName(getClassName(entryName)));
            }
        } catch (Throwable ex) {
            logger.log(Level.FINE, "", ex);
        }
        return classes;
    }

    /** 
	 * Returns the class name corresponding to the content of the given 
	 * string. If <code>str</code> does not end with ".class" then null 
	 * is returned. Otherwise, the ".class" is stripped and all 
	 * occurrences of "/" are replaced by "." (except for a leading one
	 * of course, that one is stripped as well). 
	 * 
	 * @param str the string 
	 * @return the class name or null if <code>str</code> does not
	 * end with ".class"
	 */
    public String getClassName(String str) {
        if (str.endsWith(".class")) {
            if (str.startsWith("/")) str = str.substring(1);
            str = str.replace("/", ".");
            return str.substring(0, str.length() - ".class".length());
        }
        return null;
    }

    /**
	 * Returns the Jar URL resources associated with the given packages.
	 * @param pkg the packages
	 * @return the jar URLs
	 * @throws IOException
	 */
    public Collection<URL> getJarFileResources(Package... pkgs) {
        Map<String, URL> jarNameToURL = new HashMap<String, URL>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (Package pkg : pkgs) {
            try {
                Enumeration<URL> urls = loader.getResources(getResourceName(pkg));
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    String jarName = getJarFileName(url);
                    if (jarName != null) jarNameToURL.put(jarName, url);
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error getting resources for package: " + pkg.getName(), ex);
            }
        }
        return jarNameToURL.values();
    }

    /**
	 * Returns the directory files associated with the given packages. Only the
	 * root-most directories are returned, for example if \/home\/code and
	 * \/home\/code\/util are associated resources, then only \/home\/code will
	 * be returned.
	 * @param pkgs the packages
	 * @return the directories
	 * @throws IOException
	 */
    public Map<File, Package> getDirectoryResources(Package... pkgs) {
        Map<File, Package> fileToPackage = new HashMap<File, Package>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (Package pkg : pkgs) {
            try {
                Enumeration<URL> urls = loader.getResources(getResourceName(pkg));
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    File dir = getDirectory(url);
                    if (dir != null) {
                        for (File f : fileToPackage.keySet().toArray(new File[0])) if (f.getPath().startsWith(dir.getPath())) fileToPackage.remove(f);
                        fileToPackage.put(dir, pkg);
                    }
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error getting resources for package: " + pkg.getName(), ex);
            }
        }
        return fileToPackage;
    }

    /**
	 * Returns the resource name of the given package. Used as
	 * an argument to {@link ClassLoader#getResources(String)}. 
	 * @param pkg the package
	 * @return the resource name
	 */
    public String getResourceName(Package pkg) {
        return pkg.getName().replace(".", "/");
    }

    /**
	 * Returns the directory file associated with the given URL.
	 * @param url the URL
	 * @return the directory, or null if the URL does not
	 * refer to a directory
	 */
    public File getDirectory(URL url) {
        File file = new File(url.getFile().replaceAll("%20", " "));
        if (file.isDirectory()) return file; else return null;
    }

    /**
	 * Returns the name of the jar file associated with the given URL.
	 * @param url the URL 
	 * @return the name of the jar file, or null if the URL does
	 * not refer to a jar
	 * @throws IOException if there is a problem opening a url
	 * connection
	 */
    public String getJarFileName(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            JarURLConnection jarConn = (JarURLConnection) conn;
            return jarConn.getJarFile().getName();
        }
        return null;
    }

    /**
	 * Returns the known package names that start with the given string.
	 * @param restriction the beginning of the package name
	 * @return the packages whose names start with the given string
	 */
    public Package[] getPackagesStartingWith(String restriction) {
        Set<Package> pkgs = new HashSet<Package>();
        for (Package p : Package.getPackages()) if (p.getName().startsWith(restriction)) pkgs.add(p);
        return pkgs.toArray(new Package[0]);
    }
}
