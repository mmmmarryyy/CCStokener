package net.wotonomy.foundation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import net.wotonomy.foundation.internal.NetworkClassLoader;

/**
 * An implementation of NSBundle. Unlike the standard WebObjects NSBundle, this
 * implementation loads bundles dynamically. This means that all bundles do not
 * need to exist in the classpath at startup time. Practically, this means that
 * NSBundle has a custom classloader.
 * 
 * This behaviour is not supported in Apple's WebObjects, and should be used
 * only if compatibility is not desired. It is largely intended for internal use
 * within this framework, but is exposed through bundleForURL()
 * 
 * Another difference between Wotonomy's NSBundle and Apple's implementation is
 * the ability to initialize the application with a custom resource lookup
 * "path".
 * 
 * @author cgruber@israfil.net
 * @author $Author: cgruber $
 * @version $Revision: 892 $
 * 
 */
public class NSBundle {

    public static final String BundleDidLoadNotification = "NSBundleDidLoadNotification";

    public static final String LoadedClassesNotification = "NSLoadedClassesNotification";

    private static final NSMutableArray _allBundles = new NSMutableArray();

    private static final NSMutableArray _allFrameworks = new NSMutableArray();

    private static NSMutableDictionary _languageCodes = new NSMutableDictionary();

    private static NSBundle _mainBundle = null;

    protected static NetworkClassLoader _classLoader = new NetworkClassLoader(ClassLoader.getSystemClassLoader());

    protected String name;

    protected NSMutableDictionary info = null;

    protected String path;

    protected NSMutableArray classNames = new NSMutableArray();

    protected NSMutableArray packages = new NSMutableArray();

    protected Properties properties;

    protected boolean isFramework = false;

    protected boolean loaded = false;

    protected Class principalClass;

    /**
	 * The default constructor, which is only public to support other framework
	 * functionality, and to be API compatible with Apple's WebObjects.
	 * Generally, framework users should use bundleForXXXX() methods.
	 */
    public NSBundle() {
    }

    /**
	 * @deprecated use mainBundle() to access the application bundle and
	 *             frameworkBundles() to access any frameworks.
	 */
    public static synchronized NSArray allBundles() {
        return _allBundles.immutableClone();
    }

    /**
	 * @deprecated use frameworkBundles() to access any frameworks.
	 */
    public static NSArray allFrameworks() {
        return frameworkBundles();
    }

    /**
	 * Returns the bundle that contains the provided class, if any. Otherwise,
	 * it returns null. Because NSBundles have a specialized class-loader, if
	 * any two bundles contain duiplicates of the same class, the second will
	 * fail to load. TODO: Determine if class-load scoping of duplicate classes
	 * is appropriate.
	 * 
	 * @param class1
	 * @return NSBundle
	 */
    public static synchronized NSBundle bundleForClass(Class class1) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    /**
	 * @deprecated Apple's WebObjects says you should not load from arbitrary
	 *             path.
	 * @param path
	 * @return
	 */
    public static synchronized NSBundle bundleWithPath(String path) {
        try {
            return bundleWithURL(new File(path).toURI().toURL());
        } catch (MalformedURLException e) {
            NSLog.err.appendln("Bundle path is invalid: " + path);
            return null;
        }
    }

    /**
	 * <strong>Note:</strong>This method is only in Wotonomy.
	 * 
	 * This method returns a bundle at a given URL, registering that bundle as
	 * well. If the bundle has already been loaded/registered, it is simply
	 * returned from the cache.
	 * 
	 * @param url
	 * @return
	 */
    public static synchronized NSBundle bundleWithURL(URL url) {
        NSBundle result = null;
        String sep = System.getProperty("file.separator");
        String protocol = url.getProtocol();
        if (protocol.equals("file")) {
            File f = new File(url.getPath());
            if (!f.exists()) {
                NSLog.err.appendln("Bundle not found: " + url);
                return null;
            }
            StringBuffer filename = new StringBuffer(f.getName());
            int extensionIndex = filename.lastIndexOf(".");
            if (extensionIndex == -1) {
                NSLog.err.appendln("Named URL does not point to a bundle with an extension: " + url);
                return null;
            }
            String basename = filename.substring(0, extensionIndex);
            String extension = filename.substring(extensionIndex + 1, filename.length());
            System.out.println("basename: " + basename);
            System.out.println("extension: " + extension);
            result = new NSBundle();
            result.name = basename;
            result.isFramework = extension.equals("framework");
            if (f.isDirectory()) {
                try {
                    File javadir = new File(f.getCanonicalPath() + sep + "Contents" + sep + "Resources" + sep + "Java");
                    System.out.println(javadir);
                    System.out.println(javadir.exists());
                    File[] jars = javadir.listFiles();
                } catch (IOException e) {
                }
            } else {
                throw new RuntimeException("Compressed bundle files not currently supported.");
            }
            throw new RuntimeException("Method not finished.");
        } else {
            try {
                JarInputStream j = new JarInputStream(url.openStream());
                JarEntry entry1 = j.getNextJarEntry();
                JarFile f;
                throw new RuntimeException("Method not finished.");
            } catch (IOException e) {
                NSLog.err.appendln("IOException loading framework jar from URL " + url + " - message: " + e.getLocalizedMessage());
                StringWriter stacktrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stacktrace));
                NSLog.err.appendln(stacktrace);
                return null;
            }
        }
    }

    /**
	 * This method returns a bundle, either from cache, or if it doesn't exist
	 * yet, it attempts to look it up - first from the classpath, then from the
	 * resource path. TODO: Determine if the lookup order is the desired
	 * semantic.
	 * 
	 * @param name
	 * @return
	 */
    public static synchronized NSBundle bundleForName(String name) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public static synchronized NSArray frameworkBundles() {
        return _allFrameworks.immutableClone();
    }

    /**
	 * Used to set the "Main" application bundle, in which primary resources are
	 * loaded for GUI applications. This is mostly only relevant for
	 * XXApplication objects. This should therefore not be generally used by
	 * consumers of the framework.
	 * 
	 * @param aBundle
	 */
    public static void setMainBundle(NSBundle aBundle) {
        _mainBundle = aBundle;
    }

    public static NSBundle mainBundle() {
        return _mainBundle;
    }

    /**
	 * Get the default prefix for locale. TODO: This really needs to be made
	 * dynamic somehow.
	 * 
	 * @return
	 */
    protected static String defaultLocalePrefix() {
        String language = (String) _languageCodes.objectForKey(Locale.getDefault().getLanguage());
        return language + ".lproj";
    }

    protected static synchronized NSBundle findOrCreateBundleWithPath(String s) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    /**
	 * TODO: figure out what this does.
	 * 
	 * @return
	 */
    public NSArray bundleClassPackageNames() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public String bundlePath() {
        return this.path;
    }

    /**
	 * Returns a byte array for the given resource path. TODO: Lookup semantics
	 * in WebObjects javadocs.
	 * 
	 * @param path
	 * @return
	 */
    public byte[] bytesForResourcePath(String path) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public NSArray bundleClassNames() {
        return classNames.immutableClone();
    }

    public NSDictionary infoDictionary() {
        return info;
    }

    /**
	 * Returns an input stream for a given resource path. TODO: Lookup semantics
	 * in WebObjects javadocs.
	 * 
	 * @param path
	 * @return
	 */
    public InputStream inputStreamForResourcePath(String path) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public boolean isFramework() {
        return isFramework;
    }

    public boolean load() {
        return loaded;
    }

    public String name() {
        return name;
    }

    /**
	 * @deprecated Don't use this method, use
	 *             resourcePathForLocalizedResourceNamed() instead.
	 */
    public String pathForResource(String aName, String anExtension) {
        return this.resourcePathForLocalizedResourceNamed(aName, null);
    }

    /**
	 * @deprecated Don't use this method, use
	 *             resourcePathForLocalizedResourceNamed() instead.
	 */
    public String pathForResource(String aName, String anExtension, String subDir) {
        return this.resourcePathForLocalizedResourceNamed(aName, subDir);
    }

    /**
	 * @deprecated Don't use this method, use resourcePathsForResources()
	 *             instead.
	 */
    public NSArray pathsForResources(String aName, String anExtension) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public Class principalClass() {
        return principalClass;
    }

    public Properties properties() {
        return properties;
    }

    /**
	 * @deprecated Resources are now accessed using the bytesForResourcePath()
	 *             and inputStreamForResourcePath() methods.
	 */
    public String resourcePath() {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public String resourcePathForLocalizedResourceNamed(String aName, String subDir) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public NSArray resourcePathsForDirectories(String extension, String subdirPath) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public NSArray resourcePathsForLocalizedResources(String extension, String subdirPath) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public NSArray resourcePathsForResources(String extension, String subdirPath) {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    public String toString() {
        int i = 0;
        if (classNames != null) i = classNames.count();
        return "<" + getClass().getName() + " name:'" + name + "' bundlePath:'" + path + "' packages:'" + packages + "' " + i + " classes >";
    }

    private static void _initLanguages() {
        _languageCodes.setObjectForKey("de", "German");
        _languageCodes.setObjectForKey("en", "English");
        _languageCodes.setObjectForKey("eo", "Esperanto");
        _languageCodes.setObjectForKey("es", "Spanish");
        _languageCodes.setObjectForKey("fr", "French");
        _languageCodes.setObjectForKey("ja", "Japanese");
    }

    static {
        NSBundle._initLanguages();
    }
}
