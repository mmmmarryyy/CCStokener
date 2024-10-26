package edu.ucla.loni.LOVE;

import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;

/**
 * This class will look at .class files under plugins folder
 * and retrieve these classes for use as plugins.
 * <p/>
 * It provides centralized service for plugin loading. All the
 * plugin classes that are loaded before will be stored in cache
 * for later usages.
 * <p/>
 * The reason to extend <code>ClassLoader</code> is that we can use
 * those native, protected method of it. Those method can provide many
 * powerful functionality.
 * <p/>
 * BUG/TO DO: Still don't know how to deal with Jarred class file.
 * if classes are jarred, the program can no longer list
 * all the files in the directory thus can't load classes.
 */
public class PluginLoader extends ClassLoader {

    /**
     * Cache to store all the plugins. Init size: 20.
     * Key is a string with path information like:
     * plugin.color.GrayColorMap
     */
    static Hashtable cache = new Hashtable(20);

    /**
     * Load the classes in a directory .plugins.*
     * Example of usage: loadDirectory("edu.ucla.loni.LOVE.plugin.colormap");
     *
     * @param packDir The directory to be loaded
     */
    public static Hashtable loadDirectory(String packDir) {
        Hashtable classTable = new Hashtable(5);
        URL url = PluginLoader.class.getResource("/edu/ucla/loni/LOVE/");
        try {
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                Enumeration entries = jarFile.entries();
                String path = packDir.replace('.', '/');
                while (entries.hasMoreElements()) {
                    JarEntry entry = (JarEntry) entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (entryName.endsWith(".class")) {
                            int index = entryName.lastIndexOf('/');
                            if (entryName.substring(0, index).equals(path)) {
                                String className = entryName.substring(index + 1, entryName.length() - 6);
                                Class aClass = loadAClass(packDir + "." + className);
                                if (aClass != null) {
                                    classTable.put(className, aClass);
                                }
                            }
                        }
                    }
                }
                return classTable;
            } else {
                url = PluginLoader.class.getResource("/" + packDir.replace('.', '/'));
                File classDir = new File(url.toURI());
                String[] fileList = classDir.list();
                if (fileList != null) {
                    int i;
                    for (i = 0; i < fileList.length; i++) {
                        if (fileList[i].endsWith(".class")) {
                            String className = fileList[i].substring(0, fileList[i].indexOf("."));
                            Class aClass = loadAClass(packDir + "." + className);
                            if (aClass != null) {
                                classTable.put(className, aClass);
                            }
                        }
                    }
                    return classTable;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Load a single class as specified by the parameter.
     *
     * @param className Name of the class, including package name.
     */
    public static Class loadAClass(String className) {
        Class loadedClass = (Class) cache.get(className);
        if (loadedClass != null) {
            return loadedClass;
        }
        try {
            loadedClass = Class.forName(className, true, ClassLoader.getSystemClassLoader());
            cache.put(className, loadedClass);
            return loadedClass;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Test program
     */
    public static void main(String args[]) throws Exception {
        Hashtable classTable = loadDirectory("edu.ucla.loni.LOVE");
        Enumeration e = classTable.keys();
        for (; e.hasMoreElements(); ) {
            System.out.println(e.nextElement());
        }
    }
}
