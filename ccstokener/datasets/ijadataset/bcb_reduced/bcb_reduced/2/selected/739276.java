package com.mycila.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class CustomClassLoader extends URLClassLoader {

    private final boolean childFirst;

    private final boolean childFirstResources;

    private CustomClassLoader(ClassLoader parent, boolean childFirst, boolean childFirstResources) {
        super(new URL[0], parent == null ? ClassLoader.getSystemClassLoader().getParent() : parent);
        this.childFirst = childFirst && parent != null;
        this.childFirstResources = childFirstResources && parent != null;
    }

    public CustomClassLoader add(File... files) {
        return add(Arrays.asList(files));
    }

    public CustomClassLoader add(Iterable<File> files) {
        for (URL url : toURLs(files)) addURL(url);
        return this;
    }

    public <T> Class<T> load(String className) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this);
        try {
            return (Class<T>) loadClass(className);
        } catch (ClassNotFoundException e) {
            RuntimeException re = new RuntimeException(e.getClass().getName() + ": " + e.getMessage(), e);
            re.setStackTrace(e.getStackTrace());
            throw re;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve) resolveClass(clazz);
            return (clazz);
        }
        if (!childFirst) try {
            return checkParent(name, resolve);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            return checkMe(name, resolve);
        } catch (ClassNotFoundException ignored) {
        }
        if (childFirst) try {
            return checkParent(name, resolve);
        } catch (ClassNotFoundException ignored) {
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        URL url;
        if (!childFirstResources) {
            url = getParent().getResource(name);
            if (url != null) return url;
        }
        url = findResource(name);
        if (url != null) return url;
        if (childFirstResources) url = getParent().getResource(name);
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final List<URL> urls = new LinkedList<URL>();
        if (!childFirstResources) {
            Enumeration<URL> enums = getParent().getResources(name);
            while (enums.hasMoreElements()) urls.add(enums.nextElement());
        }
        {
            Enumeration<URL> enums = findResources(name);
            while (enums.hasMoreElements()) urls.add(enums.nextElement());
        }
        if (childFirstResources) {
            Enumeration<URL> enums = getParent().getResources(name);
            while (enums.hasMoreElements()) urls.add(enums.nextElement());
        }
        return new Enumeration<URL>() {

            final Iterator<URL> it = urls.iterator();

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public URL nextElement() {
                return it.next();
            }
        };
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream in = null;
        if (!childFirstResources) {
            in = getParent().getResourceAsStream(name);
            if (in != null) return in;
        }
        URL url = findResource(name);
        if (url != null) try {
            return url.openStream();
        } catch (IOException ignored) {
        }
        if (childFirstResources) in = getParent().getResourceAsStream(name);
        return in;
    }

    private Class<?> checkMe(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findClass(name);
        if (clazz != null) if (resolve) resolveClass(clazz);
        return (clazz);
    }

    private Class<?> checkParent(String name, boolean resolve) throws ClassNotFoundException {
        ClassLoader loader = getParent();
        Class<?> clazz = loader.loadClass(name);
        if (clazz != null) if (resolve) resolveClass(clazz);
        return clazz;
    }

    private static URL[] toURLs(Iterable<File> libs) {
        List<URL> urls = new ArrayList<URL>();
        for (File lib : libs) try {
            urls.add(lib.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return urls.toArray(new URL[urls.size()]);
    }

    public static CustomClassLoader create(ClassLoader parent, boolean childFirst, boolean childFirstResources) {
        return new CustomClassLoader(parent, childFirst, childFirstResources);
    }

    public static CustomClassLoader isolated() {
        return create(null, false, true);
    }
}
