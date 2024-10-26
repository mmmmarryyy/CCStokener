package org.extwind.osgi.console.service.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.extwind.osgi.console.service.RepositoryLoader;
import org.extwind.osgi.console.service.internal.startup.Activator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Donf Yang
 * 
 */
@SuppressWarnings("restriction")
public class DirRepositoryLoader implements RepositoryLoader {

    private final Logger logger = LoggerFactory.getLogger(DirRepositoryLoader.class);

    public static final String OSGI_BUNDLE_MANIFEST = "META-INF/MANIFEST.MF";

    protected String urlLocation;

    protected String fileLocation;

    private StateObjectFactory stateObjectFactory;

    private long bundleId = -1;

    private boolean loaded;

    public DirRepositoryLoader(String location) {
        this.urlLocation = location;
        stateObjectFactory = Activator.getPlatformAdmin().getFactory();
    }

    @Override
    public synchronized BundleDescription[] loadBundles() throws Exception {
        fileLocation = urlLocation.substring(RepositoryLoader.PREFIX_FILE.length());
        File locationDir = new File(fileLocation);
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            throw new Exception("Unsupported location - " + urlLocation);
        }
        List<BundleDescription> bundles = new ArrayList<BundleDescription>();
        resolveLocation(locationDir, bundles);
        loaded = true;
        return bundles.toArray(new BundleDescription[0]);
    }

    protected void resolveLocation(File locationDir, List<BundleDescription> bundles) {
        logger.debug("Scan location - " + locationDir.getAbsolutePath());
        File[] files = locationDir.listFiles();
        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                resolveJarFile(file, bundles);
            } else if (file.isDirectory()) {
                resolveDirectory(file, bundles);
            }
        }
    }

    protected void resolveDirectory(File file, List<BundleDescription> bundles) {
        logger.debug("Resolve directory - " + file.getAbsolutePath());
        InputStream in = null;
        try {
            URL fileUrl = file.toURI().toURL();
            URL url = new URL(fileUrl + "/" + OSGI_BUNDLE_MANIFEST);
            in = url.openStream();
            Headers headers = Headers.parseManifest(in);
            BundleDescription description = stateObjectFactory.createBundleDescription(null, headers, fileUrl.toString(), bundleId--);
            bundles.add(description);
        } catch (Exception e) {
            logger.info("Unable to resolve [" + file.getAbsolutePath() + "] - " + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
        }
    }

    protected void resolveJarFile(File jarFile, List<BundleDescription> bundles) {
        logger.debug("Resolve jar file - " + jarFile.getName());
        InputStream in = null;
        try {
            URL fileUrl = jarFile.toURI().toURL();
            JarFile file = new JarFile(jarFile);
            ZipEntry zipEntry = file.getEntry(OSGI_BUNDLE_MANIFEST);
            if (zipEntry != null) {
                in = file.getInputStream(zipEntry);
                Headers headers = Headers.parseManifest(in);
                BundleDescription description = stateObjectFactory.createBundleDescription(null, headers, fileUrl.toString(), bundleId--);
                bundles.add(description);
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

    @Override
    public long getLastModify() throws Exception {
        if (!loaded) {
            throw new Exception("Repository is not load - " + fileLocation);
        }
        File locationDir = new File(fileLocation);
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            throw new Exception("Unsupported location - " + fileLocation);
        }
        return locationDir.lastModified();
    }

    @Override
    public InputStream loadBundle(String location) throws Exception {
        return new URL(location).openStream();
    }
}
