package com.explosion.utilities.classes;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Subclass implementation of <b>java.net.URLClassLoader</b> that knows how
 * to load classes from disk directories, as well as local and remote JAR
 * files.  It also implements the <code>Reloader</code> interface, to provide
 * automatic reloading support to the associated loader.
 * <p>
 * In all cases, URLs must conform to the contract specified by
 * <code>URLClassLoader</code> - any URL that ends with a "/" character is
 * assumed to represent a directory; all other URLs are assumed to be the
 * address of a JAR file.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - Local repositories are searched in
 * the order they are added via the initial constructor and/or any subsequent
 * calls to <code>addRepository()</code>.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - At present, there are no dependencies
 * from this class to any other Catalina class, so that it could be used
 * independently.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.3 $ $Date: 2005/07/06 10:13:42 $
 */
public class StandardClassLoader extends URLClassLoader {

    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader.
     */
    public StandardClassLoader() {
        super(new URL[0]);
        this.parent = getParent();
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
    }

    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader, but with a stream handler factory.
     *
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     */
    public StandardClassLoader(URLStreamHandlerFactory factory) {
        super(new URL[0], null, factory);
        this.factory = factory;
    }

    /**
     * Construct a new ClassLoader with no defined repositories and the
     * specified parent ClassLoader.
     *
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(ClassLoader parent) {
        super((new URL[0]), parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
    }

    /**
     * Construct a new ClassLoader with no defined repositories and the
     * specified parent ClassLoader.
     *
     * @param parent The parent ClassLoader
     * @param factory the URLStreamHandlerFactory to use when creating URLs
     */
    public StandardClassLoader(ClassLoader parent, URLStreamHandlerFactory factory) {
        super((new URL[0]), parent, factory);
        this.factory = factory;
    }

    /**
     * Construct a new ClassLoader with the specified repositories and
     * no parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     */
    public StandardClassLoader(String repositories[]) {
        super(convert(repositories));
        this.parent = getParent();
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++) addRepositoryInternal(repositories[i]);
        }
    }

    /**
     * Construct a new ClassLoader with the specified repositories and
     * parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(String repositories[], ClassLoader parent) {
        super(convert(repositories), parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++) addRepositoryInternal(repositories[i]);
        }
    }

    /**
     * Construct a new ClassLoader with the specified repositories and
     * parent ClassLoader.
     *
     * @param repositories The initial set of repositories
     * @param parent The parent ClassLoader
     */
    public StandardClassLoader(URL repositories[], ClassLoader parent) {
        super(repositories, parent);
        this.parent = parent;
        this.system = getSystemClassLoader();
        securityManager = System.getSecurityManager();
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++) addRepositoryInternal(repositories[i].toString());
        }
    }

    /**
     * The set of optional packages (formerly standard extensions) that
     * are available in the repositories associated with this class loader.
     * Each object in this list is of type
     * <code>org.apache.catalina.loader.Extension</code>.
     */
    protected ArrayList available = new ArrayList();

    /**
     * The debugging detail level of this component.
     */
    protected int debug = 0;

    /**
     * Should this class loader delegate to the parent class loader
     * <strong>before</strong> searching its own repositories (i.e. the
     * usual Java2 delegation model)?  If set to <code>false</code>,
     * this class loader will search its own repositories first, and
     * delegate to the parent only if the class or resource is not
     * found locally.
     */
    protected boolean delegate = false;

    /**
     * The list of local repositories, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected String repositories[] = new String[0];

    /**
     * The set of optional packages (formerly standard extensions) that
     * are required in the repositories associated with this class loader.
     * Each object in this list is of type
     * <code>org.apache.catalina.loader.Extension</code>.
     */
    protected ArrayList required = new ArrayList();

    /**
     * A list of read File and Jndi Permission's required if this loader
     * is for a web application context.
     */
    private ArrayList permissionList = new ArrayList();

    /**
     * The PermissionCollection for each CodeSource for a web
     * application context.
     */
    private HashMap loaderPC = new HashMap();

    /**
     * Instance of the SecurityManager installed.
     */
    private SecurityManager securityManager = null;

    /**
     * Flag that the security policy has been refreshed from file.
     */
    private boolean policy_refresh = false;

    /**
     * The parent class loader.
     */
    private ClassLoader parent = null;

    /**
     * The system class loader.
     */
    private ClassLoader system = null;

    /**
     * URL stream handler for additional protocols.
     */
    protected URLStreamHandlerFactory factory = null;

    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {
        return (this.debug);
    }

    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }

    /**
     * Return the "delegate first" flag for this class loader.
     */
    public boolean getDelegate() {
        return (this.delegate);
    }

    /**
     * Set the "delegate first" flag for this class loader.
     *
     * @param delegate The new "delegate first" flag
     */
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    /**
     * If there is a Java SecurityManager create a read FilePermission
     * or JndiPermission for the file directory path.
     *
     * @param path file directory path
     */
    public void setPermissions(String path) {
        if (securityManager != null) {
            if (path.startsWith("jndi:") || path.startsWith("jar:jndi:")) {
                permissionList.add(new JndiPermission(path + "*"));
            } else {
                permissionList.add(new FilePermission(path + "-", "read"));
            }
        }
    }

    /**
     * If there is a Java SecurityManager add a read FilePermission
     * or JndiPermission for URL.
     *
     * @param url URL for a file or directory on local system
     */
    public void setPermissions(URL url) {
        setPermissions(url.toString());
    }

    /**
     * Add a new repository to the set of places this ClassLoader can look for
     * classes to be loaded.
     *
     * @param repository Name of a source of classes to be loaded, such as a
     *  directory pathname, a JAR file pathname, or a ZIP file pathname
     *
     * @exception IllegalArgumentException if the specified repository is
     *  invalid or does not exist
     */
    public void addRepository(String repository) {
        if (debug >= 1) log("addRepository(" + repository + ")");
        try {
            URLStreamHandler streamHandler = null;
            String protocol = parseProtocol(repository);
            if (factory != null) streamHandler = factory.createURLStreamHandler(protocol);
            URL url = new URL(null, repository, streamHandler);
            super.addURL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.toString());
        }
        addRepositoryInternal(repository);
    }

    /**
     * Return a list of "optional packages" (formerly "standard extensions")
     * that have been declared to be available in the repositories associated
     * with this class loader, plus any parent class loader implemented with
     * the same class.
     */
    public Extension[] findAvailable() {
        ArrayList results = new ArrayList();
        Iterator available = this.available.iterator();
        while (available.hasNext()) results.add(available.next());
        ClassLoader loader = this;
        while (true) {
            loader = loader.getParent();
            if (loader == null) break;
            if (!(loader instanceof StandardClassLoader)) continue;
            Extension extensions[] = ((StandardClassLoader) loader).findAvailable();
            for (int i = 0; i < extensions.length; i++) results.add(extensions[i]);
        }
        Extension extensions[] = new Extension[results.size()];
        return ((Extension[]) results.toArray(extensions));
    }

    /**
     * Return a String array of the current repositories for this class
     * loader.  If there are no repositories, a zero-length array is
     * returned.
     */
    public String[] findRepositories() {
        return (repositories);
    }

    /**
     * Return a list of "optional packages" (formerly "standard extensions")
     * that have been declared to be required in the repositories associated
     * with this class loader, plus any parent class loader implemented with
     * the same class.
     */
    public Extension[] findRequired() {
        ArrayList results = new ArrayList();
        Iterator required = this.required.iterator();
        while (required.hasNext()) results.add(required.next());
        ClassLoader loader = this;
        while (true) {
            loader = loader.getParent();
            if (loader == null) break;
            if (!(loader instanceof StandardClassLoader)) continue;
            Extension extensions[] = ((StandardClassLoader) loader).findRequired();
            for (int i = 0; i < extensions.length; i++) results.add(extensions[i]);
        }
        Extension extensions[] = new Extension[results.size()];
        return ((Extension[]) results.toArray(extensions));
    }

    /**
     * This class loader doesn't check for reloading.
     */
    public boolean modified() {
        return (false);
    }

    /**
     * Render a String representation of this object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("StandardClassLoader\r\n");
        sb.append("  available:\r\n");
        Iterator available = this.available.iterator();
        while (available.hasNext()) {
            sb.append("    ");
            sb.append(available.next().toString());
            sb.append("\r\n");
        }
        sb.append("  delegate: ");
        sb.append(delegate);
        sb.append("\r\n");
        sb.append("  repositories:\r\n");
        for (int i = 0; i < repositories.length; i++) {
            sb.append("    ");
            sb.append(repositories[i]);
            sb.append("\r\n");
        }
        sb.append("  required:\r\n");
        Iterator required = this.required.iterator();
        while (required.hasNext()) {
            sb.append("    ");
            sb.append(required.next().toString());
            sb.append("\r\n");
        }
        if (this.parent != null) {
            sb.append("----------> Parent Classloader:\r\n");
            sb.append(this.parent.toString());
            sb.append("\r\n");
        }
        return (sb.toString());
    }

    /**
     * Find the specified class in our local repositories, if possible.  If
     * not found, throw <code>ClassNotFoundException</code>.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class findClass(String name) throws ClassNotFoundException {
        if (debug >= 3) log("    findClass(" + name + ")");
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    if (debug >= 4) log("      securityManager.checkPackageDefinition");
                    securityManager.checkPackageDefinition(name.substring(0, i));
                } catch (Exception se) {
                    if (debug >= 4) log("      -->Exception-->ClassNotFoundException", se);
                    throw new ClassNotFoundException(name);
                }
            }
        }
        Class clazz = null;
        try {
            if (debug >= 4) log("      super.findClass(" + name + ")");
            try {
                synchronized (this) {
                    clazz = findLoadedClass(name);
                    if (clazz != null) return clazz;
                    clazz = super.findClass(name);
                }
            } catch (AccessControlException ace) {
                throw new ClassNotFoundException(name);
            } catch (RuntimeException e) {
                if (debug >= 4) log("      -->RuntimeException Rethrown", e);
                throw e;
            }
            if (clazz == null) {
                if (debug >= 3) log("    --> Returning ClassNotFoundException");
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            if (debug >= 3) log("    --> Passing on ClassNotFoundException", e);
            throw e;
        }
        if (debug >= 4) log("      Returning class " + clazz);
        if ((debug >= 4) && (clazz != null)) log("      Loaded by " + clazz.getClassLoader());
        return (clazz);
    }

    /**
     * Find the specified resource in our local repository, and return a
     * <code>URL</code> refering to it, or <code>null</code> if this resource
     * cannot be found.
     *
     * @param name Name of the resource to be found
     */
    public URL findResource(String name) {
        if (debug >= 3) log("    findResource(" + name + ")");
        URL url = super.findResource(name);
        if (debug >= 3) {
            if (url != null) log("    --> Returning '" + url.toString() + "'"); else log("    --> Resource not found, returning null");
        }
        return (url);
    }

    /**
     * Return an eneration of <code>URLs</code> representing all of the
     * resources with the given name.  If no resources with this name are
     * found, return an empty eneration.
     *
     * @param name Name of the resources to be found
     *
     * @exception IOException if an input/output error occurs
     */
    public Enumeration findResources(String name) throws IOException {
        if (debug >= 3) log("    findResources(" + name + ")");
        return (super.findResources(name));
    }

    /**
     * Find the resource with the given name.  A resource is some data
     * (images, audio, text, etc.) that can be accessed by class code in a
     * way that is independent of the location of the code.  The name of a
     * resource is a "/"-separated path name that identifies the resource.
     * If the resource cannot be found, return <code>null</code>.
     * <p>
     * This method searches according to the following algorithm, returning
     * as soon as it finds the appropriate URL.  If the resource cannot be
     * found, returns <code>null</code>.
     * <ul>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>getResource()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findResource()</code> to find this resource in our
     *     locally defined repositories.</li>
     * <li>Call the <code>getResource()</code> method of the parent class
     *     loader, if any.</li>
     * </ul>
     *
     * @param name Name of the resource to return a URL for
     */
    public URL getResource(String name) {
        if (debug >= 2) log("getResource(" + name + ")");
        URL url = null;
        if (delegate) {
            if (debug >= 3) log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null) loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (debug >= 2) log("  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }
        if (debug >= 3) log("  Searching local repositories");
        url = findResource(name);
        if (url != null) {
            if (debug >= 2) log("  --> Returning '" + url.toString() + "'");
            return (url);
        }
        if (!delegate) {
            ClassLoader loader = parent;
            if (loader == null) loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (debug >= 2) log("  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }
        if (debug >= 2) log("  --> Resource not found, returning null");
        return (null);
    }

    /**
     * Find the resource with the given name, and return an input stream
     * that can be used for reading it.  The search order is as described
     * for <code>getResource()</code>, after checking to see if the resource
     * data has been previously cached.  If the resource cannot be found,
     * return <code>null</code>.
     *
     * @param name Name of the resource to return an input stream for
     */
    public InputStream getResourceAsStream(String name) {
        if (debug >= 2) log("getResourceAsStream(" + name + ")");
        InputStream stream = null;
        stream = findLoadedResource(name);
        if (stream != null) {
            if (debug >= 2) log("  --> Returning stream from cache");
            return (stream);
        }
        if (delegate) {
            if (debug >= 3) log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null) loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                if (debug >= 2) log("  --> Returning stream from parent");
                return (stream);
            }
        }
        if (debug >= 3) log("  Searching local repositories");
        URL url = findResource(name);
        if (url != null) {
            if (debug >= 2) log("  --> Returning stream from local");
            try {
                return (url.openStream());
            } catch (IOException e) {
                log("url.openStream(" + url.toString() + ")", e);
                return (null);
            }
        }
        if (!delegate) {
            if (debug >= 3) log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null) loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                if (debug >= 2) log("  --> Returning stream from parent");
                return (stream);
            }
        }
        if (debug >= 2) log("  --> Resource not found, returning null");
        return (null);
    }

    /**
     * Load the class with the specified name.  This method searches for
     * classes in the same manner as <code>loadClass(String, boolean)</code>
     * with <code>false</code> as the second argument.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name) throws ClassNotFoundException {
        return (loadClass(name, false));
    }

    /**
     * Load the class with the specified name, searching using the following
     * algorithm until it finds and returns the class.  If the class cannot
     * be found, returns <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Call <code>findLoadedClass(String)</code> to check if the
     *     class has already been loaded.  If it has, the same
     *     <code>Class</code> object is returned.</li>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>loadClass()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findClass()</code> to find this class in our locally
     *     defined repositories.</li>
     * <li>Call the <code>loadClass()</code> method of our parent
     *     class loader, if any.</li>
     * </ul>
     * If the class was found using the above steps, and the
     * <code>resolve</code> flag is <code>true</code>, this method will then
     * call <code>resolveClass(Class)</code> on the resulting Class object.
     *
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (debug >= 2) log("loadClass(" + name + ", " + resolve + ")");
        Class clazz = null;
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (debug >= 3) log("  Returning class from cache");
            if (resolve) resolveClass(clazz);
            return (clazz);
        }
        if (name.startsWith("java.")) {
            ClassLoader loader = system;
            clazz = loader.loadClass(name);
            if (clazz != null) {
                if (resolve) resolveClass(clazz);
                return (clazz);
            }
            throw new ClassNotFoundException(name);
        }
        if (securityManager != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    securityManager.checkPackageAccess(name.substring(0, i));
                } catch (SecurityException se) {
                    String error = "Security Violation, attempt to use " + "Restricted Class: " + name;
                    System.out.println(error);
                    se.printStackTrace();
                    log(error);
                    throw new ClassNotFoundException(error);
                }
            }
        }
        if (delegate) {
            if (debug >= 3) log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null) loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (debug >= 3) log("  Loading class from parent");
                    if (resolve) resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }
        if (debug >= 3) log("  Searching local repositories");
        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (debug >= 3) log("  Loading class from local repository");
                if (resolve) resolveClass(clazz);
                return (clazz);
            }
        } catch (ClassNotFoundException e) {
            ;
        }
        if (!delegate) {
            if (debug >= 3) log("  Delegating to parent classloader");
            ClassLoader loader = parent;
            if (loader == null) loader = system;
            try {
                clazz = loader.loadClass(name);
                if (clazz != null) {
                    if (debug >= 3) log("  Loading class from parent");
                    if (resolve) resolveClass(clazz);
                    return (clazz);
                }
            } catch (ClassNotFoundException e) {
                ;
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * Get the Permissions for a CodeSource.  If this instance
     * of StandardClassLoader is for a web application context,
     * add read FilePermissions for the base directory (if unpacked),
     * the context URL, and jar file resources.
     *
     * @param CodeSource where the code was loaded from
     * @return PermissionCollection for CodeSource
     */
    protected final PermissionCollection getPermissions(CodeSource codeSource) {
        if (!policy_refresh) {
            Policy policy = Policy.getPolicy();
            policy.refresh();
            policy_refresh = true;
        }
        String codeUrl = codeSource.getLocation().toString();
        PermissionCollection pc;
        if ((pc = (PermissionCollection) loaderPC.get(codeUrl)) == null) {
            pc = super.getPermissions(codeSource);
            if (pc != null) {
                Iterator perms = permissionList.iterator();
                while (perms.hasNext()) {
                    Permission p = (Permission) perms.next();
                    pc.add(p);
                }
                loaderPC.put(codeUrl, pc);
            }
        }
        return (pc);
    }

    /**
     * Parse URL protocol.
     *
     * @return String protocol
     */
    protected static String parseProtocol(String spec) {
        if (spec == null) return "";
        int pos = spec.indexOf(':');
        if (pos <= 0) return "";
        return spec.substring(0, pos).trim();
    }

    /**
     * Add a repository to our internal array only.
     *
     * @param repository The new repository
     *
     * @exception IllegalArgumentException if the manifest of a JAR file
     *  cannot be processed correctly
     */
    protected void addRepositoryInternal(String repository) {
        URLStreamHandler streamHandler = null;
        String protocol = parseProtocol(repository);
        if (factory != null) streamHandler = factory.createURLStreamHandler(protocol);
        if (!repository.endsWith(File.separator) && !repository.endsWith("/")) {
            JarFile jarFile = null;
            try {
                Manifest manifest = null;
                if (repository.startsWith("jar:")) {
                    URL url = new URL(null, repository, streamHandler);
                    JarURLConnection conn = (JarURLConnection) url.openConnection();
                    conn.setAllowUserInteraction(false);
                    conn.setDoInput(true);
                    conn.setDoOutput(false);
                    conn.connect();
                    jarFile = conn.getJarFile();
                } else if (repository.startsWith("file://")) {
                    jarFile = new JarFile(repository.substring(7));
                } else if (repository.startsWith("file:")) {
                    jarFile = new JarFile(repository.substring(5));
                } else if (repository.endsWith(".jar")) {
                    URL url = new URL(null, repository, streamHandler);
                    URLConnection conn = url.openConnection();
                    JarInputStream jis = new JarInputStream(conn.getInputStream());
                    manifest = jis.getManifest();
                } else {
                    throw new IllegalArgumentException("addRepositoryInternal:  Invalid URL '" + repository + "'");
                }
                if (!((manifest == null) && (jarFile == null))) {
                    if ((manifest == null) && (jarFile != null)) manifest = jarFile.getManifest();
                    if (manifest != null) {
                        Iterator extensions = Extension.getAvailable(manifest).iterator();
                        while (extensions.hasNext()) available.add(extensions.next());
                        extensions = Extension.getRequired(manifest).iterator();
                        while (extensions.hasNext()) required.add(extensions.next());
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IllegalArgumentException("addRepositoryInternal: " + t);
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (Throwable t) {
                    }
                }
            }
        }
        synchronized (repositories) {
            String results[] = new String[repositories.length + 1];
            System.arraycopy(repositories, 0, results, 0, repositories.length);
            results[repositories.length] = repository;
            repositories = results;
        }
    }

    /**
     * Convert an array of String to an array of URL and return it.
     *
     * @param input The array of String to be converted
     */
    protected static URL[] convert(String input[]) {
        return convert(input, null);
    }

    /**
     * Convert an array of String to an array of URL and return it.
     *
     * @param input The array of String to be converted
     * @param factory Handler factory to use to generate the URLs
     */
    protected static URL[] convert(String input[], URLStreamHandlerFactory factory) {
        URLStreamHandler streamHandler = null;
        URL url[] = new URL[input.length];
        for (int i = 0; i < url.length; i++) {
            try {
                String protocol = parseProtocol(input[i]);
                if (factory != null) streamHandler = factory.createURLStreamHandler(protocol); else streamHandler = null;
                url[i] = new URL(null, input[i], streamHandler);
            } catch (MalformedURLException e) {
                url[i] = null;
            }
        }
        return (url);
    }

    /**
     * Finds the resource with the given name if it has previously been
     * loaded and cached by this class loader, and return an input stream
     * to the resource data.  If this resource has not been cached, return
     * <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected InputStream findLoadedResource(String name) {
        return (null);
    }

    /**
     * Log a debugging output message.
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        System.out.println(message);
    }

    /**
     * Log a debugging output message with an exception.
     *
     * @param message Message to be logged
     * @param throwable Exception to be logged
     */
    private void log(String message, Throwable throwable) {
        System.out.println("StandardClassLoader: " + message);
        throwable.printStackTrace(System.out);
    }
}
