package telnetd.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Utility class implementing two simple
 * yet powerful methods for loading properties
 * (i.e. settings/configurations).
 *
 * @author Dieter Wimberger
 * @version 2.0 (16/07/2006)
 */
public final class PropertiesLoader {

    /**
   * Prevent construction of instances.
   */
    private PropertiesLoader() {
    }

    /**
   * Loads a properties file from an URL given as
   * String.
   *
   * @param url the string representing the URL.
   * @return the properties instance loaded from the given URL.
   * @throws MalformedURLException if the URL is invalid.
   * @throws IOException           if the properties cannot be loaded from
   *                               the given URL.
   */
    public static Properties loadProperties(String url) throws MalformedURLException, IOException {
        return loadProperties(new URL(url));
    }

    /**
   * Loads a properties file from a given URL.
   *
   * @param url an URL instance.
   * @return the properties instance loaded from the given URL.
   * @throws IOException if the properties cannot be loaded from
   *                     the given URL.
   */
    public static Properties loadProperties(URL url) throws IOException {
        Properties newprops = new Properties();
        InputStream in = url.openStream();
        newprops.load(in);
        in.close();
        return newprops;
    }
}
