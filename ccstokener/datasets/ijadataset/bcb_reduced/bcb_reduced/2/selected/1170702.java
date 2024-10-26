package org.allcolor.alc.filesystem.classloader;

import org.allcolor.alc.filesystem.Directory;
import org.allcolor.alc.filesystem.FileSystem;
import org.allcolor.alc.filesystem.Handler;
import org.allcolor.alc.reflect.Dynamic;
import org.allcolor.alc.thread.Mutex;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author (Author)
 * @version $Revision$
  */
public final class FileSystemClassLoader extends URLClassLoader {

    /**
	 * DOCUMENT ME!
	 */
    private final CodeSourcePermissions permissions;

    /**
	 * DOCUMENT ME!
	 */
    private final Map<String, URL> addedURL = new HashMap<String, URL>();

    /**
	 * DOCUMENT ME!
	 */
    private final Map<String, String> mapLibrary = new HashMap<String, String>();

    /**
	 * DOCUMENT ME!
	 */
    private final Map<String, java.io.File> mapjars = new HashMap<String, java.io.File>();

    /**
	 * DOCUMENT ME!
	 */
    private final Map<CodeSource, WeakReference<ProtectionDomain>> pdcache = new WeakHashMap<CodeSource, WeakReference<ProtectionDomain>>(11);

    /**
	 * DOCUMENT ME!
	 */
    private final Mutex clMutex = new Mutex();

    /**
	 * DOCUMENT ME!
	 */
    private final String fsLabel;

    /**
	 * DOCUMENT ME!
	 */
    private final String unionFs;

    /**
	 * DOCUMENT ME!
	 */
    private String contextName = null;

    /**
	 * Creates a new FileSystemClassLoader object.
	 *
	 * @param fsLabel DOCUMENT ME!
	 * @param unionFs DOCUMENT ME!
	 * @param permissions DOCUMENT ME!
	 * @param parent DOCUMENT ME!
	 */
    public FileSystemClassLoader(final String fsLabel, final String unionFs, final CodeSourcePermissions permissions, final ClassLoader parent) {
        super(new URL[0], parent);
        this.fsLabel = fsLabel;
        this.unionFs = unionFs;
        this.permissions = permissions;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    public URL findResource(final String name) {
        return this.findResource(name, true);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    @Override
    public final synchronized Enumeration<URL> findResources(final String name) throws IOException {
        final String fss[] = this.unionFs.split(":");
        final List<URL> list = new ArrayList<URL>();
        for (final String fs : fss) {
            try {
                final org.allcolor.alc.filesystem.File file = FileSystem.getFileSystem(fs).file(name);
                if (file.exists()) {
                    list.add(file.toURL());
                }
            } catch (final Exception ignore) {
            }
        }
        return new Enumeration<URL>() {

            private int index = 0;

            public boolean hasMoreElements() {
                return this.index < list.size();
            }

            public URL nextElement() {
                if (!this.hasMoreElements()) {
                    return null;
                }
                final URL file = list.get(this.index);
                this.index++;
                return file;
            }
        };
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    public final synchronized URL[] getURLs() {
        final String fss[] = this.unionFs.split(":");
        final List<URL> list = new ArrayList<URL>();
        try {
            this.clMutex.acquire();
            for (final Map.Entry<String, URL> entry : this.addedURL.entrySet()) {
                list.add(entry.getValue());
            }
        } finally {
            this.clMutex.release();
        }
        for (final String sfs : fss) {
            try {
                final FileSystem fs = FileSystem.getFileSystem(sfs);
                this._GetURLs(fs.getRoot(), list);
            } catch (final Exception ignore) {
            }
        }
        return list.toArray(new URL[list.size()]);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param contextName DOCUMENT ME!
	 */
    public void setContextName(final String contextName) {
        this.contextName = contextName;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param url DOCUMENT ME!
	 */
    @Override
    protected final synchronized void addURL(final URL url) {
        if (url == null) {
            return;
        }
        try {
            final String file = Handler.decodeURL(url.getFile());
            if (file.startsWith("/")) {
                try {
                    this.clMutex.acquire();
                    this.addedURL.put(file, url);
                } finally {
                    this.clMutex.release();
                }
            } else {
                try {
                    this.clMutex.acquire();
                    this.addedURL.put("/" + file, url);
                } finally {
                    this.clMutex.release();
                }
            }
        } catch (final IOException e) {
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws ClassNotFoundException DOCUMENT ME!
	 */
    @Override
    protected final synchronized Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            if (name.startsWith("java.") || name.startsWith("sun.")) {
                return Dynamic._.SystemClassLoader().loadClass(name);
            }
            final URL url = this.findResource(name.replace('.', '/') + ".class", false);
            if (url != null) {
                final byte bclazz[] = FileSystemClassLoader.read(url);
                final Class<?> c = this.defineClass(name, bclazz, 0, bclazz.length, this.getProtectionDomain(new CodeSource(url, (Certificate[]) null)));
                return c;
            }
        } catch (final Exception e) {
        }
        ClassLoader parent = null;
        if ((parent = this.getParent()) != null) {
            return parent.loadClass(name);
        } else {
            return Dynamic._.SystemClassLoader().loadClass(name);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param libname DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    protected final synchronized String findLibrary(final String libname) {
        try {
            final String system = System.getSystem().getName() + "/" + libname;
            final URL url = this.findResource(system);
            try {
                this.clMutex.acquire();
                String ret = this.mapLibrary.get(url.toString());
                if (ret == null) {
                    final File file = this.copyLibraryToTemp(url);
                    if (file != null) {
                        ret = file.getAbsolutePath();
                        this.mapLibrary.put(url.toExternalForm(), ret);
                    }
                }
                if (ret != null) {
                    return ret;
                }
            } finally {
                this.clMutex.release();
            }
        } catch (final Exception e) {
        }
        try {
            final FindLibrary fscl = Dynamic._.Proxy(this.getParent(), FindLibrary.class);
            return fscl.findLibrary(libname);
        } catch (final Throwable ignore) {
            final Throwable t = ignore;
            if (t.getClass() == ThreadDeath.class) {
                throw (ThreadDeath) t;
            }
            Throwable cause = ignore.getCause();
            while (cause != null) {
                if (cause.getClass() == ThreadDeath.class) {
                    throw (ThreadDeath) cause;
                }
                cause = cause.getCause();
            }
        }
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param codesource DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    protected final PermissionCollection getPermissions(final CodeSource codesource) {
        final PermissionCollection perms = super.getPermissions(codesource);
        if (this.permissions != null) {
            try {
                final PermissionCollection pc = this.permissions.getPermissions(codesource);
                if (pc != null) {
                    for (final Enumeration<Permission> it = perms.elements(); it.hasMoreElements(); ) {
                        pc.add(it.nextElement());
                    }
                    return pc;
                }
            } catch (final Exception ignore) {
            }
        } else {
            final PermissionCollection pc = new Permissions();
            for (final Enumeration<Permission> it = perms.elements(); it.hasMoreElements(); ) {
                pc.add(it.nextElement());
            }
            pc.add(new AllPermission());
            return pc;
        }
        return perms;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 * @param resolve DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws ClassNotFoundException DOCUMENT ME!
	 */
    @Override
    protected final synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        try {
            this.clMutex.acquire();
            Class<?> c = this.findLoadedClass(name);
            if (c == null) {
                c = this.findClass(name);
            }
            if (resolve) {
                this.resolveClass(c);
            }
            return c;
        } finally {
            this.clMutex.release();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param file DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws ClassNotFoundException DOCUMENT ME!
	 */
    private static final byte[] read(final URL file) throws ClassNotFoundException {
        InputStream in = null;
        try {
            in = new BufferedInputStream(file.openStream());
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            final byte buffer[] = new byte[2048];
            int inb = -1;
            while ((inb = in.read(buffer)) != -1) {
                bout.write(buffer, 0, inb);
            }
            return bout.toByteArray();
        } catch (final IOException e) {
            throw new ClassNotFoundException(file.toString());
        } finally {
            try {
                in.close();
            } catch (final Exception ignore) {
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param parent DOCUMENT ME!
	 * @param list DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    private final void _GetURLs(final Directory parent, final List<URL> list) throws IOException {
        for (final org.allcolor.alc.filesystem.File file : parent.getFiles()) {
            URL url = file.toURL();
            if (list.contains(url)) continue;
            if (file.getName().endsWith(".jar")) {
                final java.io.File copy = this.copy(file);
                if (copy != null) {
                    list.add(copy.toURI().toURL());
                }
                list.add(url);
                continue;
            }
            list.add(url);
        }
        for (final Directory dir : parent.getDirectories()) {
            this._GetURLs(dir, list);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param url DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private final File copyLibraryToTemp(final URL url) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = url.openStream();
            final File file = File.createTempFile("lib", ".so");
            file.deleteOnExit();
            out = new BufferedOutputStream(new FileOutputStream(file));
            final byte buffer[] = new byte[2048];
            int inb = -1;
            while ((inb = in.read(buffer)) != -1) {
                out.write(buffer, 0, inb);
            }
            return file;
        } catch (final IOException e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (final IOException e) {
            }
            try {
                out.close();
            } catch (final IOException e) {
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 * @param forwardToParent DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private final synchronized URL findResource(final String name, final boolean forwardToParent) {
        try {
            if (name == null) {
                return null;
            }
            if (name.startsWith("/")) {
                try {
                    this.clMutex.acquire();
                    final URL ret = this.addedURL.get(name);
                    if (ret != null) {
                        return ret;
                    }
                } finally {
                    this.clMutex.release();
                }
            } else {
                try {
                    this.clMutex.acquire();
                    final URL ret = this.addedURL.get("/" + name);
                    if (ret != null) {
                        return ret;
                    }
                } finally {
                    this.clMutex.release();
                }
            }
            if (this.fsLabel != null) {
                final URL url = Handler.URL(this.fsLabel, (name.startsWith("/") ? name : ("/" + name)));
                url.openConnection().getInputStream().close();
                return url;
            }
        } catch (final Exception e) {
        }
        if (forwardToParent) {
            ClassLoader parent = null;
            if ((parent = this.getParent()) != null) {
                return parent.getResource(name);
            } else {
                return Dynamic._.SystemClassLoader().getResource(name);
            }
        }
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param cs DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private final ProtectionDomain getProtectionDomain(final CodeSource cs) {
        if (cs == null) {
            return null;
        }
        ProtectionDomain pd = null;
        try {
            this.clMutex.acquire();
            final WeakReference<ProtectionDomain> wpd = this.pdcache.get(cs);
            pd = (wpd != null) ? wpd.get() : null;
            if (pd == null) {
                final PermissionCollection perms = this.getPermissions(cs);
                pd = new ProtectionDomain(cs, perms, this, null);
                if (pd != null) {
                    this.pdcache.put(cs, new WeakReference<ProtectionDomain>(pd));
                }
            }
        } finally {
            this.clMutex.release();
        }
        return pd;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private final java.io.File getScratchDir() {
        try {
            if (this.contextName == null) {
                return null;
            }
            final java.io.File file = java.io.File.createTempFile("directory", "dir");
            final String value = file.getParentFile().getAbsolutePath() + "/" + this.contextName + "/scratchLIB/";
            try {
                file.delete();
            } catch (final Exception ignore) {
            }
            try {
                new java.io.File(value).mkdirs();
            } catch (final Exception ignore) {
            }
            return new java.io.File(value);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param file DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private java.io.File copy(final org.allcolor.alc.filesystem.File file) {
        try {
            synchronized (this.mapjars) {
                java.io.File iofile = this.mapjars.get(file.getPath());
                if ((iofile == null) || !iofile.exists()) {
                    final File scratch = getScratchDir();
                    if (scratch == null) {
                        return null;
                    }
                    String scratchPath = scratch.getAbsolutePath();
                    if (!scratchPath.endsWith("/")) {
                        scratchPath = scratchPath + "/";
                    }
                    final File tmp = new File(scratchPath + file.getName()) {

                        private static final long serialVersionUID = 6885142596057875288L;

                        @Override
                        protected void finalize() throws Throwable {
                            super.finalize();
                            this.delete();
                        }
                    };
                    tmp.deleteOnExit();
                    final InputStream in = file.getInputStream();
                    final byte buffer[] = new byte[2048];
                    final OutputStream fout = new BufferedOutputStream(new FileOutputStream(tmp));
                    int inb = -1;
                    while ((inb = in.read(buffer)) != -1) {
                        fout.write(buffer, 0, inb);
                    }
                    fout.close();
                    in.close();
                    this.mapjars.put(file.getPath(), tmp);
                    iofile = tmp;
                }
                return iofile;
            }
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * @author (Author)
	 * @version $Revision$
	  */
    private static interface FindLibrary {

        /**
		 * DOCUMENT ME!
		 *
		 * @param libname DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public abstract String findLibrary(final String libname);
    }
}
