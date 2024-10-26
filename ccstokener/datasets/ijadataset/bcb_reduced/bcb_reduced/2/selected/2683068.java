package net.grinder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class loader related utilities.
 *
 * @author Philip Aston
 */
public class ClassLoaderUtilities {

    /**
   * Find all the resources with the given path, load them, and return their
   * contents as a list of Strings.
   *
   * <p>Property file style comments can be added using "#".</p>
   *
   * <p>Lines are processed as follows:
   * <ul>
   * <li>Comments are removed from each line.</li>
   * <li>Leading and trailing white space is removed from each line.</li>
   * <li>Blank lines are discarded.</li>
   * </ul>
   * </p>
   *
   * @param classLoader
   *          Starting class loader to search. The parent class loaders will be
   *          searched first - see {@link ClassLoader#getResources}.
   * @param resourceName
   *          The name of the resources. Multiple resources may have the same
   *          name if they are loaded from different class loaders.
   * @return The contents of the resources, line by line.
   * @throws IOException
   *           If there was a problem parsing the resources.
   */
    public static List<String> allResourceLines(ClassLoader classLoader, String resourceName) throws IOException {
        final List<String> result = new ArrayList<String>();
        final Enumeration<URL> resources = classLoader.getResources(resourceName);
        final Set<String> seenURLs = new HashSet<String>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            final String urlString = url.toString();
            if (seenURLs.contains(urlString)) {
                continue;
            }
            seenURLs.add(urlString);
            final InputStream in = url.openStream();
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    final int comment = line.indexOf('#');
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        result.add(line);
                    }
                }
                reader.close();
            } finally {
                in.close();
            }
        }
        return result;
    }
}
