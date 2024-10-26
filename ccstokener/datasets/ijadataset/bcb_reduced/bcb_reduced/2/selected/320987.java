package org.xaware.ide.xadev.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * This class utility provides helper methods for dealing with OSGI Bundles.
 *
 * @author Tim Uttormark
 */
public class BundleUtils {

    /**
     * Constuctor. Made private to enforce class utility pattern.
     */
    public BundleUtils() {
    }

    /**
     * Extracts a Bundle entry to a specified destination.
     *
     * @param bundleSymbolicName
     *            The symbolic name of the Bundle containing the entry to be
     *            extracted, e.g., "org.xaware.shared"
     * @param pathWithinBundle
     *            The path within the Bundle where the desired entry is located.
     * @param destination
     *            The destination File to which the Entry should be extracted.
     * @throws IOException
     *             if any error occurs extracting the Bundle entry.
     */
    public static void extractBundleEntry(final String bundleSymbolicName, final String pathWithinBundle, final File destination) throws IOException {
        InputStream is = null;
        OutputStream fileOutStream = null;
        try {
            final Bundle bundle = Platform.getBundle(bundleSymbolicName);
            if (bundle == null) {
                throw new IOException("Bundle " + bundleSymbolicName + " not found.");
            }
            final URL url = bundle.getEntry(pathWithinBundle);
            if (url == null) {
                throw new IOException("Bundle entry " + pathWithinBundle + " not found.");
            }
            is = url.openStream();
            final File destParentDir = destination.getParentFile();
            if (destParentDir != null) {
                destParentDir.mkdirs();
            }
            fileOutStream = new BufferedOutputStream(new FileOutputStream(destination));
            final byte[] copyBuffer = new byte[10240];
            int bytesRead;
            while ((bytesRead = is.read(copyBuffer)) != -1) {
                fileOutStream.write(copyBuffer, 0, bytesRead);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                }
            }
            if (fileOutStream != null) {
                try {
                    fileOutStream.close();
                } catch (final IOException e) {
                }
            }
        }
    }
}
