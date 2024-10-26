package sun.applet;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.SocketPermission;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.CodeSource;
import java.security.PermissionCollection;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

/**
 * This class defines the class loader for loading applet classes and
 * resources. It extends URLClassLoader to search the applet code base
 * for the class or resource after checking any loaded JAR files.
 */
public class AppletClassLoader extends URLClassLoader {

    private URL base;

    private CodeSource codesource;

    private AccessControlContext acc;

    protected AppletClassLoader(URL base) {
        super(new URL[0]);
        this.base = base;
        this.codesource = new CodeSource(base, null);
        acc = AccessController.getContext();
    }

    URL getBaseURL() {
        return base;
    }

    public URL[] getURLs() {
        URL[] jars = super.getURLs();
        URL[] urls = new URL[jars.length + 1];
        System.arraycopy(jars, 0, urls, 0, jars.length);
        urls[urls.length - 1] = base;
        return urls;
    }

    void addJar(String name) {
        URL url;
        try {
            url = new URL(base, name);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("name");
        }
        addURL(url);
    }

    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        int i = name.lastIndexOf('.');
        if (i != -1) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) sm.checkPackageAccess(name.substring(0, i));
        }
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        }
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
        }
        final String path = name.replace('.', '/').concat(".class");
        try {
            byte[] b = (byte[]) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws IOException {
                    return getBytes(new URL(base, path));
                }
            }, acc);
            if (b != null) {
                return defineClass(name, b, 0, b.length, codesource);
            } else {
                throw new ClassNotFoundException(name);
            }
        } catch (PrivilegedActionException e) {
            throw new ClassNotFoundException(name, e.getException());
        }
    }

    /**
     * Returns the permissions for the given codesource object.
     * The implementation of this method first calls super.getPermissions,
     * to get the permissions
     * granted by the super class, and then adds additional permissions
     * based on the URL of the codesource.
     * <p>
     * If the protocol is "file"
     * and the path specifies a file, permission is granted to read all files 
     * and (recursively) all files and subdirectories contained in 
     * that directory. This is so applets with a codebase of
     * file:/blah/some.jar can read in file:/blah/, which is needed to
     * be backward compatible. We also add permission to connect back to
     * the "localhost".
     *
     * @param codesource the codesource
     * @return the permissions granted to the codesource
     */
    protected PermissionCollection getPermissions(CodeSource codesource) {
        final PermissionCollection perms = super.getPermissions(codesource);
        URL url = codesource.getLocation();
        if (url.getProtocol().equals("file")) {
            String path = url.getFile().replace('/', File.separatorChar);
            if (!path.endsWith(File.separator)) {
                int endIndex = path.lastIndexOf(File.separatorChar);
                if (endIndex != -1) {
                    path = path.substring(0, endIndex + 1) + "-";
                    perms.add(new FilePermission(path, "read"));
                }
            }
            perms.add(new SocketPermission("localhost", "connect,accept"));
            AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    try {
                        String host = InetAddress.getLocalHost().getHostName();
                        perms.add(new SocketPermission(host, "connect,accept"));
                    } catch (UnknownHostException uhe) {
                    }
                    return null;
                }
            });
            if (base.getProtocol().equals("file")) {
                String bpath = base.getFile().replace('/', File.separatorChar);
                if (bpath.endsWith(File.separator)) {
                    bpath += "-";
                }
                perms.add(new FilePermission(bpath, "read"));
            }
        }
        return perms;
    }

    private static byte[] getBytes(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        if (uc instanceof java.net.HttpURLConnection) {
            java.net.HttpURLConnection huc = (java.net.HttpURLConnection) uc;
            int code = huc.getResponseCode();
            if (code >= java.net.HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new IOException("open HTTP connection failed.");
            }
        }
        int len = uc.getContentLength();
        InputStream in = uc.getInputStream();
        byte[] b;
        try {
            if (len != -1) {
                b = new byte[len];
                while (len > 0) {
                    int n = in.read(b, b.length - len, len);
                    if (n == -1) {
                        throw new IOException("unexpected EOF");
                    }
                    len -= n;
                }
            } else {
                b = new byte[1024];
                int total = 0;
                while ((len = in.read(b, total, b.length - total)) != -1) {
                    total += len;
                    if (total >= b.length) {
                        byte[] tmp = new byte[total * 2];
                        System.arraycopy(b, 0, tmp, 0, total);
                        b = tmp;
                    }
                }
                if (total != b.length) {
                    byte[] tmp = new byte[total];
                    System.arraycopy(b, 0, tmp, 0, total);
                    b = tmp;
                }
            }
        } finally {
            in.close();
        }
        return b;
    }

    public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null) {
            try {
                url = new URL(base, name);
                if (!resourceExists(url)) url = null;
            } catch (Exception e) {
                url = null;
            }
        }
        return url;
    }

    private boolean resourceExists(URL url) {
        boolean ok = true;
        try {
            URLConnection conn = url.openConnection();
            if (conn instanceof java.net.HttpURLConnection) {
                java.net.HttpURLConnection hconn = (java.net.HttpURLConnection) conn;
                int code = hconn.getResponseCode();
                if (code == java.net.HttpURLConnection.HTTP_OK) {
                    return true;
                }
                if (code >= java.net.HttpURLConnection.HTTP_BAD_REQUEST) {
                    return false;
                }
            } else {
                InputStream is = url.openStream();
                is.close();
            }
        } catch (Exception ex) {
            ok = false;
        }
        return ok;
    }

    public Enumeration findResources(String name) throws IOException {
        URL u = new URL(base, name);
        if (!resourceExists(u)) {
            u = null;
        }
        final Enumeration e = super.findResources(name);
        final URL url = u;
        return new Enumeration() {

            private boolean done;

            public Object nextElement() {
                if (!done) {
                    if (e.hasMoreElements()) {
                        return e.nextElement();
                    }
                    done = true;
                    if (url != null) {
                        return url;
                    }
                }
                throw new NoSuchElementException();
            }

            public boolean hasMoreElements() {
                return !done && (e.hasMoreElements() || url != null);
            }
        };
    }

    Class loadCode(String name) throws ClassNotFoundException {
        name = name.replace('/', '.');
        name = name.replace(File.separatorChar, '.');
        String fullName = name;
        if (name.endsWith(".class") || name.endsWith(".java")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        try {
            return loadClass(name);
        } catch (ClassNotFoundException e) {
        }
        return loadClass(fullName);
    }

    private AppletThreadGroup threadGroup;

    private AppContext appContext;

    synchronized ThreadGroup getThreadGroup() {
        if (threadGroup == null || threadGroup.isDestroyed()) {
            AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    threadGroup = new AppletThreadGroup(base + "-threadGroup");
                    AppContextCreator creatorThread = new AppContextCreator(threadGroup);
                    creatorThread.setContextClassLoader(AppletClassLoader.this);
                    synchronized (creatorThread.syncObject) {
                        creatorThread.start();
                        try {
                            creatorThread.syncObject.wait();
                        } catch (InterruptedException e) {
                        }
                        appContext = creatorThread.appContext;
                    }
                    return null;
                }
            });
        }
        return threadGroup;
    }

    AppContext getAppContext() {
        return appContext;
    }

    int usageCount = 0;

    /**
     * Grab this AppletClassLoader and its ThreadGroup/AppContext, so they
     * won't be destroyed.
     */
    synchronized void grab() {
        usageCount++;
        getThreadGroup();
    }

    /**
     * Release this AppletClassLoader and its ThreadGroup/AppContext.
     * If nothing else has grabbed this AppletClassLoader, its ThreadGroup
     * and AppContext will be destroyed.
     * 
     * Because this method may destroy the AppletClassLoader's ThreadGroup,
     * this method should NOT be called from within the AppletClassLoader's
     * ThreadGroup.
     */
    synchronized void release() {
        if (usageCount > 1) {
            --usageCount;
        } else {
            if (appContext != null) {
                try {
                    appContext.dispose();
                } catch (IllegalThreadStateException e) {
                }
            }
            usageCount = 0;
            appContext = null;
            threadGroup = null;
        }
    }

    private static AppletMessageHandler mh = new AppletMessageHandler("appletclassloader");

    private static void printError(String name, Throwable e) {
        String s = null;
        if (e == null) {
            s = mh.getMessage("filenotfound", name);
        } else if (e instanceof IOException) {
            s = mh.getMessage("fileioexception", name);
        } else if (e instanceof ClassFormatError) {
            s = mh.getMessage("fileformat", name);
        } else if (e instanceof ThreadDeath) {
            s = mh.getMessage("filedeath", name);
        } else if (e instanceof Error) {
            s = mh.getMessage("fileerror", e.toString(), name);
        }
        if (s != null) {
            System.err.println(s);
        }
    }
}

class AppContextCreator extends Thread {

    Object syncObject = new Object();

    AppContext appContext = null;

    AppContextCreator(ThreadGroup group) {
        super(group, "AppContextCreator");
    }

    public void run() {
        synchronized (syncObject) {
            appContext = SunToolkit.createNewAppContext();
            syncObject.notifyAll();
        }
    }
}
