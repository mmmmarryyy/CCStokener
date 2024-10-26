package org.radeox.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * After the Service class from Sun and the Apache project.
 * With help from Fr�d�ric Miserey.
 * <p/>
 * credits Fr�d�ric Miserey, Joseph Oettinger
 *
 * @author Matthias L. Jugel
 * @version $id$
 */
public class Service {

    public static final int RESOURCE = 0;

    public static final int CLASS = 1;

    public static final int INSTANCE = 2;

    static HashMap services = new HashMap();

    public static synchronized Iterator providerNames(Class cls) {
        return providers(cls, Service.RESOURCE);
    }

    public static synchronized Iterator providerClasses(Class cls) {
        return providers(cls, false);
    }

    public static synchronized Iterator providers(Class cls) {
        return providers(cls, true);
    }

    public static synchronized Iterator providers(Class cls, boolean instantiate) {
        return providers(cls, instantiate ? Service.INSTANCE : Service.CLASS);
    }

    public static synchronized Iterator providers(Class cls, int providerKind) {
        ClassLoader classLoader = cls.getClassLoader();
        String providerFile = "META-INF/services/" + cls.getName();
        List providers = (List) services.get(providerFile);
        if (providers != null) {
            return providers.iterator();
        }
        providers = new ArrayList();
        services.put(providerFile, providers);
        try {
            Enumeration providerFiles = classLoader.getResources(providerFile);
            if (providerFiles.hasMoreElements()) {
                while (providerFiles.hasMoreElements()) {
                    try {
                        URL url = (URL) providerFiles.nextElement();
                        Reader reader = new InputStreamReader(url.openStream(), "UTF-8");
                        switch(providerKind) {
                            case Service.RESOURCE:
                                loadResources(reader, classLoader, providers);
                                break;
                            case Service.CLASS:
                                loadClasses(reader, classLoader, providers);
                                break;
                            case Service.INSTANCE:
                                loadInstances(reader, classLoader, providers);
                                break;
                        }
                    } catch (Exception ex) {
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
                    loadInstances(reader, classLoader, providers);
                }
            }
        } catch (IOException ioe) {
        }
        return providers.iterator();
    }

    private static void loadResources(Reader input, ClassLoader classLoader, List providers) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        String line = reader.readLine();
        while (line != null) {
            providers.add(line);
            line = reader.readLine();
        }
    }

    private static void loadClasses(Reader input, ClassLoader classLoader, List classes) throws IOException {
        Iterator classesIt = readLines(input).iterator();
        while (classesIt.hasNext()) {
            String className = (String) classesIt.next();
            try {
                classes.add(classLoader.loadClass(className));
            } catch (ClassNotFoundException e) {
            }
        }
    }

    private static void loadInstances(Reader input, ClassLoader classLoader, List providers) throws IOException {
        Iterator classesIt = readLines(input).iterator();
        while (classesIt.hasNext()) {
            String className = (String) classesIt.next();
            int modifierIndex = className.indexOf('_');
            String modifier = null;
            if (modifierIndex != -1) {
                modifier = className.substring(modifierIndex + 1);
                className = className.substring(0, modifierIndex);
            }
            try {
                Class klass = classLoader.loadClass(className);
                Object obj = klass.newInstance();
                if (null != modifier) {
                    Method setModifierMethod = klass.getMethod("setModifier", new Class[] { String.class });
                    setModifierMethod.invoke(obj, new Object[] { modifier });
                }
                providers.add(obj);
            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e) {
            } catch (ClassNotFoundException e) {
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
   * Read all lines from the reader and filter out comment lines starting
   * with a hash mark (#)
   *
   * @param input the stream reader to contain lines
   * @return a list containing all significant lines
   * @throws IOException if the reader was interrupted
   */
    private static List readLines(Reader input) throws IOException {
        List linesList = new ArrayList();
        BufferedReader reader = new BufferedReader(input);
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                int idx = line.indexOf('#');
                if (idx != -1) {
                    line = line.substring(0, idx);
                }
                line = line.trim();
                if (line.length() > 0) {
                    linesList.add(line);
                }
            } catch (Exception ex) {
            }
        }
        return linesList;
    }
}
