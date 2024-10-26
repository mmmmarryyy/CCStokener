package fr.macymed.modulo.platform.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import fr.macymed.commons.bcm.BytecodeTransformer;
import fr.macymed.commons.lang.IteratorEnumeration;
import fr.macymed.commons.logging.Logger;
import fr.macymed.commons.logging.LoggerFactory;
import fr.macymed.commons.message.MessageFactory;
import fr.macymed.commons.message.Messages;
import fr.macymed.commons.message.PropertiesProvider;
import fr.macymed.commons.message.ProviderMessages;
import fr.macymed.modulo.framework.module.ModulePermission;
import fr.macymed.modulo.platform.archive.ModuleArchive;

/** 
 * <p>
 * This class loader is used to loads classes from module archive.
 * </p>
 * <p>
 * This loader also maintains a list of dependencies. That's means that if a class (or a resource) cannot be found locally, the loader will look trough all the dependency loader.
 * </p>
 * <p>
 * It also allows byte-code transformation dedied methods.
 * </p>
 * @author <a href="mailto:alexandre.cartapanis@macymed.fr">Cartapanis Alexandre</a>
 * @version 2.23.18
 * @since Modulo Platform 2.0
 */
public class ModuleClassLoader extends SecureClassLoader {

    /** The id of the message provider. */
    private static final String MESSAGES_PROVIDER = "fr.macymed.modulo.platform.loader.messages";

    /** The archive of the module. */
    protected ModuleArchive archive;

    /** The id of the module. */
    protected String id;

    /** The list of dependant class loader. */
    private List<ModuleClassLoader> dependenciesLoader;

    /** The list of resolved dependency. */
    private List<ModuleDependency> dependencies;

    /** The context to be used when loading classes and resources */
    private AccessControlContext controlContext;

    /** The logger tools used for debugging. */
    protected Logger logger;

    /** The messages used for debugging. */
    protected Messages messages;

    /** Used to prevent asynchronous concurrent access. */
    private final Lock lock = new ReentrantLock();

    /**
     * <p>
     * Creates a new ModuleClassLoader.
     * </p>
     * @param _parent The parent class loader.
     * @param _archive The module archive used to locates classes and resources.
     */
    public ModuleClassLoader(ClassLoader _parent, ModuleArchive _archive) {
        super(_parent);
        this.controlContext = AccessController.getContext();
        this.logger = LoggerFactory.getInstance().getLogger(this.getClass().getName());
        if (MessageFactory.getMessageProvider(MESSAGES_PROVIDER) == null) {
            PropertiesProvider provider = new PropertiesProvider(MESSAGES_PROVIDER, this.getClass().getClassLoader());
            MessageFactory.addMessageProvider(MESSAGES_PROVIDER, provider);
        }
        this.messages = new ProviderMessages(MESSAGES_PROVIDER);
        this.dependenciesLoader = new ArrayList<ModuleClassLoader>();
        this.dependencies = new ArrayList<ModuleDependency>();
        this.archive = _archive;
        this.id = _archive.getDefinition().getRegisteringId();
    }

    /**
     * 
     * <p>
     * Adds a Module Dependency and the associated class loader. That's means that if a class (or a resource) cannot be found locally, the loader will look trough all the loader added using this method.
     * </p>
     * @param _depend The dependency "resolved" by the loader.
     * @param _loader The dependency class loader to add.
     */
    protected void addDependency(ModuleDependency _depend, ModuleClassLoader _loader) {
        if (_depend != null && _loader != null) {
            if ((!this.dependencies.contains(_depend)) && (!this.dependenciesLoader.contains(_loader)) && (_loader != this)) {
                this.dependencies.add(_depend);
                this.dependenciesLoader.add(_loader);
            }
        }
    }

    /**
     * <p>
     * Removes a dependency and the associated class loader from the list of dependencies.
     * </p>
     * @param _depend The dependency to remove.
     * @param _loader The dependency class loader to remove.
     */
    protected void removeDependency(ModuleDependency _depend, ModuleClassLoader _loader) {
        if (_depend != null && _loader != null) {
            if (this.dependencies.contains(_depend) && this.dependenciesLoader.contains(_loader)) {
                this.dependenciesLoader.remove(_loader);
                this.dependencies.remove(_depend);
            }
        }
    }

    /**
     * <p>
     * Returns the list of dependency class loaders
     * </p>
     * @return <code>List<ModuleClassLoader></code> - The list of dependencies.
     */
    protected List<ModuleClassLoader> getDependencyClassLoaders() {
        return this.dependenciesLoader;
    }

    /**
     * <p>
     * Returns the list of dependency (ie resolved).
     * </p>
     * @return <code>The list of dependency this loader contains (ie is resolved).
     */
    protected List<ModuleDependency> getDependencies() {
        return this.dependencies;
    }

    /**
     * <p>
     * Loads the class with the specified binary name.
     * </p>
     * <p>
     * This methods is equivalent to {@link #loadClass(String, boolean) loadClass(_name, false)}.
     * </p>
     * @param _name The binary name of the class.
     * @return <code>Class<?></code> - The resulting Class object.
     * @throws ClassNotFoundException If the class was not found.
     */
    @Override
    public Class<?> loadClass(String _name) throws ClassNotFoundException {
        return loadClass(_name, false);
    }

    /**
     * <p>
     * Loads the class with the specified binary name.
     * </p>
     * <p>
     * This methods is equivalent to {@link #loadClass(String, boolean, BytecodeTransformer) loadClass(_name, false, _transformer)}.
     * </p>
     * @param _name The binary name of the class.
     * @param _transformer The transformer used to transform bytecodes.
     * @return <code>Class<?></code> - The resulting Class object.
     * @throws ClassNotFoundException If the class was not found.
     */
    public Class<?> loadClass(String _name, BytecodeTransformer _transformer) throws ClassNotFoundException {
        return loadClass(_name, false, _transformer);
    }

    /**
     * <p>
     * Loads the class with the specified binary name.
     * </p>
     * <p>
     * This default implementation of this method searches for classes in the following order:
     * <li>
     *  <ul>Invoke {@link #findLoadedClass(String)} to check if the class has already been loaded.</ul>
     *  <ul>Invoke the <code>loadClass</code> method on the parent class loader. If the parent is null the class loader built-in to the virtual machine is used, instead.</ul>
     * </li>
     * <p>
     *  If the class was found using the above steps, and the <code>_resolve</code> flag is true, this method will then invoke the {@link #resolveClass(Class)} method on the resulting Class object.
     * </p>
     * @param _name The binary name of the class.
     * @param _resolve If true then resolve the class.
     * @return <code>Class<?></code> - The resulting Class object.
     * @throws ClassNotFoundException If the class was not found.
     */
    @Override
    protected Class<?> loadClass(String _name, boolean _resolve) throws ClassNotFoundException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.loadingclass", new Object[] { _name }));
        this.lock.lock();
        try {
            Class cla = this.findLoadedClass(_name);
            if (cla == null) {
                for (ModuleClassLoader loader : this.dependenciesLoader) {
                    cla = loader.findLoadedClass(_name);
                    if (cla != null) {
                        break;
                    }
                }
            }
            if (cla == null) {
                try {
                    if (this.getParent() != null) {
                        cla = this.getParent().loadClass(_name);
                    } else {
                        cla = this.findSystemClass(_name);
                    }
                } catch (ClassNotFoundException e) {
                    cla = this.findClass(_name);
                }
            }
            if (_resolve) {
                this.resolveClass(cla);
            }
            return cla;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * <p>
     * Loads the class with the specified binary name.
     * </p>
     * <p>
     * This default implementation of this method searches for classes in the following order:
     * <li>
     *  <ul>Invoke {@link #findLoadedClass(String)} to check if the class has already been loaded.</ul>
     *  <ul>Invoke the <code>loadClass</code> method on the parent class loader. If the parent is null the class loader built-in to the virtual machine is used, instead.</ul>
     * </li>
     * <p>
     *  If the class was found using the above steps, and the <code>_resolve</code> flag is true, this method will then invoke the {@link #resolveClass(Class)} method on the resulting Class object.
     * </p>
     * @param _name The binary name of the class.
     * @param _resolve If true then resolve the class.
     * @param _transformer The transformer used to transform bytecodes.
     * @return <code>Class<?></code> - The resulting Class object.
     * @throws ClassNotFoundException If the class was not found.
     */
    protected Class<?> loadClass(String _name, boolean _resolve, BytecodeTransformer _transformer) throws ClassNotFoundException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.loadingclasstransform", new Object[] { _name, _transformer }));
        this.lock.lock();
        try {
            Class cla = this.findLoadedClass(_name);
            if (cla == null) {
                for (ModuleClassLoader loader : this.dependenciesLoader) {
                    cla = loader.findLoadedClass(_name);
                    if (cla != null) {
                        break;
                    }
                }
            }
            if (cla == null) {
                try {
                    if (this.getParent() != null) {
                        cla = this.getParent().loadClass(_name);
                    } else {
                        cla = this.findSystemClass(_name);
                    }
                    this.logger.warning(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.notransform", new Object[] { _name, _transformer }));
                } catch (ClassNotFoundException excp) {
                    cla = this.findClass(_name, _transformer);
                }
            } else {
                this.logger.warning(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.notransform", new Object[] { _name, _transformer }));
            }
            if (_resolve) {
                this.resolveClass(cla);
            }
            return cla;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * <p>
     * This method overriden from <code>ClassLoader</code>. It is implemented such that it finds classes from the {@link fr.macymed.modulo.platform.archive.ModuleArchive module's archive}.
     * </p>
     * @param _name The name of the class to load.
     * @return <code>Class<?></code> - The loaded class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    @Override
    protected Class<?> findClass(final String _name) throws ClassNotFoundException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findingclass", new Object[] { _name }));
        Class<?> cla = null;
        try {
            cla = this.findLocalClass(_name);
        } catch (ClassNotFoundException excp) {
            for (ModuleClassLoader loader : this.dependenciesLoader) {
                try {
                    cla = loader.findLocalClass(_name);
                } catch (ClassNotFoundException excp2) {
                }
                if (cla != null) {
                    return cla;
                }
            }
        }
        if (cla == null) {
            throw new ClassNotFoundException(_name);
        }
        return cla;
    }

    /**
     * <p>
     * This method overriden from <code>ClassLoader</code>. It is implemented such that it finds classes from the {@link fr.macymed.modulo.platform.archive.ModuleArchive module's archive}.
     * </p>
     * @param _name The name of the class to load.
     * @param _transformer The transformer used to transform bytecodes.
     * @return <code>Class<?></code> - The loaded class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    protected Class<?> findClass(final String _name, final BytecodeTransformer _transformer) throws ClassNotFoundException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findingclasstransform", new Object[] { _name, _transformer }));
        Class<?> cla = null;
        try {
            cla = this.findLocalClass(_name, _transformer);
        } catch (ClassNotFoundException excp) {
            for (ModuleClassLoader loader : this.dependenciesLoader) {
                try {
                    cla = loader.findLocalClass(_name, _transformer);
                } catch (ClassNotFoundException excp2) {
                }
                if (cla != null) {
                    return cla;
                }
            }
        }
        if (cla == null) {
            throw new ClassNotFoundException(_name);
        }
        return cla;
    }

    /**
     * <p>
     * Finds class only into the local archive.
     * </p>
     * @param _name The name of the class to load.
     * @return <code>Class<?></code> - The loaded class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    private Class<?> findLocalClass(final String _name) throws ClassNotFoundException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findinglocalclass", new Object[] { _name }));
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {

                public Class<?> run() throws ClassNotFoundException {
                    String path = _name.replace('.', '/').concat(".class");
                    byte[] bytes = ModuleClassLoader.this.archive.getBytes(path);
                    if (bytes != null) {
                        try {
                            return ModuleClassLoader.this.defineClass(_name, bytes);
                        } catch (IOException excp) {
                            throw new ClassNotFoundException(_name, excp);
                        }
                    }
                    throw new ClassNotFoundException(_name);
                }
            }, this.controlContext);
        } catch (PrivilegedActionException excp) {
            try {
                throw (ClassNotFoundException) excp.getException();
            } catch (Exception excp1) {
                throw new ClassNotFoundException(excp.getMessage(), excp);
            }
        }
    }

    /**
     * <p>
     * Finds class only into the local archive.
     * </p>
     * @param _name The name of the class to load.
     * @param _transformer The transformer used to transform bytecodes.
     * @return <code>Class<?></code> - The loaded class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    private Class<?> findLocalClass(final String _name, final BytecodeTransformer _transformer) throws ClassNotFoundException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findinglocalclasstransform", new Object[] { _name, _transformer }));
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {

                public Class<?> run() throws ClassNotFoundException {
                    String path = _name.replace('.', '/').concat(".class");
                    byte[] bytes = ModuleClassLoader.this.archive.getBytes(path);
                    if (bytes != null) {
                        try {
                            return ModuleClassLoader.this.defineClass(_name, bytes, _transformer);
                        } catch (IOException excp) {
                            throw new ClassNotFoundException(_name, excp);
                        }
                    }
                    throw new ClassNotFoundException(_name);
                }
            }, this.controlContext);
        } catch (PrivilegedActionException excp) {
            try {
                throw (ClassNotFoundException) excp.getException();
            } catch (Exception excp1) {
                throw new ClassNotFoundException(excp.getMessage(), excp);
            }
        }
    }

    /**
     * <p>
     * This method overriden from <code>ClassLoader</code>. It is implemented such that it finds resources from the {@link fr.macymed.modulo.platform.archive.ModuleArchive module's archive}.
     * </p>
     * @param _name The name of the resource to load.
     * @return <code>URL</code> - The <code>URL</code> associated with the resource or <code>null</code>.
     */
    @Override
    protected URL findResource(String _name) {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findingresource", new Object[] { _name }));
        URL url = this.findLocalResource(_name);
        if (url == null) {
            for (ModuleClassLoader loader : this.dependenciesLoader) {
                url = loader.findLocalResource(_name);
                if (url != null) {
                    return url;
                }
            }
        }
        return url;
    }

    /**
     * <p>
     * Finds the resource with the specified name into the local archive
     * </p>
     * @param _name the name of the resource
     * @return a <code>URL</code> for the resource, or <code>null</code> if the resource could not be found.
     */
    private URL findLocalResource(final String _name) {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findinglocalresource", new Object[] { _name }));
        URL url = AccessController.doPrivileged(new PrivilegedAction<URL>() {

            public URL run() {
                try {
                    return ModuleClassLoader.this.archive.getURL(_name);
                } catch (Exception excp) {
                    ModuleClassLoader.this.logger.info(ModuleClassLoader.this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.urlcreationerror", new Object[] { _name }), excp);
                    return null;
                }
            }
        }, this.controlContext);
        return url != null ? this.checkURL(url) : null;
    }

    /**
     * <p>
     * This method overriden from <code>ClassLoader</code>. It is implemented such that it finds resources from the {@link fr.macymed.modulo.platform.archive.ModuleArchive module's archive}.
     * </p>
     * @param _name The resource name.
     * @return <code>Enumeration<URL></code> - An enumeration of URLs objects for the resources.
     * @throws IOException If an error occurs.
     */
    @Override
    protected Enumeration<URL> findResources(String _name) throws IOException {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findingresources", new Object[] { _name }));
        List<URL> resources = new ArrayList<URL>();
        Enumeration<URL> enume = super.findResources(_name);
        while (enume.hasMoreElements()) {
            resources.add(enume.nextElement());
        }
        enume = this.findLocalResources(_name);
        while (enume.hasMoreElements()) {
            resources.add(enume.nextElement());
        }
        for (ModuleClassLoader loader : this.dependenciesLoader) {
            enume = loader.findLocalResources(_name);
            while (enume.hasMoreElements()) {
                resources.add(enume.nextElement());
            }
        }
        return new IteratorEnumeration<URL>(resources.iterator());
    }

    /**
     * 
     * @param _name The resource name.
     * @return <code>Enumeration<URL></code> - An enumeration of URLs objects for the resources.
     */
    private Enumeration<URL> findLocalResources(final String _name) {
        this.logger.debug(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.findinglocalresources", new Object[] { _name }));
        List<URL> resources = new ArrayList<URL>();
        URL res = this.findLocalResource(_name);
        if (res != null) {
            resources.add(res);
        }
        return new IteratorEnumeration<URL>(resources.iterator());
    }

    /**
     * <p>
     * Defines a Class using the class bytes obtained from the specified bytes. The resulting Class must be resolved before it can be used.
     * </p>
     * @param _name The name of the class.
     * @param _bytes The bytes used to define the class.
     * @return <code>Class<?></code> - The loaded class.
     * @throws IOException If an error occurs while defining class.
     */
    protected Class<?> defineClass(String _name, byte[] _bytes) throws IOException {
        int i = _name.lastIndexOf('.');
        URL url = this.archive.getURL();
        if (i != -1) {
            String pkgname = _name.substring(0, i);
            Package pkg = this.getPackage(pkgname);
            Manifest man = this.archive.getManifest();
            if (pkg != null) {
                if (pkg.isSealed()) {
                    if (!pkg.isSealed(url)) {
                        throw new SecurityException(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.sealederror", new Object[] { pkgname }));
                    }
                } else {
                    if ((man != null) && this.isSealed(pkgname, man)) {
                        throw new SecurityException(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.sealingerror", new Object[] { pkgname }));
                    }
                }
            } else {
                if (man != null) {
                    this.definePackage(pkgname, man, url);
                } else {
                    this.definePackage(pkgname, null, null, null, null, null, null, null);
                }
            }
        }
        String path = _name.replace('.', '/').concat(".class");
        CodeSource cs = new CodeSource(url, this.archive.getCodeSigners(path));
        return this.defineClass(_name, _bytes, 0, _bytes.length, cs);
    }

    /**
     * <p>
     * Defines a Class using the class bytes obtained from the specified bytes. The resulting Class must be resolved before it can be used.
     * </p>
     * @param _name The name of the class.
     * @param _bytes The bytes used to define the class.
     * @param _transformer The transformer used to transform bytecodes.
     * @return <code>Class<?></code> - The loaded class.
     * @throws IOException If an error occurs while defining class.
     */
    protected Class<?> defineClass(String _name, byte[] _bytes, BytecodeTransformer _transformer) throws IOException {
        return this.defineClass(_name, _transformer.transform(_name, _bytes));
    }

    /**
     * <p>
     * Defines a new package by name in this ClassLoader. The attributes contained in the specified Manifest will be used to obtain package version and sealing information. For sealed packages, the additional URL specifies the code source URL from which the package was loaded.
     * </p>
     * @param _name The package name.
     * @param _manifest The Manifest containing package version and sealing information.
     * @param _url The code source url for the package, or null if none.
     * @exception IllegalArgumentException if the package name duplicates an existing package either in this class loader or one of its ancestors.
     * @return <code>Package</code> - The newly defined Package object.
     */
    private Package definePackage(String _name, Manifest _manifest, URL _url) throws IllegalArgumentException {
        String path = _name.replace('.', '/').concat("/");
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        String sealed = null;
        URL sealBase = null;
        Attributes attr = _manifest.getAttributes(path);
        if (attr != null) {
            specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Name.SEALED);
        }
        attr = _manifest.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = _url;
        }
        return definePackage(_name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    /**
     * <p>
     * Returns true if the specified package name is sealed according to the given manifest.
     * </p>
     * @param _name The package name.
     * @param _manifest The Manifest containing package version and sealing information.
     * @return <code>boolean</code> - True if the specified package is sealed, false otherwise.
     */
    private boolean isSealed(String _name, Manifest _manifest) {
        String path = _name.replace('.', '/').concat("/");
        Attributes attr = _manifest.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = _manifest.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /**
     * <p>
     * Returns the permissions for the given codesource object.
     * </p>
     * <p>
     * The implementation of this method first calls <code>super.getPermissions</code> and then adds permissions based on the URL of the codesource.
     * </p>
     * <p>
     * Because the URL will always use the modulo protocol, the returned list will always contains a <code>ModulePermission(moduleId, "read")</code>.
     * </p>
     * @param _codeSource The code source.
     * @return <code>PermissionCollection</code> - The permissions granted to the code source.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource _codeSource) {
        PermissionCollection perms = super.getPermissions(_codeSource);
        URL url = _codeSource.getLocation();
        Permission perm = null;
        URLConnection urlConnection = null;
        try {
            urlConnection = url.openConnection();
            urlConnection.connect();
            perm = urlConnection.getPermission();
        } catch (IOException excp) {
            perm = null;
            urlConnection = null;
        }
        if (perm == null) {
            perm = new ModulePermission(url.getHost(), "read");
        }
        if (perm != null) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                final Permission fp = perm;
                AccessController.doPrivileged(new PrivilegedAction<Object>() {

                    public Object run() throws SecurityException {
                        sm.checkPermission(fp);
                        return null;
                    }
                }, this.controlContext);
            }
            perms.add(perm);
        }
        return perms;
    }

    /**
     * <p>
     * Checks whether the resource URL should be returned. Return null on security check failure.
     * </p>
     * @param _url The url to check.
     * @return <code>URL</code> - The specified URL, or null on security check failure.
     */
    protected URL checkURL(URL _url) {
        try {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                URLConnection urlConnection = _url.openConnection();
                Permission perm = urlConnection.getPermission();
                if (perm != null) {
                    security.checkPermission(perm);
                }
            }
        } catch (Exception excp) {
            if (_url != null) {
                this.logger.info(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.urlerror", new Object[] { _url }), excp);
            } else {
                this.logger.info(this.messages.getMessage("fr.macymed.modulo.platform.loader.ModuleClassLoader.urlerror", new Object[] { "null" }), excp);
            }
            return null;
        }
        return _url;
    }

    /**
     * <p>
     * Returns a string representation of this object.
     * </p>
     * @return <code>String</code> - A string representation of this object.
     */
    @Override
    public String toString() {
        return "ModuleClassLoader[" + this.id + "]";
    }
}
