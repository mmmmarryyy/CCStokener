package net.sf.moviekebab.toolset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.MissingResourceException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for resources.
 *
 * @author Laurent Caillette
 */
public final class ResourceTools {

    private static final Log LOG = LogFactory.getLog(ResourceTools.class);

    private ResourceTools() {
    }

    public static URL getResourceUrl(Class owningClass, String resourceName) {
        final String fullTemplateGroupName = "/" + ClassUtils.getPackageName(owningClass).replace('.', '/') + "/" + resourceName;
        return owningClass.getResource(fullTemplateGroupName);
    }

    public static byte[] readResource(Class owningClass, String resourceName) {
        final URL url = getResourceUrl(owningClass, resourceName);
        if (null == url) {
            throw new MissingResourceException(owningClass.toString() + " key '" + resourceName + "'", owningClass.toString(), resourceName);
        }
        LOG.info("Loading resource '" + url.toExternalForm() + "' " + "from " + owningClass);
        final InputStream inputStream;
        try {
            inputStream = url.openStream();
        } catch (IOException e) {
            throw new RuntimeException("Should not happpen", e);
        }
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Should not happpen", e);
        }
        return outputStream.toByteArray();
    }

    /**
   * Reads a resource known to fit in a <code>String</code>.
   * @param owningClass the class accessing to the resource.
   * @param resourceName the resource name as defined from owningClass.
   * @param charset the charset, no default should be used but the known
   *     encoding on the development platform the resource was created with.
   * @return a non-null, possibly empty <code>String</code>.
   * @throws RuntimeException in case of IO or encoding problem.
   */
    public static String readStringResource(Class owningClass, String resourceName, Charset charset) {
        final byte[] bytes = readResource(owningClass, resourceName);
        try {
            return new String(bytes, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
   * Copy a resource into given directory, no subdirectory created.
   */
    public static void copyResourceToFile(Class owningClass, String resourceName, File destinationDir) {
        final byte[] resourceBytes = readResource(owningClass, resourceName);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(resourceBytes);
        final File destinationFile = new File(destinationDir, resourceName);
        final FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(destinationFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
