package org.jdeluxe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.jdeluxe.Constants;
import org.osgi.framework.Bundle;

/**
 * The Class JdxUtils.
 */
public class JdxUtils {

    /**
	 * Stream to string.
	 *
	 * @param in the in
	 *
	 * @return the string
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static String streamToString(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    /**
	 * Url content to string.
	 *
	 * @param url the url
	 * @param encoding the encoding
	 *
	 * @return the string
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static String urlContentToString(URL url, String encoding) throws IOException {
        String out = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), Constants.ENCODING));
        String line;
        while ((line = in.readLine()) != null) {
            out += line;
        }
        in.close();
        return out;
    }

    /**
	 * Stream to string.
	 *
	 * @param in the in
	 * @param encoding the encoding
	 *
	 * @return the string
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static String streamToString(InputStream in, String encoding) throws IOException {
        String out = null;
        BufferedReader i = new BufferedReader(new InputStreamReader(in, encoding));
        String line = i.readLine();
        if (line != null) {
            out = "";
        }
        while (line != null) {
            out += line;
            line = i.readLine();
        }
        return out;
    }

    /**
	 * Gets the resource input stream.
	 *
	 * @param pluginRelativePath the plugin relative path
	 *
	 * @return the resource input stream
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static InputStream getResourceInputStream(String pluginRelativePath) throws IOException {
        URL url = getResourceUrl(pluginRelativePath);
        return url.openStream();
    }

    /**
	 * Gets the resource string.
	 *
	 * @param pluginRelativePath the plugin relative path
	 *
	 * @return the resource string
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static String getResourceString(String pluginRelativePath) throws IOException {
        URL url = getResourceUrl(pluginRelativePath);
        String out = JdxUtils.urlContentToString(url, Constants.ENCODING);
        return out;
    }

    /**
	 * Gets the resource url.
	 *
	 * @param pluginRelativePath the plugin relative path
	 *
	 * @return the resource url
	 */
    public static URL getResourceUrl(String pluginRelativePath) {
        Bundle bundle = Platform.getBundle(Constants.ID_PLUGIN);
        IPath path = new Path(pluginRelativePath);
        URL url = FileLocator.find(bundle, path, null);
        return url;
    }

    /**
	 * Gets the absolute path.
	 *
	 * @param pluginRelativePath the plugin relative path
	 *
	 * @return the absolute path
	 */
    public static String getAbsolutePath(String pluginRelativePath) {
        URL url = getResourceUrl(pluginRelativePath);
        URI uri = null;
        try {
            uri = new URI(url.toString());
        } catch (URISyntaxException e) {
        }
        return new File(uri).getAbsolutePath();
    }

    /**
	 * Gets the file.
	 *
	 * @param pluginRelativePath the plugin relative path
	 *
	 * @return the file
	 */
    public static File getFile(String pluginRelativePath) {
        URL url = getResourceUrl(pluginRelativePath);
        URI uri = null;
        try {
            uri = new URI(url.toString());
        } catch (URISyntaxException e) {
        }
        return new File(uri);
    }

    /**
	 * Stack trace to string.
	 *
	 * @param ex the ex
	 *
	 * @return the string
	 */
    public static String StackTraceToString(Exception ex) {
        String out = "";
        StackTraceElement[] stack = ex.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            out += stack[i].toString() + "\n";
        }
        return out;
    }

    /**
	 * Checks if is xml extension.
	 *
	 * @param fileExtension the file extension
	 *
	 * @return true, if is xml extension
	 */
    public static boolean isXmlExtension(String fileExtension) {
        boolean out = false;
        if (fileExtension.matches(Constants.REGEX_XSD_EXTENSION) || fileExtension.matches(Constants.REGEX_XML_EXTENSION) || fileExtension.matches(Constants.REGEX_JDC_EXTENSION) || fileExtension.matches(Constants.REGEX_JDX_EXTENSION)) {
            out = true;
        }
        return out;
    }
}
