package org.eclipse.osgi.internal.baseadaptor;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.BundleException;

public class SystemBundleData extends BaseData {

    private static final String OSGI_FRAMEWORK = "osgi.framework";

    public SystemBundleData(BaseAdaptor adaptor) throws BundleException {
        super(0, adaptor);
        File osgiBase = getOsgiBase();
        createBundleFile(osgiBase);
        manifest = createManifest(osgiBase);
        setMetaData();
        setLastModified(System.currentTimeMillis());
    }

    private File getOsgiBase() {
        String frameworkLocation = FrameworkProperties.getProperty(SystemBundleData.OSGI_FRAMEWORK);
        if (frameworkLocation != null) return new File(frameworkLocation.substring(5));
        try {
            URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
            return new File(url.getPath());
        } catch (Throwable e) {
        }
        frameworkLocation = FrameworkProperties.getProperty("user.dir");
        if (frameworkLocation != null) return new File(frameworkLocation);
        return null;
    }

    private Headers createManifest(File osgiBase) throws BundleException {
        InputStream in = null;
        if (osgiBase != null && osgiBase.exists()) try {
            BundleEntry entry = getBundleFile().getEntry(Constants.OSGI_BUNDLE_MANIFEST);
            if (entry != null) in = entry.getInputStream();
        } catch (IOException e) {
        }
        if (in == null) in = getManifestAsResource();
        if (Debug.DEBUG && Debug.DEBUG_GENERAL) if (in == null) Debug.println("Unable to find system bundle manifest " + Constants.OSGI_BUNDLE_MANIFEST);
        if (in == null) throw new BundleException(AdaptorMsg.SYSTEMBUNDLE_MISSING_MANIFEST);
        return Headers.parseManifest(in);
    }

    private InputStream getManifestAsResource() {
        ClassLoader cl = getClass().getClassLoader();
        try {
            Enumeration manifests = cl != null ? cl.getResources(Constants.OSGI_BUNDLE_MANIFEST) : ClassLoader.getSystemResources(Constants.OSGI_BUNDLE_MANIFEST);
            while (manifests.hasMoreElements()) {
                URL url = (URL) manifests.nextElement();
                try {
                    Headers headers = Headers.parseManifest(url.openStream());
                    if ("true".equals(headers.get(Constants.ECLIPSE_SYSTEMBUNDLE))) return url.openStream();
                } catch (BundleException e) {
                }
            }
        } catch (IOException e) {
        }
        return null;
    }

    private void createBundleFile(File osgiBase) {
        if (osgiBase != null) try {
            bundleFile = getAdaptor().createBundleFile(osgiBase, this);
        } catch (IOException e) {
        } else bundleFile = new BundleFile(osgiBase) {

            public File getFile(String path, boolean nativeCode) {
                return null;
            }

            public BundleEntry getEntry(String path) {
                return null;
            }

            public Enumeration getEntryPaths(String path) {
                return null;
            }

            public void close() {
            }

            public void open() {
            }

            public boolean containsDir(String dir) {
                return false;
            }
        };
    }

    private void setMetaData() throws BundleException {
        setLocation(Constants.SYSTEM_BUNDLE_LOCATION);
        BaseStorageHook.loadManifest(this, manifest);
    }

    public BundleClassLoader createClassLoader(ClassLoaderDelegate delegate, BundleProtectionDomain domain, String[] bundleclasspath) {
        return null;
    }

    public File createGenerationDir() {
        return null;
    }

    public String findLibrary(String libname) {
        return null;
    }

    public void installNativeCode(String[] nativepaths) throws BundleException {
    }

    public File getDataFile(String path) {
        return null;
    }

    public int getStartLevel() {
        return 0;
    }

    public int getStatus() {
        return 0;
    }

    public void save() {
    }
}
