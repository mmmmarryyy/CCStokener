package org.apache.harmony.rmi;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.server.LoaderHandler;
import java.rmi.server.RMIClassLoaderSpi;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.harmony.rmi.common.GetStringPropAction;
import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;
import org.apache.harmony.rmi.internal.nls.Messages;

/**
 * Default implementation of RMIClassLoaderSpi.
 *
 * @author  Mikhail A. Markov
 */
public class DefaultRMIClassLoaderSpi extends RMIClassLoaderSpi implements LoaderHandler {

    private static String userCodeBase;

    private static final Map<TableKey, WeakReference<URLLoader>> urlLoaders = new HashMap<TableKey, WeakReference<URLLoader>>();

    static {
        String codebaseVal = (String) AccessController.doPrivileged(new GetStringPropAction(RMIProperties.CODEBASE_PROP));
        userCodeBase = (codebaseVal == null || codebaseVal.trim().length() == 0) ? null : codebaseVal.trim();
    }

    private static final RMILog loaderLog = RMILog.getLoaderLog();

    /**
     * Constructs DefaultRMIClassLoaderSpi.
     */
    public DefaultRMIClassLoaderSpi() {
    }

    /**
     * @see RMIClassLoaderSpi.loadProxyClass(String, String[], ClassLoader)
     */
    public Class loadProxyClass(String codebase, String[] interf, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if (loaderLog.isLoggable(RMILog.VERBOSE)) {
            loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.25", new Object[] { Arrays.asList(interf), ((codebase == null) ? "" : codebase), defaultLoader }));
        }
        Class[] interfCl = new Class[interf.length];
        ClassLoader codebaseLoader = null;
        Exception ex = null;
        stringToURLs(codebase);
        try {
            codebaseLoader = getClassLoader1(codebase);
        } catch (SecurityException se) {
            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.26", ((codebase == null) ? "" : codebase)));
            }
            ex = se;
        }
        boolean failed = false;
        if (defaultLoader != null) {
            for (int i = 0; i < interf.length; ++i) {
                try {
                    interfCl[i] = Class.forName(interf[i], false, defaultLoader);
                } catch (Exception ex1) {
                    if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                        loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.27", new Object[] { interf[i], defaultLoader, ex1 }));
                    }
                    failed = true;
                }
            }
        }
        if (failed || (defaultLoader == null)) {
            if (ex != null) {
                ClassLoader curLoader = Thread.currentThread().getContextClassLoader();
                if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                    loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.28", curLoader));
                }
                codebaseLoader = curLoader;
            }
            for (int i = 0; i < interf.length; ++i) {
                try {
                    interfCl[i] = Class.forName(interf[i], false, codebaseLoader);
                } catch (Exception ex1) {
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.29", interf[i], codebaseLoader));
                    }
                    if (ex != null) {
                        String msg = Messages.getString("rmi.log.2A", ((codebase == null) ? "" : codebase));
                        if (loaderLog.isLoggable(RMILog.BRIEF)) {
                            loaderLog.log(RMILog.BRIEF, msg);
                        }
                        throw new ClassNotFoundException(msg, ex);
                    } else {
                        throw new ClassNotFoundException(Messages.getString("rmi.25"), ex1);
                    }
                }
            }
        }
        boolean allPublic = true;
        ClassLoader interfLoader = null;
        boolean sameLoader = true;
        for (int i = 0; i < interfCl.length; ++i) {
            if (!Modifier.isPublic(interfCl[i].getModifiers())) {
                allPublic = false;
                ClassLoader loader = interfCl[i].getClassLoader();
                if (interfLoader == null) {
                    interfLoader = loader;
                } else if (!interfLoader.equals(loader)) {
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2B", new Object[] { interfCl[i], loader, interfLoader }));
                    }
                    sameLoader = false;
                }
            }
        }
        if (allPublic) {
            Class proxyCl = null;
            try {
                proxyCl = Proxy.getProxyClass(codebaseLoader, interfCl);
                if (loaderLog.isLoggable(RMILog.BRIEF)) {
                    loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2C", proxyCl, codebaseLoader));
                }
            } catch (IllegalArgumentException iae) {
                try {
                    proxyCl = Proxy.getProxyClass(defaultLoader, interfCl);
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2C", proxyCl, defaultLoader));
                    }
                } catch (IllegalArgumentException iae1) {
                    if (loaderLog.isLoggable(RMILog.BRIEF)) {
                        loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2D", codebaseLoader, defaultLoader));
                    }
                    throw new ClassNotFoundException(Messages.getString("rmi.25"), iae1);
                }
            }
            return proxyCl;
        }
        if (sameLoader) {
            Class proxyCl = Proxy.getProxyClass(interfLoader, interfCl);
            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2C", proxyCl, interfLoader));
            }
            return proxyCl;
        }
        throw new LinkageError(Messages.getString("rmi.25"));
    }

    /**
     * @see RMIClassLoaderSpi.loadClass(String, String, ClassLoader)
     */
    public Class loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if (loaderLog.isLoggable(RMILog.VERBOSE)) {
            loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.2E", new Object[] { name, ((codebase == null) ? "" : codebase), defaultLoader }));
        }
        stringToURLs(codebase);
        try {
            if (defaultLoader != null) {
                Class c = Class.forName(name, false, defaultLoader);
                if (loaderLog.isLoggable(RMILog.BRIEF)) {
                    loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.2F", name, defaultLoader));
                }
                return c;
            }
        } catch (ClassNotFoundException cnfe) {
        }
        ClassLoader codebaseLoader = null;
        Exception ex = null;
        try {
            codebaseLoader = getClassLoader1(codebase);
        } catch (SecurityException se) {
            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.30", ((codebase == null) ? "" : codebase)));
            }
            ex = se;
        }
        Class c;
        if (ex != null) {
            ClassLoader curLoader = Thread.currentThread().getContextClassLoader();
            if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.31", curLoader));
            }
            try {
                c = Class.forName(name, false, curLoader);
            } catch (ClassNotFoundException cnfe1) {
                if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                    loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.32", name));
                }
                throw new ClassNotFoundException(Messages.getString("rmi.94", name, ((codebase == null) ? "" : codebase)), ex);
            }
            if (loaderLog.isLoggable(RMILog.BRIEF)) {
                loaderLog.log(RMILog.BRIEF, Messages.getString("rmi.log.34", name));
            }
        } else {
            c = Class.forName(name, false, codebaseLoader);
            if (loaderLog.isLoggable(RMILog.VERBOSE)) {
                loaderLog.log(RMILog.VERBOSE, Messages.getString("rmi.log.35", name, codebaseLoader));
            }
        }
        return c;
    }

    /**
     * @see RMIClassLoaderSpi.getClassAnnotation(Class)
     */
    public String getClassAnnotation(Class cl) {
        ClassLoader loader = cl.getClassLoader();
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        if (loader == systemLoader || (systemLoader != null && loader == systemLoader.getParent())) {
            return userCodeBase;
        }
        if (loader instanceof URLLoader) {
            return ((URLLoader) loader).getAnnotations();
        } else if (loader instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) loader).getURLs();
            String annot = urlsToCodebase(urls);
            if (annot == null) {
                return userCodeBase;
            }
            SecurityManager mgr = System.getSecurityManager();
            if (mgr != null) {
                try {
                    for (int i = 0; i < urls.length; ++i) {
                        Permission p = urls[i].openConnection().getPermission();
                        if (p != null) {
                            mgr.checkPermission(p);
                        }
                    }
                } catch (SecurityException se) {
                    return userCodeBase;
                } catch (IOException ioe) {
                    return userCodeBase;
                }
            }
            return annot;
        } else {
            return userCodeBase;
        }
    }

    /**
     * @see RMIClassLoaderSpi.getClassLoader(String)
     */
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        stringToURLs(codebase);
        SecurityManager mgr = System.getSecurityManager();
        if (mgr == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        mgr.checkPermission(new RuntimePermission("getClassLoader"));
        return getClassLoader1(codebase);
    }

    /**
     * @see LoaderHandler.loadClass(String)
     */
    public Class loadClass(String name) throws MalformedURLException, ClassNotFoundException {
        return loadClass(null, name, null);
    }

    /**
     * @see LoaderHandler.loadClass(URL, String)
     */
    public Class loadClass(URL codebase, String name) throws MalformedURLException, ClassNotFoundException {
        return loadClass(codebase.toExternalForm(), name, null);
    }

    /**
     * Always returns null.
     * This method came from LoaderHandler class and not used.
     *
     * @see LoaderHandler.getSecurityContext(ClassLoader)
     */
    public Object getSecurityContext(ClassLoader loader) {
        return null;
    }

    private static ClassLoader getClassLoader1(String codebase) throws MalformedURLException {
        SecurityManager mgr = System.getSecurityManager();
        ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
        if (mgr == null) {
            return parentLoader;
        }
        if (codebase == null) {
            if (userCodeBase != null) {
                codebase = userCodeBase;
            } else {
                return parentLoader;
            }
        }
        URLLoader loader = getClassLoaderNoCheck(parentLoader, codebase);
        if (loader != null) {
            loader.checkPermissions();
        }
        return loader;
    }

    private static URLLoader getClassLoaderNoCheck(ClassLoader parentLoader, String codebase) throws MalformedURLException {
        TableKey key = new TableKey(parentLoader, codebase);
        URLLoader loader = null;
        synchronized (urlLoaders) {
            if (urlLoaders.containsKey(key)) {
                loader = urlLoaders.get(key).get();
                if (loader == null) {
                    urlLoaders.remove(key);
                } else {
                    return loader;
                }
            }
            AccessControlContext ctx = createLoaderACC(key.getURLs());
            class CreateLoaderAction implements PrivilegedAction<URLLoader> {

                URL[] urls;

                ClassLoader parentLoader;

                public CreateLoaderAction(URL[] urls, ClassLoader parentLoader) {
                    this.urls = urls;
                    this.parentLoader = parentLoader;
                }

                public URLLoader run() {
                    return new URLLoader(urls, parentLoader);
                }
            }
            loader = AccessController.doPrivileged(new CreateLoaderAction(key.getURLs(), parentLoader), ctx);
            urlLoaders.put(key, new WeakReference<URLLoader>(loader));
            return loader;
        }
    }

    private static AccessControlContext createLoaderACC(URL[] urls) {
        PermissionCollection perms = (PermissionCollection) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                CodeSource cs = new CodeSource(null, (Certificate[]) null);
                Policy policy = Policy.getPolicy();
                if (policy != null) {
                    return policy.getPermissions(cs);
                }
                return new Permissions();
            }
        });
        addURLsPerms(urls, perms, true);
        perms.add(new RuntimePermission("createClassLoader"));
        ProtectionDomain[] domains;
        if (urls.length == 0) {
            domains = new ProtectionDomain[] { new ProtectionDomain(new CodeSource(null, (Certificate[]) null), perms) };
        } else {
            domains = new ProtectionDomain[urls.length];
            for (int i = 0; i < urls.length; ++i) {
                domains[i] = new ProtectionDomain(new CodeSource(urls[i], (Certificate[]) null), perms);
            }
        }
        return new AccessControlContext(domains);
    }

    private static PermissionCollection addURLsPerms(URL[] urls, PermissionCollection perms, boolean forACC) {
        for (int i = 0; i < urls.length; ++i) {
            Permission perm = null;
            try {
                perm = urls[i].openConnection().getPermission();
            } catch (IOException ioe) {
                continue;
            }
            if (perm == null) {
                continue;
            }
            if (perm instanceof FilePermission) {
                String str = perm.getName();
                int idx = str.lastIndexOf(File.separatorChar);
                if (!str.endsWith(File.separator)) {
                    perms.add(perm);
                } else {
                    perms.add(new FilePermission(str + "-", "read"));
                }
            } else {
                perms.add(perm);
                if (forACC) {
                    String host = urls[i].getHost();
                    if (host != null) {
                        perms.add(new SocketPermission(host, "connect, accept"));
                    }
                }
            }
        }
        return perms;
    }

    private static URL[] stringToURLs(String list) throws MalformedURLException {
        if (list == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(list);
        URL[] urls = new URL[tok.countTokens()];
        for (int i = 0; i < urls.length; ++i) {
            urls[i] = new URL(tok.nextToken());
        }
        return urls;
    }

    private static String urlsToCodebase(URL[] urls) {
        if (urls == null || urls.length == 0) {
            return null;
        }
        String str = "";
        for (int i = 0; i < urls.length - 1; ++i) {
            str += urls[i].toExternalForm() + " ";
        }
        return str + urls[urls.length - 1].toExternalForm();
    }

    private static class URLLoader extends URLClassLoader {

        private String annot;

        private Permissions perms;

        URLLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            perms = new Permissions();
            addURLsPerms(urls, perms, false);
            annot = urlsToCodebase(urls);
        }

        String getAnnotations() {
            return annot;
        }

        void checkPermissions() {
            SecurityManager mgr = System.getSecurityManager();
            if (mgr != null) {
                for (Enumeration en = perms.elements(); en.hasMoreElements(); mgr.checkPermission((Permission) en.nextElement())) {
                }
            }
        }

        /**
         * Returns string representation of this loader.
         *
         * @return string representation of this loader
         */
        public String toString() {
            return getClass().getName() + "[annot:\"" + annot + "\"]";
        }
    }

    private static class TableKey {

        private ClassLoader loader;

        private URL[] urls;

        private int hashCode;

        TableKey(ClassLoader loader, String codebase) throws MalformedURLException {
            this(loader, stringToURLs(codebase));
        }

        TableKey(ClassLoader loader, URL[] urls) {
            this.loader = loader;
            this.urls = urls;
            hashCode = (loader == null) ? 0 : loader.hashCode();
            for (int i = 0; i < urls.length; ++i) {
                hashCode ^= urls[i].hashCode();
            }
        }

        ClassLoader getLoader() {
            return loader;
        }

        public URL[] getURLs() {
            return urls;
        }

        /**
         * Compares this object with another one. Returns true if the object for
         * comparison is an instance of TableKey and they contained the same
         * loader and urls fields.
         *
         * @param obj object for comparison
         *
         * @return true if object specified is equal to this TableKey and false
         *         otherwise
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof TableKey)) {
                return false;
            }
            TableKey key = (TableKey) obj;
            if (hashCode() != key.hashCode()) {
                return false;
            }
            return ((loader != null) ? loader.equals(key.loader) : (key.loader == null)) && ((urls != null) ? Arrays.equals(urls, key.urls) : (key.urls == null));
        }

        /**
         * Returns hash code for this TableKey.
         *
         * @return hash code for this TableKey
         */
        public int hashCode() {
            return hashCode;
        }
    }
}
