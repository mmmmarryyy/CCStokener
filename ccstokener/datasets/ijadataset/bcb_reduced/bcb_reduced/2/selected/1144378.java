package vqwiki.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Adapted for JDK 1.4 from org.apache.xbean.finder.ResourceFinder
 *
 * @author David Blevins
 * @version $Rev$ $Date$
 */
public class ResourceFinder {

    private final URL[] urls;

    private final String path;

    private final ClassLoader classLoader;

    private final List resourcesNotLoaded = new ArrayList();

    public ResourceFinder(URL[] urls) {
        this(null, Thread.currentThread().getContextClassLoader(), urls);
    }

    public ResourceFinder(String path) {
        this(path, Thread.currentThread().getContextClassLoader(), null);
    }

    public ResourceFinder(String path, URL[] urls) {
        this(path, Thread.currentThread().getContextClassLoader(), urls);
    }

    public ResourceFinder(String path, ClassLoader classLoader) {
        this(path, classLoader, null);
    }

    public ResourceFinder(String path, ClassLoader classLoader, URL[] urls) {
        if (path == null) {
            path = "";
        } else if (path.length() > 0 && !path.endsWith("/")) {
            path += "/";
        }
        this.path = path;
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        this.classLoader = classLoader;
        for (int i = 0; urls != null && i < urls.length; i++) {
            URL url = urls[i];
            if (url == null || isDirectory(url) || url.getProtocol().equals("jar")) {
                continue;
            }
            try {
                urls[i] = new URL("jar", "", -1, url.toString() + "!/");
            } catch (MalformedURLException e) {
            }
        }
        this.urls = (urls == null || urls.length == 0) ? null : urls;
    }

    private static boolean isDirectory(URL url) {
        String file = url.getFile();
        return (file.length() > 0 && file.charAt(file.length() - 1) == '/');
    }

    /**
     * Returns a list of resources that could not be loaded in the last invoked findAvailable* or
     * mapAvailable* methods.
     * <p/>
     * The list will only contain entries of resources that match the requirements
     * of the last invoked findAvailable* or mapAvailable* methods, but were unable to be
     * loaded and included in their results.
     * <p/>
     * The list returned is unmodifiable and the results of this method will change
     * after each invocation of a findAvailable* or mapAvailable* methods.
     * <p/>
     * This method is not thread safe.
     */
    public List getResourcesNotLoaded() {
        return Collections.unmodifiableList(resourcesNotLoaded);
    }

    public URL find(String uri) throws IOException {
        String fullUri = path + uri;
        URL resource = getResource(fullUri);
        if (resource == null) {
            throw new IOException("Could not find resource '" + fullUri + "'");
        }
        return resource;
    }

    public List findAll(String uri) throws IOException {
        String fullUri = path + uri;
        Enumeration resources = getResources(fullUri);
        List list = new ArrayList();
        while (resources.hasMoreElements()) {
            URL url = (URL) resources.nextElement();
            list.add(url);
        }
        return list;
    }

    /**
     * Reads the contents of the URL as a {@link String}'s and returns it.
     *
     * @param uri
     * @return a stringified content of a resource
     * @throws IOException if a resource pointed out by the uri param could not be find
     * @see ClassLoader#getResource(String)
     */
    public String findString(String uri) throws IOException {
        String fullUri = path + uri;
        URL resource = getResource(fullUri);
        if (resource == null) {
            throw new IOException("Could not find a resource in : " + fullUri);
        }
        return readContents(resource);
    }

    /**
     * Reads the contents of the found URLs as a list of {@link String}'s and returns them.
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if any of the found URLs are unable to be read.
     */
    public List findAllStrings(String uri) throws IOException {
        String fulluri = path + uri;
        List strings = new ArrayList();
        Enumeration resources = getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = (URL) resources.nextElement();
            String string = readContents(url);
            strings.add(string);
        }
        return strings;
    }

    /**
     * Reads the contents of the found URLs as a Strings and returns them.
     * Individual URLs that cannot be read are skipped and added to the
     * list of 'resourcesNotLoaded'
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List findAvailableStrings(String uri) throws IOException {
        resourcesNotLoaded.clear();
        String fulluri = path + uri;
        List strings = new ArrayList();
        Enumeration resources = getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = (URL) resources.nextElement();
            try {
                String string = readContents(url);
                strings.add(string);
            } catch (IOException notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return strings;
    }

    /**
     * Reads the contents of all non-directory URLs immediately under the specified
     * location and returns them in a map keyed by the file name.
     * <p/>
     * Any URLs that cannot be read will cause an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/serializables/one
     * META-INF/serializables/two
     * META-INF/serializables/three
     * META-INF/serializables/four/foo.txt
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("serializables");
     * map.contains("one");  // true
     * map.contains("two");  // true
     * map.contains("three");  // true
     * map.contains("four");  // false
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if any of the urls cannot be read
     */
    public Map mapAllStrings(String uri) throws IOException {
        Map strings = new HashMap();
        Map resourcesMap = getResourcesMap(uri);
        for (Iterator iterator = resourcesMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            String value = readContents(url);
            strings.put(name, value);
        }
        return strings;
    }

    /**
     * Reads the contents of all non-directory URLs immediately under the specified
     * location and returns them in a map keyed by the file name.
     * <p/>
     * Individual URLs that cannot be read are skipped and added to the
     * list of 'resourcesNotLoaded'
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/serializables/one
     * META-INF/serializables/two      # not readable
     * META-INF/serializables/three
     * META-INF/serializables/four/foo.txt
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("serializables");
     * map.contains("one");  // true
     * map.contains("two");  // false
     * map.contains("three");  // true
     * map.contains("four");  // false
     *
     * @param uri
     * @return a list of the content of each resource URL found
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map mapAvailableStrings(String uri) throws IOException {
        resourcesNotLoaded.clear();
        Map strings = new HashMap();
        Map resourcesMap = getResourcesMap(uri);
        for (Iterator iterator = resourcesMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String name = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            try {
                String value = readContents(url);
                strings.put(name, value);
            } catch (IOException notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return strings;
    }

    /**
     * Executes {@link #findString(String)} assuming the contents URL found is the name of
     * a class that should be loaded and returned.
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Class findClass(String uri) throws IOException, ClassNotFoundException {
        String className = findString(uri);
        return (Class) classLoader.loadClass(className);
    }

    /**
     * Executes findAllStrings assuming the strings are
     * the names of a classes that should be loaded and returned.
     * <p/>
     * Any URL or class that cannot be loaded will cause an exception to be thrown.
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public List findAllClasses(String uri) throws IOException, ClassNotFoundException {
        List classes = new ArrayList();
        List strings = findAllStrings(uri);
        for (int i = 0; i < strings.size(); i++) {
            String className = (String) strings.get(i);
            Class clazz = classLoader.loadClass(className);
            classes.add(clazz);
        }
        return classes;
    }

    /**
     * Executes findAvailableStrings assuming the strings are
     * the names of a classes that should be loaded and returned.
     * <p/>
     * Any class that cannot be loaded will be skipped and placed in the
     * 'resourcesNotLoaded' collection.
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List findAvailableClasses(String uri) throws IOException {
        resourcesNotLoaded.clear();
        List classes = new ArrayList();
        List strings = findAvailableStrings(uri);
        for (int i = 0; i < strings.size(); i++) {
            String className = (String) strings.get(i);
            try {
                Class clazz = classLoader.loadClass(className);
                classes.add(clazz);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return classes;
    }

    /**
     * Executes mapAllStrings assuming the value of each entry in the
     * map is the name of a class that should be loaded.
     * <p/>
     * Any class that cannot be loaded will be cause an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/xmlparsers/xerces
     * META-INF/xmlparsers/crimson
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("xmlparsers");
     * map.contains("xerces");  // true
     * map.contains("crimson");  // true
     * Class xercesClass = map.get("xerces");
     * Class crimsonClass = map.get("crimson");
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Map mapAllClasses(String uri) throws IOException, ClassNotFoundException {
        Map classes = new HashMap();
        Map map = mapAllStrings(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            Class clazz = classLoader.loadClass(className);
            classes.put(string, clazz);
        }
        return classes;
    }

    /**
     * Executes mapAvailableStrings assuming the value of each entry in the
     * map is the name of a class that should be loaded.
     * <p/>
     * Any class that cannot be loaded will be skipped and placed in the
     * 'resourcesNotLoaded' collection.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/xmlparsers/xerces
     * META-INF/xmlparsers/crimson
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAvailableStrings("xmlparsers");
     * map.contains("xerces");  // true
     * map.contains("crimson");  // true
     * Class xercesClass = map.get("xerces");
     * Class crimsonClass = map.get("crimson");
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map mapAvailableClasses(String uri) throws IOException {
        resourcesNotLoaded.clear();
        Map classes = new HashMap();
        Map map = mapAvailableStrings(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            try {
                Class clazz = classLoader.loadClass(className);
                classes.put(string, clazz);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return classes;
    }

    /**
     * Assumes the class specified points to a file in the classpath that contains
     * the name of a class that implements or is a subclass of the specfied class.
     * <p/>
     * Any class that cannot be loaded will be cause an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/java.io.InputStream    # contains the classname org.acme.AcmeInputStream
     * META-INF/java.io.OutputStream
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Class clazz = finder.findImplementation(java.io.InputStream.class);
     * clazz.getName();  // returns "org.acme.AcmeInputStream"
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException            if the URL cannot be read
     * @throws ClassNotFoundException if the class found is not loadable
     * @throws ClassCastException     if the class found is not assignable to the specified superclass or interface
     */
    public Class findImplementation(Class interfase) throws IOException, ClassNotFoundException {
        String className = findString(interfase.getName());
        Class impl = classLoader.loadClass(className);
        if (!interfase.isAssignableFrom(impl)) {
            throw new ClassCastException("Class not of type: " + interfase.getName());
        }
        return impl;
    }

    /**
     * Assumes the class specified points to a file in the classpath that contains
     * the name of a class that implements or is a subclass of the specfied class.
     * <p/>
     * Any class that cannot be loaded or assigned to the specified interface will be cause
     * an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/java.io.InputStream    # contains the classname org.acme.AcmeInputStream
     * META-INF/java.io.InputStream    # contains the classname org.widget.NeatoInputStream
     * META-INF/java.io.InputStream    # contains the classname com.foo.BarInputStream
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List classes = finder.findAllImplementations(java.io.InputStream.class);
     * classes.contains("org.acme.AcmeInputStream");  // true
     * classes.contains("org.widget.NeatoInputStream");  // true
     * classes.contains("com.foo.BarInputStream");  // true
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException            if the URL cannot be read
     * @throws ClassNotFoundException if the class found is not loadable
     * @throws ClassCastException     if the class found is not assignable to the specified superclass or interface
     */
    public List findAllImplementations(Class interfase) throws IOException, ClassNotFoundException {
        List implementations = new ArrayList();
        List strings = findAllStrings(interfase.getName());
        for (int i = 0; i < strings.size(); i++) {
            String className = (String) strings.get(i);
            Class impl = classLoader.loadClass(className);
            if (!interfase.isAssignableFrom(impl)) {
                throw new ClassCastException("Class not of type: " + interfase.getName());
            }
            implementations.add(impl);
        }
        return implementations;
    }

    /**
     * Assumes the class specified points to a file in the classpath that contains
     * the name of a class that implements or is a subclass of the specfied class.
     * <p/>
     * Any class that cannot be loaded or are not assignable to the specified class will be
     * skipped and placed in the 'resourcesNotLoaded' collection.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/java.io.InputStream    # contains the classname org.acme.AcmeInputStream
     * META-INF/java.io.InputStream    # contains the classname org.widget.NeatoInputStream
     * META-INF/java.io.InputStream    # contains the classname com.foo.BarInputStream
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List classes = finder.findAllImplementations(java.io.InputStream.class);
     * classes.contains("org.acme.AcmeInputStream");  // true
     * classes.contains("org.widget.NeatoInputStream");  // true
     * classes.contains("com.foo.BarInputStream");  // true
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List findAvailableImplementations(Class interfase) throws IOException {
        resourcesNotLoaded.clear();
        List implementations = new ArrayList();
        List strings = findAvailableStrings(interfase.getName());
        for (int i = 0; i < strings.size(); i++) {
            String className = (String) strings.get(i);
            try {
                Class impl = classLoader.loadClass(className);
                if (interfase.isAssignableFrom(impl)) {
                    implementations.add(impl);
                } else {
                    resourcesNotLoaded.add(className);
                }
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return implementations;
    }

    /**
     * Assumes the class specified points to a directory in the classpath that holds files
     * containing the name of a class that implements or is a subclass of the specfied class.
     * <p/>
     * Any class that cannot be loaded or assigned to the specified interface will be cause
     * an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/java.net.URLStreamHandler/jar
     * META-INF/java.net.URLStreamHandler/file
     * META-INF/java.net.URLStreamHandler/http
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAllImplementations(java.net.URLStreamHandler.class);
     * Class jarUrlHandler = map.get("jar");
     * Class fileUrlHandler = map.get("file");
     * Class httpUrlHandler = map.get("http");
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException            if the URL cannot be read
     * @throws ClassNotFoundException if the class found is not loadable
     * @throws ClassCastException     if the class found is not assignable to the specified superclass or interface
     */
    public Map mapAllImplementations(Class interfase) throws IOException, ClassNotFoundException {
        Map implementations = new HashMap();
        Map map = mapAllStrings(interfase.getName());
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            Class impl = classLoader.loadClass(className);
            if (!interfase.isAssignableFrom(impl)) {
                throw new ClassCastException("Class not of type: " + interfase.getName());
            }
            implementations.put(string, impl);
        }
        return implementations;
    }

    /**
     * Assumes the class specified points to a directory in the classpath that holds files
     * containing the name of a class that implements or is a subclass of the specfied class.
     * <p/>
     * Any class that cannot be loaded or are not assignable to the specified class will be
     * skipped and placed in the 'resourcesNotLoaded' collection.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/java.net.URLStreamHandler/jar
     * META-INF/java.net.URLStreamHandler/file
     * META-INF/java.net.URLStreamHandler/http
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Map map = finder.mapAllImplementations(java.net.URLStreamHandler.class);
     * Class jarUrlHandler = map.get("jar");
     * Class fileUrlHandler = map.get("file");
     * Class httpUrlHandler = map.get("http");
     *
     * @param interfase a superclass or interface
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map mapAvailableImplementations(Class interfase) throws IOException {
        resourcesNotLoaded.clear();
        Map implementations = new HashMap();
        Map map = mapAvailableStrings(interfase.getName());
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            String className = (String) entry.getValue();
            try {
                Class impl = classLoader.loadClass(className);
                if (interfase.isAssignableFrom(impl)) {
                    implementations.put(string, impl);
                } else {
                    resourcesNotLoaded.add(className);
                }
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(className);
            }
        }
        return implementations;
    }

    /**
     * Finds the corresponding resource and reads it in as a properties file
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/widget.properties
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * Properties widgetProps = finder.findProperties("widget.properties");
     *
     * @param uri
     * @return
     * @throws IOException if the URL cannot be read or is not in properties file format
     */
    public Properties findProperties(String uri) throws IOException {
        String fulluri = path + uri;
        URL resource = getResource(fulluri);
        if (resource == null) {
            throw new IOException("Could not find resource: " + fulluri);
        }
        return loadProperties(resource);
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     * <p/>
     * Any URL that cannot be read in as a properties file will cause an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/app.properties
     * META-INF/app.properties
     * META-INF/app.properties
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List appProps = finder.findAllProperties("app.properties");
     *
     * @param uri
     * @return
     * @throws IOException if the URL cannot be read or is not in properties file format
     */
    public List findAllProperties(String uri) throws IOException {
        String fulluri = path + uri;
        List properties = new ArrayList();
        Enumeration resources = getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = (URL) resources.nextElement();
            Properties props = loadProperties(url);
            properties.add(props);
        }
        return properties;
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     * <p/>
     * Any URL that cannot be read in as a properties file will be added to the
     * 'resourcesNotLoaded' collection.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/app.properties
     * META-INF/app.properties
     * META-INF/app.properties
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List appProps = finder.findAvailableProperties("app.properties");
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public List findAvailableProperties(String uri) throws IOException {
        resourcesNotLoaded.clear();
        String fulluri = path + uri;
        List properties = new ArrayList();
        Enumeration resources = getResources(fulluri);
        while (resources.hasMoreElements()) {
            URL url = (URL) resources.nextElement();
            try {
                Properties props = loadProperties(url);
                properties.add(props);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return properties;
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     * <p/>
     * Any URL that cannot be read in as a properties file will cause an exception to be thrown.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/jdbcDrivers/oracle.properties
     * META-INF/jdbcDrivers/mysql.props
     * META-INF/jdbcDrivers/derby
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List driversList = finder.findAvailableProperties("jdbcDrivers");
     * Properties oracleProps = driversList.get("oracle.properties");
     * Properties mysqlProps = driversList.get("mysql.props");
     * Properties derbyProps = driversList.get("derby");
     *
     * @param uri
     * @return
     * @throws IOException if the URL cannot be read or is not in properties file format
     */
    public Map mapAllProperties(String uri) throws IOException {
        Map propertiesMap = new HashMap();
        Map map = getResourcesMap(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            Properties properties = loadProperties(url);
            propertiesMap.put(string, properties);
        }
        return propertiesMap;
    }

    /**
     * Finds the corresponding resources and reads them in as a properties files
     * <p/>
     * Any URL that cannot be read in as a properties file will be added to the
     * 'resourcesNotLoaded' collection.
     * <p/>
     * Example classpath:
     * <p/>
     * META-INF/jdbcDrivers/oracle.properties
     * META-INF/jdbcDrivers/mysql.props
     * META-INF/jdbcDrivers/derby
     * <p/>
     * ResourceFinder finder = new ResourceFinder("META-INF/");
     * List driversList = finder.findAvailableProperties("jdbcDrivers");
     * Properties oracleProps = driversList.get("oracle.properties");
     * Properties mysqlProps = driversList.get("mysql.props");
     * Properties derbyProps = driversList.get("derby");
     *
     * @param uri
     * @return
     * @throws IOException if classLoader.getResources throws an exception
     */
    public Map mapAvailableProperties(String uri) throws IOException {
        resourcesNotLoaded.clear();
        Map propertiesMap = new HashMap();
        Map map = getResourcesMap(uri);
        for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String string = (String) entry.getKey();
            URL url = (URL) entry.getValue();
            try {
                Properties properties = loadProperties(url);
                propertiesMap.put(string, properties);
            } catch (Exception notAvailable) {
                resourcesNotLoaded.add(url.toExternalForm());
            }
        }
        return propertiesMap;
    }

    public Map getResourcesMap(String uri) throws IOException {
        String basePath = path + uri;
        Map resources = new HashMap();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        Enumeration urls = getResources(basePath);
        while (urls.hasMoreElements()) {
            URL location = (URL) urls.nextElement();
            try {
                if (location.getProtocol().equals("jar")) {
                    readJarEntries(location, basePath, resources);
                } else if (location.getProtocol().equals("file")) {
                    readDirectoryEntries(location, resources);
                }
            } catch (Exception e) {
            }
        }
        return resources;
    }

    private static void readDirectoryEntries(URL location, Map resources) throws MalformedURLException {
        File dir = new File(URLDecoder.decode(location.getPath()));
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.isDirectory()) {
                    String name = file.getName();
                    URL url = file.toURI().toURL();
                    resources.put(name, url);
                }
            }
        }
    }

    private static void readJarEntries(URL location, String basePath, Map resources) throws IOException {
        JarURLConnection conn = (JarURLConnection) location.openConnection();
        JarFile jarfile = null;
        jarfile = conn.getJarFile();
        Enumeration entries = jarfile.entries();
        while (entries != null && entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith(basePath) || name.length() == basePath.length()) {
                continue;
            }
            name = name.substring(basePath.length());
            if (name.contains("/")) {
                continue;
            }
            URL resource = new URL(location, name);
            resources.put(name, resource);
        }
    }

    private Properties loadProperties(URL resource) throws IOException {
        InputStream in = resource.openStream();
        BufferedInputStream reader = null;
        try {
            reader = new BufferedInputStream(in);
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } finally {
            try {
                in.close();
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    private String readContents(URL resource) throws IOException {
        InputStream in = resource.openStream();
        BufferedInputStream reader = null;
        StringBuffer sb = new StringBuffer();
        try {
            reader = new BufferedInputStream(in);
            int b = reader.read();
            while (b != -1) {
                sb.append((char) b);
                b = reader.read();
            }
            return sb.toString().trim();
        } finally {
            try {
                in.close();
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    private URL getResource(String fullUri) {
        if (urls == null) {
            return classLoader.getResource(fullUri);
        }
        return findResource(fullUri, urls);
    }

    private Enumeration getResources(String fulluri) throws IOException {
        if (urls == null) {
            return classLoader.getResources(fulluri);
        }
        Vector resources = new Vector();
        for (int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            URL resource = findResource(fulluri, new URL[] { url });
            if (resource != null) {
                resources.add(resource);
            }
        }
        return resources.elements();
    }

    private URL findResource(String resourceName, URL[] search) {
        for (int i = 0; i < search.length; i++) {
            URL currentUrl = search[i];
            if (currentUrl == null) {
                continue;
            }
            try {
                String protocol = currentUrl.getProtocol();
                if (protocol.equals("jar")) {
                    URL jarURL = ((JarURLConnection) currentUrl.openConnection()).getJarFileURL();
                    JarFile jarFile;
                    JarURLConnection juc;
                    try {
                        juc = (JarURLConnection) new URL("jar", "", jarURL.toExternalForm() + "!/").openConnection();
                        jarFile = juc.getJarFile();
                    } catch (IOException e) {
                        search[i] = null;
                        throw e;
                    }
                    try {
                        juc = (JarURLConnection) new URL("jar", "", jarURL.toExternalForm() + "!/").openConnection();
                        jarFile = juc.getJarFile();
                        String entryName;
                        if (currentUrl.getFile().endsWith("!/")) {
                            entryName = resourceName;
                        } else {
                            String file = currentUrl.getFile();
                            int sepIdx = file.lastIndexOf("!/");
                            if (sepIdx == -1) {
                                search[i] = null;
                                continue;
                            }
                            sepIdx += 2;
                            StringBuffer sb = new StringBuffer(file.length() - sepIdx + resourceName.length());
                            sb.append(file.substring(sepIdx));
                            sb.append(resourceName);
                            entryName = sb.toString();
                        }
                        if (entryName.equals("META-INF/") && jarFile.getEntry("META-INF/MANIFEST.MF") != null) {
                            return targetURL(currentUrl, "META-INF/MANIFEST.MF");
                        }
                        if (jarFile.getEntry(entryName) != null) {
                            return targetURL(currentUrl, resourceName);
                        }
                    } finally {
                        if (!juc.getUseCaches()) {
                            try {
                                jarFile.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                } else if (protocol.equals("file")) {
                    String baseFile = currentUrl.getFile();
                    String host = currentUrl.getHost();
                    int hostLength = 0;
                    if (host != null) {
                        hostLength = host.length();
                    }
                    StringBuffer buf = new StringBuffer(2 + hostLength + baseFile.length() + resourceName.length());
                    if (hostLength > 0) {
                        buf.append("//").append(host);
                    }
                    buf.append(baseFile);
                    String fixedResName = resourceName;
                    while (fixedResName.startsWith("/") || fixedResName.startsWith("\\")) {
                        fixedResName = fixedResName.substring(1);
                    }
                    buf.append(fixedResName);
                    String filename = buf.toString();
                    File file = new File(filename);
                    File file2 = new File(URLDecoder.decode(filename));
                    if (file.exists() || file2.exists()) {
                        return targetURL(currentUrl, fixedResName);
                    }
                } else {
                    URL resourceURL = targetURL(currentUrl, resourceName);
                    URLConnection urlConnection = resourceURL.openConnection();
                    try {
                        urlConnection.getInputStream().close();
                    } catch (SecurityException e) {
                        return null;
                    }
                    if (!resourceURL.getProtocol().equals("http")) {
                        return resourceURL;
                    }
                    int code = ((HttpURLConnection) urlConnection).getResponseCode();
                    if (code >= 200 && code < 300) {
                        return resourceURL;
                    }
                }
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            } catch (SecurityException e) {
            }
        }
        return null;
    }

    private URL targetURL(URL base, String name) throws MalformedURLException {
        StringBuffer sb = new StringBuffer(base.getFile().length() + name.length());
        sb.append(base.getFile());
        sb.append(name);
        String file = sb.toString();
        return new URL(base.getProtocol(), base.getHost(), base.getPort(), file, null);
    }
}
