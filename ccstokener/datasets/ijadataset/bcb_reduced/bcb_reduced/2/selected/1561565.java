package net.sourceforge.javautil.classloader.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import net.sourceforge.javautil.classloader.resolver.IClassDependencyPool;
import net.sourceforge.javautil.classloader.resolver.IClassPackage;
import net.sourceforge.javautil.classloader.resolver.impl.ClassDependencyPoolImpl;
import net.sourceforge.javautil.classloader.source.ClassSource;
import net.sourceforge.javautil.classloader.source.CompositeClassSource;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.VirtualArtifactNotFoundException;
import net.sourceforge.javautil.common.logging.ILogger;
import net.sourceforge.javautil.common.logging.LoggingContext;
import net.sourceforge.javautil.common.reflection.cache.ClassCache;

/**
 * A class context is a {@link ClassLoader} whose class resolving logic is defined by 
 * an arbitrary {@link IClassLoaderHeiarchy}.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: ClassContext.java 2709 2010-12-31 00:46:45Z ponderator $
 */
public class ClassContext extends URLClassLoader {

    protected IClassLoaderHeiarchy heiarchy;

    protected long modified = System.currentTimeMillis();

    protected CompositeClassSource all;

    protected CompositeClassSource nonPackageResources;

    protected IClassDependencyPool pool;

    protected ClassFileTransformer transformer;

    protected ILogger log = LoggingContext.getContextualLogger(ClassContext.class);

    /**
	 * This will store the pool used and extract the class sources for it.
	 * 
	 * @see #ClassContext(IClassLoaderHeiarchy, Collection)
	 */
    public ClassContext(IClassLoaderHeiarchy heiarchy, ClassDependencyPoolImpl pool) {
        this(heiarchy, pool, new ClassSource[0]);
    }

    /**
	 * This will take the sources collection which will be translated and passed onto as an array.
	 * 
	 * @see #ClassContext(IClassLoaderHeiarchy, ClassSource...)
	 */
    public ClassContext(IClassLoaderHeiarchy heiarchy, Collection<ClassSource> sources) {
        this(heiarchy, sources.toArray(new ClassSource[sources.size()]));
    }

    /**
	 * @param heiarchy The heiarchy to use
	 * @param sources One or more sources that compose what this class context refers to
	 */
    public ClassContext(IClassLoaderHeiarchy heiarchy, ClassSource... sources) {
        this(heiarchy, null, sources);
    }

    public ClassContext(IClassLoaderHeiarchy heiarchy, IClassDependencyPool pool, ClassSource... sources) {
        super(new URL[0]);
        this.heiarchy = heiarchy;
        this.nonPackageResources = sources.length == 1 && sources[0] instanceof CompositeClassSource ? (CompositeClassSource) sources[0] : new CompositeClassSource(sources);
        this.setPool(pool);
        log.info("Class Loader Configuration: ");
        for (ClassSource src : this.nonPackageResources.getAllNonCompositeSources()) {
            log.info("   ----> " + src);
        }
        if (this.pool != null) {
            log.info(" ");
            for (IClassPackage pkg : this.pool.getPackages(true)) {
                ClassSource src = pkg.getMainJarSource();
                log.info("   ----> " + pkg + "/" + src);
                src.cleanup();
            }
        }
    }

    @Override
    public URL[] getURLs() {
        return this.getAllSources().getUrls().toArray(new URL[0]);
    }

    /**
	 * @return The dependency pool that was provided (when using {@link #ClassContext(IClassLoaderHeiarchy, ClassDependencyPoolImpl)} or
	 * {@link #setPool(ClassDependencyPoolImpl)}), or null if none was provided
	 */
    public IClassDependencyPool getPool() {
        return pool;
    }

    /**
	 * If this is called after the pool has been initialized, it will throw an {@link IllegalArgumentException}.
	 * 
	 * @param pool The pool related to this context
	 * @return A reference to this object for chaining
	 */
    public ClassContext setPool(IClassDependencyPool pool) {
        if (this.pool != null) throw new IllegalArgumentException("A pool has already been assigned to this class context");
        this.all = pool == null ? this.nonPackageResources : new CompositeClassSource(pool.getCompositeClassSource(), this.nonPackageResources);
        this.pool = pool;
        if (this.pool != null) {
            log.info(" ");
            for (IClassPackage pkg : this.pool.getPackages(true)) {
                ClassSource src = pkg.getMainJarSource();
                log.info("   ----> " + pkg + "/" + src);
                src.cleanup();
            }
        }
        return this;
    }

    /**
	 * @return The composite class source composing non {@link IClassPackage} sources
	 */
    public CompositeClassSource getNonPackageSources() {
        return this.nonPackageResources;
    }

    /**
	 * @return The heiarchy logic to use in connection with this class context
	 */
    public IClassLoaderHeiarchy getHeiarchy() {
        return heiarchy;
    }

    /**
	 * @return The transformer used by this class loader, or null if no transformer is being used
	 */
    public ClassFileTransformer getTransformer() {
        return transformer;
    }

    public ClassContext setTransformer(ClassFileTransformer transformer) {
        this.transformer = transformer;
        return this;
    }

    /**
	 * @return All class sources including non {@link IClassPackage} sources and {@link IClassPackage} sources
	 */
    public CompositeClassSource getAllSources() {
        if (this.pool != null && this.pool.getLastModified() > this.modified) {
            this.modified = this.pool.getLastModified();
            this.all = new CompositeClassSource(this.pool.getCompositeClassSource(), this.nonPackageResources);
            log.info(" ");
            for (IClassPackage pkg : this.pool.getPackages(true)) {
                ClassSource src = pkg.getMainJarSource();
                log.info("   ----> " + pkg + "/" + src);
                src.cleanup();
            }
        }
        return this.all;
    }

    @Override
    protected String findLibrary(String libname) {
        URL url = this.getAllSources().getResource(System.mapLibraryName(libname));
        if (url != null && url.toExternalForm().startsWith("file:/")) {
            return url.toExternalForm().substring(6);
        }
        return null;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return this.loadClass(heiarchy.createRootContext(this, name, resolve));
        } catch (LinkageError error) {
            if (this.pool != null) {
                this.pool.printHeiarchy(System.out);
            }
            throw error;
        }
    }

    public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(heiarchy.createRootContext(this, name, false));
    }

    /**
	 * @param ctx The current class loading context
	 * @return The class that is specified by the context
	 * @throws ClassNotFoundException
	 */
    public Class<?> loadClass(IClassLoadingContext ctx) throws ClassNotFoundException {
        while (heiarchy.assignNextClassLoader(ctx)) {
            try {
                ClassLoader loader = ctx.getCurrentLoader();
                if (loader instanceof ClassContext) {
                    if (loader == this) {
                        return this.loadClass(ctx.getSearchInfo(), ctx.isResolveNeeded());
                    } else if (loader == heiarchy.getParent()) {
                        return ((ClassContext) loader).resolveFromChild(this, ctx);
                    } else return ((ClassContext) loader).loadClass(heiarchy.createChildContext(this, (ClassContext) loader, ctx));
                } else return loader.loadClass(ctx.getSearchInfo().getFullClassName());
            } catch (ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(ctx.getSearchInfo().getFullClassName());
    }

    /**
	 * @param csi The search info for the class in question
	 * @param resolve True if it should be resolved, otherwise false
	 * @return The class that was found
	 * @throws ClassNotFoundException
	 */
    protected synchronized Class<?> loadClass(ClassSearchInfo csi, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = this.findLoadedClass(csi.getFullClassName());
        if (clazz == null) clazz = this.findClass(csi);
        if (resolve) {
            try {
                this.resolveClass(clazz);
            } catch (LinkageError error) {
                if (this.pool != null) this.pool.printHeiarchy(System.out);
                throw error;
            }
        }
        return clazz;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return this.findClass(new ClassSearchInfo(name));
    }

    /**
	 * @param csi The search info for the class in question
	 * @return The class that was found
	 * @throws ClassNotFoundException
	 */
    protected Class<?> findClass(ClassSearchInfo csi) throws ClassNotFoundException {
        ClassInfo info = this.getAllSources().getIfHasClass(csi);
        if (info == null) throw new ClassNotFoundException(csi.getFullClassName());
        return this.defineClass(info);
    }

    /**
	 * @param info The info about the found class
	 * @return A byte[] compiled class, this will take care of package sealing
	 * @throws ClassNotFoundException
	 */
    protected Class<?> defineClass(ClassInfo info) throws ClassNotFoundException {
        byte[] data = info.getClassData();
        try {
            if (transformer != null) data = transformer.transform(this, info.getFullyQualifiedName(), null, null, data);
        } catch (IllegalClassFormatException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
        String pkgname = info.getPackageName();
        if (pkgname != null && !pkgname.equals("")) {
            Package pkg = getPackage(pkgname);
            Manifest man = info.getSource().getManifest();
            if (pkg != null) info.getSource().verifySeal(pkg, man); else {
                if (man != null) definePackage(pkgname, man, info.getSource().getURL()); else definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
        try {
            return defineClass(info.getFullyQualifiedName(), data, 0, data.length);
        } catch (LinkageError e) {
            throw new ClassLoadingException("Could not define class: " + info.getFullyQualifiedName(), e);
        }
    }

    /**
	 * @param name The name of the package
	 * @param man The corresponding manifest file
	 * @param url The url of the sealed jar, maybe null
	 * @return The newly created package
	 * @throws IllegalArgumentException
	 */
    protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        String path = name.replace('.', '/').concat("/");
        String specTitle = null, specVersion = null, specVendor = null;
        String implTitle = null, implVersion = null, implVendor = null;
        String sealed = null;
        URL sealBase = null;
        Attributes attr = man.getAttributes(path);
        if (attr != null) {
            specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            if (specVersion == null) specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            if (specVendor == null) specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            if (implTitle == null) implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            if (implVersion == null) implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            if (implVendor == null) implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            if (sealed == null) sealed = attr.getValue(Name.SEALED);
        }
        if ("true".equalsIgnoreCase(sealed)) sealBase = url;
        return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    @Override
    public URL findResource(String name) {
        try {
            return this.getAllSources().getResource(name);
        } catch (VirtualArtifactNotFoundException e) {
            return null;
        }
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        if (this.getAllSources() instanceof CompositeClassSource) {
            return Collections.enumeration(this.getAllSources().getResources(name));
        } else {
            List<URL> url = new ArrayList<URL>();
            url.add(this.findResource(name));
            return Collections.enumeration(url);
        }
    }

    /**
	 * @param child The child context using this context for child linking/resolution
	 * @param ctx The current class loading context
	 * @return The class found
	 * @throws ClassNotFoundException
	 */
    public Class<?> resolveFromChild(ClassContext child, IClassLoadingContext ctx) throws ClassNotFoundException {
        return this.loadClass(heiarchy.createParentContext(this, child, ctx));
    }

    @Override
    public URL getResource(String name) {
        return this.getResource(heiarchy.createRootResourceContext(this, name));
    }

    /**
	 * @param ctx The current resource loading context
	 * @return A URL pointing to the resource, or null if it could not be found
	 */
    public URL getResource(IClassResourceLoadingContext ctx) {
        URL url = this.findResource(ctx.getResourcePath());
        if (url != null) return url;
        while (url == null && heiarchy.assignNextResourceLoader(ctx)) {
            ClassLoader loader = ctx.getCurrentLoader();
            if (loader == this) continue;
            if (loader instanceof ClassContext) {
                if (loader == heiarchy.getParent()) {
                    url = ((ClassContext) loader).resolveFromChild(this, ctx);
                } else url = ((ClassContext) loader).getResource(heiarchy.createChildResourceContext(this, (ClassContext) loader, ctx));
            } else url = loader.getResource(ctx.getResourcePath());
        }
        return url;
    }

    /**
	 * @param child The child context call this context as a parent
	 * @param ctx The resource loading context currently in use
	 * @return A URL from this parent for the resource, or null if it could not be located
	 */
    public URL resolveFromChild(ClassContext child, IClassResourceLoadingContext ctx) {
        return this.getResource(heiarchy.createParentResourceContext(this, child, ctx));
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        try {
            URL url = this.getResource(name);
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return Collections.enumeration(this.getResources(heiarchy.createRootResourceContext(this, name)));
    }

    /**
	 * @param ctx The current resource loading context
	 * @return A list of URL's (possibly empty) pointing to the resources that matched
	 * @throws IOException
	 */
    public List<URL> getResources(IClassResourceLoadingContext ctx) throws IOException {
        Set<URL> urls = new LinkedHashSet<URL>(this.getAllSources().getResources(ctx.getResourcePath()));
        while (heiarchy.assignNextResourceLoader(ctx)) {
            ClassLoader loader = ctx.getCurrentLoader();
            if (loader == this) continue;
            if (loader instanceof ClassContext) {
                urls.addAll(((ClassContext) loader).getResources(heiarchy.createChildResourceContext(this, (ClassContext) loader, ctx)));
            } else {
                urls.addAll(Collections.list(loader.getResources(ctx.getResourcePath())));
            }
        }
        return new ArrayList<URL>(urls);
    }

    /**
	 * This MUST be called before discarding use of object, in order to allow internal system
	 * links to be cleared, if any.
	 */
    public void cleanup() {
        ClassCache.getCache().remove(this);
        this.getAllSources().cleanup();
    }
}
