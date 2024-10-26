package radeox.util;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * After the Service class from Sun and the Apache project. With help from
 * Fr�d�ric Miserey.
 * 
 * @credits Fr�d�ric Miserey, Joseph Oettinger
 * @author Matthias L. Jugel
 * @version $id$
 */
public class Service {

    static HashMap<String, List> services = new HashMap<String, List>();

    public static synchronized Iterator providerClasses(Class cls) {
        return providers(cls, false);
    }

    public static synchronized Iterator providers(Class cls) {
        return providers(cls, true);
    }

    public static synchronized Iterator providers(Class cls, boolean instantiate) {
        ClassLoader classLoader = cls.getClassLoader();
        String providerFile = "radeox/config/" + cls.getName();
        List providers = services.get(providerFile);
        if (providers != null) {
            return providers.iterator();
        }
        providers = new ArrayList();
        services.put(providerFile, providers);
        try {
            Enumeration providerFiles = classLoader.getResources(providerFile);
            if (providerFiles.hasMoreElements()) {
                while (providerFiles.hasMoreElements()) {
                    URL url = (URL) providerFiles.nextElement();
                    Reader reader = new InputStreamReader(url.openStream(), "UTF-8");
                    if (instantiate) {
                        loadResource(reader, classLoader, providers);
                    } else {
                        loadClasses(reader, classLoader, providers);
                    }
                }
            } else {
                InputStream is = classLoader.getResourceAsStream(providerFile);
                if (is == null) {
                    providerFile = providerFile.substring(providerFile.lastIndexOf('.') + 1);
                    is = classLoader.getResourceAsStream(providerFile);
                }
                if (is != null) {
                    Reader reader = new InputStreamReader(is, "UTF-8");
                    loadResource(reader, classLoader, providers);
                }
            }
        } catch (IOException ioe) {
            System.out.println("Error loading service providers file: " + ioe.getMessage());
        }
        return providers.iterator();
    }

    private static List loadClasses(Reader input, ClassLoader classLoader, List classes) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        String line = reader.readLine();
        while (line != null) {
            int idx = line.indexOf('#');
            if (idx != -1) {
                line = line.substring(0, idx);
            }
            line = line.trim();
            if (line.length() > 0) {
                try {
                    classes.add(classLoader.loadClass(line));
                } catch (ClassNotFoundException e) {
                    System.out.println("Cannot find the class: " + line);
                }
            }
            line = reader.readLine();
        }
        return classes;
    }

    private static void loadResource(Reader input, ClassLoader classLoader, List providers) throws IOException {
        List classes = new ArrayList();
        loadClasses(input, classLoader, classes);
        Iterator iterator = classes.iterator();
        while (iterator.hasNext()) {
            Class klass = (Class) iterator.next();
            try {
                Object obj = klass.newInstance();
                providers.add(obj);
            } catch (InstantiationException e) {
                System.out.println("Error initializing " + klass.getName() + ": " + e.getMessage());
            } catch (IllegalAccessException e) {
                System.out.println("Error initializing " + klass.getName() + ": " + e.getMessage());
            }
        }
    }
}
