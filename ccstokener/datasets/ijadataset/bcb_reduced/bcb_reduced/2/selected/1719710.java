package net.sourceforge.javautil.classloader.source;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.logging.Logger;
import net.sourceforge.javautil.classloader.impl.ClassInfo;
import net.sourceforge.javautil.classloader.impl.ClassSearchInfo;
import net.sourceforge.javautil.classloader.impl.PackageSearchInfo;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualArtifact;
import net.sourceforge.javautil.common.visitor.IVisitorSimple;

/**
 * This is an abstraction about a class source (normally considered a jar, or package of classes)
 * and allows tracing back to the source information about a class and also caching about class lookup
 * for more efficiency.
 * 
 * @author ponder
 * @author $Author: ponderator $
 * @version $Id: ClassSource.java 2678 2010-12-24 04:18:29Z ponderator $
 */
public abstract class ClassSource implements Cloneable {

    protected static final ThreadLocal<byte[]> buffer = new ThreadLocal<byte[]>();

    protected Logger log;

    protected final String name;

    protected List<Package> pkgOk = new ArrayList<Package>();

    protected List<Package> pkgFail = new ArrayList<Package>();

    protected Manifest manifest;

    protected ClassSourceAdapter adapter;

    public <I extends IVisitorSimple<ClassSourceVisitorContext>> I accept(I visitor) {
        return new ClassSourceVisitorContext(this).visit(visitor);
    }

    /**
	 * @param name The name of this class source
	 */
    public ClassSource(String name) {
        this.name = name;
    }

    /**
	 * @param scl Called by the class loader to allow for initialization
	 */
    public void initialize(ClassLoader scl) {
    }

    /**
	 * @return The logger for this class source
	 */
    public Logger getLogger() {
        if (log == null) log = Logger.getLogger(getClass().getName());
        return log;
    }

    /**
	 * @return The collection of packages currently loaded from previous scans
	 */
    public abstract Collection<String> getPackages();

    /**
	 * @param info The info about the class to search for
	 * @return True if this source does contain the class, otherwise false
	 */
    public abstract boolean hasClass(ClassSearchInfo info);

    /**
	 * @param info The info about the class to search for
	 * @return The info about the class and its true {@link ClassSource}
	 * @throws ClassNotFoundException
	 */
    public abstract ClassInfo getClassInfo(ClassSearchInfo info) throws ClassNotFoundException;

    public abstract ClassSource clone() throws CloneNotSupportedException;

    /**
	 * @return The adapter being used to adapt/transform byte code from this source, or null if no adapters are currently being used
	 */
    public ClassSourceAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(ClassSourceAdapter adapter) {
        this.adapter = adapter;
    }

    /**
	 * @param info The info about the class to search for
	 * @return The byte[] array of data representing the class byte code (possibly adapted/transformed)
	 * @throws ClassNotFoundException
	 * 
	 * @see {@link #getAdapter()}
	 */
    public byte[] load(ClassSearchInfo info) throws ClassNotFoundException {
        return adapter == null ? this.loadInternal(info) : adapter.load(this, new ClassInfo(info, this));
    }

    protected byte[] getBuffer() {
        if (buffer.get() == null) buffer.set(new byte[1024 * 100]);
        return buffer.get();
    }

    /**
	 * @param info The info about the class to search for
	 * @return The byte[] array of data representing the class byte code
	 * @throws ClassNotFoundException
	 */
    protected abstract byte[] loadInternal(ClassSearchInfo info) throws ClassNotFoundException;

    /**
	 * @param info The info about the package to search for
	 * @return True if this class source does have a package according to the info, otherwise false
	 */
    public abstract boolean hasPackage(PackageSearchInfo info);

    /**
	 * @param info The name of the package to search for
	 * @return True if this class source does have a parent package by that name, otherwise false
	 */
    public abstract boolean hasParentPackage(String packageName);

    /**
	 * @param resourceName The name of the resource to search for (relative path)
	 * @return True if this class source contains the resource, otherwise false
	 */
    public abstract boolean hasResource(String resourceName);

    /**
	 * @param parentResource The name of a parent resource (like a directory)
	 * @return True if this class source contains the parent resource, otherwise false
	 */
    public abstract boolean hasParentResource(String parentResource);

    /**
	 * @param resourceName The name of the resource to retrieve
	 * @return A URL providing access to the resource, otherwise null if it does not exist
	 * @throws MalformedURLException
	 */
    public abstract URL getResource(String resourceName);

    /**
	 * @return True if this class source has searched all possible internal sources for classes, otherwise false (may indicate a partial search)
	 */
    public abstract boolean hasSearchedAll();

    /**
	 * @return True if this source has access to directory information (like empty parent directories), otherwise false
	 */
    public boolean hasDirectories() {
        return true;
    }

    /**
	 * @return The name of this class source, passed to {@link #ClassSource(String)}
	 */
    public String getName() {
        return name;
    }

    /**
	 * Called when this class source is no longer needed or is to be reused
	 */
    public void cleanup() {
    }

    /**
	 * @return The URL of this class source
	 */
    public abstract URL getURL();

    /**
	 * @return The resource names in this class source.
	 */
    public abstract Collection<String> getResourceNames();

    /**
	 * @return The collection of all the class names available in this class source
	 */
    public abstract Collection<String> getClassNames();

    /**
	 * @param info The info about the package for which to search for classes
	 * @return A collection of class names belonging to the package
	 */
    public abstract List<String> getClassNamesForPackage(PackageSearchInfo info);

    /**
	 * @return True if the raw source of this class source has been modified
	 */
    public abstract boolean isHasBeenModified();

    /**
	 * @return True if any .class file has been modified
	 */
    public abstract boolean isHasClassesBeenModified();

    /**
	 * @return The timestamp of the class with the most recent last modified date
	 */
    public abstract long getLastModifiedClass();

    /**
	 * @return The last modified date for this resource
	 */
    public abstract long getLastModified();

    /**
	 * @return Returns a virtual artifact for accessing the class source, otherwise null
	 */
    public abstract IVirtualArtifact getVirtualArtifact();

    /**
	 * Reload this class source, clearing any cached information
	 */
    public void reload() {
    }

    /**
	 * @param pkg The package for which to verify a seal
	 * @param man The manifest file related to this class source
	 * @throws SecurityException
	 */
    public final void verifySeal(Package pkg, Manifest man) throws SecurityException {
        if (pkgOk.contains(pkg)) return;
        if (!pkgFail.contains(pkg)) {
            if (!pkg.isSealed(this.getURL()) && pkg.isSealed()) pkgFail.add(pkg); else {
                pkgOk.add(pkg);
                return;
            }
        }
        if (pkgFail.contains(pkg)) throw new SecurityException("sealing violation: package " + pkg.getName() + " is sealed");
    }

    /**
	 * @param name The name of the package
	 * @param man The manifest for this class source
	 * @return True if the package has already been sealed, otherwise false
	 */
    private boolean isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) sealed = attr.getValue(Name.SEALED);
        if (sealed == null) if ((attr = man.getMainAttributes()) != null) sealed = attr.getValue(Name.SEALED);
        return "true".equalsIgnoreCase(sealed);
    }

    /**
   * @param resourceName The name of a resource
   * @return An input stream for reading the resource, otherwise null if there is an {@link IOException}
   */
    public InputStream getResourceAsStream(String resourceName) {
        try {
            return this.getResource(resourceName).openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
	 * @param info The info about a class to search for
	 * @return The {@link ClassInfo} if the class does exist in this class source, otherwise null
	 * @throws ClassNotFoundException
	 */
    public ClassInfo getIfHasClass(ClassSearchInfo info) throws ClassNotFoundException {
        if (this.hasClass(info)) return this.getClassInfo(info);
        return null;
    }

    /**
	 * @return Retrieve the manifest for this class source, otherwise null if it does not exist
	 */
    public Manifest getManifest() {
        if (manifest != null) return manifest;
        try {
            URL url = this.getResource("META-INF/MANIFEST.MF");
            if (url != null) return manifest = new Manifest(url.openStream()); else return null;
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * Utility method for caching package information.
	 * 
	 * @param info The package information defining a package
	 * @param packages The packages that have been found and need to be cached
	 */
    protected void storeUniquePacakges(PackageSearchInfo info, List<String> packages) {
        List<String> parents = info.getPackages();
        for (int p = 0; p < parents.size(); p++) if (!packages.contains(parents.get(p))) packages.add(parents.get(p));
    }

    public String toString() {
        return getClass().getSimpleName() + ": [" + name + "]";
    }

    public boolean equals(Object object) {
        if (!(object instanceof ClassSource)) return false;
        if (this == object) return true;
        ClassSource compare = (ClassSource) object;
        if (compare instanceof CompositeClassSource) {
            if (!(this instanceof CompositeClassSource)) return false;
            if (((CompositeClassSource) compare).getSources().size() != ((CompositeClassSource) this).getSources().size()) return false;
            for (ClassSource src : ((CompositeClassSource) this).getSources()) {
                if (!((CompositeClassSource) compare).contains(src)) return false;
            }
            return true;
        } else {
            if (this instanceof CompositeClassSource) return false;
            return this.getURL().equals(compare.getURL());
        }
    }
}
