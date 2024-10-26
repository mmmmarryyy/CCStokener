package jaxlib.net.repository;

import java.io.File;
import java.io.FilePermission;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import jaxlib.col.GenericContainer;
import jaxlib.io.IO;
import jaxlib.lang.Classes;
import jaxlib.lang.Exceptions;
import jaxlib.lang.Ints;
import jaxlib.net.URIs;
import jaxlib.ref.ReferenceType;
import jaxlib.util.CheckArg;

/**
 * A classloader which delegates to an underlying {@link URLRepository}.
 * <p>
 * The repository can be seen as the classpath of the classloader.
 * </p><p>
 * Optionally an {@code URLRepositoryClassLoader} tries to load classes and resources from its repository 
 * prior to trying the parent classloader. 
 * </p><p>
 * Another option is to keep classes weakly or softly referenced in memory instead of strongly referenced. If 
 * not strongly referenced then all classes loaded from a shared codesource will be unloaded once neither one 
 * of the classes nor the codesource specific classloader are referenced anymore.
 * </p><p>
 * If an {@code URLRepositoryClassLoader} uses weak or soft references, then for each codesource of the 
 * repository the classloader creates a child classloader which finally loads the class. A codesource is an 
 * URL returned by {@link URLResource#getRepositoryURL() URLResource.getRepositoryURL()}. Thus a call to 
 * {@link Class#getClassLoader() Class.getClassLoader()} will not return the {@code URLRepositoryClassLoader} 
 * itself but a classloader which uses the {@code URLRepositoryClassLoader} as its 
 * {@link ClassLoader#getParent() parent classloader}.
 * </p><p>
 * The {@link AccessControlContext} of the thread that created the instance of 
 * {@code URLRepositoryClassLoader} will be used when subsequently loading classes and resources. 
 * </p><p>
 * Implementation note: To speed up classloading {@code URLRepositoryClassLoader} instances are avoiding
 * to synchronize the whole classloading process. Thus subclasses which want to {@link #defineClass define 
 * classes} manually should either ensure the class never will be located in the repository, or should 
 * synchronize to the {@code URLRepositoryClassLoader} for themselves. Otherwise the VM could throw errors 
 * because of duplicate classes.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: URLRepositoryClassLoader.java 1413 2005-06-30 19:14:29Z joerg_wassmer $
 */
public class URLRepositoryClassLoader extends SecureClassLoader {

    /**
   * The context of the thread which created this classloader.
   * Used when loading classes.
   */
    private final AccessControlContext accessControlContext;

    /**
   * STRONG, WEAK or SOFT, never PHANTOM and never null.
   */
    final ReferenceType codeSourceReferenceType;

    /**
   * The parent classloader of this one.
   * Stored here to avoid unnecessary security checks by ClassLoader.getParent().
   * May be null.
   */
    private final ClassLoader parent;

    /**
   * If true (default) classes are loaded from parent classloader first.
   * Otherwise loading from repository has priority.
   */
    private final boolean parentFirst;

    /**
   * The repository which locates classfiles and other resources for this classloader.
   */
    private final URLRepository repository;

    /**
   * key = codesource
   * Not thread-safe.
   */
    private final Map<URL, URLRepositoryClassLoader.CodeSourceClassLoader> urlClassLoaders;

    /**
   * Maps names of weakly or softly referenced loaded classes to their (weakly or softly referenced)
   * classloader.
   * Null if classes are stored strongly referenced.
   * Not thread-safe.
   */
    private final Map<String, URLRepositoryClassLoader.CodeSourceClassLoader> weakClasses;

    /**
   * Maps packages of weakly or softly referenced loaded classes to their (weakly or softly referenced)
   * classloader.
   * Null if classes are stored strongly referenced.
   * Not thread-safe.
   */
    private final Map<String, URLRepositoryClassLoader.CodeSourceClassLoader> weakPackages;

    /**
   * Used to avoid duplicate queries.
   * Not required and thus null if parent is queried first or parent is not null.
   * Thread-safe.
   */
    private final Map<Thread, ?> threadsToSkipFind;

    /**
   * Creates a new {@code URLRepositoryClassLoader} which never unloads classes and prefers loading classes 
   * and resources from the system classloader.
   *
   * @param repository
   *  the repository which will locate classes and resources.
   *
   * @throws NullPointerException
   *  if {@code repository == null}.
   * @throws SecurityException
   *  if a security manager exists and its {@link SecurityManager#checkCreateClassLoader()} method doesn't 
   *  allow creation of a new class loader.
   *
   * @alias {@code URLRepositoryClassLoader(repository, true, ReferenceType.STRONG)}
   *
   * @see ClassLoader#getSystemClassLoader()
   *
   * @since JaXLib 1.0
   */
    public URLRepositoryClassLoader(URLRepository repository) {
        this((Void) null, repository, true, ReferenceType.STRONG);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkCreateClassLoader();
    }

    /**
   * Creates a new {@code URLRepositoryClassLoader} which never unloads classes.
   *
   * @param repository
   *  the repository which will locate classes and resources.
   * @param parentClassLoaderPreferred
   *  if {@code true} the classloader will first try to load classes and resources from the system classloader
   *  before querying the specified repository. If {@code false} then for all resources and all classes except
   *  those whose name starts with {@code "java."} the repository will be queried prior to the parent 
   *  classloader.
   *
   * @throws NullPointerException
   *  if {@code repository == null}.
   * @throws SecurityException
   *  if a security manager exists and its {@link SecurityManager#checkCreateClassLoader()} method doesn't 
   *  allow creation of a new class loader.
   *
   * @alias {@code URLRepositoryClassLoader(repository, parentClassLoaderPreferred, ReferenceType.STRONG)}
   *
   * @see #isParentClassLoaderPreferred()
   * @see ClassLoader#getSystemClassLoader()
   *
   * @since JaXLib 1.0
   */
    public URLRepositoryClassLoader(URLRepository repository, boolean parentClassLoaderPreferred) {
        this((Void) null, repository, parentClassLoaderPreferred, ReferenceType.STRONG);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkCreateClassLoader();
    }

    /**
   * Creates a new {@code URLRepositoryClassLoader} which optionally unloads classes.
   *
   * @param repository
   *  the repository which will locate classes and resources.
   * @param parentClassLoaderPreferred
   *  if {@code true} the classloader will first try to load classes and resources from the system classloader
   *  before querying the specified repository. If {@code false} then for all resources and all classes except
   *  those whose name starts with {@code "java."} the repository will be queried prior to the parent 
   *  classloader.
   * @param codeSourceReferenceType
   *  if not {@link ReferenceType#STRONG} then classes loaded from a codesource will be unloaded if none of 
   *  the classes loaded from the same codesource are referenced anymore. A codesource is an URL returned by
   *  {@link URLResource#getRepositoryURL() URLResource.getRepositoryURL()}.
   *
   * @throws IllegalArgumentException
   *  if <tt>codeSourceReferenceType == {@link ReferenceType#PHANTOM}</tt>.
   * @throws NullPointerException
   *  if {@code (repository == null) || (codeSourceReferenceType == null)}.
   * @throws SecurityException
   *  if a security manager exists and its {@link SecurityManager#checkCreateClassLoader()} method doesn't 
   *  allow creation of a new class loader.
   *
   * @see #isParentClassLoaderPreferred()
   * @see #getCodeSourceReferenceType()
   * @see ClassLoader#getSystemClassLoader()
   *
   * @since JaXLib 1.0
   */
    public URLRepositoryClassLoader(URLRepository repository, boolean parentClassLoaderPreferred, ReferenceType codeSourceReferenceType) {
        this((Void) null, repository, parentClassLoaderPreferred, codeSourceReferenceType);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkCreateClassLoader();
    }

    /**
   * Creates a new {@code URLRepositoryClassLoader} which never unloads classes and prefers loading classes 
   * and resources from the specified parent classloader, 
   *
   * @param parent
   *  the parent classloader to delegate to. If {@code null} then the default delegation classloader will be
   *  used.
   * @param repository
   *  the repository which will locate classes and resources.
   *
   * @throws NullPointerException
   *  if {@code repository == null}.
   * @throws SecurityException
   *  if a security manager exists and its {@link SecurityManager#checkCreateClassLoader()} method doesn't 
   *  allow creation of a new class loader.
   *
   * @alias {@code URLRepositoryClassLoader(parent, repository, true, ReferenceType.STRONG)}
   *
   * @since JaXLib 1.0
   */
    public URLRepositoryClassLoader(ClassLoader parent, URLRepository repository) {
        this((Void) null, parent, repository, true, ReferenceType.STRONG);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkCreateClassLoader();
    }

    /**
   * Creates a new {@code URLRepositoryClassLoader} which never unloads classes.
   *
   * @param parent
   *  the parent classloader to delegate to. If {@code null} then the default delegation classloader will be
   *  used.
   * @param repository
   *  the repository which will locate classes and resources.
   * @param parentClassLoaderPreferred
   *  if {@code true} the classloader will first try to load classes and resources from the parent classloader
   *  before querying the specified repository. If {@code false} then for all resources and all classes except
   *  those with name beginning with {@code "java."} the repository will be queried prior to the parent 
   *  classloader.
   *
   * @throws NullPointerException
   *  if {@code (repository == null) || (codeSourceReferenceType == null)}.
   * @throws SecurityException
   *  if a security manager exists and its {@link SecurityManager#checkCreateClassLoader()} method doesn't 
   *  allow creation of a new class loader.
   *
   * @alias 
   *  {@code URLRepositoryClassLoader(parent, repository, parentClassLoaderPreferred, ReferenceType.STRONG)}
   *
   * @see #isParentClassLoaderPreferred()
   *
   * @since JaXLib 1.0
   */
    public URLRepositoryClassLoader(ClassLoader parent, URLRepository repository, boolean parentClassLoaderPreferred) {
        this((Void) null, parent, repository, parentClassLoaderPreferred, ReferenceType.STRONG);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkCreateClassLoader();
    }

    /**
   * Creates a new {@code URLRepositoryClassLoader} which optionally unloads classes.
   *
   * @param parent
   *  the parent classloader to delegate to. If {@code null} then the default delegation classloader will be
   *  used.
   * @param repository
   *  the repository which will locate classes and resources.
   * @param parentClassLoaderPreferred
   *  if {@code true} the classloader will first try to load classes and resources from the parent classloader
   *  before querying the specified repository. If {@code false} then for all resources and all classes except
   *  those with name beginning with {@code "java."} the repository will be queried prior to the parent 
   *  classloader.
   * @param codeSourceReferenceType
   *  if not {@link ReferenceType#STRONG} then classes loaded from a codesource will be unloaded if none of 
   *  the classes loaded from the same codesource are referenced anymore. A codesource is an URL returned by
   *  {@link URLResource#getRepositoryURL() URLResource.getRepositoryURL()}.
   *
   * @throws IllegalArgumentException
   *  if <tt>codeSourceReferenceType == {@link ReferenceType#PHANTOM}</tt>.
   * @throws NullPointerException
   *  if {@code (repository == null) || (codeSourceReferenceType == null)}.
   * @throws SecurityException
   *  if a security manager exists and its {@link SecurityManager#checkCreateClassLoader()} method doesn't 
   *  allow creation of a new class loader.
   *
   * @see #isParentClassLoaderPreferred()
   * @see #getCodeSourceReferenceType()
   *
   * @since JaXLib 1.0
   */
    public URLRepositoryClassLoader(ClassLoader parent, URLRepository repository, boolean parentClassLoaderPreferred, ReferenceType codeSourceReferenceType) {
        this((Void) null, parent, repository, parentClassLoaderPreferred, codeSourceReferenceType);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkCreateClassLoader();
    }

    private URLRepositoryClassLoader(Void dummy, URLRepository repository, boolean parentClassLoaderPreferred, ReferenceType codeSourceReferenceType) {
        super();
        CheckArg.notNull(repository, "repository");
        CheckArg.notNull(codeSourceReferenceType, "codeSourceReferenceType");
        CheckArg.equalsNot(codeSourceReferenceType, ReferenceType.PHANTOM, "codeSourceReferenceType");
        this.accessControlContext = AccessController.getContext();
        this.codeSourceReferenceType = codeSourceReferenceType;
        this.parent = super.getParent();
        this.parentFirst = parentClassLoaderPreferred;
        this.repository = repository;
        this.urlClassLoaders = codeSourceReferenceType.createValueRefHashMap();
        this.threadsToSkipFind = !this.parentFirst && (this.parent == null) ? null : Collections.synchronizedMap(new IdentityHashMap<Thread, Object>());
        if (codeSourceReferenceType == ReferenceType.STRONG) {
            this.weakClasses = null;
            this.weakPackages = null;
        } else {
            this.weakClasses = codeSourceReferenceType.createValueRefHashMap();
            this.weakPackages = codeSourceReferenceType.createValueRefHashMap();
        }
    }

    private URLRepositoryClassLoader(Void dummy, ClassLoader parent, URLRepository repository, boolean parentClassLoaderPreferred, ReferenceType codeSourceReferenceType) {
        super(parent);
        CheckArg.notNull(repository, "repository");
        CheckArg.notNull(codeSourceReferenceType, "codeSourceReferenceType");
        CheckArg.equalsNot(codeSourceReferenceType, ReferenceType.PHANTOM, "codeSourceReferenceType");
        this.accessControlContext = AccessController.getContext();
        this.codeSourceReferenceType = codeSourceReferenceType;
        this.parent = (parent == null) ? super.getParent() : parent;
        this.parentFirst = parentClassLoaderPreferred;
        this.repository = repository;
        this.urlClassLoaders = codeSourceReferenceType.createValueRefHashMap();
        this.threadsToSkipFind = !this.parentFirst && (this.parent == null) ? null : Collections.synchronizedMap(new IdentityHashMap<Thread, Object>());
        if (codeSourceReferenceType == ReferenceType.STRONG) {
            this.weakClasses = null;
            this.weakPackages = null;
        } else {
            this.weakClasses = codeSourceReferenceType.createValueRefHashMap();
            this.weakPackages = codeSourceReferenceType.createValueRefHashMap();
        }
    }

    private URLRepositoryClassLoader.CodeSourceClassLoader getCodeSourceClassLoader(URL codeSourceURL) {
        CodeSourceClassLoader cl;
        synchronized (this.urlClassLoaders) {
            cl = this.urlClassLoaders.get(codeSourceURL);
        }
        if (cl == null) {
            cl = new CodeSourceClassLoader(this, codeSourceURL);
            synchronized (this.urlClassLoaders) {
                CodeSourceClassLoader existing = this.urlClassLoaders.get(codeSourceURL);
                if (existing == null) this.urlClassLoaders.put(codeSourceURL, cl); else cl = existing;
            }
        }
        return cl;
    }

    /**
   * This method is called for each class defined by this classloader immediately after it has been defined.
   * The default implementation does nothing.
   * <p>
   * If this classloader is configured to store classes weakly or softly referenced then the classloader of
   * the specified class is a child classloader of this one.
   * </p><p>
   * This method gets called by {@link #findClass(String)}. The current thread is the thread which caused the
   * class to be loaded. The call is executed outside of the privileged {@link AccessControlContext} of this
   * classloader.
   * </p>
   *
   * @param c
   *  the newly defined class.
   *
   * @since JaXLib 1.0
   */
    protected void definedClass(Class<?> c) {
    }

    /**
   * A classloader is equal to nothing but itself.
   *
   * @since JaXLib 1.0
   */
    @Override
    public final boolean equals(Object o) {
        return o == this;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if ((this.threadsToSkipFind != null) && this.threadsToSkipFind.keySet().remove(Thread.currentThread())) {
            throw new ClassNotFoundException(name);
        } else if (name.startsWith("java.")) {
            throw new ClassNotFoundException(name);
        } else {
            if (this.weakClasses != null) {
                CodeSourceClassLoader cl;
                synchronized (this.weakClasses) {
                    cl = this.weakClasses.get(name);
                }
                if (cl != null) {
                    Class<?> c = cl.getLoadedClass(name);
                    if (c == null) throw new AssertionError("expected class already being defined: " + name);
                    return c;
                }
            }
            Thread thread = Thread.currentThread();
            boolean interrupted = thread.isInterrupted();
            if (interrupted) Thread.interrupted();
            Class<?> c;
            try {
                c = AccessController.doPrivileged(new PrivilegedExceptionAction<Class>() {

                    final String resName = name.replace('.', '/').concat(".class");

                    public Class<?> run() throws URLResourceException, IOException, ClassNotFoundException {
                        URLResourceConnection resCon = URLRepositoryClassLoader.this.repository.openResourceConnection(resName);
                        if (resCon == null) {
                            throw new ClassNotFoundException(name);
                        } else {
                            CodeSourceClassLoader cl = URLRepositoryClassLoader.this.getCodeSourceClassLoader(resCon.getURLResource().getRepositoryURL());
                            URLConnection co = resCon.getURLConnection();
                            resCon = null;
                            return cl.findClass(name, co);
                        }
                    }
                }, this.accessControlContext);
            } catch (PrivilegedActionException ex) {
                Exception cause = ex.getException();
                if (cause instanceof ClassNotFoundException) throw (ClassNotFoundException) cause; else throw (ClassNotFoundException) new ClassNotFoundException(name, ex);
            } finally {
                if (interrupted) thread.interrupt();
            }
            definedClass(c);
            return c;
        }
    }

    @Override
    public URL findResource(final String name) {
        if ((this.threadsToSkipFind != null) && this.threadsToSkipFind.keySet().remove(Thread.currentThread())) {
            return null;
        }
        Thread thread = Thread.currentThread();
        boolean interrupted = thread.isInterrupted();
        if (interrupted) Thread.interrupted();
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {

                public URL run() throws URLResourceException {
                    URLResource r = URLRepositoryClassLoader.this.repository.getResource(name);
                    return (r == null) ? null : r.getResourceURL();
                }
            }, this.accessControlContext);
        } catch (PrivilegedActionException ex) {
            return null;
        } finally {
            if (interrupted) thread.interrupt();
        }
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if ((this.threadsToSkipFind != null) && this.threadsToSkipFind.keySet().remove(Thread.currentThread())) {
            List<URL> list = Collections.emptyList();
            return Collections.enumeration(list);
        } else {
            return new URLRepositoryClassLoader.FindResourcesEnumeration(name);
        }
    }

    /**
   * Returns the {@code AccessControlContext} this classloader uses when loading classes.
   * The returned context is the one of the thread that created this classloader.
   * <p>
   * This method gets never called by the {@code URLRepositoryClassLoader} class itself. Thus subclasses
   * can not change the context.
   * </p>
   *
   * @return
   *  the context, never {@code null}.
   *
   * @since JaXLib 1.0
   */
    protected AccessControlContext getAccessControlContext() {
        return this.accessControlContext;
    }

    /**
   * Returns the type how classes of a codesource are referenced.
   *
   * @see URLRepositoryClassLoader class documentation for details.
   *
   * @since JaXLib 1.0
   */
    public final ReferenceType getCodeSourceReferenceType() {
        return this.codeSourceReferenceType;
    }

    @Override
    protected Package getPackage(String name) {
        boolean parentFirst = this.parentFirst || name.startsWith("java.") || name.equals("java");
        Package p = null;
        if (parentFirst) p = super.getPackage(name);
        if (p == null) {
            if (this.weakPackages != null) {
                CodeSourceClassLoader cl;
                synchronized (this.weakPackages) {
                    cl = this.weakPackages.get(name);
                }
                if (cl != null) p = cl.getPrivatePackage(name);
            }
            if ((p == null) && !parentFirst) p = super.getPackage(name);
        }
        return p;
    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return getCodeSourceClassLoader(codesource.getLocation()).getPermissions(codesource);
    }

    @Override
    public URL getResource(String name) {
        if (this.parentFirst || name.startsWith("java/") || name.startsWith("/java/")) {
            return super.getResource(name);
        } else {
            URL url = findResource(name);
            if (url == null) {
                if (this.parent != null) {
                    url = this.parent.getResource(name);
                } else {
                    Thread thread = Thread.currentThread();
                    this.threadsToSkipFind.put(thread, null);
                    try {
                        url = super.getResource(name);
                    } finally {
                        this.threadsToSkipFind.remove(thread);
                    }
                }
            }
            return url;
        }
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        boolean parentFirst = this.parentFirst || name.startsWith("java/") || name.startsWith("/java/");
        if (parentFirst) {
            if (this.parent != null) {
                InputStream in = this.parent.getResourceAsStream(name);
                if (in != null) return in;
            } else {
                Thread thread = Thread.currentThread();
                this.threadsToSkipFind.put(thread, null);
                try {
                    InputStream in = super.getResourceAsStream(name);
                    if (in != null) return in;
                } finally {
                    this.threadsToSkipFind.remove(thread);
                }
            }
        }
        Thread thread = Thread.currentThread();
        boolean interrupted = thread.isInterrupted();
        if (interrupted) Thread.interrupted();
        try {
            URLResourceConnection resco = this.repository.openResourceConnection(name);
            if (resco != null) return resco.getURLConnection().getInputStream();
        } catch (IOException ex) {
        } finally {
            if (interrupted) thread.interrupt();
        }
        if (parentFirst) {
            return null;
        } else {
            if (this.parent != null) {
                return this.parent.getResourceAsStream(name);
            } else {
                this.threadsToSkipFind.put(thread, null);
                try {
                    return super.getResourceAsStream(name);
                } finally {
                    this.threadsToSkipFind.remove(thread);
                }
            }
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (this.parentFirst || name.startsWith("java/") || name.startsWith("/java/")) {
            return super.getResources(name);
        } else {
            if (this.parent != null) {
                return new URLRepositoryClassLoader.CompoundEnumeration(findResources(name), this.parent.getResources(name));
            } else {
                Enumeration<URL> left = findResources(name);
                Thread thread = Thread.currentThread();
                this.threadsToSkipFind.put(thread, null);
                try {
                    return new URLRepositoryClassLoader.CompoundEnumeration(left, super.getResources(name));
                } finally {
                    this.threadsToSkipFind.remove(thread);
                }
            }
        }
    }

    /**
   * Returns the identity hashcode of this classloader instance.
   *
   * @since JaXLib 1.0
   */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
   * Returns {@code true} if this classloader preferrs loading classes and resources from its parent 
   * classloader before loading them from the underlying {@code URLRepository}.
   * <p>
   * This read-only property is specified via the constructor of {@code URLRepositoryClassLoader}.
   * Note that classes whose name starts with {@code "java."} are always loaded through the parent 
   * classloader.
   * </p>
   *
   * @since JaXLib 1.0
   */
    public final boolean isParentClassLoaderPreferred() {
        return this.parentFirst;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            String pkgName = Classes.getPackageName(name);
            if (pkgName != null) sm.checkPackageAccess(pkgName);
        }
        if (this.parentFirst || name.startsWith("java.")) {
            if (this.parent == null) {
                return super.loadClass(name, resolve);
            } else {
                Class c = findLoadedClass(name);
                if (c == null) {
                    try {
                        c = this.parent.loadClass(name);
                    } catch (ClassNotFoundException ex) {
                    }
                    if (c == null) {
                        c = findClass(name);
                        if (c == null) throw new ClassNotFoundException(name);
                    }
                }
                if (resolve) resolveClass(c);
                return c;
            }
        } else {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ex) {
                }
                if (c == null) {
                    if (this.parent != null) {
                        c = this.parent.loadClass(name);
                    } else {
                        Thread thread = Thread.currentThread();
                        this.threadsToSkipFind.put(thread, null);
                        try {
                            return super.loadClass(name, resolve);
                        } finally {
                            this.threadsToSkipFind.remove(thread);
                        }
                    }
                    if (c == null) throw new ClassNotFoundException(name);
                }
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }

    /**
   * Reads the complete content of the specified connection's input stream into a buffer.
   * The bytes should make up a Java classfile, but could be errornous. To avoid loading big invalid files 
   * this method should check whether the header of the input contains the classfile magic {@code 0xCAFEBABE} 
   * while reading the bytes and abort with a {@link ClassFormatError} if not.
   * <p>
   * The current thread is the thread which requests the class to be loaded. The call is executed inside of 
   * the privileged {@link AccessControlContext} of this classloader.
   * </p>
   *
   * @return
   *  a bytebuffer whose {@link ByteBuffer#remaining() remaining} bytes will form the named Java class.
   *
   * @param name
   *  the name of the class to be readed.
   * @param co
   *  the connection to the classfile.
   *
   * @throws IOException 
   *  if an I/O error occurs.
   *
   * @since JaXLib 1.0
   */
    protected ByteBuffer readClassFile(String name, URLConnection co) throws IOException {
        int estimatedSize = Math.min(128000, co.getContentLength());
        if (estimatedSize < 0) estimatedSize = 8192; else if (estimatedSize == 0) estimatedSize = 128;
        InputStream in = co.getInputStream();
        try {
            byte[] buf = new byte[estimatedSize];
            int readed = 0;
            boolean magicChecked = false;
            while (true) {
                if (readed == buf.length) {
                    byte[] newBuf = new byte[buf.length * 2];
                    System.arraycopy(buf, 0, newBuf, 0, buf.length);
                    buf = newBuf;
                }
                int step = magicChecked ? (buf.length - readed) : Math.min(8192, buf.length - readed);
                step = in.read(buf, readed, step);
                if (step > 0) {
                    if (!magicChecked && (readed >= 4)) {
                        magicChecked = true;
                        int magic = Ints.fromBytes(buf, 0);
                        if (magic != 0xCAFEBABE) {
                            buf = null;
                            throw new ClassFormatError("invalid Java classfile, expected magic 0xCAFEBABE but got " + Integer.toHexString(magic) + "\n  classname = " + name + "\n  url       = " + co.getURL());
                        }
                    }
                    readed += step;
                } else if (step < 0) {
                    break;
                }
            }
            in.close();
            return ByteBuffer.wrap(buf, 0, readed);
        } catch (Throwable ex) {
            throw IO.close(in, ex);
        }
    }

    /**
   * Classloader for single codesources.
   * If the URLRepositoryClassLoader is not configured to store classes weakly or softly referenced then
   * CodeSourceClassLoader loads classes into the URLRepositoryClassLoader instead of itself.
   * <p>
   * CodeSourceClassLoader instances are also serving as an synchonization monitor. We synchronize all
   * threads loading classes from the same codesource. Thus multiple threads can load classes at the same time
   * if the classes are located at different codesources.
   * </p>
   */
    private static final class CodeSourceClassLoader extends SecureClassLoader {

        private static String getAttribute(Attributes.Name name, Attributes attr, Attributes parent) {
            String v = (attr == null) ? null : attr.getValue(name);
            return ((v == null) && (parent != null)) ? parent.getValue(name) : v;
        }

        private final CodeSource codeSource;

        private final Manifest manifest;

        private final URLRepositoryClassLoader parent;

        private final PermissionCollection permissions;

        /**
     * All packages defined by this classloader itself.
     * Unused and thus null if the URLRepositoryClassLoader stores classes strongly referenced.
     */
        private final HashMap<String, Package> privatePackages;

        CodeSourceClassLoader(URLRepositoryClassLoader parent, URL codeSource) {
            super(parent);
            this.parent = parent;
            this.codeSource = new CodeSource(codeSource, (CodeSigner[]) null);
            this.manifest = createManifest();
            this.permissions = createPermissions();
            if (parent.codeSourceReferenceType == ReferenceType.STRONG) this.privatePackages = null; else this.privatePackages = new HashMap<String, Package>();
        }

        @Override
        protected void finalize() throws Throwable {
            if (this.parent.codeSourceReferenceType != ReferenceType.STRONG) finalize0();
            super.finalize();
        }

        private void finalize0() {
            if (this.parent.weakClasses instanceof GenericContainer) {
                synchronized (this.parent.weakClasses) {
                    ((GenericContainer) this.parent.weakClasses).trimToSize();
                }
            }
            if (this.parent.weakPackages instanceof GenericContainer) {
                synchronized (this.parent.weakPackages) {
                    ((GenericContainer) this.parent.weakPackages).trimToSize();
                }
            }
        }

        private Manifest createManifest() {
            URL url = this.codeSource.getLocation();
            InputStream in = null;
            try {
                if ("jar".equals(url.getProtocol())) {
                    URLConnection co = url.openConnection();
                    if (co instanceof JarURLConnection) {
                        return ((JarURLConnection) co).getManifest();
                    }
                }
                in = new URL(url, "META-INF/MANIFEST.MF").openStream();
                Manifest mf = new Manifest(in);
                in.close();
                return mf;
            } catch (IOException ex) {
                try {
                    if (in != null) in.close();
                } finally {
                    return null;
                }
            }
        }

        private PermissionCollection createPermissions() {
            PermissionCollection perms = super.getPermissions(this.codeSource);
            URL url = this.codeSource.getLocation();
            Permission p;
            URLConnection urlConnection;
            try {
                urlConnection = url.openConnection();
                p = urlConnection.getPermission();
            } catch (IOException ex) {
                p = null;
                urlConnection = null;
            }
            if (p instanceof FilePermission) {
                String path = p.getName();
                if (path.endsWith(File.separator)) {
                    path += "-";
                    p = new FilePermission(path, "read");
                }
            } else if ((p == null) && (url.getProtocol().equals("file"))) {
                String path = url.getFile().replace('/', File.separatorChar);
                try {
                    path = URIs.decode(path, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    throw new AssertionError(ex);
                } catch (CharacterCodingException ex) {
                    throw new RuntimeException("unable to decode url: " + path, ex);
                } catch (URISyntaxException ex) {
                    throw new RuntimeException("malformed url: " + path, ex);
                }
                if (path.endsWith(File.separator)) path += "-";
                p = new FilePermission(path, "read");
            } else {
                URL locUrl = url;
                if (urlConnection instanceof JarURLConnection) locUrl = ((JarURLConnection) urlConnection).getJarFileURL();
                String host = locUrl.getHost();
                if (host == null) host = "localhost";
                p = new SocketPermission(host, "connect,accept");
            }
            if (p != null) {
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) sm.checkPermission(p);
                perms.add(p);
            }
            return perms;
        }

        private Package definePackage(String name, URL url) {
            if (this.manifest == null) {
                return definePackage(name, null, null, null, null, null, null, null);
            } else {
                Attributes a = this.manifest.getAttributes(name.replace('.', '/').concat("/"));
                Attributes b = this.manifest.getMainAttributes();
                return definePackage(name, getAttribute(Attributes.Name.SPECIFICATION_TITLE, a, b), getAttribute(Attributes.Name.SPECIFICATION_VERSION, a, b), getAttribute(Attributes.Name.SPECIFICATION_VENDOR, a, b), getAttribute(Attributes.Name.IMPLEMENTATION_TITLE, a, b), getAttribute(Attributes.Name.IMPLEMENTATION_VERSION, a, b), getAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, a, b), "true".equalsIgnoreCase(getAttribute(Attributes.Name.SEALED, a, b)) ? url : null);
            }
        }

        @Override
        protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
            if (this.parent.codeSourceReferenceType == ReferenceType.STRONG) {
                return this.parent.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
            } else {
                Package p = super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
                name = p.getName();
                this.privatePackages.put(name, p);
                synchronized (this.parent.weakPackages) {
                    if (!this.parent.weakPackages.containsKey(name)) this.parent.weakPackages.put(name, this);
                }
                return p;
            }
        }

        private boolean isSealed(String name) {
            return (this.manifest != null) && "true".equalsIgnoreCase(getAttribute(Attributes.Name.SEALED, this.manifest.getAttributes(name.replace('.', '/').concat("/")), this.manifest.getMainAttributes()));
        }

        /**
     * Called by parent.findClass(name)
     * Synchronized to avoid defining same class more than once, which would cause a error to be thrown
     * by ClassLoader.defineClass().
     */
        synchronized Class<?> findClass(String name, URLConnection co) throws IOException {
            if (this.parent.codeSourceReferenceType != ReferenceType.STRONG) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass != null) return loadedClass;
            }
            String pkgName = Classes.getPackageName(name);
            if (pkgName != null) {
                Package pkg = getPackage(pkgName);
                if (pkg == null) {
                    definePackage(pkgName, this.codeSource.getLocation());
                } else if (pkg.isSealed()) {
                    if (!pkg.isSealed(this.codeSource.getLocation())) throw new SecurityException("sealing violation: package " + pkgName + " is sealed");
                } else if (isSealed(pkgName)) {
                    throw new SecurityException("sealing violation: can't seal package " + pkgName + ": already loaded");
                }
            }
            CodeSigner[] codeSigners = null;
            if (co instanceof JarURLConnection) {
                JarURLConnection jar = (JarURLConnection) co;
                JarEntry jarEntry = jar.getJarEntry();
                if (jarEntry != null) codeSigners = jarEntry.getCodeSigners();
            }
            ByteBuffer bb = this.parent.readClassFile(name, co);
            co = null;
            CodeSource cs = (codeSigners == null) ? this.codeSource : new CodeSource(this.codeSource.getLocation(), codeSigners);
            if (this.parent.codeSourceReferenceType == ReferenceType.STRONG) {
                return this.parent.defineClass(name, bb, cs);
            } else {
                Class<?> c = super.defineClass(name, bb, cs);
                name = c.getName();
                synchronized (this.parent.weakClasses) {
                    this.parent.weakClasses.put(name, this);
                }
                return c;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException();
        }

        @Override
        public URL findResource(String name) {
            return null;
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            List<URL> list = Collections.emptyList();
            return Collections.enumeration(list);
        }

        Class<?> getLoadedClass(String name) {
            return findLoadedClass(name);
        }

        @Override
        protected PermissionCollection getPermissions(CodeSource codesource) {
            if (this.codeSource.getLocation().equals(codesource.getLocation())) return this.permissions; else return super.getPermissions(codesource);
        }

        Package getPrivatePackage(String name) {
            assert (this.privatePackages != null);
            Package p = this.privatePackages.get(name);
            assert (p != null);
            return p;
        }

        @Override
        public URL getResource(String name) {
            return this.parent.getResource(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return this.parent.getResourceAsStream(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return this.parent.getResources(name);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (this.parent.codeSourceReferenceType != ReferenceType.STRONG) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass != null) {
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        String pkgName = Classes.getPackageName(name);
                        if (pkgName != null) sm.checkPackageAccess(pkgName);
                    }
                    return loadedClass;
                }
            }
            return this.parent.loadClass(name, resolve);
        }
    }

    private static final class CompoundEnumeration<E> implements Enumeration<E> {

        private Enumeration<E> left;

        private Enumeration<E> right;

        CompoundEnumeration(Enumeration<E> left, Enumeration<E> right) {
            super();
            this.left = left;
            this.right = right;
        }

        public boolean hasMoreElements() {
            Enumeration<E> e = this.left;
            if ((e == null) || !e.hasMoreElements()) {
                this.left = null;
                e = this.right;
                if ((e == null) || !e.hasMoreElements()) {
                    this.right = null;
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }

        public E nextElement() {
            Enumeration<E> e = this.left;
            if ((e == null) || !e.hasMoreElements()) {
                this.left = null;
                e = this.right;
                if ((e == null) || !e.hasMoreElements()) {
                    this.right = null;
                    throw new NoSuchElementException();
                } else {
                    return e.nextElement();
                }
            } else {
                return e.nextElement();
            }
        }
    }

    private final class FindResourcesEnumeration extends Object implements Enumeration<URL>, PrivilegedAction<URL> {

        private Iterator<URLResource> delegate;

        private URL next;

        FindResourcesEnumeration(String name) {
            super();
            this.delegate = URLRepositoryClassLoader.this.repository.iterateAllLocationsOf(name);
        }

        public URL nextElement() {
            if (!hasMoreElements()) throw new NoSuchElementException();
            URL next = this.next;
            this.next = null;
            return next;
        }

        public boolean hasMoreElements() {
            if (this.next != null) {
                return true;
            } else if (this.delegate == null) {
                return false;
            } else {
                URL next = AccessController.doPrivileged(this, URLRepositoryClassLoader.this.accessControlContext);
                this.next = next;
                return next != null;
            }
        }

        public URL run() {
            int failedLoops = 0;
            URL failedURL = null;
            while (true) {
                try {
                    if (this.delegate.hasNext()) {
                        return this.delegate.next().getResourceURL();
                    } else {
                        this.delegate = null;
                        return null;
                    }
                } catch (RuntimeException ex) {
                    if (ex.getCause() instanceof URLResourceException) {
                        if (Exceptions.getThreadInterruptionCause(ex) != null) {
                            throw new IllegalStateException("thread interruption", ex);
                        } else {
                            URLResource res = ((URLResourceException) ex.getCause()).getResource();
                            URL url = (res == null) ? null : res.getResourceURL();
                            if ((url == null) || url.equals(failedURL)) {
                                if (++failedLoops > 32) {
                                    throw new IllegalStateException("too many failures (" + failedLoops + ") for same url (" + url + ")", ex);
                                }
                            } else {
                                failedLoops = 0;
                                failedURL = url;
                            }
                        }
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }
}
