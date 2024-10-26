package com.thesett.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Properties;

/**
 * PropertiesHelper defines some static methods which are useful when working with properties files.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th>Responsibilities<th>Collaborations
 * <tr><td>Read properties from an input stream
 * <tr><td>Read properties from a file
 * <tr><td>Read properties from a URL
 * <tr><td>Read properties given a path to a file
 * <tr><td>Trim any whitespace from property values
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class PropertiesHelper implements Serializable {

    /**
     * Get properties from an input stream.
     *
     * @param  is The input stream.
     *
     * @return The properties loaded from the input stream.
     *
     * @throws IOException If the is an I/O error reading from the stream.
     */
    public static Properties getProperties(InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        return properties;
    }

    /**
     * Get properties from a file.
     *
     * @param  file The file.
     *
     * @return The properties loaded from the file.
     *
     * @throws IOException If there is an I/O error reading from the file.
     */
    public static Properties getProperties(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        Properties properties = getProperties(is);
        is.close();
        return properties;
    }

    /**
     * Get properties from a url.
     *
     * @param  url The URL.
     *
     * @return The properties loaded from the url.
     *
     * @throws IOException If there is an I/O error reading from the URL.
     */
    public static Properties getProperties(URL url) throws IOException {
        InputStream is = url.openStream();
        Properties properties = getProperties(is);
        is.close();
        return properties;
    }

    /**
     * Get properties from a path name. The path name may refer to either a file or a URL.
     *
     * @param  pathname The path name.
     *
     * @return The properties loaded from the file or URL.
     *
     * @throws IOException If there is an I/O error reading from the URL or file named by the path.
     */
    public static Properties getProperties(String pathname) throws IOException {
        if (pathname == null) {
            return null;
        }
        if (isURL(pathname)) {
            return getProperties(new URL(pathname));
        } else {
            return getProperties(new File(pathname));
        }
    }

    /**
     * Trims whitespace from property values. This method returns a new set of properties the same as the properties
     * specified as an argument but with any white space removed by the {@link java.lang.String#trim} method.
     *
     * @param  properties The properties to trim whitespace from.
     *
     * @return The white space trimmed properties.
     */
    public static Properties trim(Properties properties) {
        Properties trimmedProperties = new Properties();
        for (Object o : properties.keySet()) {
            String next = (String) o;
            String nextValue = properties.getProperty(next);
            if (nextValue != null) {
                nextValue.trim();
            }
            trimmedProperties.setProperty(next, nextValue);
        }
        return trimmedProperties;
    }

    /**
     * Helper method. Guesses whether a string is a URL or not. A String is considered to be a url if it begins with
     * http:, ftp:, or uucp:.
     *
     * @param  name The string to test for being a URL.
     *
     * @return True if the string is a URL and false if not.
     */
    private static boolean isURL(String name) {
        return (name.toLowerCase().startsWith("http:") || name.toLowerCase().startsWith("ftp:") || name.toLowerCase().startsWith("uucp:"));
    }
}
