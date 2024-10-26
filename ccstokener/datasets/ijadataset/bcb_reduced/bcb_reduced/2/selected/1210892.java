package bpiwowar.argparser.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import bpiwowar.argparser.Logger;

/**
 * Methods used for introspection
 * 
 * @author B. Piwowarski
 * @date 24/11/2006
 * 
 */
public class Introspection {

    public static Logger logger = Logger.getLogger();

    /**
	 * Get the list of class which implement a given class
	 * 
	 * @param which
	 *            The base class
	 * @param packageName
	 *            the package where classes are searched
	 * @param levels
	 *            number of levels to be explored (-1 for infinity)
	 * @return an array of objects of the given class
	 */
    public static Class<?>[] getImplementors(final Class<?> which, final String packageName, final int levels) {
        final ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        Introspection.addImplementors(list, which, packageName, levels);
        final Class<?>[] objects = (Class<?>[]) java.lang.reflect.Array.newInstance(Class.class, list.size());
        return list.toArray(objects);
    }

    /**
	 * A checker class
	 * 
	 * @author bpiwowar
	 * @date Nov 7, 2007
	 */
    public interface Checker {

        boolean accepts(Class<?> aClass);
    }

    public static void addImplementors(final ArrayList<Class<?>> list, final Class<?> which, final String packageName, final int levels) {
        addClasses(new Checker() {

            public boolean accepts(Class<?> aClass) {
                return which.isAssignableFrom(aClass);
            }
        }, list, packageName, levels);
    }

    public static ArrayList<Class<?>> getClasses(final Checker checker, final String packageName, final int levels) {
        final ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        Introspection.addClasses(checker, list, packageName, levels);
        return list;
    }

    /**
	 * Add classes to the list
	 * 
	 * @param checker
	 *            Used to filter the classes
	 * @param list
	 *            The list to fill
	 * @param packageName
	 *            The package to be analyzed
	 * @param levels
	 *            The maximum number of recursion within the structure (or -1 if
	 *            infinite)
	 */
    public static void addClasses(final Checker checker, final ArrayList<Class<?>> list, final String packageName, final int levels) {
        final String name = "/" + packageName.replace('.', '/');
        final URL url = Introspection.class.getResource(name);
        if (url == null) return;
        addClasses(checker, list, packageName, levels, url);
    }

    /**
	 * @param checker
	 * @param list
	 * @param packageName
	 * @param levels
	 * @param url
	 */
    public static void addClasses(final Checker checker, final ArrayList<Class<?>> list, final String packageName, final int levels, final URL url) {
        final File directory = new File(url.getFile());
        if (directory.exists()) addClasses(checker, list, packageName, levels, directory); else try {
            final JarURLConnection conn = (JarURLConnection) url.openConnection();
            addClasses(checker, list, levels, conn, packageName.replace('.', '/'));
        } catch (final IOException ioex) {
            System.err.println(ioex);
        }
    }

    /**
	 * @param checker
	 * @param list
	 * @param levels
	 * @param conn
	 * @param prefix
	 * @throws IOException
	 */
    public static void addClasses(final Checker checker, final ArrayList<Class<?>> list, final int levels, final JarURLConnection conn, final String prefix) throws IOException {
        final JarFile jfile = conn.getJarFile();
        logger.debug("Exploring jar file %s with prefix %s", jfile.getName(), prefix);
        final Enumeration<?> e = jfile.entries();
        while (e.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry) e.nextElement();
            final String entryname = entry.getName();
            if (entryname.startsWith(prefix) && entryname.endsWith(".class")) {
                if (levels >= 0) {
                    int n = 0;
                    for (int i = prefix.length() + 1; i < entryname.length() && n <= levels; i++) if (entryname.charAt(i) == '/') n++;
                    if (n > levels) continue;
                }
                String classname = entryname.substring(0, entryname.length() - 6);
                if (classname.startsWith("/")) classname = classname.substring(1);
                classname = classname.replace('/', '.');
                try {
                    logger.debug("Testing class " + classname);
                    final Class<?> oclass = Class.forName(classname);
                    if (checker.accepts(oclass)) list.add(oclass);
                } catch (final Exception ex) {
                    logger.debug("Caught exception " + ex);
                }
            }
        }
    }

    /**
	 * @param checker
	 * @param list
	 * @param packageName
	 * @param levels
	 * @param directory
	 */
    public static void addClasses(final Checker checker, final ArrayList<Class<?>> list, final String packageName, final int levels, final File directory) {
        final File[] files = directory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (levels != 0 && files[i].isDirectory()) addClasses(checker, list, packageName + "." + files[i].getName(), levels - 1);
                if (files[i].getName().endsWith(".class")) {
                    String classname = files[i].getName().substring(0, files[i].getName().length() - 6);
                    try {
                        classname = packageName + "." + classname;
                        final Class<?> oclass = Class.forName(classname);
                        if (checker.accepts(oclass)) list.add(oclass);
                    } catch (final Exception e) {
                    }
                }
            }
        }
    }
}
