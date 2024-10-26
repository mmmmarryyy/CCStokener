package org.freeworld.priplan.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SPIUtil {

    public static <T> List<Class<? extends T>> getImplementations(Class<T> type) {
        if (type == null) throw new IllegalArgumentException("Implementation class cannot be null");
        List<String> classNamesFound = getClassNames(type.getName());
        List<Class<? extends T>> classes = new ArrayList<Class<? extends T>>();
        for (String className : classNamesFound) {
            try {
                Class<?> cls = Class.forName(className);
                if (type.isAssignableFrom(cls)) classes.add((Class<? extends T>) cls);
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return classes;
    }

    public static <T> List<Class<? extends T>> getImplementations(String customBundleName) {
        List<String> classNamesFound = getClassNames(customBundleName);
        List<Class<? extends T>> classes = new ArrayList<Class<? extends T>>();
        for (String className : classNamesFound) {
            try {
                classes.add((Class<? extends T>) Class.forName(className));
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return classes;
    }

    private static List<String> getClassNames(String className) {
        List<String> retr = new ArrayList<String>();
        try {
            ClassLoader classLoader = SPIUtil.class.getClassLoader();
            Enumeration<URL> spiUrls = classLoader.getResources("META-INF/service/" + className);
            while (spiUrls.hasMoreElements()) {
                URL url = spiUrls.nextElement();
                InputStream stream = (InputStream) url.openConnection().getContent();
                if (stream == null) continue;
                StringBuffer out = new StringBuffer();
                byte[] b = new byte[4096];
                for (int n; (n = stream.read(b)) != -1; ) {
                    out.append(new String(b, 0, n));
                }
                String[] items = out.toString().split("\n");
                for (int i = 0; i < items.length; i++) retr.add(items[i].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retr;
    }
}
