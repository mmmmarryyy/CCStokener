package org.opensourcephysics.tools;

import java.applet.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.OSPRuntime;
import java.security.AccessControlException;

/**
 * This defines static methods for loading resources.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ResourceLoader {

    protected static ArrayList searchPaths = new ArrayList();

    protected static int maxPaths = 20;

    protected static Hashtable resources = new Hashtable();

    protected static boolean cacheEnabled = false;

    protected static Map zipLoaders = new TreeMap();

    protected static URLClassLoader xsetZipLoader;

    protected static ArrayList extractExtensions = new ArrayList();

    /**
    * Private constructor to prevent instantiation.
    */
    private ResourceLoader() {
    }

    /**
    * Gets a resource specified by name. If no resource is found using the name
    * alone, the searchPaths are searched.
    *
    * @param name the file or URL name
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String name) {
        return getResource(name, true);
    }

    /**
    * Gets a resource specified by name and Class. If no resource is found using
    * the name alone, the searchPaths are searched.
    * Files are searched only if searchFile is true.
    *
    * @param name the file or URL name
    * @param searchFiles true to search files
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String name, boolean searchFiles) {
        return getResource(name, Resource.class, searchFiles);
    }

    /**
    * Gets a resource specified by name and Class. If no resource is found using
    * the name alone, the searchPaths are searched.
    *
    * @param name the file or URL name
    * @param type the Class providing default ClassLoader resource loading
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String name, Class type) {
        return getResource(name, type, true);
    }

    /**
    * Gets a resource specified by name and Class. If no resource is found using
    * the name alone, the searchPaths are searched.
    * Files are searched only if searchFile is true.
    *
    * @param name the file or URL name
    * @param type the Class providing default ClassLoader resource loading
    * @param searchFiles true to search files
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String name, Class type, boolean searchFiles) {
        if ((name == null) || name.equals("")) {
            return null;
        }
        if (name.startsWith("./")) name = name.substring(2);
        Resource res = findResource(name, type, searchFiles);
        if (res != null) {
            return res;
        }
        StringBuffer err = new StringBuffer("Not found: " + name);
        err.append(" [searched " + name);
        for (Iterator it = searchPaths.iterator(); it.hasNext(); ) {
            String path = getPath((String) it.next(), name);
            res = findResource(path, type, searchFiles);
            if (res != null) {
                return res;
            }
            err.append(";" + path);
        }
        err.append("]");
        OSPLog.fine(err.toString());
        return null;
    }

    /**
    * Gets a resource specified by base path and name. If base path is relative
    * and no resource is found using the base alone, the searchPaths are
    * searched.
    *
    * @param basePath the base path
    * @param name the file or URL name
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String basePath, String name) {
        return getResource(basePath, name, Resource.class);
    }

    /**
    * Gets a resource specified by base path and name. If base path is relative
    * and no resource is found using the base alone, the searchPaths are
    * searched. Files are searched only if searchFile is true.
    *
    * @param basePath the base path
    * @param name the file or URL name
    * @param searchFiles true to search files
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String basePath, String name, boolean searchFiles) {
        return getResource(basePath, name, Resource.class, searchFiles);
    }

    /**
    * Gets a resource specified by base path, name and class. If base path is
    * relative and no resource is found using the base alone, the searchPaths
    * are searched.
    *
    * @param basePath the base path
    * @param name the file or URL name
    * @param type the Class providing ClassLoader resource loading
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String basePath, String name, Class type) {
        return getResource(basePath, name, type, true);
    }

    /**
    * Gets a resource specified by base path, name and class. If base path is
    * relative and no resource is found using the base alone, the searchPaths
    * are searched. Files are searched only if searchFile is true.
    *
    * @param basePath the base path
    * @param name the file or URL name
    * @param type the Class providing ClassLoader resource loading
    * @param searchFiles true to search files
    * @return the Resource, or null if none found
    */
    public static Resource getResource(String basePath, String name, Class type, boolean searchFiles) {
        if (basePath == null) {
            return getResource(name, type);
        }
        if (name.startsWith("./")) name = name.substring(2);
        String path = getPath(basePath, name);
        Resource res = findResource(path, type, searchFiles);
        if (res != null) {
            return res;
        }
        if (basePath.startsWith("/") || (basePath.indexOf(":/") > -1)) {
            return null;
        }
        StringBuffer err = new StringBuffer("Not found: " + path);
        err.append(" [searched " + path);
        if (OSPRuntime.applet != null) {
            String docBase = OSPRuntime.applet.getDocumentBase().toExternalForm();
            docBase = XML.getDirectoryPath(docBase) + "/";
            path = getPath(getPath(docBase, basePath), name);
            res = findResource(path, type, searchFiles);
            if (res != null) {
                return res;
            }
            err.append(";" + path);
            String codeBase = OSPRuntime.applet.getCodeBase().toExternalForm();
            if (!codeBase.equals(docBase)) {
                path = getPath(getPath(codeBase, basePath), name);
                res = findResource(path, type, searchFiles);
                if (res != null) {
                    return res;
                }
                err.append(";" + path);
            }
        }
        for (Iterator it = searchPaths.iterator(); it.hasNext(); ) {
            path = getPath(getPath((String) it.next(), basePath), name);
            res = findResource(path, type, searchFiles);
            if (res != null) {
                return res;
            }
            err.append(";" + path);
        }
        err.append("]");
        OSPLog.fine(err.toString());
        return null;
    }

    /**
    * Adds a path at the beginning of the searchPaths list.
    *
    * @param base the base path to add
    */
    public static void addSearchPath(String base) {
        if ((base == null) || base.equals("") || (maxPaths < 1)) {
            return;
        }
        synchronized (searchPaths) {
            if (searchPaths.contains(base)) {
                searchPaths.remove(base);
            } else {
                OSPLog.fine("Added path: " + base);
            }
            searchPaths.add(0, base);
            while (searchPaths.size() > Math.max(maxPaths, 0)) {
                base = (String) searchPaths.get(searchPaths.size() - 1);
                OSPLog.fine("Removed path: " + base);
                searchPaths.remove(base);
            }
        }
    }

    /**
    * Removes a path from the searchPaths list.
    *
    * @param base the base path to remove
    */
    public static void removeSearchPath(String base) {
        if ((base == null) || base.equals("")) {
            return;
        }
        synchronized (searchPaths) {
            if (searchPaths.contains(base)) {
                OSPLog.fine("Removed path: " + base);
                searchPaths.remove(base);
            }
        }
    }

    /**
    * Sets the cacheEnabled property.
    *
    * @param enabled true to enable the cache
    */
    public static void setCacheEnabled(boolean enabled) {
        cacheEnabled = enabled;
    }

    /**
    * Gets the cacheEnabled property.
    *
    * @return true if the cache is enabled
    */
    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
    * Adds an extension to the end of the extractExtensions list.
    * Files with this extension found inside jars are extracted before loading.
    *
    * @param extension the extension to add
    */
    public static void addExtractExtension(String extension) {
        if ((extension == null) || extension.equals("")) {
            return;
        }
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        OSPLog.finest("Added extension: " + extension);
        synchronized (extractExtensions) {
            extractExtensions.add(extension);
        }
    }

    public static InputStream openInputStream(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.openInputStream();
    }

    public static Reader openReader(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.openReader();
    }

    public static String getString(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.getString();
    }

    public static ImageIcon getIcon(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.getIcon();
    }

    public static Image getImage(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.getImage();
    }

    public static BufferedImage getBufferedImage(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.getBufferedImage();
    }

    public static AudioClip getAudioClip(String path) {
        Resource res = getResource(path);
        return (res == null) ? null : res.getAudioClip();
    }

    /**
    * Creates a Resource from a file.
    *
    * @param path the file path
    * @return the resource, if any
    */
    private static Resource createFileResource(String path) {
        if (OSPRuntime.applet != null) {
            return null;
        }
        if ((path.indexOf(".zip") > -1) || (path.indexOf(".jar") > -1)) {
            return null;
        }
        File file = new File(path);
        try {
            if (file.exists() && file.canRead()) {
                Resource res = new Resource(file);
                if (path.endsWith("xset")) {
                    xsetZipLoader = null;
                }
                OSPLog.fine("File: " + XML.forwardSlash(res.getAbsolutePath()));
                return res;
            }
        } catch (AccessControlException ex) {
        }
        return null;
    }

    /**
    * Creates a Resource from a URL.
    *
    * @param path the url path
    * @return the resource, if any
    */
    private static Resource createURLResource(String path) {
        if ((path.indexOf(".zip") > -1) || (path.indexOf(".jar") > -1)) {
            return null;
        }
        Resource res = null;
        if (path.indexOf(":/") > -1) {
            try {
                URL url = new URL(path);
                res = createResource(url);
            } catch (Exception ex) {
            }
        } else {
            if ((OSPRuntime.applet != null) && !path.startsWith("/")) {
                URL docBase = OSPRuntime.applet.getDocumentBase();
                try {
                    URL url = new URL(docBase, path);
                    res = createResource(url);
                } catch (Exception ex) {
                }
                if (res == null) {
                    URL codeBase = OSPRuntime.applet.getCodeBase();
                    String s = XML.getDirectoryPath(docBase.toExternalForm()) + "/";
                    if (!codeBase.toExternalForm().equals(s)) {
                        try {
                            URL url = new URL(codeBase, path);
                            res = createResource(url);
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }
        if (res != null) {
            if (path.endsWith(".xset")) {
                xsetZipLoader = null;
            }
            OSPLog.fine("URL: " + XML.forwardSlash(res.getAbsolutePath()));
        }
        return res;
    }

    /**
    * Creates a Resource from within a zip or jar file.
    *
    * @param path the file path
    * @return the resource, if any
    */
    private static Resource createZipResource(String path) {
        String base = null;
        String fileName = path;
        int i = path.indexOf("zip!/");
        if (i == -1) {
            i = path.indexOf("jar!/");
        }
        if (i > -1) {
            base = path.substring(0, i + 3);
            fileName = path.substring(i + 5);
        }
        if (base == null) {
            if (path.endsWith(".zip") || path.endsWith(".jar")) {
                String name = XML.stripExtension(XML.getName(path));
                base = path;
                fileName = name + ".xset";
            } else if (path.endsWith(".xset")) {
                base = path.substring(0, path.length() - 4) + "zip";
            }
        }
        URLClassLoader zipLoader = null;
        URL url = null;
        if (base != null) {
            zipLoader = (URLClassLoader) zipLoaders.get(base);
            if (zipLoader != null) {
                url = zipLoader.findResource(fileName);
            } else {
                try {
                    URL[] urls = new URL[] { new URL("file", null, base) };
                    zipLoader = new URLClassLoader(urls);
                    url = zipLoader.findResource(fileName);
                    if (url == null) {
                        URL classURL = Resource.class.getResource("/" + base);
                        if (classURL != null) {
                            urls = new URL[] { classURL };
                            zipLoader = new URLClassLoader(urls);
                            url = zipLoader.findResource(fileName);
                        }
                    }
                    if (url != null) {
                        zipLoaders.put(base, zipLoader);
                    }
                } catch (Exception ex) {
                }
            }
        }
        if ((url == null) && (xsetZipLoader != null)) {
            url = xsetZipLoader.findResource(fileName);
            if (url != null) {
                Iterator it = zipLoaders.keySet().iterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    if (zipLoaders.get(key) == xsetZipLoader) {
                        base = (String) key;
                        break;
                    }
                }
            }
        }
        String launchJarPath = OSPRuntime.getLaunchJarPath();
        if ((url == null) && (launchJarPath != null)) {
            zipLoader = (URLClassLoader) zipLoaders.get(launchJarPath);
            if (zipLoader != null) {
                url = zipLoader.findResource(fileName);
            } else {
                try {
                    URL[] urls = new URL[] { new URL("file", null, launchJarPath) };
                    zipLoader = new URLClassLoader(urls);
                    url = zipLoader.findResource(fileName);
                    if (url == null) {
                        URL classURL = Resource.class.getResource("/" + launchJarPath);
                        if (classURL != null) {
                            urls = new URL[] { classURL };
                            zipLoader = new URLClassLoader(urls);
                            url = zipLoader.findResource(fileName);
                        }
                    }
                    if (url != null) {
                        zipLoaders.put(launchJarPath, zipLoader);
                    }
                } catch (Exception ex) {
                }
            }
            if (url != null) base = launchJarPath;
        }
        if (url != null) {
            Iterator it = extractExtensions.iterator();
            while (it.hasNext()) {
                String ext = (String) it.next();
                if (url.getFile().endsWith(ext)) {
                    File zipFile = new File(base);
                    JarTool.extract(zipFile, path, path);
                    return createFileResource(path);
                }
            }
            try {
                Resource res = createResource(url);
                if ((res == null) || (res.getAbsolutePath().indexOf(path) == -1)) {
                    return null;
                }
                if (fileName.endsWith("xset")) {
                    xsetZipLoader = zipLoader;
                }
                OSPLog.fine("Zip: " + XML.forwardSlash(res.getAbsolutePath()));
                return res;
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
    * Creates a Resource from a class resource, typically in a jar file.
    *
    * @param name the resource name
    * @param type the class providing the classloader
    * @return the resource, if any
    */
    private static Resource createClassResource(String name, Class type) {
        if (name.indexOf(":/") != -1) {
            return null;
        }
        String fullName = name;
        int i = name.indexOf("jar!/");
        if (i != -1) {
            name = name.substring(i + 5);
        }
        Resource res = null;
        try {
            URL url = type.getResource("/" + name);
            res = createResource(url);
        } catch (Exception ex) {
        }
        if (res == null) {
            try {
                URL url = type.getResource(name);
                res = createResource(url);
            } catch (Exception ex) {
            }
        }
        if (res != null) {
            String path = XML.forwardSlash(res.getAbsolutePath());
            if ((path.indexOf("/jre") > -1) && (path.indexOf("/lib") > -1)) {
                return null;
            }
            if (path.indexOf(fullName) == -1) {
                return null;
            }
            if (name.endsWith("xset")) {
                xsetZipLoader = null;
            }
            OSPLog.fine("Class resource: " + path);
            OSPRuntime.setLaunchJarPath(path);
        }
        return res;
    }

    /**
    * Creates a Resource.
    *
    * @param url the URL
    * @return the resource, if any
    * @throws IOException
    */
    private static Resource createResource(URL url) throws IOException {
        if (url == null) {
            return null;
        }
        InputStream stream = url.openStream();
        if (stream.read() == -1) {
            return null;
        }
        stream.close();
        return new Resource(url);
    }

    private static Resource findResource(String path, Class type, boolean searchFiles) {
        path = path.replaceAll("/\\./", "/");
        if (type == null) type = Resource.class;
        Resource res = null;
        if (cacheEnabled) {
            res = (Resource) resources.get(path);
            if (res != null && (searchFiles || res.getFile() == null)) {
                OSPLog.finest("Found in cache: " + path);
                return res;
            }
        }
        if ((searchFiles && (res = createFileResource(path)) != null) || (res = createURLResource(path)) != null || (res = createZipResource(path)) != null || (res = createClassResource(path, type)) != null) {
            if (cacheEnabled) {
                resources.put(path, res);
            }
            return res;
        }
        return null;
    }

    /**
    * Gets a path from a base path and file name.
    *
    * @param base the base path
    * @param name the file name
    * @return the path
    */
    private static String getPath(String base, String name) {
        if (base == null) {
            base = "";
        }
        if (base.endsWith(".jar") || base.endsWith(".zip")) {
            base += "!";
        }
        String path = XML.getResolvedPath(name, base);
        if ((System.getProperty("os.name").indexOf("Mac") > -1) && path.startsWith("file:/") && !path.startsWith("file:///")) {
            path = path.substring(6);
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            path = "file:///" + path;
        }
        return path;
    }
}
