package org.openje.http.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * A servlet class loader for .
 *
 * @author Yuji Yamaguchi
 * @version $Revision: 1.1.1.1 $
 */
public class HServletClassLoader extends ClassLoader {

    private static final boolean debug = Jasper.debug;

    private final URL baseURL;

    private Vector trustedHosts = new Vector();

    /**
     * Creates a new HServletClassLoader's instance.
     *
     * @param trustedHosts hosts which are trusted so that classes can 
     *                     be loaded from these hosts.
     * @param baseURL a base URL of the class load path
     */
    public HServletClassLoader(Vector trustedHosts, URL baseURL) {
        this.baseURL = baseURL;
        this.trustedHosts = trustedHosts;
    }

    /**
     * Loads a classes specified as a name
     *
     * @param name class name
     * @param resolve ? (pending)
     */
    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c;
        try {
            return findSystemClass(name);
        } catch (ClassNotFoundException e) {
        }
        c = findLoadedClass(name);
        if (c == null) {
            byte data[] = loadClassData(name);
            c = defineClass(null, data, 0, data.length);
        }
        if (resolve) resolveClass(c);
        return c;
    }

    static Object getSecurityContext(ClassLoader loader) {
        URL url = null;
        if (loader != null && loader instanceof HServletClassLoader) {
            HServletClassLoader hloader = (HServletClassLoader) loader;
            url = hloader.baseURL;
        }
        return url;
    }

    private byte[] loadClassData(String name) throws ClassNotFoundException {
        if (debug) {
            System.err.println("loadClassData: " + baseURL);
        }
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - 6);
        }
        name = name.replace('.', '/');
        name += ".class";
        InputStream is = null;
        try {
            URL url = new URL(baseURL, name);
            is = url.openStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            return os.toByteArray();
        } catch (MalformedURLException e) {
            throw new ClassNotFoundException(e.toString());
        } catch (IOException e) {
            throw new ClassNotFoundException(e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
