package jmax.project;

import java.util.*;
import java.io.*;
import java.net.*;
import jmax.commons.*;
import jmax.registry.*;

/**
 * The Package finder class.
 * Looks for new packages in the packagePath, and handle downloading
 * and unjarring when needed, and return a new MaxPackage instance 
 * that is not loaded and with meta data not loaded.
 * Manage the list of searched URLs.
 */
class MaxPackageFinder {

    class ProxyPreferencesListener implements RegistryPathListener {

        public void elementLoaded(RegistryElement element) {
            String proxyUrl = element.getProperty("host");
            String proxyPort = element.getProperty("port");
            String nonProxyHosts = element.getProperty("nonProxyHosts");
            Properties settings = System.getProperties();
            settings.put("http.proxyHost", proxyUrl);
            settings.put("http.proxyPort", proxyPort);
            settings.put("http.nonProxyHosts", nonProxyHosts);
        }
    }

    class PathElement {

        boolean valid;

        String path;

        File file;

        URL url;

        PathElement(File file) {
            path = null;
            url = null;
            this.file = file;
            valid = file.exists();
        }

        PathElement(String path) throws MaxError {
            this.path = path;
            if (path.startsWith("http:")) {
                try {
                    url = new URL(path);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("HEAD");
                    valid = (con.getResponseCode() == HttpURLConnection.HTTP_OK);
                } catch (Exception e) {
                    valid = false;
                }
            } else {
                if (path.startsWith("jmax:")) file = new File(Registry.resolveJMaxURI(path)); else file = new File(path);
                valid = file.exists();
            }
        }

        void setValid(boolean valid) {
            this.valid = valid;
        }

        boolean isValid() {
            return valid;
        }

        boolean isHttp() {
            return (url != null);
        }

        boolean isFile() {
            return ((file != null) && file.isFile());
        }

        boolean isDirectory() {
            return ((file != null) && file.isDirectory());
        }

        File getFile() {
            return file;
        }

        File getDirectory() {
            return file;
        }

        File getFileFor(String name) {
            if (file.isFile()) return file; else {
                final String suffix = MaxPackageConstants.PACKAGE_JAR_SUFFIX;
                return new File(file + File.separator + name + suffix);
            }
        }

        File getDirectoryFor(String name) {
            return new File(file + File.separator + name);
        }

        URL getURLFor(String name) {
            try {
                final String suffix = MaxPackageConstants.PACKAGE_JAR_SUFFIX;
                return new URL(path + File.separator + name + suffix);
            } catch (java.net.MalformedURLException e) {
                return null;
            }
        }

        public String toString() {
            if (url != null) return "#<PathElement " + url + " " + hashCode() + ">"; else if (file != null) return "#<PathElement " + file + " " + hashCode() + ">"; else if (path != null) return "#<PathElement " + path + " " + hashCode() + ">"; else return "#<PathElement unknown " + hashCode() + ">";
        }
    }

    class PathTableListener implements RegistryPathListener {

        public void elementLoaded(RegistryElement element) throws MaxError {
            String uriProp = MaxPackageConstants.PACKAGEPATH_URI_ATTRIBUTE;
            addPackagePath(element.getProperty(uriProp));
        }
    }

    private boolean useNetwork;

    private boolean useCache;

    private MaxPackageManager manager;

    List<PathElement> packagePathTable = new LinkedList<PathElement>();

    MaxPackageFinder(MaxPackageManager manager, boolean useNetwork, boolean useCache) {
        this.manager = manager;
        this.useCache = useCache;
        this.useNetwork = useNetwork;
        Registry.addPathListener("/jmax/preferences/proxy", new ProxyPreferencesListener());
        Registry.addPathListener("/jmax/preferences/packages/path", new PathTableListener());
    }

    void addPackagePath(String name) throws MaxError {
        packagePathTable.add(new PathElement(name));
    }

    /** Find a package from an explicitly given jar filename or from a directory.
      First check the cache anyway.
  */
    MaxPackage findPackage(String name, File file) throws MaxError {
        PathElement path = new PathElement(file);
        return findPackage(name, path);
    }

    /** Find a package from an explicitly given url.
      First check the cache anyway.
  */
    MaxPackage findPackage(String name, String url) throws MaxError {
        PathElement path = new PathElement(url);
        return findPackage(name, path);
    }

    private MaxPackage findPackage(String name, PathElement path) throws MaxError {
        MaxPackage maxPackage;
        if (!path.isValid()) return null;
        if (path.isHttp()) {
            if (!useNetwork) return null;
            maxPackage = makeJarHttpPackage(path, name);
            if (maxPackage != null) return maxPackage; else return null;
        } else if (path.isDirectory()) {
            maxPackage = makeDirPackage(path, name, true);
            if (maxPackage != null) return maxPackage; else return null;
        } else if (path.isFile()) {
            maxPackage = makeJarFilePackage(path, name);
            if (maxPackage != null) return maxPackage; else return null;
        } else {
            return null;
        }
    }

    MaxPackage findPackage(String name) throws MaxError {
        MaxPackage maxPackage;
        for (PathElement path : packagePathTable) {
            if (!path.isValid()) continue;
            if (path.isHttp()) {
                if (!useNetwork) continue;
                maxPackage = makeJarHttpPackage(path, name);
                if (maxPackage != null) return maxPackage;
            } else {
                maxPackage = makeJarFilePackage(path, name);
                if (maxPackage != null) return maxPackage;
                maxPackage = makeDirPackage(path, name, false);
                if (maxPackage != null) return maxPackage;
            }
        }
        return null;
    }

    /** Try to build a package from the cache (as a directory).
   * If the path is a file, check first for a directory named
   * as the package, including a file named package.jpkd
   */
    private MaxPackage makeCachePackage(String name) throws RegistryException {
        final String suffix = MaxPackageConstants.PACKAGE_DESCRIPTOR_SUFFIX;
        File cacheDir = new File(Registry.resolveJMaxURI(MaxPackageConstants.PACKAGE_CACHE_DIR_URL));
        File packageDir = new File(cacheDir, name);
        File packageXmlFile = new File(packageDir, name + suffix);
        if (packageDir.exists() && packageDir.isDirectory() && packageXmlFile.exists() && packageXmlFile.isFile()) {
            MaxPackage jMaxPackage = new MaxPackage(name, manager);
            jMaxPackage.setRootDirectory(packageDir);
            return jMaxPackage;
        } else return null;
    }

    /** Try to build a package from a directory.
   * If the path is a file, check first for a directory named
   * as the package, including a file named package.xml
   */
    private MaxPackage makeDirPackage(PathElement path, String name, boolean directPath) {
        final String suffix = MaxPackageConstants.PACKAGE_DESCRIPTOR_SUFFIX;
        File packageDir;
        File packageXmlFile;
        if (directPath) packageDir = path.getDirectory(); else packageDir = path.getDirectoryFor(name);
        packageXmlFile = new File(packageDir, name + suffix);
        if (packageDir.exists() && packageDir.isDirectory() && packageXmlFile.exists() && packageXmlFile.isFile()) {
            MaxPackage jMaxPackage = new MaxPackage(name, manager);
            jMaxPackage.setRootDirectory(packageDir);
            return jMaxPackage;
        } else return null;
    }

    /**
   * Try to build a package from a jar.
   *
   * The unpack method will take care of deleting older version
   * of the same package.
   */
    private MaxPackage makeJarFilePackage(PathElement path, String name) throws MaxError {
        File packageJarFile = path.getFileFor(name);
        try {
            if (packageJarFile.exists() && packageJarFile.isFile()) {
                MaxJarFilePackage jMaxPackage = new MaxJarFilePackage(name, packageJarFile, manager);
                jMaxPackage.unpack();
                return jMaxPackage;
            } else return null;
        } catch (IOException e) {
            throw new MaxError("Cannot load jar package", "File error looking for package " + name + " in path " + path);
        }
    }

    /**
   * Try to build a package from a remote http jar.
   *
   * First, check the cache, and reuse the package if there.
   */
    private MaxPackage makeJarHttpPackage(PathElement path, String name) throws MaxError {
        if (useCache) {
            MaxPackage cachePackage = makeCachePackage(name);
            if (cachePackage != null) return cachePackage;
        }
        URL packageUrl = path.getURLFor(name);
        try {
            MaxJarHttpPackage jMaxPackage = new MaxJarHttpPackage(name, packageUrl, manager);
            if (jMaxPackage.unpack()) return jMaxPackage; else return null;
        } catch (IOException e) {
            path.setValid(false);
            throw new MaxError("Cannot load jar package", "Network error looking for package " + name + " in path " + path);
        }
    }
}
