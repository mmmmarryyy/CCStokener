package org.eclipse.osgi.framework.internal.core;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.BundleException;

public class SystemBundleData extends AbstractBundleData {

    public static final String OSGI_FRAMEWORK = "osgi.framework";

    public SystemBundleData(AbstractFrameworkAdaptor adaptor) throws BundleException {
        super(adaptor, 0);
        File osgiBase = getOsgiBase();
        createBundleFile(osgiBase);
        manifest = createManifest(osgiBase);
        setMetaData();
        setLastModified(System.currentTimeMillis());
    }

    private File getOsgiBase() {
        String frameworkLocation = System.getProperty(OSGI_FRAMEWORK);
        if (frameworkLocation != null) return new File(frameworkLocation.substring(5));
        try {
            URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
            return new File(url.getPath());
        } catch (Throwable e) {
        }
        frameworkLocation = System.getProperty("user.dir");
        if (frameworkLocation != null) return new File(frameworkLocation);
        return null;
    }

    private Headers createManifest(File osgiBase) throws BundleException {
        InputStream in = null;
        if (osgiBase != null && osgiBase.exists()) {
            try {
                BundleEntry entry = baseBundleFile.getEntry(Constants.OSGI_BUNDLE_MANIFEST);
                if (entry != null) in = entry.getInputStream();
            } catch (IOException e) {
            }
        }
        if (in == null) {
            in = getManifestAsResource();
        }
        if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
            if (in == null) {
                Debug.println("Unable to find system bundle manifest " + Constants.OSGI_BUNDLE_MANIFEST);
            }
        }
        if (in == null) throw new BundleException(AdaptorMsg.SYSTEMBUNDLE_MISSING_MANIFEST);
        Headers systemManifest = Headers.parseManifest(in);
        String exportPackages = adaptor.getExportPackages();
        String exportServices = adaptor.getExportServices();
        String providePackages = adaptor.getProvidePackages();
        if (exportPackages != null) appendManifestValue(systemManifest, Constants.EXPORT_PACKAGE, exportPackages);
        if (exportServices != null) appendManifestValue(systemManifest, Constants.EXPORT_SERVICE, exportServices);
        if (providePackages != null) appendManifestValue(systemManifest, Constants.PROVIDE_PACKAGE, providePackages);
        return systemManifest;
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

    private void appendManifestValue(Headers systemManifest, String header, String append) {
        String newValue = (String) systemManifest.get(header);
        if (newValue == null) {
            newValue = append;
        } else {
            newValue += "," + append;
        }
        systemManifest.set(header, null);
        systemManifest.set(header, newValue);
    }

    private void createBundleFile(File osgiBase) {
        if (osgiBase != null) try {
            baseBundleFile = adaptor.createBundleFile(osgiBase, this);
        } catch (IOException e) {
        } else baseBundleFile = new BundleFile(osgiBase) {

            public File getFile(String path) {
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
        loadFromManifest();
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

    public String[] getBundleSigners() {
        return null;
    }
}
