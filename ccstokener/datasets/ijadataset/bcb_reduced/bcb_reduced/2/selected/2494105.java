package net.jini.loader.pref;

import com.sun.jini.loader.pref.internal.PreferredResources;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Set;
import java.util.HashSet;
import net.jini.loader.ClassAnnotation;
import net.jini.loader.DownloadPermission;

/**
 * A class loader that supports preferred classes.
 * 
 * <p>
 * A preferred class is a class that is to be loaded by a class loader without
 * the loader delegating to its parent class loader first. Resources may also be
 * preferred.
 * 
 * <p>
 * Like {@link java.net.URLClassLoader}, <code>PreferredClassLoader</code> loads
 * classes and resources from a search path of URLs. If a URL in the path ends
 * with a <code>'/'</code>, it is assumed to refer to a directory; otherwise,
 * the URL is assumed to refer to a JAR file.
 * 
 * <p>
 * The location of the first URL in the path can contain a <i>preferred list</i>
 * for the entire path. A preferred list declares names of certain classes and
 * other resources throughout the path as being <i>preferred</i> or not. When a
 * <code>PreferredClassLoader</code> is asked to load a class or resource that
 * is preferred (according to the preferred list) and the class or resource
 * exists in the loader's path of URLs, the loader will not delegate first to
 * its parent class loader as it otherwise would do; instead, it will attempt to
 * load the class or resource from its own path of URLs only.
 * 
 * <p>
 * The preferred list for a path of URLs, if one exists, is located relative to
 * the first URL in the path. If the first URL refers to a JAR file, then the
 * preferred list is the contents of the file named
 * <code>"META-INF/PREFERRED.LIST"</code> within that JAR file. If the first URL
 * refers to a directory, then the preferred list is the contents of the file at
 * the location <code>"META-INF/PREFERRED.LIST"</code> relative to that
 * directory URL. If there is no preferred list at the required location, then
 * no classes or resources are preferred for the path of URLs. A preferred list
 * at any other location (such as relative to one of the other URLs in the path)
 * is ignored.
 * 
 * <p>
 * Note that a class or resource is only considered to be preferred if the
 * preferred list declares the name of the class or resource as being preferred
 * and the class or resource actually exists in the path of URLs.
 * 
 * <h3>Preferred List Syntax</h3>
 * 
 * A preferred list is a UTF-8 encoded text file, with lines separated by
 * CR&nbsp;LF, LF, or CR (not followed by an LF). Multiple whitespace characters
 * in a line are equivalent to a single whitespace character, and whitespace
 * characters at the beginning or end of a line are ignored. If the first
 * non-whitespace character of a line is <code>'#'</code>, the line is a comment
 * and is equivalent to a blank line.
 * 
 * <p>
 * The first line of a preferred list must contain a version number in the
 * following format:
 * 
 * <pre>
 *     PreferredResources-Version: 1.&lt;i&gt;x&lt;/i&gt;
 * </pre>
 * 
 * This specification defines only version 1.0, but
 * <code>PreferredClassLoader</code> will parse any version 1.<i>x</i>,
 * <i>x</i>>=0 with the format and semantics specified here.
 * 
 * <p>
 * After the version number line, a preferred list comprises an optional default
 * preferred entry followed by zero or more named preferred entries. A preferred
 * list must contain either a default preferred entry or at least one named
 * preferred entry. Blank lines are allowed before and after preferred entries,
 * as well as between the lines of a named preferred entry.
 * 
 * <p>
 * A default preferred entry is a single line in the following format:
 * 
 * <pre>
 *     Preferred: &lt;i&gt;preferred-setting&lt;/i&gt;
 * </pre>
 * 
 * where <i>preferred-setting</i> is a non-empty sequence of characters. If
 * <i>preferred-setting</i> equals <code>"true"</code> (case insensitive), then
 * resource names not matched by any of the named preferred entries are by
 * default preferred; otherwise, resource names not matched by any of the named
 * preferred entries are by default not preferred. If there is no default
 * preferred entry, then resource names are by default not preferred.
 * 
 * <p>
 * A named preferred entry is two lines in the following format:
 * 
 * <pre>
 *     Name: &lt;i&gt;name-expression&lt;/i&gt;
 *     Preferred: &lt;i&gt;preferred-setting&lt;/i&gt;
 * </pre>
 * 
 * where <i>name-expression</i> and <i>preferred-setting</i> are non-empty
 * sequences of characters. If <i>preferred-setting</i> equals
 * <code>"true"</code> (case insensitive), then resource names that are matched
 * by <i>name-expression</i> (and not any more specific named preferred entries)
 * are preferred; otherwise, resource names that are matched by
 * <i>name-expression</i> (and not any more specific named preferred entries)
 * are not preferred.
 * 
 * <p>
 * If <i>name-expression</i> ends with <code>".class"</code>, it matches a class
 * whose binary name is <i>name-expression</i> without the <code>".class"</code>
 * suffix and with each <code>'/'</code> character replaced with a
 * <code>'.'</code>. It also matches any class whose binary name starts with
 * that same value followed by a <code>'$'</code>; this rule is intended to
 * match nested classes that have an enclosing class of that name, so that the
 * preferred settings of a class and all of its nested classes are the same by
 * default. It is possible, but strongly discouraged, to override the preferred
 * setting of a nested class with a named preferred entry that explicitly
 * matches the nested class's binary name.
 * 
 * <p>
 * <i>name-expression</i> may match arbitrary resource names as well as class
 * names, with path elements separated by <code>'/'</code> characters.
 * 
 * <p>
 * If <i>name-expression</i> ends with <code>"/"</code> or <code>"/*"</code>,
 * then the entry is a directory wildcard entry that matches all resources
 * (including classes) in the named directory. If <i>name-expression</i> ends
 * with <code>"/-"</code>, then the entry is a namespace wildcard entry that
 * matches all resources (including classes) in the named directory and all of
 * its subdirectories.
 * 
 * <p>
 * When more than one named preferred entry matches a class or resource name,
 * then the most specific entry takes precedence. A non-wildcard entry is more
 * specific than a wildcard entry. A directory wildcard entry is more specific
 * than a namespace wildcard entry. A namespace wildcard entry with more path
 * elements is more specific than a namespace wildcard entry with fewer path
 * elements. Given two non-wildcard entries, the entry with the longer
 * <i>name-expression</i> is more specific (this rule is only significant when
 * matching a class). The order of named preferred entries is insignificant.
 * 
 * <h3>Example Preferred List</h3>
 * 
 * <p>
 * Following is an example preferred list:
 * 
 * <pre>
 *     PreferredResources-Version: 1.0
 *     Preferred: false
 *     Name: com/foo/FooBar.class
 *     Preferred: true
 *     Name: com/foo/*
 *     Preferred: false
 *     Name: com/foo/-
 *     Preferred: true
 *     Name: image-files/*
 *     Preferred: mumble
 * </pre>
 * 
 * <p>
 * The class <code>com.foo.FooBar</code> is preferred, as well as any nested
 * classes that have it as an enclosing class. All other classes in the
 * <code>com.foo</code> package are not preferred because of the directory
 * wildcard entry. Classes in subpackages of <code>com.foo</code> are preferred
 * because of the namespace wildcard entry. Resources in the directory
 * <code>"com/foo/"</code> are not preferred, and resources in subdirectories of
 * <code>"com/foo/"</code> are preferred. Resources in the directory
 * <code>"image-files/"</code> are not preferred because preferred settings
 * other than <code>"true"</code> are interpreted as false. Classes that are in
 * a package named <code>com.bar</code> are not preferred because of the default
 * preferred entry.
 * 
 * @author Sun Microsystems, Inc.
 * @since 2.0
 **/
public class PreferredClassLoader extends URLClassLoader implements ClassAnnotation {

    /**
	 * well known name of resource that contains the preferred list in a path of
	 * URLs
	 **/
    private static final String PREF_NAME = "META-INF/PREFERRED.LIST";

    /** first URL in the path, or null if none */
    private final URL firstURL;

    /** class annotation string for classes defined by this loader */
    private final String exportAnnotation;

    /** permissions required to access loader through public API */
    private final Permissions permissions;

    /** security context for loading classes and resources */
    private final AccessControlContext acc;

    /** permission required to download code? */
    private final boolean requireDlPerm;

    /** URLStreamHandler to use when creating new "jar:" URLs */
    private final URLStreamHandler jarHandler;

    /** PreferredResources for this loader (null if no preferred list) */
    private PreferredResources preferredResources;

    /** true if preferredResources has been successfully initialized */
    private boolean preferredResourcesInitialized = false;

    private static final Permission downloadPermission = new DownloadPermission();

    /**
	 * Creates a new <code>PreferredClassLoader</code> that loads classes and
	 * resources from the specified path of URLs and delegates to the specified
	 * parent class loader.
	 * 
	 * <p>
	 * If <code>exportAnnotation</code> is not <code>null</code>, then it will
	 * be used as the return value of the loader's {@link #getClassAnnotation
	 * getClassAnnotation} method. If <code>exportAnnotation</code> is
	 * <code>null</code>, the loader's <code>getClassAnnotation</code> method
	 * will return a space-separated list of the URLs in the specified path. The
	 * <code>exportAnnotation</code> parameter can be used to specify so-called
	 * "export" URLs, from which other parties should load classes defined by
	 * the loader and which are different from the "import" URLs that the
	 * classes are actually loaded from.
	 * 
	 * <p>
	 * If <code>requireDlPerm</code> is <code>true</code>, the loader's
	 * {@link #getPermissions getPermissions} method will require that the
	 * {@link CodeSource} of any class defined by the loader is granted
	 * {@link DownloadPermission}.
	 * 
	 * @param urls
	 *            the path of URLs to load classes and resources from
	 * 
	 * @param parent
	 *            the parent class loader for delegation
	 * 
	 * @param exportAnnotation
	 *            the export class annotation string to use for classes defined
	 *            by this loader, or <code>null</code>
	 * 
	 * @param requireDlPerm
	 *            if <code>true</code>, the loader will only define classes with
	 *            a {@link CodeSource} that is granted
	 *            {@link DownloadPermission}
	 * 
	 * @throws SecurityException
	 *             if there is a security manager and an invocation of its
	 *             {@link SecurityManager#checkCreateClassLoader
	 *             checkCreateClassLoader} method fails
	 **/
    public PreferredClassLoader(URL[] urls, ClassLoader parent, String exportAnnotation, boolean requireDlPerm) {
        this(urls, parent, exportAnnotation, requireDlPerm, null);
    }

    /**
	 * Creates a new <code>PreferredClassLoader</code> that loads classes and
	 * resources from the specified path of URLs, delegates to the specified
	 * parent class loader, and uses the specified
	 * {@link URLStreamHandlerFactory} when creating new URL objects. This
	 * constructor passes <code>factory</code> to the superclass constructor
	 * that has a <code>URLStreamHandlerFactory</code> parameter.
	 * 
	 * <p>
	 * If <code>exportAnnotation</code> is not <code>null</code>, then it will
	 * be used as the return value of the loader's {@link #getClassAnnotation
	 * getClassAnnotation} method. If <code>exportAnnotation</code> is
	 * <code>null</code>, the loader's <code>getClassAnnotation</code> method
	 * will return a space-separated list of the URLs in the specified path. The
	 * <code>exportAnnotation</code> parameter can be used to specify so-called
	 * "export" URLs, from which other parties should load classes defined by
	 * the loader and which are different from the "import" URLs that the
	 * classes are actually loaded from.
	 * 
	 * <p>
	 * If <code>requireDlPerm</code> is <code>true</code>, the loader's
	 * {@link #getPermissions getPermissions} method will require that the
	 * {@link CodeSource} of any class defined by the loader is granted
	 * {@link DownloadPermission}.
	 * 
	 * @param urls
	 *            the path of URLs to load classes and resources from
	 * 
	 * @param parent
	 *            the parent class loader for delegation
	 * 
	 * @param exportAnnotation
	 *            the export class annotation string to use for classes defined
	 *            by this loader, or <code>null</code>
	 * 
	 * @param requireDlPerm
	 *            if <code>true</code>, the loader will only define classes with
	 *            a {@link CodeSource} that is granted
	 *            {@link DownloadPermission}
	 * 
	 * @param factory
	 *            the <code>URLStreamHandlerFactory</code> to use when creating
	 *            new URL objects, or <code>null</code>
	 * 
	 * @throws SecurityException
	 *             if there is a security manager and an invocation of its
	 *             {@link SecurityManager#checkCreateClassLoader
	 *             checkCreateClassLoader} method fails
	 * 
	 * @since 2.1
	 **/
    public PreferredClassLoader(URL[] urls, ClassLoader parent, String exportAnnotation, boolean requireDlPerm, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        firstURL = (urls.length > 0 ? urls[0] : null);
        if (exportAnnotation != null) {
            this.exportAnnotation = exportAnnotation;
        } else {
            this.exportAnnotation = urlsToPath(urls);
        }
        this.requireDlPerm = requireDlPerm;
        if (factory != null) {
            jarHandler = factory.createURLStreamHandler("jar");
        } else {
            jarHandler = null;
        }
        acc = AccessController.getContext();
        permissions = new Permissions();
        addPermissionsForURLs(urls, permissions, false);
    }

    /**
	 * Convert an array of URL objects into a corresponding string containing a
	 * space-separated list of URLs.
	 * 
	 * Note that if the array has zero elements, the return value is null, not
	 * the empty string.
	 */
    static String urlsToPath(URL[] urls) {
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
	 * Creates a new instance of <code>PreferredClassLoader</code> that loads
	 * classes and resources from the specified path of URLs and delegates to
	 * the specified parent class loader.
	 * 
	 * <p>
	 * The <code>exportAnnotation</code> and <code>requireDlPerm</code>
	 * parameters have the same semantics as they do for the constructors.
	 * 
	 * <p>
	 * The {@link #loadClass loadClass} method of the returned
	 * <code>PreferredClassLoader</code> will, if there is a security manager,
	 * invoke its {@link SecurityManager#checkPackageAccess checkPackageAccess}
	 * method with the package name of the class to load before attempting to
	 * load the class; this could result in a <code>SecurityException</code>
	 * being thrown from <code>loadClass</code>.
	 * 
	 * @param urls
	 *            the path of URLs to load classes and resources from
	 * 
	 * @param parent
	 *            the parent class loader for delegation
	 * 
	 * @param exportAnnotation
	 *            the export class annotation string to use for classes defined
	 *            by this loader, or <code>null</code>
	 * 
	 * @param requireDlPerm
	 *            if <code>true</code>, the loader will only define classes with
	 *            a {@link CodeSource} that is granted
	 *            {@link DownloadPermission}
	 * 
	 * @return the new <code>PreferredClassLoader</code> instance
	 * 
	 * @throws SecurityException
	 *             if the current security context does not have the permissions
	 *             necessary to connect to all of the URLs in <code>urls</code>
	 **/
    public static PreferredClassLoader newInstance(final URL[] urls, final ClassLoader parent, final String exportAnnotation, final boolean requireDlPerm) {
        Permissions perms = new Permissions();
        addPermissionsForURLs(urls, perms, false);
        checkPermissions(perms);
        AccessControlContext acc = getLoaderAccessControlContext(urls);
        return (PreferredClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return new PreferredFactoryClassLoader(urls, parent, exportAnnotation, requireDlPerm);
            }
        }, acc);
    }

    /**
	 * If a preferred list exists relative to the first URL of this loader's
	 * path, sets this loader's PreferredResources according to that preferred
	 * list. If no preferred list exists relative to the first URL, leaves this
	 * loader's PreferredResources null.
	 * 
	 * Throws IOException if an I/O exception occurs from which the existence of
	 * a preferred list relative to the first URL cannot be definitely
	 * determined.
	 * 
	 * This method must only be invoked while synchronized on this
	 * PreferredClassLoader, and it must not be invoked again after it has
	 * completed successfully.
	 **/
    private void initializePreferredResources() throws IOException {
        assert Thread.holdsLock(this);
        assert preferredResources == null;
        if (firstURL != null) {
            InputStream prefIn = getPreferredInputStream(firstURL);
            if (prefIn != null) {
                try {
                    preferredResources = new PreferredResources(prefIn);
                } finally {
                    try {
                        prefIn.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
	 * Returns an InputStream from which the preferred list relative to the
	 * specified URL can be read, or null if the there is definitely no
	 * preferred list relative to the URL. If the URL's path ends with "/", then
	 * the preferred list is sought at the location "META-INF/PREFERRED.LIST"
	 * relative to the URL; otherwise, the URL is assumed to refer to a JAR
	 * file, and the preferred list is sought within that JAR file, as the entry
	 * named "META-INF/PREFERRED.LIST".
	 * 
	 * Throws IOException if an I/O exception occurs from which the existence of
	 * a preferred list relative to the specified URL cannot be definitely
	 * determined.
	 **/
    private InputStream getPreferredInputStream(URL firstURL) throws IOException {
        URL prefListURL = null;
        try {
            URL baseURL;
            if (firstURL.getFile().endsWith("/")) {
                baseURL = firstURL;
            } else {
                if (jarExists(firstURL)) {
                    baseURL = getBaseJarURL(firstURL);
                } else {
                    return null;
                }
            }
            prefListURL = new URL(baseURL, PREF_NAME);
            URLConnection preferredConnection = getPreferredConnection(prefListURL, false);
            if (preferredConnection != null) {
                return preferredConnection.getInputStream();
            } else {
                return null;
            }
        } catch (IOException e) {
            if (firstURL.getProtocol().equals("file") || e instanceof FileNotFoundException) {
                return null;
            } else {
                throw e;
            }
        }
    }

    private static final Set existSet = new HashSet(11);

    private boolean jarExists(URL firstURL) throws IOException {
        boolean exists;
        synchronized (existSet) {
            exists = existSet.contains(firstURL);
        }
        if (!exists) {
            exists = (getPreferredConnection(firstURL, true) != null);
            if (exists) {
                synchronized (existSet) {
                    existSet.add(firstURL);
                }
            }
        }
        return exists;
    }

    /**
	 * Returns a "jar:" URL for the root directory of the JAR file at the
	 * specified URL. If this loader was constructed with a
	 * URLStreamHandlerFactory, then the returned URL will have a
	 * URLStreamHandler that was created by the factory.
	 **/
    private URL getBaseJarURL(final URL url) throws MalformedURLException {
        if (jarHandler == null) {
            return new URL("jar", "", -1, url + "!/");
        } else {
            try {
                return (URL) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                    public Object run() throws MalformedURLException {
                        return new URL("jar", "", -1, url + "!/", jarHandler);
                    }
                });
            } catch (PrivilegedActionException e) {
                throw (MalformedURLException) e.getCause();
            }
        }
    }

    /**
	 * Obtain a url connection from which an input stream that contains a
	 * preferred list can be obtained.
	 * 
	 * For http urls, attempts to use http response codes to determine if a
	 * preferred list exists or is definitely not found. Simply attempts to open
	 * a connection to other kinds of non-file urls. If the attempt fails, an
	 * IOException is thrown to user code.
	 * 
	 * Returns null if the preferred list definitely does not exist. Rethrows
	 * all indefinite IOExceptions generated while trying to open a connection
	 * to the preferred list.
	 * 
	 * The caller has the option to close the connection after the resource has
	 * been detected (as will happen when probing for a PREFERRED.LIST).
	 */
    private URLConnection getPreferredConnection(URL url, boolean closeAfter) throws IOException {
        URLConnection preferredConnection = null;
        if (url.getProtocol().equals("file")) {
            return url.openConnection();
        }
        URLConnection closeConn = null;
        URLConnection conn = null;
        try {
            closeConn = url.openConnection();
            conn = closeConn;
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection hconn = (HttpURLConnection) conn;
                if (closeAfter) {
                    hconn.setRequestMethod("HEAD");
                }
                int responseCode = hconn.getResponseCode();
                switch(responseCode) {
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_NOT_AUTHORITATIVE:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                    case HttpURLConnection.HTTP_FORBIDDEN:
                    case HttpURLConnection.HTTP_GONE:
                        conn = null;
                        break;
                    default:
                        throw new IOException("Indefinite http response for " + "preferred list request:" + hconn.getResponseMessage());
                }
            }
        } finally {
            if (closeAfter && (closeConn != null)) {
                try {
                    closeConn.getInputStream().close();
                } catch (IOException e) {
                }
            }
        }
        return conn;
    }

    /**
	 * Returns <code>true</code> if a class or resource with the specified name
	 * is preferred for this class loader, and <code>false</code> if a class or
	 * resource with the specified name is not preferred for this loader.
	 * 
	 * <p>
	 * If <code>isClass</code> is <code>true</code>, then <code>name</code> is
	 * interpreted as the binary name of a class; otherwise, <code>name</code>
	 * is interpreted as the full path of a resource.
	 * 
	 * <p>
	 * This method only returns <code>true</code> if a class or resource with
	 * the specified name exists in the this loader's path of URLs and the name
	 * is preferred in the preferred list. This method returns
	 * <code>false</code> if the name is not preferred in the preferred list or
	 * if the name is preferred with the default preferred entry or a wildcard
	 * preferred entry and the class or resource does not exist in the path of
	 * URLs.
	 * 
	 * @param name
	 *            the name of the class or resource
	 * 
	 * @param isClass
	 *            <code>true</code> if <code>name</code> is a binary class name,
	 *            and <code>false</code> if <code>name</code> is the full path
	 *            of a resource
	 * 
	 * @return <code>true</code> if a class or resource named <code>name</code>
	 *         is preferred for this loader, and <code>false</code> if a class
	 *         or resource named <code>name</code> is not preferred for this
	 *         loader
	 * 
	 * @throws IOException
	 *             if the preferred list cannot definitely be determined to
	 *             exist or not exist, or if the preferred list contains a
	 *             syntax error, or if the name is preferred with the default
	 *             preferred entry or a wildcard preferred entry and the class
	 *             or resource cannot definitely be determined to exist or not
	 *             exist in the path of URLs, or if the name is preferred with a
	 *             non-wildcard entry and the class or resource does not exist
	 *             or cannot definitely be determined to exist in the path of
	 *             URLs
	 **/
    protected boolean isPreferredResource(final String name, final boolean isClass) throws IOException {
        try {
            return ((Boolean) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws IOException {
                    boolean b = isPreferredResource0(name, isClass);
                    return Boolean.valueOf(b);
                }
            }, acc)).booleanValue();
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    private synchronized boolean isPreferredResource0(String name, boolean isClass) throws IOException {
        if (!preferredResourcesInitialized) {
            initializePreferredResources();
            preferredResourcesInitialized = true;
        }
        if (preferredResources == null) {
            return false;
        }
        String resourceName = name;
        if (isClass) {
            resourceName = name.replace('.', '/') + ".class";
        }
        boolean resourcePreferred = false;
        int state = preferredResources.getNameState(resourceName, isClass);
        switch(state) {
            case PreferredResources.NAME_NOT_PREFERRED:
                resourcePreferred = false;
                break;
            case PreferredResources.NAME_PREFERRED_RESOURCE_EXISTS:
                resourcePreferred = true;
                break;
            case PreferredResources.NAME_NO_PREFERENCE:
                Boolean wildcardPref = preferredResources.getWildcardPreference(resourceName);
                if (wildcardPref == null) {
                    wildcardPref = preferredResources.getDefaultPreference();
                }
                if (wildcardPref.booleanValue()) {
                    resourcePreferred = findResourceUpdateState(name, resourceName);
                }
                break;
            case PreferredResources.NAME_PREFERRED:
                resourcePreferred = findResourceUpdateState(name, resourceName);
                if (!resourcePreferred) {
                    throw new IOException("no resource found for " + "complete preferred name");
                }
                break;
            default:
                throw new Error("unknown preference state");
        }
        return resourcePreferred;
    }

    private boolean findResourceUpdateState(String name, String resourceName) throws IOException {
        assert Thread.holdsLock(this);
        boolean resourcePreferred = false;
        if (findResource(resourceName) != null) {
            preferredResources.setNameState(resourceName, PreferredResources.NAME_PREFERRED_RESOURCE_EXISTS);
            resourcePreferred = true;
        }
        return resourcePreferred;
    }

    /**
	 * Loads a class with the specified name.
	 * 
	 * <p>
	 * <code>PreferredClassLoader</code> implements this method as follows:
	 * 
	 * <p>
	 * This method first invokes {@link #findLoadedClass findLoadedClass} with
	 * <code>name</code>; if <code>findLoadedClass</code> returns a non-
	 * <code>null</code> <code>Class</code>, then this method returns that
	 * <code>Class</code>.
	 * 
	 * <p>
	 * Otherwise, this method invokes {@link #isPreferredResource
	 * isPreferredResource} with <code>name</code> as the first argument and
	 * <code>true</code> as the second argument:
	 * 
	 * <ul>
	 * 
	 * <li>If <code>isPreferredResource</code> throws an
	 * <code>IOException</code>, then this method throws a
	 * <code>ClassNotFoundException</code> containing the
	 * <code>IOException</code> as its cause.
	 * 
	 * <li>If <code>isPreferredResource</code> returns <code>true</code>, then
	 * this method invokes {@link #findClass findClass} with <code>name</code>.
	 * If <code>findClass</code> throws an exception, then this method throws
	 * that exception. Otherwise, this method returns the <code>Class</code>
	 * returned by <code>findClass</code>, and if <code>resolve</code> is
	 * <code>true</code>, {@link #resolveClass resolveClass} is invoked with the
	 * <code>Class</code> before returning.
	 * 
	 * <li>If <code>isPreferredResource</code> returns <code>false</code>, then
	 * this method invokes the superclass implementation of
	 * {@link ClassLoader#loadClass(String,boolean) loadClass} with
	 * <code>name</code> and <code>resolve</code> and returns the result. If the
	 * superclass's <code>loadClass</code> throws an exception, then this method
	 * throws that exception.
	 * 
	 * </ul>
	 * 
	 * @param name
	 *            the binary name of the class to load
	 * 
	 * @param resolve
	 *            if <code>true</code>, then {@link #resolveClass resolveClass}
	 *            will be invoked with the loaded class before returning
	 * 
	 * @return the loaded class
	 * 
	 * @throws ClassNotFoundException
	 *             if the class could not be found
	 **/
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            boolean preferred;
            try {
                preferred = isPreferredResource(name, true);
            } catch (IOException e) {
                throw new ClassNotFoundException(name + " (could not determine preferred setting; " + (firstURL != null ? "first URL: \"" + firstURL + "\"" : "no URLs") + ")", e);
            }
            if (preferred) {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } else {
                return super.loadClass(name, resolve);
            }
        }
        return c;
    }

    /**
	 * Gets a resource with the specified name.
	 * 
	 * <p>
	 * <code>PreferredClassLoader</code> implements this method as follows:
	 * 
	 * <p>
	 * This method invokes {@link #isPreferredResource isPreferredResource} with
	 * <code>name</code> as the first argument and <code>false</code> as the
	 * second argument:
	 * 
	 * <ul>
	 * 
	 * <li>If <code>isPreferredResource</code> throws an
	 * <code>IOException</code>, then this method returns <code>null</code>.
	 * 
	 * <li>If <code>isPreferredResource</code> returns <code>true</code>, then
	 * this method invokes {@link #findResource findResource} with
	 * <code>name</code> and returns the result.
	 * 
	 * <li>If <code>isPreferredResource</code> returns <code>false</code>, then
	 * this method invokes the superclass implementation of
	 * {@link ClassLoader#getResource getResource} with <code>name</code> and
	 * returns the result.
	 * 
	 * </ul>
	 * 
	 * @param name
	 *            the name of the resource to get
	 * 
	 * @return a <code>URL</code> for the resource, or <code>null</code> if the
	 *         resource could not be found
	 **/
    public URL getResource(String name) {
        try {
            return (isPreferredResource(name, false) ? findResource(name) : super.getResource(name));
        } catch (IOException e) {
        }
        return null;
    }

    protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
        try {
            return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        } catch (IllegalArgumentException e) {
            return getPackage(name);
        }
    }

    /**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * <code>PreferredClassLoader</code> implements this method as follows:
	 * 
	 * <p>
	 * If this <code>PreferredClassLoader</code> was constructed with a non-
	 * <code>null</code> export class annotation string, then this method
	 * returns that string. Otherwise, this method returns a space-separated
	 * list of this loader's path of URLs.
	 **/
    public String getClassAnnotation() {
        return exportAnnotation;
    }

    /**
	 * Check that the current access control context has all of the permissions
	 * necessary to load classes from this loader.
	 */
    void checkPermissions() {
        checkPermissions(permissions);
    }

    /**
	 * Check that the current access control context has all of the given
	 * permissions.
	 */
    private static void checkPermissions(Permissions perms) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Enumeration en = perms.elements();
            while (en.hasMoreElements()) {
                sm.checkPermission((Permission) en.nextElement());
            }
        }
    }

    /**
	 * Returns the static permissions to be automatically granted to classes
	 * loaded from the specified {@link CodeSource} and defined by this class
	 * loader.
	 * 
	 * <p>
	 * <code>PreferredClassLoader</code> implements this method as follows:
	 * 
	 * <p>
	 * If there is a security manager and this <code>PreferredClassLoader</code>
	 * was constructed to enforce {@link DownloadPermission}, then this method
	 * checks that the current security policy grants the specified
	 * <code>CodeSource</code> the permission
	 * <code>DownloadPermission("permit")</code>; if that check fails, then this
	 * method throws a <code>SecurityException</code>.
	 * 
	 * <p>
	 * Then this method invokes the superclass implementation of
	 * {@link #getPermissions getPermissions} and returns the result.
	 * 
	 * @param codeSource
	 *            the <code>CodeSource</code> to return the permissions to be
	 *            granted to
	 * 
	 * @return the permissions to be granted to the <code>CodeSource</code>
	 * 
	 * @throws SecurityException
	 *             if there is a security manager, this
	 *             <code>PreferredClassLoader</code> was constructed to enforce
	 *             <code>DownloadPermission</code>, and the current security
	 *             policy does not grant the specified <code>CodeSource</code>
	 *             the permission <code>DownloadPermission("permit")</code>
	 **/
    protected PermissionCollection getPermissions(CodeSource codeSource) {
        if (requireDlPerm) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                ProtectionDomain pd = new ProtectionDomain(codeSource, null, this, null);
                if (!pd.implies(downloadPermission)) {
                    throw new SecurityException("CodeSource not permitted to define class: " + codeSource);
                }
            }
        }
        return super.getPermissions(codeSource);
    }

    /**
	 * Returns a string representation of this class loader.
	 **/
    public String toString() {
        return super.toString() + "[\"" + exportAnnotation + "\"]";
    }

    /**
	 * Return the access control context that a loader for the given codebase
	 * URL path should execute with.
	 */
    static AccessControlContext getLoaderAccessControlContext(URL[] urls) {
        PermissionCollection perms = (PermissionCollection) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                CodeSource codesource = new CodeSource(null, (Certificate[]) null);
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
        ProtectionDomain pd = new ProtectionDomain(new CodeSource((urls.length > 0 ? urls[0] : null), (Certificate[]) null), perms);
        return new AccessControlContext(new ProtectionDomain[] { pd });
    }

    /**
	 * Adds to the specified permission collection the permissions necessary to
	 * load classes from a loader with the specified URL path; if "forLoader" is
	 * true, also adds URL-specific permissions necessary for the security
	 * context that such a loader operates within, such as permissions necessary
	 * for granting automatic permissions to classes defined by the loader. A
	 * given permission is only added to the collection if it is not already
	 * implied by the collection.
	 **/
    static void addPermissionsForURLs(URL[] urls, PermissionCollection perms, boolean forLoader) {
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
}
