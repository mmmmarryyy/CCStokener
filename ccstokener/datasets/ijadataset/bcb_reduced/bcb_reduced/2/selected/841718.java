package agentgui.core.jade;

import jade.util.ClassFinderFilter;
import jade.util.ClassFinderListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This utility class was based originally on Daniel Le Berre's <code>RTSI</code>
 * class. This class can be called in different modes, but the principal use is
 * to determine what subclasses/implementations of a given class/interface exist
 * in the current runtime environment.
 * 
 * @author Daniel Le Berre, Elliott Wade, Paolo Cancedda
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClassFinder {

    private Class searchClass = null;

    private Map classpathLocations = new HashMap();

    private Map results = new HashMap();

    private List errors = new ArrayList();

    private boolean working = false;

    private ClassFinderListener listener;

    private ClassFinderFilter filter;

    private boolean stopSearch;

    public void setStopSearch(boolean stopSearch) {
        this.stopSearch = stopSearch;
    }

    public boolean isStopSearch() {
        return stopSearch;
    }

    public boolean isWorking() {
        return working;
    }

    public ClassFinder() {
        refreshLocations();
    }

    /**
	 * Rescan the classpath, cacheing all possible file locations.
	 */
    public final void refreshLocations() {
        synchronized (classpathLocations) {
            classpathLocations = getClasspathLocations();
        }
    }

    public final Vector findSubclasses(String fqcn) {
        return findSubclasses(fqcn, null, null);
    }

    /**
	 * @param fqcn
	 *            Name of superclass/interface on which to search
	 */
    public final Vector findSubclasses(String fqcn, ClassFinderListener aListener, ClassFinderFilter aFilter) {
        synchronized (classpathLocations) {
            synchronized (results) {
                this.listener = aListener;
                this.filter = aFilter;
                try {
                    working = true;
                    searchClass = null;
                    errors = new ArrayList();
                    results = new TreeMap(CLASS_COMPARATOR);
                    if (fqcn.startsWith(".") || fqcn.endsWith(".")) {
                        return new Vector();
                    }
                    try {
                        searchClass = callClassForName(fqcn);
                    } catch (Throwable t) {
                        errors.add(t);
                        return new Vector();
                    }
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
	 * The result of the last search is cached in this object, along with the
	 * URL that corresponds to each class returned. This method may be called to
	 * query the cache for the location at which the given class was found.
	 * <code>null</code> will be returned if the given class was not found
	 * during the last search, or if the result cache has been cleared.
	 */
    public final URL getLocationOf(Class cls) {
        if (results != null) return (URL) results.get(cls); else return null;
    }

    /**
	 * Determine every URL location defined by the current classpath, and it's
	 * associated package name.
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
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Object nextURL = it.next();
            @SuppressWarnings("unused") URL url = (URL) nextURL;
        }
        return map;
    }

    private static final FileFilter DIRECTORIES_ONLY = new FileFilter() {

        public boolean accept(File f) {
            if (f.exists() && f.isDirectory()) return true; else return false;
        }
    };

    private static final FileFilter CLASSES_ONLY = new FileFilter() {

        public boolean accept(File f) {
            if (f.exists() && f.isFile() && f.canRead()) return f.getName().endsWith(".class"); else return false;
        }
    };

    private static final Comparator URL_COMPARATOR = new Comparator() {

        public int compare(Object u1, Object u2) {
            return String.valueOf(u1).compareTo(String.valueOf(u2));
        }
    };

    private static final Comparator CLASS_COMPARATOR = new Comparator() {

        public int compare(Object c1, Object c2) {
            return String.valueOf(c1).compareTo(String.valueOf(c2));
        }
    };

    private final void include(String name, File file, Map map) {
        if (!file.exists()) return;
        if (!file.isDirectory()) {
            includeJar(file, map);
            return;
        }
        if (name == null) name = ""; else name += ".";
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

    private void includeJar(File file, Map map) {
        if (file.isDirectory()) return;
        URL jarURL = null;
        JarFile jar = null;
        try {
            String canonicalPath = file.getCanonicalPath();
            if (!canonicalPath.startsWith("/")) {
                canonicalPath = "/" + canonicalPath;
            }
            jarURL = new URL("file:" + canonicalPath);
            jarURL = new URL("jar:" + jarURL.toExternalForm() + "!/");
            URLConnection urlConnection = jarURL.openConnection();
            JarURLConnection conn = (JarURLConnection) urlConnection;
            jar = conn.getJarFile();
        } catch (Exception e) {
            return;
        }
        if (jar == null || jarURL == null) return;
        map.put(jarURL, "");
        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (entry.isDirectory()) {
                if (entry.getName().toUpperCase().equals("META-INF/")) continue;
                try {
                    map.put(new URL(jarURL.toExternalForm() + entry.getName()), packageNameFor(entry));
                } catch (MalformedURLException murl) {
                    continue;
                }
            }
        }
    }

    private static String packageNameFor(JarEntry entry) {
        if (entry == null) return "";
        String s = entry.getName();
        if (s == null) return "";
        if (s.length() == 0) return s;
        if (s.startsWith("/")) s = s.substring(1, s.length());
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.replace('/', '.');
    }

    private final Vector findSubclasses(Class superClass, Map locations) {
        Set setOfClasses = new TreeSet(CLASS_COMPARATOR);
        Vector v = new Vector();
        Iterator it = locations.keySet().iterator();
        while (it.hasNext() && isStopSearch() == false) {
            Object nextURL = it.next();
            if (nextURL instanceof URL) {
                URL url = (URL) nextURL;
                findSubclasses(url, (String) locations.get(url), superClass, setOfClasses);
            }
        }
        Iterator iterator = setOfClasses.iterator();
        while (iterator.hasNext()) {
            v.add(iterator.next());
        }
        return v;
    }

    private void manageClass(Set setOfClasses, Class superClass, Class c, URL url) {
        boolean include;
        include = superClass.isAssignableFrom(c);
        if (include && filter != null) {
            include = filter.include(superClass, c);
        }
        if (include) {
            results.put(c, url);
            if (setOfClasses.add(c)) {
                if (listener != null) {
                    listener.add(c, url);
                }
            }
        }
    }

    private final void findSubclasses(URL location, String packageName, Class superClass, Set setOfClasses) {
        synchronized (results) {
            String fqcn = searchClass.getName();
            List knownLocations = new ArrayList();
            knownLocations.add(location);
            for (int loc = 0; loc < knownLocations.size(); loc++) {
                URL url = (URL) knownLocations.get(loc);
                File directory = new File(url.getFile());
                if (directory.exists()) {
                    File[] files = directory.listFiles(CLASSES_ONLY);
                    for (int i = 0; i < files.length; i++) {
                        String filename = files[i].getName();
                        String classname = filename.substring(0, filename.length() - 6);
                        try {
                            if (!fqcn.equals(packageName + "." + classname)) {
                                Class c = callClassForName(packageName + "." + classname);
                                manageClass(setOfClasses, superClass, c, url);
                            }
                        } catch (Throwable t) {
                            errors.add(t);
                        }
                    }
                } else {
                    try {
                        URLConnection urlConnection = url.openConnection();
                        JarURLConnection conn = (JarURLConnection) urlConnection;
                        JarFile jarFile = conn.getJarFile();
                        Enumeration e = jarFile.entries();
                        while (e.hasMoreElements()) {
                            JarEntry entry = (JarEntry) e.nextElement();
                            String entryname = entry.getName();
                            if (!entry.isDirectory() && entryname.endsWith(".class")) {
                                String classname = entryname.substring(0, entryname.length() - 6);
                                if (classname.startsWith("/")) classname = classname.substring(1);
                                classname = classname.replace('/', '.');
                                try {
                                    if (!fqcn.equals(classname)) {
                                        Class c = callClassForName(classname);
                                        manageClass(setOfClasses, superClass, c, url);
                                    }
                                } catch (Throwable t) {
                                    errors.add(t);
                                }
                            }
                        }
                    } catch (IOException ioex) {
                        errors.add(ioex);
                    }
                }
            }
        }
    }

    private Class callClassForName(String classname) throws ClassNotFoundException {
        return Class.forName(classname, false, getClass().getClassLoader());
    }
}
