package org.qedeq.base.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This utility class was based originally on Daniel Le Berre's <code>RTSI</code> class.
 * This class can be called in different modes, but the principal use is to determine what
 * subclasses/implementations of a given class/interface exist in the current
 * runtime environment.
 * @author Daniel Le Berre, Elliott Wade
 * <p>
 * Michael Meyling made some ugly hacks to get some JUnit test checks working.
 * Maybe this class can later be used for finding plugins dynamically.
 */
public class ClassFinder {

    private Class searchClass = null;

    private Map classpathLocations = new HashMap();

    private Map results = new HashMap();

    private Set negativeResults = new TreeSet(CLASS_COMPARATOR);

    private List errors = new ArrayList();

    private boolean working = false;

    private String start;

    public ClassFinder() {
        refreshLocations();
    }

    public boolean isWorking() {
        return working;
    }

    /**
     * Rescan the classpath, cacheing all possible file locations.
     */
    public final void refreshLocations() {
        synchronized (classpathLocations) {
            classpathLocations = getClasspathLocations();
        }
    }

    /**
     * @param fqcn Name of superclass/interface on which to search
     */
    public final Set findSubclasses(final String fqcn, final String start) {
        synchronized (classpathLocations) {
            synchronized (results) {
                try {
                    working = true;
                    searchClass = null;
                    errors = new ArrayList();
                    results = new TreeMap(CLASS_COMPARATOR);
                    negativeResults = new TreeSet(CLASS_COMPARATOR);
                    if (fqcn.startsWith(".") || fqcn.endsWith(".")) {
                        return new TreeSet(CLASS_COMPARATOR);
                    }
                    try {
                        searchClass = Class.forName(fqcn);
                    } catch (Throwable ex) {
                        errors.add(ex);
                        return new TreeSet(CLASS_COMPARATOR);
                    }
                    this.start = start;
                    return findSubclasses(searchClass, classpathLocations);
                } finally {
                    working = false;
                }
            }
        }
    }

    public final List getErrors() {
        return new ArrayList(errors);
    }

    /**
     * The result of the last search is cached in this object, along
     * with the URL that corresponds to each class returned. This
     * method may be called to query the cache for the location at
     * which the given class was found. <code>null</code> will be
     * returned if the given class was not found during the last
     * search, or if the result cache has been cleared.
     */
    public final URL getLocationOf(final Class cls) {
        if (results != null) {
            return (URL) results.get(cls);
        } else {
            return null;
        }
    }

    /**
     * The negative result of the last search is cached in this object.
     */
    public final boolean hasNoLocation(final String cls) {
        if (!cls.startsWith(start)) {
            return true;
        }
        return negativeResults != null && negativeResults.contains(cls);
    }

    /**
     * Determine every URL location defined by the current classpath, and
     * it's associated package name.
     */
    public final Map getClasspathLocations() {
        Map map = new TreeMap(URL_COMPARATOR);
        File file = null;
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(classpath, pathSep);
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            file = new File(path);
            include(null, file, map);
        }
        return map;
    }

    private static final FileFilter DIRECTORIES_ONLY = new FileFilter() {

        public boolean accept(final File f) {
            if (f.exists() && f.isDirectory()) {
                return true;
            }
            return false;
        }
    };

    private static final Comparator URL_COMPARATOR = new Comparator() {

        public int compare(final Object u1, final Object u2) {
            return String.valueOf(u1).compareTo(String.valueOf(u2));
        }
    };

    public static final Comparator CLASS_COMPARATOR = new Comparator() {

        public int compare(final Object c1, final Object c2) {
            return String.valueOf(c1).compareTo(String.valueOf(c2));
        }
    };

    private final void include(final String fName, final File file, final Map map) {
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            includeJar(file, map);
            return;
        }
        String name = fName;
        if (name == null) {
            name = "";
        } else {
            name += ".";
        }
        File[] dirs = file.listFiles(DIRECTORIES_ONLY);
        for (int i = 0; i < dirs.length; i++) {
            try {
                map.put(new URL("file://" + dirs[i].getCanonicalPath()), name + dirs[i].getName());
            } catch (IOException ioe) {
                return;
            }
            include(name + dirs[i].getName(), dirs[i], map);
        }
    }

    private void includeJar(final File file, final Map map) {
        if (file.isDirectory()) {
            return;
        }
        URL jarURL = null;
        JarFile jar = null;
        try {
            String canonicalPath = file.getCanonicalPath();
            if (!canonicalPath.startsWith("/")) {
                canonicalPath = "/" + canonicalPath;
            }
            jarURL = new URL("file:" + canonicalPath);
            jarURL = new URL("jar:" + jarURL.toExternalForm() + "!/");
            JarURLConnection conn = (JarURLConnection) jarURL.openConnection();
            jar = conn.getJarFile();
        } catch (Exception e) {
            return;
        }
        if (jar == null || jarURL == null) {
            return;
        }
        map.put(jarURL, "");
        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (entry.isDirectory()) {
                if (entry.getName().toUpperCase().equals("META-INF/")) {
                    continue;
                }
                try {
                    map.put(new URL(jarURL.toExternalForm() + entry.getName()), packageNameFor(entry));
                } catch (MalformedURLException murl) {
                    continue;
                }
            }
        }
    }

    private static String packageNameFor(final JarEntry entry) {
        if (entry == null) {
            return "";
        }
        String s = entry.getName();
        if (s == null) {
            return "";
        }
        if (s.length() == 0) {
            return s;
        }
        if (s.startsWith("/")) {
            s = s.substring(1, s.length());
        }
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.replace('/', '.');
    }

    private final Set findSubclasses(final Class superClass, final Map locations) {
        Set v = new TreeSet(CLASS_COMPARATOR);
        Set w = null;
        Iterator it = locations.keySet().iterator();
        while (it.hasNext()) {
            URL url = (URL) it.next();
            w = findSubclasses(url, (String) locations.get(url), superClass);
            if (w != null && (w.size() > 0)) {
                v.addAll(w);
            }
        }
        return v;
    }

    private final Set findSubclasses(final URL location, final String packageName, final Class superClass) {
        synchronized (results) {
            Map thisResult = new TreeMap(CLASS_COMPARATOR);
            Set v = new TreeSet(CLASS_COMPARATOR);
            String fqcn = searchClass.getName();
            List knownLocations = new ArrayList();
            knownLocations.add(location);
            for (int loc = 0; loc < knownLocations.size(); loc++) {
                URL url = (URL) knownLocations.get(loc);
                File directory = new File(url.getFile());
                if (directory.exists()) {
                    String[] files = directory.list();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].endsWith(".class")) {
                            String classname = files[i].substring(0, files[i].length() - 6);
                            String cls = packageName + "." + classname;
                            if (hasNoLocation(cls)) {
                                continue;
                            }
                            try {
                                Class c = Class.forName(cls);
                                if (superClass.isAssignableFrom(c) && !fqcn.equals(cls)) {
                                    thisResult.put(c, url);
                                }
                            } catch (ClassNotFoundException cnfex) {
                                errors.add(cnfex);
                                negativeResults.add(cls);
                            } catch (Throwable ex) {
                                errors.add(ex);
                                negativeResults.add(cls);
                            }
                        }
                    }
                } else {
                    try {
                        JarURLConnection conn = (JarURLConnection) url.openConnection();
                        JarFile jarFile = conn.getJarFile();
                        Enumeration e = jarFile.entries();
                        while (e.hasMoreElements()) {
                            JarEntry entry = (JarEntry) e.nextElement();
                            String entryname = entry.getName();
                            if (!entry.isDirectory() && entryname.endsWith(".class")) {
                                String classname = entryname.substring(0, entryname.length() - 6);
                                if (classname.startsWith("/")) {
                                    classname = classname.substring(1);
                                }
                                classname = classname.replace('/', '.');
                                if (hasNoLocation(classname)) {
                                    continue;
                                }
                                try {
                                    Class c = Class.forName(classname);
                                    if (superClass.isAssignableFrom(c) && !fqcn.equals(classname)) {
                                        thisResult.put(c, url);
                                    }
                                } catch (ClassNotFoundException cnfex) {
                                    errors.add(cnfex);
                                    negativeResults.add(classname);
                                } catch (NoClassDefFoundError ncdfe) {
                                    errors.add(ncdfe);
                                    negativeResults.add(classname);
                                } catch (UnsatisfiedLinkError ule) {
                                    errors.add(ule);
                                    negativeResults.add(classname);
                                } catch (Exception exception) {
                                    errors.add(exception);
                                    negativeResults.add(classname);
                                } catch (Error error) {
                                    errors.add(error);
                                    negativeResults.add(classname);
                                }
                            }
                        }
                    } catch (IOException ioex) {
                        errors.add(ioex);
                    }
                }
            }
            results.putAll(thisResult);
            Iterator it = thisResult.keySet().iterator();
            while (it.hasNext()) {
                v.add(it.next());
            }
            return v;
        }
    }

    private static final String getPackagePath(final String packageName) {
        String path = new String(packageName);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        path = path.replace('.', '/');
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return path;
    }

    public static void main(final String[] args) {
        ClassFinder finder = null;
        Set v = null;
        List errors = null;
        if (args.length == 2) {
            finder = new ClassFinder();
            v = finder.findSubclasses(args[0], args[1]);
            errors = finder.getErrors();
        } else {
            System.out.println("Usage: java ClassFinder <fully.qualified.superclass.name> " + "<look only at classes starting with this>");
            return;
        }
        System.out.println("RESULTS:");
        if (v != null && v.size() > 0) {
            Iterator i = v.iterator();
            while (i.hasNext()) {
                Class cls = (Class) i.next();
                System.out.println(cls + " in " + ((finder != null) ? String.valueOf(finder.getLocationOf(cls)) : "?"));
            }
        } else {
            System.out.println("No subclasses of " + args[0] + " found.");
        }
    }
}
