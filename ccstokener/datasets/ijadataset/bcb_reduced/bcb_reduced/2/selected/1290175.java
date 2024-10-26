package org.eclipse.core.runtime.internal.adaptor;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.osgi.framework.internal.core.BundleURLConnection;
import org.eclipse.osgi.service.urlconversion.URLConverter;

/**
 * The service implementation that allows bundleresource or bundleentry
 * URLs to be converted to native file URLs on the local file system.
 * 
 * <p>Internal class.</p>
 */
public class URLConverterImpl implements URLConverter {

    public URL convertToFileURL(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof BundleURLConnection) {
            return ((BundleURLConnection) connection).getFileURL();
        } else {
            return url;
        }
    }

    public URL convertToLocalURL(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof BundleURLConnection) {
            return ((BundleURLConnection) connection).getLocalURL();
        } else {
            return url;
        }
    }
}
