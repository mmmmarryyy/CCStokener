package org.viewaframework.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Mario Garcia
 *
 */
public class ClassLocator {

    private static final String BLANK = "";

    private static final String JAR_PROTOCOL = "jar:";

    private static final char POINT = '.';

    private static final char SLASH_URL_TYPE = '/';

    private static final String SUFFIX_CLASS = ".class";

    /**
     * Recursive method used to find all classes in a given directory and subdirs OR inside a jar package.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException 
     */
    @SuppressWarnings("all")
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException, IOException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            String urlPath = JAR_PROTOCOL + directory.getPath();
            String packageEntry = packageName.replace(POINT, SLASH_URL_TYPE) + SLASH_URL_TYPE;
            URL url = new URL(urlPath);
            JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
            JarFile jar = urlConnection.getJarFile();
            Enumeration<JarEntry> enumex = jar.entries();
            while (enumex.hasMoreElements()) {
                JarEntry entry = enumex.nextElement();
                String entryName = entry.getName();
                String cleanEntryName = entryName.replace(SLASH_URL_TYPE, POINT).replace(SUFFIX_CLASS, BLANK);
                if (entryName.contains(packageEntry) && !entryName.equals(packageEntry)) {
                    classes.add(ClassLocator.class.getClassLoader().loadClass(cleanEntryName));
                }
            }
        } else {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(POINT + BLANK);
                    classes.addAll(findClasses(file, packageName + POINT + file.getName()));
                } else if (file.getName().endsWith(SUFFIX_CLASS)) {
                    classes.add(Class.forName(packageName + POINT + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("all")
    public static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace(POINT, SLASH_URL_TYPE);
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }
}
