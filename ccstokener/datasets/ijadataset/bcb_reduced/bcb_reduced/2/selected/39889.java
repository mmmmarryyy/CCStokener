package org.apache.myfaces.trinidad.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

public final class URLUtils {

    private URLUtils() {
    }

    public static long getLastModified(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            String externalForm = url.toExternalForm();
            File file = new File(externalForm.substring(5));
            return file.lastModified();
        } else {
            return getLastModified(url.openConnection());
        }
    }

    public static long getLastModified(URLConnection connection) throws IOException {
        long modified;
        if (connection instanceof JarURLConnection) {
            URL jarFileUrl = ((JarURLConnection) connection).getJarFileURL();
            URLConnection jarFileConnection = jarFileUrl.openConnection();
            try {
                modified = jarFileConnection.getLastModified();
            } finally {
                try {
                    jarFileConnection.getInputStream().close();
                } catch (Exception exception) {
                }
            }
        } else {
            modified = connection.getLastModified();
        }
        return modified;
    }
}
