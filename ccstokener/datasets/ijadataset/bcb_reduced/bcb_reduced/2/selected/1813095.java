package org.eclipse.equinox.internal.security.storage;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;

/**
 * PLEASE READ BEFORE CHANGING THIS FILE
 * 
 * At present most of the methods expect only file URLs. The API methods
 * take URLs for possible future expansion, and there is some code below
 * that would work with some other URL types, but the only supported URL
 * types at this time are file URLs. Also note that URL paths should not
 * be encoded (spaces should be spaces, not "%x20"). 
 *  
 * On encoding: Java documentation recommends using File.toURI().toURL().
 * However, in this process non-alphanumeric characters (including spaces)
 * get encoded and can not be used with the rest of Eclipse methods that
 * expect non-encoded strings.
 */
public class StorageUtils {

    /**
	 * Default name of the storage file
	 */
    private static final String propertiesFileName = ".eclipse/org.eclipse.equinox.security/secure_storage";

    /**
	 * Default locations:
	 * 1) user.home
	 * 2) Eclipse config location
	 */
    public static URL getDefaultLocation() throws IOException {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File file = new File(userHome, propertiesFileName);
            return file.toURL();
        }
        URL installLocation = AuthPlugin.getDefault().getConfigURL();
        if (installLocation != null && isFile(installLocation)) {
            File file = new File(installLocation.getPath(), propertiesFileName);
            return file.toURL();
        }
        throw new IOException(SecAuthMessages.loginNoDefaultLocation);
    }

    public static OutputStream getOutputStream(URL url) throws IOException {
        if (isFile(url)) {
            File file = new File(url.getPath());
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
            }
            return new FileOutputStream(file);
        }
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        return connection.getOutputStream();
    }

    public static InputStream getInputStream(URL url) throws IOException {
        if (url == null) return null;
        try {
            return url.openStream();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static boolean delete(URL url) {
        if (isFile(url)) {
            File file = new File(url.getPath());
            return file.delete();
        }
        return false;
    }

    public static boolean exists(URL url) {
        if (isFile(url)) {
            File file = new File(url.getPath());
            return file.exists();
        }
        return true;
    }

    public static boolean isFile(URL url) {
        return ("file".equals(url.getProtocol()));
    }
}
