package org.OpenJIT;

import java.io.*;
import java.util.*;
import java.net.*;
import sun.net.www.protocol.http.HttpURLConnection;

public class OpenJITLoader extends ClassLoader {

    private static Properties props;

    private static boolean verbose;

    static {
        props = new Properties();
        try {
            File file = new File("OpenJIT.properties");
            FileInputStream in = new FileInputStream(file);
            props.load(in);
            in.close();
        } catch (Exception e) {
        }
    }

    public static String getProperty(String key) {
        String str = props.getProperty(key);
        if (str == null) str = System.getProperty(key);
        return str;
    }

    public static boolean getPropertyBoolean(String key, boolean default_value) {
        String str = getProperty(key);
        if (str == null) return default_value;
        if (str.compareTo("true") == 0) return true;
        if (str.compareTo("false") == 0) return false;
        return default_value;
    }

    URL base;

    public OpenJITLoader(URL base) {
        this.base = base;
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class cl = findLoadedClass(name);
        if (cl == null) {
            if (name.regionMatches(0, "org.OpenJIT.", 0, 12)) {
                try {
                    String cname = name.replace('.', '/') + ".class";
                    URL url = new URL(base, cname);
                    cl = loadClass(name, url);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    return findSystemClass(name);
                } catch (ClassNotFoundException e) {
                }
            }
        }
        if (cl == null) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(cl);
        }
        return cl;
    }

    private Class loadClass(String name, URL url) throws IOException {
        InputStream in = null;
        try {
            URLConnection c = url.openConnection();
            c.setAllowUserInteraction(false);
            in = HttpURLConnection.openConnectionCheckRedirects(c);
            int len = c.getContentLength();
            byte data[] = new byte[(len == -1) ? 4096 : len];
            int total = 0, n;
            while ((n = in.read(data, total, data.length - total)) >= 0) {
                if ((total += n) == data.length) {
                    if (len < 0) {
                        byte newdata[] = new byte[total * 2];
                        System.arraycopy(data, 0, newdata, 0, total);
                        data = newdata;
                    } else {
                        break;
                    }
                }
            }
            if (verbose) System.err.println("load:" + name + "\t" + total + "bytes");
            in.close();
            in = null;
            return defineClass(name, data, 0, total);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (in != null) {
                in.close();
                System.gc();
                System.runFinalization();
            }
        }
    }

    public static ClassLoader bootLoader() {
        String url = getProperty("compile.URL");
        if (url == null) return null;
        if (getProperty("compile.verbose") != null) verbose = true;
        try {
            URL base = new URL(url);
            ClassLoader loader = new OpenJITLoader(base);
            return loader;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
