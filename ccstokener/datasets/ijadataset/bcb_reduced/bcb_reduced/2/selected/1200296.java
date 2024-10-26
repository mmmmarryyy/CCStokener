package org.adapit.wctoolkit.models.util.infra;

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
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.log4j.Logger;

/**
 * This utility class was based originally on <a
 * href="private.php?do=newpm&u=47838">Daniel Le Berre</a>'s <code>RTSI</code>
 * class. This class can be called in different modes, but the principal use is
 * to determine what subclasses/implementations of a given class/interface exist
 * in the current runtime environment.
 * 
 * @author Daniel Le Berre, Elliott Wade
 */
public class ClassFinder {

    private Class<?> searchClass = null;

    private Map<URL, String> classpathLocations = new HashMap<URL, String>();

    private Map<Class<?>, URL> results = new HashMap<Class<?>, URL>();

    protected Logger logger;

    private static ClassFinder instance;

    public static ClassFinder getInstance() {
        if (instance == null) {
            instance = new ClassFinder();
        }
        return instance;
    }

    private ClassFinder() {
        logger = Logger.getLogger(getClass());
        refreshLocations();
    }

    /**
	 * Rescan the classpath, cacheing all possible file locations.
	 */
    public final void refreshLocations() {
        synchronized (classpathLocations) {
            try {
                classpathLocations = getClasspathLocations();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
	 * @param fqcn
	 *            Name of superclass/interface on which to search
	 */
    public final Vector<Class<?>> findSubclasses(String fqcn) throws Exception {
        synchronized (classpathLocations) {
            synchronized (results) {
                try {
                    searchClass = null;
                    results = new TreeMap<Class<?>, URL>(CLASS_COMPARATOR);
                    if (fqcn.startsWith(".") || fqcn.endsWith(".")) {
                        return new Vector<Class<?>>();
                    }
                    try {
                        searchClass = Class.forName(fqcn);
                    } catch (ClassNotFoundException ex) {
                        logger.error(ex.getMessage());
                        return new Vector<Class<?>>();
                    }
                    return findSubclasses(searchClass, classpathLocations);
                } finally {
                }
            }
        }
    }

    /**
	 * The result of the last search is cached in this object, along with the
	 * URL that corresponds to each class returned. This method may be called to
	 * query the cache for the location at which the given class was found.
	 * <code>null</code> will be returned if the given class was not found
	 * during the last search, or if the result cache has been cleared.
	 */
    public final URL getLocationOf(Class<?> cls) {
        if (results != null) return results.get(cls); else return null;
    }

    /**
	 * Determine every URL location defined by the current classpath, and it's
	 * associated package name.
	 */
    public final Map<URL, String> getClasspathLocations() throws Exception {
        Map<URL, String> map = new TreeMap<URL, String>(URL_COMPARATOR);
        File file = null;
        List<String> excludeList = new ArrayList<String>();
        excludeList.add("log4j-1.2.11.jar");
        excludeList.add("antlr.jar");
        excludeList.add("ejb3-persistence.jar");
        excludeList.add("junit.jar");
        excludeList.add("org.hamcrest.core_1.1.0.v20090501071000.jar");
        excludeList.add("velocity-1.4.jar");
        excludeList.add("commons-collections.jar");
        excludeList.add("aopalliance.jar");
        excludeList.add("commons-attributes-api.jar");
        excludeList.add("commons-attributes-compiler.jar");
        excludeList.add("commons-logging.jar");
        excludeList.add("nsuml.jar");
        excludeList.add("ocl-argo.jar");
        excludeList.add("org-netbeans-modules-form.jar");
        excludeList.add("spring.jar");
        excludeList.add("xercesImpl.jar");
        excludeList.add("xml-apis.jar");
        excludeList.add("xalan.jar");
        excludeList.add("CLooks_120.jar");
        excludeList.add("kunststoff.jar");
        excludeList.add("OfficeLnFs_1.1.1.jar");
        excludeList.add("l2fprod-common-all.jar");
        excludeList.add("log4j-1.2.11.jar");
        excludeList.add("scannotation-1.0.2.jar");
        excludeList.add("antlrworks-1.3.jar");
        excludeList.add("core-3.3.0-v_771.jar");
        excludeList.add(".cp");
        excludeList.add("jcalendar.jar");
        excludeList.add("MultipleGradientPaint.jar");
        excludeList.add("swing-layout.jar");
        excludeList.add("swing-worker.jar");
        excludeList.add("swingx-0.9.1.jar");
        excludeList.add("swingx-2008_01_20.jar");
        excludeList.add("activation.jar");
        excludeList.add("ajaxtags-1.3-beta-rc7.jar");
        excludeList.add("antlr-2.7.6.jar");
        excludeList.add("asm.jar");
        excludeList.add("asm-attrs.jar");
        excludeList.add("commons-codec.jar");
        excludeList.add("commons-dbcp.jar");
        excludeList.add("commons-digester.jar");
        excludeList.add("commons-discovery.jar");
        excludeList.add("commons-fileupload.jar");
        excludeList.add("commons-httpclient.jar");
        excludeList.add("commons-io.jar");
        excludeList.add("commons-javaflow.jar");
        excludeList.add("commons-lang.jar");
        excludeList.add("commons-pool.jar");
        excludeList.add("commons-validator.jar");
        excludeList.add("connector.jar");
        excludeList.add("dom4j-1.6.1.jar");
        excludeList.add("ehcache-1.2.3.jar");
        excludeList.add("Filters.jar");
        excludeList.add("hibernate3.jar");
        excludeList.add("hibernate-annotations.jar");
        excludeList.add("hibernate-entitymanager.jar");
        excludeList.add("itext-1.02b.jar");
        excludeList.add("jai_codec.jar");
        excludeList.add("jai_core.jar");
        excludeList.add("jakarta-oro.jar");
        excludeList.add("javassist.jar");
        excludeList.add("jaxen-1.1-beta-7.jar");
        excludeList.add("jboleto-0.9.6.jar");
        excludeList.add("jboss-archive-browsing.jar");
        excludeList.add("jboss-common.jar");
        excludeList.add("jdbc2_0-stdext.jar");
        excludeList.add("jdt-compiler.jar");
        excludeList.add("jmock-1.1.0RC1.jar");
        excludeList.add("jstl.jar");
        excludeList.add("lucene-1.4.3.jar");
        excludeList.add("mail.jar");
        excludeList.add("mlibwrapper_jai.jar");
        excludeList.add("odmg.jar");
        excludeList.add("ojdbc14_g.jar");
        excludeList.add("pdfbox-0.6.5.jar");
        excludeList.add("poi-2.0-final-20040126.jar");
        excludeList.add("poi-2.5.1.jar");
        excludeList.add("postgresql-8.1-404.jdbc3.jar");
        excludeList.add("qdox-1.6.jar");
        excludeList.add("spring-aspects.jar");
        excludeList.add("spring-hibernate3.jar");
        excludeList.add("spring-mock.jar");
        excludeList.add("spring-modules-jakarta-commons.jar");
        excludeList.add("spring-modules-validation.jar");
        excludeList.add("standard.jar");
        excludeList.add("velocity-tools-generic-1.1.jar");
        excludeList.add("brazilutils-0.1.1.jar");
        excludeList.add("commons-logging-api.jar");
        excludeList.add("commons-modeler-1.1.jar");
        excludeList.add("itext-1.3.6-2006-02-10.jar");
        excludeList.add("quartz-all-1.6.0.jar");
        excludeList.add("3.51b12_jpedal_gpl.jar");
        excludeList.add("AlgoBros3_20041119.jar");
        excludeList.add("fhlaf_1.0b2_bin.jar");
        excludeList.add("jane-0.1.jar");
        excludeList.add("looks-2.1.4.jar");
        excludeList.add("metouia.jar");
        excludeList.add("oalnf.jar");
        excludeList.add("OfficeLnFs_2.6.jar");
        excludeList.add("PDFRenderer-2007_12_23.jar");
        excludeList.add("PgsLookAndFeel.jar");
        excludeList.add("PgsLookAndFeel-jide.jar");
        excludeList.add("quaqua.jar");
        excludeList.add("squareness.jar");
        excludeList.add("commons-beanutils-core-1.7.0.jar");
        excludeList.add("jh.jar");
        excludeList.add("jhall.jar");
        excludeList.add("jhbasic.jar");
        excludeList.add("jsearch.jar");
        excludeList.add("umldiagrams.jar");
        excludeList.add("hibernate-commons-annotations.jar");
        excludeList.add("javassist-3.4.GA.jar");
        excludeList.add("jta-1.1.jar");
        excludeList.add("slf4j-api-1.5.6.jar");
        excludeList.add("slf4j-log4j12-1.5.6.jar");
        excludeList.add("log4j-1.2.15.jar");
        excludeList.add("swingaddons.jar");
        excludeList.add("jsp-api.jar");
        excludeList.add("servlet-api.jar");
        excludeList.add("jasper.jar");
        excludeList.add("jasperreports-3.1.0.jar");
        excludeList.add("jcalendar.jar");
        logger.info("Ignored artifacts for class loader " + excludeList);
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(classpath, pathSep);
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            if (contains(excludeList, path)) {
                continue;
            }
            logger.info("Searching path for following class loader: " + path);
            file = new File(path);
            include(null, file, map);
        }
        return map;
    }

    private boolean contains(List<String> arr, String value) throws Exception {
        if (arr != null && !arr.isEmpty()) {
            for (String str : arr) {
                if (value.indexOf(str) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private final FileFilter DIRECTORIES_ONLY = new FileFilter() {

        public boolean accept(File f) {
            if (f.exists() && f.isDirectory()) return true; else return false;
        }
    };

    private final Comparator<URL> URL_COMPARATOR = new Comparator<URL>() {

        public int compare(URL u1, URL u2) {
            return String.valueOf(u1).compareTo(String.valueOf(u2));
        }
    };

    private final Comparator<Class<?>> CLASS_COMPARATOR = new Comparator<Class<?>>() {

        public int compare(Class<?> c1, Class<?> c2) {
            return String.valueOf(c1).compareTo(String.valueOf(c2));
        }
    };

    private final void include(String name, File file, Map<URL, String> map) {
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

    private void includeJar(File file, Map<URL, String> map) {
        if (file.isDirectory()) return;
        URL jarURL = null;
        JarFile jar = null;
        try {
            jarURL = new URL("file:/" + file.getCanonicalPath());
            jarURL = new URL("jar:" + jarURL.toExternalForm() + "!/");
            JarURLConnection conn = (JarURLConnection) jarURL.openConnection();
            jar = conn.getJarFile();
        } catch (Exception e) {
            return;
        }
        if (jar == null || jarURL == null) return;
        map.put(jarURL, "");
        Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
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

    private String packageNameFor(JarEntry entry) {
        if (entry == null) return "";
        String s = entry.getName();
        if (s == null) return "";
        if (s.length() == 0) return s;
        if (s.startsWith("/")) s = s.substring(1, s.length());
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s.replace('/', '.');
    }

    private final Vector<Class<?>> findSubclasses(Class<?> superClass, Map<URL, String> locations) throws Exception {
        Vector<Class<?>> v = new Vector<Class<?>>();
        Vector<Class<?>> w = null;
        Iterator<URL> it = locations.keySet().iterator();
        while (it.hasNext()) {
            URL url = it.next();
            w = findSubclasses(url, locations.get(url), superClass);
            if (w != null && (w.size() > 0)) v.addAll(w);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private final Vector<Class<?>> findSubclasses(URL location, String packageName, Class<?> superClass) throws Exception {
        synchronized (results) {
            Map<Class<?>, URL> thisResult = new TreeMap<Class<?>, URL>(CLASS_COMPARATOR);
            Vector<Class<?>> v = new Vector<Class<?>>();
            String fqcn = searchClass.getName();
            List<URL> knownLocations = new ArrayList<URL>();
            knownLocations.add(location);
            for (int loc = 0; loc < knownLocations.size(); loc++) {
                URL url = knownLocations.get(loc);
                File directory = new File(url.getFile());
                if (directory.exists()) {
                    String[] files = directory.list();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].endsWith(".class")) {
                            String classname = files[i].substring(0, files[i].length() - 6);
                            try {
                                Class<?> c = Class.forName(packageName + "." + classname);
                                if (superClass.isAssignableFrom(c) && !fqcn.equals(packageName + "." + classname)) {
                                    thisResult.put(c, url);
                                }
                            } catch (java.lang.ExceptionInInitializerError err) {
                                logger.warn(err);
                            } catch (java.lang.NoClassDefFoundError err) {
                                logger.warn(err);
                            } catch (ClassNotFoundException cnfex) {
                                logger.warn(cnfex);
                            } catch (Exception ex) {
                                logger.warn(ex.getMessage());
                            }
                        }
                    }
                } else {
                    try {
                        JarURLConnection conn = (JarURLConnection) url.openConnection();
                        JarFile jarFile = conn.getJarFile();
                        Enumeration<JarEntry> e = jarFile.entries();
                        while (e.hasMoreElements()) {
                            JarEntry entry = e.nextElement();
                            String entryname = entry.getName();
                            if (!entry.isDirectory() && entryname.endsWith(".class")) {
                                String classname = entryname.substring(0, entryname.length() - 6);
                                if (classname.startsWith("/")) classname = classname.substring(1);
                                classname = classname.replace('/', '.');
                                try {
                                    Class c = Class.forName(classname);
                                    if (superClass.isAssignableFrom(c) && !fqcn.equals(classname)) {
                                        thisResult.put(c, url);
                                    }
                                } catch (java.lang.ExceptionInInitializerError err) {
                                    logger.warn(err);
                                } catch (ClassNotFoundException cnfex) {
                                    logger.warn(cnfex);
                                } catch (NoClassDefFoundError ncdfe) {
                                    logger.warn(ncdfe);
                                } catch (UnsatisfiedLinkError ule) {
                                    logger.warn(ule);
                                } catch (Exception exception) {
                                    logger.warn(exception);
                                } catch (Error error) {
                                    logger.warn(error);
                                }
                            }
                        }
                    } catch (IOException ioex) {
                        logger.error(ioex);
                    }
                }
            }
            results.putAll(thisResult);
            Iterator<Class<?>> it = thisResult.keySet().iterator();
            while (it.hasNext()) {
                v.add(it.next());
            }
            return v;
        }
    }
}
