package org.disbelieve.joffree;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import org.disbelieve.joffree.exception.JOFfreeException;

/**
 * A static {@code Map} of {@link JOFfreeConverter} implementations to the {@code
 * Class}' they handle
 */
public class JOFfreeConverters {

    private static Hashtable<Class<?>, JOFfreeConverter> converters;

    static {
        converters = new Hashtable<Class<?>, JOFfreeConverter>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(JOFfreeConverter.class.getPackage().getName().replace('.', '/') + "/converter/");
        loadConverters(url);
    }

    private JOFfreeConverters() {
    }

    /**
     * Returns the {@code JOFfreeConverter} responsible for handling the {@code
     * Class} passed in.
     * <p>
     * If no {@code JOFfreeConverter} can be found for the class that is passed in
     * but one is found for a super-class's or interface of the the {@code
     * Class} passed in an attempt will be made to use that instead.
     * 
     * @param clazz
     *            The {@code Class} for which you want to retrieve the {@code
     *            JOFfreeConverter} for.
     * @return the converter registered for the class passed in
     * @see JOFfreeConverter
     */
    protected static JOFfreeConverter getConverter(Class<?> clazz) {
        JOFfreeConverter converter = converters.get(clazz);
        if (converter == null) {
            for (Class<?> iface : clazz.getInterfaces()) {
                converter = converters.get(iface);
                if (converter == null) {
                    converter = getConverter(iface);
                }
                if (converter != null) {
                    break;
                }
            }
        }
        return converter;
    }

    /**
     * Registers a {@code JOFfreeConverter} to handle marshalling and un-marshalling
     * between JSON Strings and Objects.
     * <p>
     * Main use is to add converters for classes this library does not yet
     * handle, but can also be used to override existing behavior.
     * 
     * @param converter
     *            An implementation of the {@link JOFfreeConverter} interface
     * @see JOFfreeConverter
     */
    protected static void registerConverter(JOFfreeConverter converter) {
        converters.put(converter.handles(), converter);
    }

    /**
     * Finds all classes the implement the {@code JOFfreeConverter} interface,
     * instantiates them, and places them in a static {@code Map}.
     * <p>
     * The {@code java.net.URL} is for a jar file unless {@code JOFfreeConverters}
     * is being read from within JUnit test case.
     * 
     * @param url
     *            The {@code java.net.URL} that references the location of the
     *            {@code JOFfreeConverter}'s.
     */
    private static void loadConverters(URL url) {
        try {
            if (url.toString().contains("jar!")) {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                Enumeration<JarEntry> entries = connection.getJarFile().entries();
                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement().getName();
                    if (entryName.contains("/converter/") && entryName.endsWith(".class")) {
                        entryName = (entryName.split(".class")[0]).replace('/', '.');
                        Class<?> clazz = Class.forName(entryName);
                        JOFfreeConverter converter = (JOFfreeConverter) clazz.newInstance();
                        converters.put(converter.handles(), converter);
                    }
                }
            } else {
                File dir = new File(new URI(url.toString()));
                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        loadConverters(file.toURL());
                    } else {
                        String className = file.getAbsolutePath();
                        if (className.endsWith(".class")) {
                            className = className.substring(className.indexOf("org"), className.lastIndexOf('.'));
                            className = className.replace('/', '.');
                            JOFfreeConverter converter = (JOFfreeConverter) Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
                            converters.put(converter.handles(), converter);
                        }
                    }
                }
            }
        } catch (Exception e) {
            JOFfreeException ex = new JOFfreeException("error loading converters: ", e);
            ex.printStackTrace();
        }
    }
}
