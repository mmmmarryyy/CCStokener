package com.ivis.xprocess.framework.schema;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ivis.xprocess.core.Xtask;

/**
 * Build a schema class list of elements from a jar.
 *
 */
public class JarSchemaSource implements SchemaSource {

    private static final Logger logger = Logger.getLogger(JarSchemaSource.class.getName());

    public Set<String> getClassNames() {
        Set<String> ret = null;
        try {
            ret = getFileNames(SchemaBuilder.CORE_PACKAGE_PATH, Xtask.class.getSimpleName() + SchemaBuilder.CLASS_EXTENSION, SchemaBuilder.CLASS_EXTENSION);
        } catch (Exception e) {
            logger.severe("Can't get list of core classes");
        }
        return ret;
    }

    /**
     * @param packageName
     * @param anchor
     * @param targetExtension
     * @return a set of all xelement implementation names
     * @throws Exception
     */
    public Set<String> getFileNames(String packageName, String anchor, String targetExtension) throws Exception {
        Set<String> classNames = new HashSet<String>();
        ClassLoader newLoader = Thread.currentThread().getContextClassLoader();
        File root = null;
        try {
            String sep = packageName.endsWith("/") ? "" : "/";
            String resource = packageName + sep + anchor;
            URL url = newLoader.getResource(resource);
            if (url == null) {
                throw new Exception("can't find resource '" + resource + "' in classpath");
            }
            URI uri = url.toURI();
            String scheme = uri.getScheme();
            if (scheme.equalsIgnoreCase("jar")) {
                classNames = getEntriesFromJar(url, packageName, targetExtension);
                return classNames;
            }
            root = new File(uri);
            root = root.getParentFile();
            if (root.isDirectory()) {
                File child;
                for (String className : root.list()) {
                    child = new File(className);
                    if (!child.isDirectory() && className.endsWith(targetExtension)) {
                        classNames.add(className);
                    }
                }
            } else {
                throw new Exception("Not a directory:" + root);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem obtaining schema class list.", e);
            throw new Exception(e);
        }
        return classNames;
    }

    private HashSet<String> getEntriesFromJar(URL url, String searchPrefix, String searchExtension) throws Exception {
        HashSet<String> ret = new HashSet<String>();
        JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
        JarFile jarfile = jarConnection.getJarFile();
        Enumeration<JarEntry> entries = jarfile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            int pathIndex = name.lastIndexOf('/');
            String path = name.substring(0, pathIndex + 1);
            if (path.equals(searchPrefix)) {
                String shortName = name.substring(pathIndex + 1);
                if (shortName.endsWith(searchExtension)) {
                    ret.add(shortName);
                }
            }
        }
        return ret;
    }
}
