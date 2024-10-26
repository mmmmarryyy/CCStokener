package de.carne.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import de.carne.io.Closeables;
import de.carne.util.logging.Log;

/**
 * Utility class used to access services defined via a Jar's <code>META-INF/services/*</code> files.
 */
public final class Services {

    private static final Log LOG = new Log(Services.class);

    /**
	 * Get all class names providing a specific service.
	 * 
	 * @param clazz The service class to retrieve the class names for.
	 * @param loader The <code>ClassLoader</code> to use to search the <code>META-INF/services/*</code> files.
	 * @return The found class names.
	 * @throws IOException If an I/O error occurs.
	 */
    public static String[] getProviderNames(Class<? extends Object> clazz, ClassLoader loader) throws IOException {
        assert clazz != null;
        assert loader != null;
        final LinkedList<String> names = new LinkedList<String>();
        final Enumeration<URL> urls = loader.getResources("META-INF/services/" + clazz.getName());
        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();
            InputStream urlIS = null;
            try {
                urlIS = url.openStream();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(urlIS, "UTF-8"));
                String line = reader.readLine();
                while (line != null) {
                    final StringTokenizer lineTokens = new StringTokenizer(line);
                    boolean lineDone = false;
                    while (lineTokens.hasMoreTokens() && !lineDone) {
                        final String lineToken = lineTokens.nextToken();
                        if (lineToken.charAt(0) != '#') {
                            names.add(lineToken);
                        } else {
                            lineDone = true;
                        }
                    }
                    line = reader.readLine();
                }
            } finally {
                urlIS = Closeables.saveClose(urlIS);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /**
	 * Get all class names providing a specific service.
	 * 
	 * @param clazz The service class to retrieve the class names for.
	 * @return The found class names.
	 * @throws IOException If an I/O error occurs.
	 */
    public static String[] getProviderNames(Class<? extends Object> clazz) throws IOException {
        return getProviderNames(clazz, Thread.currentThread().getContextClassLoader());
    }

    /**
	 * Instantiates all providers for a specific service.
	 * 
	 * @param <T> The service class.
	 * @param clazz The service class to instantiate the providers for.
	 * @param loader The <code>ClassLoader</code> to use to load the service classes.
	 * @return The instantiated services.
	 * @throws IOException If an I/O error occurs.
	 * @throws ClassNotFoundException If one of the service classes cannot be found.
	 * @throws IllegalAccessException If one of the service classes cannot be accessed.
	 * @throws InstantiationException If one of the service classes cannot be instantiated.
	 */
    public static <T> List<T> getProviders(Class<T> clazz, ClassLoader loader) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        final String[] names = getProviderNames(clazz, loader);
        final List<T> providers = new ArrayList<T>();
        for (final String name : names) {
            providers.add(loader.loadClass(name).asSubclass(clazz).newInstance());
            LOG.info("Service ''{0}'' loaded", name);
        }
        return providers;
    }

    /**
	 * Instantiates all providers for a specific service.
	 * 
	 * @param <T> The service class.
	 * @param clazz The service class to instantiate the providers for.
	 * @return The instantiated services.
	 * @throws IOException If an I/O error occurs.
	 * @throws ClassNotFoundException If one of the service classes cannot be found.
	 * @throws IllegalAccessException If one of the service classes cannot be accessed.
	 * @throws InstantiationException If one of the service classes cannot be instantiated.
	 */
    public static <T> List<T> getProviders(Class<T> clazz) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        return getProviders(clazz, Thread.currentThread().getContextClassLoader());
    }
}
