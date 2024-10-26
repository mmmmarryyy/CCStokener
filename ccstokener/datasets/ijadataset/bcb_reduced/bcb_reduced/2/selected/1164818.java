package org.extwind.osgi.repository.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.extwind.osgi.launch.remote.BundleDefinition;
import org.extwind.osgi.repository.RepositoryLoader;

/**
 * @author Donf Yang
 * 
 */
public class DirRepositoryLoader implements RepositoryLoader {

    private static org.apache.juli.logging.Log logger = org.apache.juli.logging.LogFactory.getLog(DirRepositoryLoader.class);

    public static final String OSGI_BUNDLE_MANIFEST = "META-INF/MANIFEST.MF";

    protected String location;

    private boolean loaded;

    public DirRepositoryLoader(String location) {
        this.location = location;
    }

    @Override
    public BundleDefinition[] loadBundles() throws Exception {
        if (location.startsWith(RepositoryLoader.PREFIX_FILE)) {
            location = location.substring(RepositoryLoader.PREFIX_FILE.length());
        }
        File locationDir = new File(location);
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            throw new Exception("Unsupported location - " + location);
        }
        List<BundleDefinition> bundles = new ArrayList<BundleDefinition>();
        resolveLocation(location, bundles);
        loaded = true;
        return bundles.toArray(new BundleDefinition[0]);
    }

    protected void resolveLocation(String location, List<BundleDefinition> bundles) {
        logger.debug("Scan location - " + location);
        File locationDir = new File(location);
        File[] files = locationDir.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                resolveJarFile(file, bundles);
            } else if (file.isDirectory()) {
                resolveDirectory(file, bundles);
            }
        }
    }

    protected void resolveDirectory(File file, List<BundleDefinition> bundles) {
        logger.debug("Resolve directory - " + file.getAbsolutePath());
        InputStream in = null;
        try {
            URL fileUrl = file.toURI().toURL();
            URL url = new URL(fileUrl + "/" + OSGI_BUNDLE_MANIFEST);
            in = url.openStream();
            Headers headers = Headers.parseManifest(in);
            addBundleDefinition(bundles, fileUrl.toString(), headers);
        } catch (Exception e) {
            logger.debug("Unable to resolve [" + file.getAbsolutePath() + "] - " + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
        }
    }

    protected void resolveJarFile(File jarFile, List<BundleDefinition> bundles) {
        logger.debug("Resolve jar file - " + jarFile.getName());
        InputStream in = null;
        try {
            URL fileUrl = jarFile.toURI().toURL();
            JarFile file = new JarFile(jarFile);
            ZipEntry zipEntry = file.getEntry(OSGI_BUNDLE_MANIFEST);
            if (zipEntry != null) {
                in = file.getInputStream(zipEntry);
                Headers headers = Headers.parseManifest(in);
                addBundleDefinition(bundles, fileUrl.toString(), headers);
            }
        } catch (Exception e) {
            logger.info("Unable to resolve [" + jarFile.getName() + "] - " + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addBundleDefinition(List<BundleDefinition> bundles, String location, Headers header) {
        Hashtable table = new Hashtable(header.size());
        Enumeration en = header.keys();
        while (en.hasMoreElements()) {
            Object key = en.nextElement();
            table.put(key, header.get(key));
        }
        BundleDefinition bundle = new BundleDefinition(location, table);
        bundles.add(bundle);
        logger.debug("Add bundle - " + location);
    }

    @Override
    public long getLastModify() throws Exception {
        if (!loaded) {
            throw new Exception("Repository is not load - " + location);
        }
        File locationDir = new File(location);
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            throw new Exception("Unsupported location - " + location);
        }
        return locationDir.lastModified();
    }

    @Override
    public InputStream loadBundle(BundleDefinition bundle) throws Exception {
        return new URL(bundle.getLocation()).openStream();
    }
}
