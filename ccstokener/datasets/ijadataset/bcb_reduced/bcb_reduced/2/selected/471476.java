package com.sun.star.lib.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class can be used as a loader for application classes which use UNO.
 *
 * <p>The Loader class detects a UNO installation on the system and adds the
 * UNO jar files to the search path of a customized class loader, which is used
 * for loading the application classes.</p>
 */
public final class Loader {

    private static ClassLoader m_Loader = null;

    /**
     * do not instantiate
     */
    private Loader() {
    }

    /**
     * The main method instantiates a customized class loader with the
     * UNO jar files added to the search path and loads the application class,
     * which is specified in the Main-Class attribute of the
     * com/sun/star/lib/Loader.class entry of the manifest file or
     * as first parameter in the argument list.
     */
    public static void main(String[] arguments) throws Exception {
        String className = null;
        Class clazz = Loader.class;
        ClassLoader loader = clazz.getClassLoader();
        Vector res = new Vector();
        try {
            Enumeration en = loader.getResources("META-INF/MANIFEST.MF");
            while (en.hasMoreElements()) {
                res.add((URL) en.nextElement());
            }
            for (int i = res.size() - 1; i >= 0; i--) {
                URL jarurl = (URL) res.elementAt(i);
                try {
                    JarURLConnection jarConnection = (JarURLConnection) jarurl.openConnection();
                    Manifest mf = jarConnection.getManifest();
                    Attributes attrs = (Attributes) mf.getAttributes("com/sun/star/lib/loader/Loader.class");
                    if (attrs != null) {
                        className = attrs.getValue("Application-Class");
                        if (className != null) break;
                    }
                } catch (IOException e) {
                    System.err.println("com.sun.star.lib.loader.Loader::" + "main: bad manifest file: " + e);
                }
            }
        } catch (IOException e) {
            System.err.println("com.sun.star.lib.loader.Loader::" + "main: cannot get manifest resources: " + e);
        }
        String[] args = arguments;
        if (className == null) {
            className = System.getProperty("Application-Class");
            if (className == null) {
                if (arguments.length > 0) {
                    className = arguments[0];
                    args = new String[arguments.length - 1];
                    System.arraycopy(arguments, 1, args, 0, args.length);
                } else {
                    throw new IllegalArgumentException("The name of the class to be loaded must be either " + "specified in the Main-Class attribute of the " + "com/sun/star/lib/loader/Loader.class entry " + "of the manifest file or as a command line argument.");
                }
            }
        }
        if (className != null) {
            ClassLoader cl = getCustomLoader();
            Class c = cl.loadClass(className);
            Method m = c.getMethod("main", new Class[] { String[].class });
            m.invoke(null, new Object[] { args });
        }
    }

    /**
     * Gets the customized class loader with the UNO jar files added to the
     * search path.
     *
     * @return the customized class loader       
     */
    public static synchronized ClassLoader getCustomLoader() {
        final String CLASSESDIR = "classes";
        final String JUHJAR = "juh.jar";
        if (m_Loader == null) {
            Vector vec = new Vector();
            String classpath = null;
            try {
                classpath = System.getProperty("java.class.path");
            } catch (SecurityException e) {
                System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: cannot get system property " + "java.class.path: " + e);
            }
            if (classpath != null) {
                addUrls(vec, classpath, File.pathSeparator);
            }
            String path = InstallationFinder.getPath();
            if (path != null) {
                File fClassesDir = new File(path, CLASSESDIR);
                File fJuh = new File(fClassesDir, JUHJAR);
                if (fJuh.exists()) {
                    URL[] clurls = new URL[1];
                    try {
                        clurls[0] = fJuh.toURL();
                        ClassLoader cl = new CustomURLClassLoader(clurls);
                        Class c = cl.loadClass("com.sun.star.comp.helper.UnoInfo");
                        Method m = c.getMethod("getJars", (Class[]) null);
                        URL[] jarurls = (URL[]) m.invoke(null, (Object[]) null);
                        for (int i = 0; i < jarurls.length; i++) {
                            vec.add(jarurls[i]);
                        }
                    } catch (MalformedURLException e) {
                        System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: cannot add UNO jar files: " + e);
                    } catch (ClassNotFoundException e) {
                        System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: cannot add UNO jar files: " + e);
                    } catch (NoSuchMethodException e) {
                        System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: cannot add UNO jar files: " + e);
                    } catch (IllegalAccessException e) {
                        System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: cannot add UNO jar files: " + e);
                    } catch (InvocationTargetException e) {
                        System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: cannot add UNO jar files: " + e);
                    }
                } else {
                    callUnoinfo(path, vec);
                }
            } else {
                System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: no UNO installation found!");
            }
            URL[] urls = new URL[vec.size()];
            vec.toArray(urls);
            m_Loader = new CustomURLClassLoader(urls);
        }
        return m_Loader;
    }

    private static void addUrls(Vector urls, String data, String delimiter) {
        StringTokenizer tokens = new StringTokenizer(data, delimiter);
        while (tokens.hasMoreTokens()) {
            try {
                urls.add(new File(tokens.nextToken()).toURL());
            } catch (MalformedURLException e) {
                System.err.println("com.sun.star.lib.loader.Loader::" + "getCustomLoader: bad pathname: " + e);
            }
        }
    }

    private static void callUnoinfo(String path, Vector urls) {
        Process p;
        try {
            p = Runtime.getRuntime().exec(new String[] { new File(path, "unoinfo").getPath(), "java" });
        } catch (IOException e) {
            System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader: exec" + " unoinfo: " + e);
            return;
        }
        new Drain(p.getErrorStream()).start();
        int code;
        byte[] buf = new byte[1000];
        int n = 0;
        try {
            InputStream s = p.getInputStream();
            code = s.read();
            for (; ; ) {
                if (n == buf.length) {
                    if (n > Integer.MAX_VALUE / 2) {
                        System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader:" + " too much unoinfo output");
                        return;
                    }
                    byte[] buf2 = new byte[2 * n];
                    for (int i = 0; i < n; ++i) {
                        buf2[i] = buf[i];
                    }
                    buf = buf2;
                }
                int k = s.read(buf, n, buf.length - n);
                if (k == -1) {
                    break;
                }
                n += k;
            }
        } catch (IOException e) {
            System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader: reading" + " unoinfo output: " + e);
            return;
        }
        int ev;
        try {
            ev = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader: waiting for" + " unoinfo: " + e);
            return;
        }
        if (ev != 0) {
            System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader: unoinfo" + " exit value " + n);
            return;
        }
        String s;
        if (code == '0') {
            s = new String(buf);
        } else if (code == '1') {
            try {
                s = new String(buf, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader:" + " transforming unoinfo output: " + e);
                return;
            }
        } else {
            System.err.println("com.sun.star.lib.loader.Loader::getCustomLoader: bad unoinfo" + " output");
            return;
        }
        addUrls(urls, s, "\0");
    }

    private static final class Drain extends Thread {

        public Drain(InputStream stream) {
            super("unoinfo stderr drain");
            this.stream = stream;
        }

        public void run() {
            try {
                while (stream.read() != -1) {
                }
            } catch (IOException e) {
            }
        }

        private final InputStream stream;
    }

    /**
     * A customized class loader which is used to load classes and resources
     * from a search path of user-defined URLs.
     */
    private static final class CustomURLClassLoader extends URLClassLoader {

        public CustomURLClassLoader(URL[] urls) {
            super(urls);
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

        protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = super.findClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name, resolve);
                } catch (SecurityException e) {
                    return super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
