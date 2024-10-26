package org.slasoi.seval.repository.helper;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.osgi.framework.internal.core.BundleURLConnection;

/**
 * This helper provides functionality to access resources of this package
 * independent of the type of deployment of the repository bundle.
 * Resources can be made available as File object whether the code is
 * in the development environment, a JARed Eclipse plug-in or a general OSGi bundle.
 * 
 * It takes use of a temporary directory which is cleaned up when the class is finalized.
 * 
 * @author Benjamin Klatt
 * @author brosch
 */
public class ResourceHelper {

    /** Log4j functionality. */
    private static final Logger logger = Logger.getLogger(ResourceHelper.class.getName());

    /** The temporary working directory to unpack resources. */
    private File tempDir = null;

    /** A singleton pattern ensures the existence of only one ResourceHelper. */
    private static ResourceHelper singletonInstance = new ResourceHelper();

    /** Reference to an IOHelper for stream copy functionality. */
    private IOHelper helper = IOHelper.getHelper();

    /** Directory with the common pcm model files such as the resource type repository. */
    private File commonPCMModelDir = null;

    /**
     * Retrieves the singleton instance.
     * 
     * @return the singleton instance
     */
    public static ResourceHelper getHelper() {
        return singletonInstance;
    }

    /**
     * Constructor initializing the temporary directory.
     * 
     * The constructor is made private according to the singleton pattern.
     */
    private ResourceHelper() {
        tempDir = new File(new File(System.getProperty("java.io.tmpdir")), "SLASOI_" + this.hashCode());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.cleanupTempDir();
    }

    /**
     * Unpacks the resources that are contained in the repository library into the user's temporary directory.
     * 
     * Note that this only works if called for the exported jar archive with resource included.
     * 
     * @throws IOException
     */
    public void unpackResources() throws IOException {
        this.commonPCMModelDir = unpackResource("CommonPCMModels");
    }

    /**
     * Unpacks the resources that are contained in the repository library into the user's temporary directory.
     * 
     * Note that this only works if called for the exported jar archive with resource included.
     * 
     * @throws IOException
     */
    public File unpackResource(String resourceName) throws IOException {
        File file = null;
        URL url = getURL("/" + resourceName);
        URLConnection urlConn = url.openConnection();
        if (urlConn instanceof JarURLConnection) {
            file = handleJarFile((JarURLConnection) urlConn, resourceName);
        } else if (urlConn instanceof BundleURLConnection) {
            file = handleBundleFile((BundleURLConnection) urlConn);
        } else {
            file = handleClassicFile(resourceName);
        }
        return file;
    }

    /**
     * Handle a connection to a regular file.
     */
    private File handleClassicFile(String resourceName) {
        String orcDataDirPath = this.getClass().getClassLoader().getResource(resourceName).getFile();
        return new File(orcDataDirPath);
    }

    /**
     * Extract the files from a bundle.
     * @param bundleConnection The connection to the bundle to extract from.
     * @return File the url to the unpacked file.
     * @throws IOException thrown if the files could not be read successfully.
     */
    private File handleBundleFile(BundleURLConnection bundleConnection) throws IOException {
        return new File(bundleConnection.getFileURL().getFile());
    }

    /**
     * Extract the files if they are located in a jar file.
     * @param jarConnection The connection to the jar file.
     * @throws IOException thrown if the files could not be read successfully.
     */
    private File handleJarFile(JarURLConnection jarConnection, String resourceName) throws IOException {
        File file = null;
        JarFile jarFile = jarConnection.getJarFile();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            File resourceFile = unpackResource(tempDir, entry.getName(), entry.isDirectory(), resourceName);
            if (resourceFile.getName().equals(resourceName)) {
                file = resourceFile;
            }
        }
        return file;
    }

    /**
     * Clean up the temporary directory used to unpack the resources.
     */
    private void cleanupTempDir() {
        helper.deleteDir(tempDir);
    }

    /**
     * Unpacks a single resource (directory or file) into the file system.
     * 
     * @param tempDir
     *            the base location for unpacking resources
     * @param resourceName
     *            the name of the resource
     * @param isDirectory
     *            indicates if the resource is a directory
     * @return the target location of the unpacked resource
     * @throws IOException
     */
    private File unpackResource(File tempDir, String resourceName, boolean isDirectory, String requiredResourceName) throws IOException {
        File targetPath = new File(tempDir, resourceName);
        if (!resourceName.contains(requiredResourceName + "/")) {
            return targetPath;
        }
        if (isDirectory) {
            if (!targetPath.exists()) {
                targetPath.mkdirs();
            }
            return targetPath;
        }
        if (targetPath.exists()) {
            targetPath.delete();
        }
        targetPath.createNewFile();
        URL url = getURL("/" + resourceName);
        logger.debug("Resource URL = " + url.toString());
        FileUtils.copyURLToFile(url, targetPath);
        logger.debug("Unpacked Resource " + resourceName + " to " + targetPath.getAbsolutePath());
        return targetPath;
    }

    /**
     * Retrieves a URL from a given resource name.
     * 
     * Note: The resource must be located in the class path.
     * 
     * @param resourceName
     *            the resource name
     * @return the URL of the resource
     */
    private URL getURL(String resourceName) {
        URL url = ClassLoader.getSystemResource(resourceName);
        if (url == null) {
            url = getClass().getResource(resourceName);
        }
        return url;
    }

    /**
     * Set the common pcm model directory.
     * @param commonPCMModelDir the commonPCMModelDir to set
     */
    public void setCommonPCMModelDir(File commonPCMModelDir) {
        this.commonPCMModelDir = commonPCMModelDir;
    }

    /**
     * Get the common pcm model directory.
     * @return the commonPCMModelDir
     */
    public File getCommonPCMModelDir() {
        return commonPCMModelDir;
    }
}
