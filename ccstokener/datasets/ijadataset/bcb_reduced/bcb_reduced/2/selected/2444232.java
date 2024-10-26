package sun.rmi.server;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.rmi.server.LogStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import sun.rmi.runtime.Log;
import sun.security.action.GetPropertyAction;

/**
 * <code>LoaderHandler</code> provides the implementation of the static
 * methods of the <code>java.rmi.server.RMIClassLoader</code> class.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @author      Laird Dornin
 */
public final class LoaderHandler {

    /** RMI class loader log level */
    static final int logLevel = LogStream.parseLevel(java.security.AccessController.doPrivileged(new GetPropertyAction("sun.rmi.loader.logLevel")));

    static final Log loaderLog = Log.getLog("sun.rmi.loader", "loader", LoaderHandler.logLevel);

    /**
     * value of "java.rmi.server.codebase" property, as cached at class
     * initialization time.  It may contain malformed URLs.
     */
    private static String codebaseProperty = null;

    static {
        String prop = java.security.AccessController.doPrivileged(new GetPropertyAction("java.rmi.server.codebase"));
        if (prop != null && prop.trim().length() > 0) {
            codebaseProperty = prop;
        }
    }

    /** list of URLs represented by the codebase property, if valid */
    private static URL[] codebaseURLs = null;

    /** table of class loaders that use codebase property for annotation */
    private static final Map<ClassLoader, Void> codebaseLoaders = Collections.synchronizedMap(new IdentityHashMap<ClassLoader, Void>(5));

    static {
        for (ClassLoader codebaseLoader = ClassLoader.getSystemClassLoader(); codebaseLoader != null; codebaseLoader = codebaseLoader.getParent()) {
            codebaseLoaders.put(codebaseLoader, null);
        }
    }

    /**
     * table mapping codebase URL path and context class loader pairs
     * to class loader instances.  Entries hold class loaders with weak
     * references, so this table does not prevent loaders from being
     * garbage collected.
     */
    private static final HashMap<LoaderKey, LoaderEntry> loaderTable = new HashMap<LoaderKey, LoaderEntry>(5);

    /** reference queue for cleared class loader entries */
    private static final ReferenceQueue<Loader> refQueue = new ReferenceQueue<Loader>();

    private LoaderHandler() {
    }

    /**
     * Returns an array of URLs initialized with the value of the
     * java.rmi.server.codebase property as the URL path.
     */
    private static synchronized URL[] getDefaultCodebaseURLs() throws MalformedURLException {
        if (codebaseURLs == null) {
            if (codebaseProperty != null) {
                codebaseURLs = pathToURLs(codebaseProperty);
            } else {
                codebaseURLs = new URL[0];
            }
        }
        return codebaseURLs;
    }

    /**
     * Load a class from a network location (one or more URLs),
     * but first try to resolve the named class through the given
     * "default loader".
     */
    public static Class loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if (loaderLog.isLoggable(Log.BRIEF)) {
            loaderLog.log(Log.BRIEF, "name = \"" + name + "\", " + "codebase = \"" + (codebase != null ? codebase : "") + "\"" + (defaultLoader != null ? ", defaultLoader = " + defaultLoader : ""));
        }
        URL[] urls;
        if (codebase != null) {
            urls = pathToURLs(codebase);
        } else {
            urls = getDefaultCodebaseURLs();
        }
        if (defaultLoader != null) {
            try {
                Class c = Class.forName(name, false, defaultLoader);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE, "class \"" + name + "\" found via defaultLoader, " + "defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException e) {
            }
        }
        return loadClass(urls, name);
    }

    /**
     * Returns the class annotation (representing the location for
     * a class) that RMI will use to annotate the call stream when
     * marshalling objects of the given class.
     */
    public static String getClassAnnotation(Class cl) {
        String name = cl.getName();
        int nameLength = name.length();
        if (nameLength > 0 && name.charAt(0) == '[') {
            int i = 1;
            while (nameLength > i && name.charAt(i) == '[') {
                i++;
            }
            if (nameLength > i && name.charAt(i) != 'L') {
                return null;
            }
        }
        ClassLoader loader = cl.getClassLoader();
        if (loader == null || codebaseLoaders.containsKey(loader)) {
            return codebaseProperty;
        }
        String annotation = null;
        if (loader instanceof Loader) {
            annotation = ((Loader) loader).getClassAnnotation();
        } else if (loader instanceof URLClassLoader) {
            try {
                URL[] urls = ((URLClassLoader) loader).getURLs();
                if (urls != null) {
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        Permissions perms = new Permissions();
                        for (int i = 0; i < urls.length; i++) {
                            Permission p = urls[i].openConnection().getPermission();
                            if (p != null) {
                                if (!perms.implies(p)) {
                                    sm.checkPermission(p);
                                    perms.add(p);
                                }
                            }
                        }
                    }
                    annotation = urlsToPath(urls);
                }
            } catch (SecurityException e) {
            } catch (IOException e) {
            }
        }
        if (annotation != null) {
            return annotation;
        } else {
            return codebaseProperty;
        }
    }

    /**
     * Returns a classloader that loads classes from the given codebase URL
     * path.  The parent classloader of the returned classloader is the
     * context class loader.
     */
    public static ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        ClassLoader parent = getRMIContextClassLoader();
        URL[] urls;
        if (codebase != null) {
            urls = pathToURLs(codebase);
        } else {
            urls = getDefaultCodebaseURLs();
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        } else {
            return parent;
        }
        Loader loader = lookupLoader(urls, parent);
        if (loader != null) {
            loader.checkPermissions();
        }
        return loader;
    }

    /**
     * Return the security context of the given class loader.
     */
    public static Object getSecurityContext(ClassLoader loader) {
        if (loader instanceof Loader) {
            URL[] urls = ((Loader) loader).getURLs();
            if (urls.length > 0) {
                return urls[0];
            }
        }
        return null;
    }

    /**
     * Register a class loader as one whose classes should always be
     * annotated with the value of the "java.rmi.server.codebase" property.
     */
    public static void registerCodebaseLoader(ClassLoader loader) {
        codebaseLoaders.put(loader, null);
    }

    /**
     * Load a class from the RMI class loader corresponding to the given
     * codebase URL path in the current execution context.
     */
    private static Class loadClass(URL[] urls, String name) throws ClassNotFoundException {
        ClassLoader parent = getRMIContextClassLoader();
        if (loaderLog.isLoggable(Log.VERBOSE)) {
            loaderLog.log(Log.VERBOSE, "(thread context class loader: " + parent + ")");
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            try {
                Class c = Class.forName(name, false, parent);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE, "class \"" + name + "\" found via " + "thread context class loader " + "(no security manager: codebase disabled), " + "defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException e) {
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF, "class \"" + name + "\" not found via " + "thread context class loader " + "(no security manager: codebase disabled)", e);
                }
                throw new ClassNotFoundException(e.getMessage() + " (no security manager: RMI class loader disabled)", e.getException());
            }
        }
        Loader loader = lookupLoader(urls, parent);
        try {
            if (loader != null) {
                loader.checkPermissions();
            }
        } catch (SecurityException e) {
            try {
                Class c = Class.forName(name, false, parent);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE, "class \"" + name + "\" found via " + "thread context class loader " + "(access to codebase denied), " + "defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException unimportant) {
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF, "class \"" + name + "\" not found via " + "thread context class loader " + "(access to codebase denied)", e);
                }
                throw new ClassNotFoundException("access to class loader denied", e);
            }
        }
        try {
            Class c = Class.forName(name, false, loader);
            if (loaderLog.isLoggable(Log.VERBOSE)) {
                loaderLog.log(Log.VERBOSE, "class \"" + name + "\" " + "found via codebase, " + "defined by " + c.getClassLoader());
            }
            return c;
        } catch (ClassNotFoundException e) {
            if (loaderLog.isLoggable(Log.BRIEF)) {
                loaderLog.log(Log.BRIEF, "class \"" + name + "\" not found via codebase", e);
            }
            throw e;
        }
    }

    /**
     * Define and return a dynamic proxy class in a class loader with
     * URLs supplied in the given location.  The proxy class will
     * implement interface classes named by the given array of
     * interface names.
     */
    public static Class loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if (loaderLog.isLoggable(Log.BRIEF)) {
            loaderLog.log(Log.BRIEF, "interfaces = " + Arrays.asList(interfaces) + ", " + "codebase = \"" + (codebase != null ? codebase : "") + "\"" + (defaultLoader != null ? ", defaultLoader = " + defaultLoader : ""));
        }
        ClassLoader parent = getRMIContextClassLoader();
        if (loaderLog.isLoggable(Log.VERBOSE)) {
            loaderLog.log(Log.VERBOSE, "(thread context class loader: " + parent + ")");
        }
        URL[] urls;
        if (codebase != null) {
            urls = pathToURLs(codebase);
        } else {
            urls = getDefaultCodebaseURLs();
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            try {
                Class c = loadProxyClass(interfaces, defaultLoader, parent, false);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE, "(no security manager: codebase disabled) " + "proxy class defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException e) {
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF, "(no security manager: codebase disabled) " + "proxy class resolution failed", e);
                }
                throw new ClassNotFoundException(e.getMessage() + " (no security manager: RMI class loader disabled)", e.getException());
            }
        }
        Loader loader = lookupLoader(urls, parent);
        try {
            if (loader != null) {
                loader.checkPermissions();
            }
        } catch (SecurityException e) {
            try {
                Class c = loadProxyClass(interfaces, defaultLoader, parent, false);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE, "(access to codebase denied) " + "proxy class defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException unimportant) {
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF, "(access to codebase denied) " + "proxy class resolution failed", e);
                }
                throw new ClassNotFoundException("access to class loader denied", e);
            }
        }
        try {
            Class c = loadProxyClass(interfaces, defaultLoader, loader, true);
            if (loaderLog.isLoggable(Log.VERBOSE)) {
                loaderLog.log(Log.VERBOSE, "proxy class defined by " + c.getClassLoader());
            }
            return c;
        } catch (ClassNotFoundException e) {
            if (loaderLog.isLoggable(Log.BRIEF)) {
                loaderLog.log(Log.BRIEF, "proxy class resolution failed", e);
            }
            throw e;
        }
    }

    /**
     * Define a proxy class in the default loader if appropriate.
     * Define the class in an RMI class loader otherwise.  The proxy
     * class will implement classes which are named in the supplied
     * interfaceNames.
     */
    private static Class loadProxyClass(String[] interfaceNames, ClassLoader defaultLoader, ClassLoader codebaseLoader, boolean preferCodebase) throws ClassNotFoundException {
        ClassLoader proxyLoader = null;
        Class[] classObjs = new Class[interfaceNames.length];
        boolean[] nonpublic = { false };
        defaultLoaderCase: if (defaultLoader != null) {
            try {
                proxyLoader = loadProxyInterfaces(interfaceNames, defaultLoader, classObjs, nonpublic);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    ClassLoader[] definingLoaders = new ClassLoader[classObjs.length];
                    for (int i = 0; i < definingLoaders.length; i++) {
                        definingLoaders[i] = classObjs[i].getClassLoader();
                    }
                    loaderLog.log(Log.VERBOSE, "proxy interfaces found via defaultLoader, " + "defined by " + Arrays.asList(definingLoaders));
                }
            } catch (ClassNotFoundException e) {
                break defaultLoaderCase;
            }
            if (!nonpublic[0]) {
                if (preferCodebase) {
                    try {
                        return Proxy.getProxyClass(codebaseLoader, classObjs);
                    } catch (IllegalArgumentException e) {
                    }
                }
                proxyLoader = defaultLoader;
            }
            return loadProxyClass(proxyLoader, classObjs);
        }
        nonpublic[0] = false;
        proxyLoader = loadProxyInterfaces(interfaceNames, codebaseLoader, classObjs, nonpublic);
        if (loaderLog.isLoggable(Log.VERBOSE)) {
            ClassLoader[] definingLoaders = new ClassLoader[classObjs.length];
            for (int i = 0; i < definingLoaders.length; i++) {
                definingLoaders[i] = classObjs[i].getClassLoader();
            }
            loaderLog.log(Log.VERBOSE, "proxy interfaces found via codebase, " + "defined by " + Arrays.asList(definingLoaders));
        }
        if (!nonpublic[0]) {
            proxyLoader = codebaseLoader;
        }
        return loadProxyClass(proxyLoader, classObjs);
    }

    /**
     * Define a proxy class in the given class loader.  The proxy
     * class will implement the given interfaces Classes.
     */
    private static Class loadProxyClass(ClassLoader loader, Class[] interfaces) throws ClassNotFoundException {
        try {
            return Proxy.getProxyClass(loader, interfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException("error creating dynamic proxy class", e);
        }
    }

    private static ClassLoader loadProxyInterfaces(String[] interfaces, ClassLoader loader, Class[] classObjs, boolean[] nonpublic) throws ClassNotFoundException {
        ClassLoader nonpublicLoader = null;
        for (int i = 0; i < interfaces.length; i++) {
            Class cl = (classObjs[i] = Class.forName(interfaces[i], false, loader));
            if (!Modifier.isPublic(cl.getModifiers())) {
                ClassLoader current = cl.getClassLoader();
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE, "non-public interface \"" + interfaces[i] + "\" defined by " + current);
                }
                if (!nonpublic[0]) {
                    nonpublicLoader = current;
                    nonpublic[0] = true;
                } else if (current != nonpublicLoader) {
                    throw new IllegalAccessError("non-public interfaces defined in different " + "class loaders");
                }
            }
        }
        return nonpublicLoader;
    }

    /**
     * Convert a string containing a space-separated list of URLs into a
     * corresponding array of URL objects, throwing a MalformedURLException
     * if any of the URLs are invalid.
     */
    private static URL[] pathToURLs(String path) throws MalformedURLException {
        synchronized (pathToURLsCache) {
            Object[] v = pathToURLsCache.get(path);
            if (v != null) {
                return ((URL[]) v[0]);
            }
        }
        StringTokenizer st = new StringTokenizer(path);
        URL[] urls = new URL[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            urls[i] = new URL(st.nextToken());
        }
        synchronized (pathToURLsCache) {
            pathToURLsCache.put(path, new Object[] { urls, new SoftReference<String>(path) });
        }
        return urls;
    }

    /** map from weak(key=string) to [URL[], soft(key)] */
    private static final Map<String, Object[]> pathToURLsCache = new WeakHashMap<String, Object[]>(5);

    /**
     * Convert an array of URL objects into a corresponding string
     * containing a space-separated list of URLs.
     *
     * Note that if the array has zero elements, the return value is
     * null, not the empty string.
     */
    private static String urlsToPath(URL[] urls) {
        if (urls.length == 0) {
            return null;
        } else if (urls.length == 1) {
            return urls[0].toExternalForm();
        } else {
            StringBuffer path = new StringBuffer(urls[0].toExternalForm());
            for (int i = 1; i < urls.length; i++) {
                path.append(' ');
                path.append(urls[i].toExternalForm());
            }
            return path.toString();
        }
    }

    /**
     * Return the class loader to be used as the parent for an RMI class
     * loader used in the current execution context.
     */
    private static ClassLoader getRMIContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Look up the RMI class loader for the given codebase URL path
     * and the given parent class loader.  A new class loader instance
     * will be created and returned if no match is found.
     */
    private static Loader lookupLoader(final URL[] urls, final ClassLoader parent) {
        LoaderEntry entry;
        Loader loader;
        synchronized (LoaderHandler.class) {
            while ((entry = (LoaderEntry) refQueue.poll()) != null) {
                if (!entry.removed) {
                    loaderTable.remove(entry.key);
                }
            }
            LoaderKey key = new LoaderKey(urls, parent);
            entry = loaderTable.get(key);
            if (entry == null || (loader = entry.get()) == null) {
                if (entry != null) {
                    loaderTable.remove(key);
                    entry.removed = true;
                }
                AccessControlContext acc = getLoaderAccessControlContext(urls);
                loader = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Loader>() {

                    public Loader run() {
                        return new Loader(urls, parent);
                    }
                }, acc);
                entry = new LoaderEntry(key, loader);
                loaderTable.put(key, entry);
            }
        }
        return loader;
    }

    /**
     * LoaderKey holds a codebase URL path and parent class loader pair
     * used to look up RMI class loader instances in its class loader cache.
     */
    private static class LoaderKey {

        private URL[] urls;

        private ClassLoader parent;

        private int hashValue;

        public LoaderKey(URL[] urls, ClassLoader parent) {
            this.urls = urls;
            this.parent = parent;
            if (parent != null) {
                hashValue = parent.hashCode();
            }
            for (int i = 0; i < urls.length; i++) {
                hashValue ^= urls[i].hashCode();
            }
        }

        public int hashCode() {
            return hashValue;
        }

        public boolean equals(Object obj) {
            if (obj instanceof LoaderKey) {
                LoaderKey other = (LoaderKey) obj;
                if (parent != other.parent) {
                    return false;
                }
                if (urls == other.urls) {
                    return true;
                }
                if (urls.length != other.urls.length) {
                    return false;
                }
                for (int i = 0; i < urls.length; i++) {
                    if (!urls[i].equals(other.urls[i])) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * LoaderEntry contains a weak reference to an RMIClassLoader.  The
     * weak reference is registered with the private static "refQueue"
     * queue.  The entry contains the codebase URL path and parent class
     * loader key for the loader so that the mapping can be removed from
     * the table efficiently when the weak reference is cleared.
     */
    private static class LoaderEntry extends WeakReference<Loader> {

        public LoaderKey key;

        /**
         * set to true if the entry has been removed from the table
         * because it has been replaced, so it should not be attempted
         * to be removed again
         */
        public boolean removed = false;

        public LoaderEntry(LoaderKey key, Loader loader) {
            super(loader, refQueue);
            this.key = key;
        }
    }

    /**
     * Return the access control context that a loader for the given
     * codebase URL path should execute with.
     */
    private static AccessControlContext getLoaderAccessControlContext(URL[] urls) {
        PermissionCollection perms = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<PermissionCollection>() {

            public PermissionCollection run() {
                CodeSource codesource = new CodeSource(null, (java.security.cert.Certificate[]) null);
                Policy p = java.security.Policy.getPolicy();
                if (p != null) {
                    return p.getPermissions(codesource);
                } else {
                    return new Permissions();
                }
            }
        });
        perms.add(new RuntimePermission("createClassLoader"));
        perms.add(new java.util.PropertyPermission("java.*", "read"));
        addPermissionsForURLs(urls, perms, true);
        ProtectionDomain pd = new ProtectionDomain(new CodeSource((urls.length > 0 ? urls[0] : null), (java.security.cert.Certificate[]) null), perms);
        return new AccessControlContext(new ProtectionDomain[] { pd });
    }

    /**
     * Adds to the specified permission collection the permissions
     * necessary to load classes from a loader with the specified URL
     * path; if "forLoader" is true, also adds URL-specific
     * permissions necessary for the security context that such a
     * loader operates within, such as permissions necessary for
     * granting automatic permissions to classes defined by the
     * loader.  A given permission is only added to the collection if
     * it is not already implied by the collection.
     */
    private static void addPermissionsForURLs(URL[] urls, PermissionCollection perms, boolean forLoader) {
        for (int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            try {
                URLConnection urlConnection = url.openConnection();
                Permission p = urlConnection.getPermission();
                if (p != null) {
                    if (p instanceof FilePermission) {
                        String path = p.getName();
                        int endIndex = path.lastIndexOf(File.separatorChar);
                        if (endIndex != -1) {
                            path = path.substring(0, endIndex + 1);
                            if (path.endsWith(File.separator)) {
                                path += "-";
                            }
                            Permission p2 = new FilePermission(path, "read");
                            if (!perms.implies(p2)) {
                                perms.add(p2);
                            }
                            perms.add(new FilePermission(path, "read"));
                        } else {
                            if (!perms.implies(p)) {
                                perms.add(p);
                            }
                        }
                    } else {
                        if (!perms.implies(p)) {
                            perms.add(p);
                        }
                        if (forLoader) {
                            URL hostURL = url;
                            for (URLConnection conn = urlConnection; conn instanceof JarURLConnection; ) {
                                hostURL = ((JarURLConnection) conn).getJarFileURL();
                                conn = hostURL.openConnection();
                            }
                            String host = hostURL.getHost();
                            if (host != null && p.implies(new SocketPermission(host, "resolve"))) {
                                Permission p2 = new SocketPermission(host, "connect,accept");
                                if (!perms.implies(p2)) {
                                    perms.add(p2);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * Loader is the actual class of the RMI class loaders created
     * by the RMIClassLoader static methods.
     */
    private static class Loader extends URLClassLoader {

        /** parent class loader, kept here as an optimization */
        private ClassLoader parent;

        /** string form of loader's codebase URL path, also an optimization */
        private String annotation;

        /** permissions required to access loader through public API */
        private Permissions permissions;

        private Loader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            this.parent = parent;
            permissions = new Permissions();
            addPermissionsForURLs(urls, permissions, false);
            annotation = urlsToPath(urls);
        }

        /**
         * Return the string to be annotated with all classes loaded from
         * this class loader.
         */
        public String getClassAnnotation() {
            return annotation;
        }

        /**
         * Check that the current access control context has all of the
         * permissions necessary to load classes from this loader.
         */
        private void checkPermissions() {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                Enumeration enum_ = permissions.elements();
                while (enum_.hasMoreElements()) {
                    sm.checkPermission((Permission) enum_.nextElement());
                }
            }
        }

        /**
         * Return the permissions to be granted to code loaded from the
         * given code source.
         */
        protected PermissionCollection getPermissions(CodeSource codesource) {
            PermissionCollection perms = super.getPermissions(codesource);
            return perms;
        }

        /**
         * Return a string representation of this loader (useful for
         * debugging).
         */
        public String toString() {
            return super.toString() + "[\"" + annotation + "\"]";
        }
    }
}
