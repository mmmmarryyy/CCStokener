package com.mycila.ujd.impl;

import com.mycila.ujd.api.ContainedClass;
import com.mycila.ujd.api.Container;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class ContainerImpl implements Container {

    private final URL url;

    private final Set<ContainedClass> containedClasses = new HashSet<ContainedClass>();

    private final ContainerType containerType;

    ContainerImpl(URL url) {
        this.url = url;
        this.containerType = ContainerType.from(url);
    }

    public URL getURL() {
        return url;
    }

    public synchronized Iterable<? extends ContainedClass> getClasses() {
        if (containedClasses.isEmpty()) {
            switch(containerType) {
                case DIR:
                    {
                        final File base;
                        try {
                            base = new File(url.toURI()).getCanonicalFile();
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        final int len = base.getAbsolutePath().length();
                        final Queue<File> folders = new LinkedList<File>();
                        folders.add(base);
                        while (!folders.isEmpty()) {
                            final File[] files = folders.poll().listFiles();
                            for (File file : files) {
                                if (file.isDirectory()) folders.add(file); else if (file.getName().endsWith(".class")) {
                                    try {
                                        containedClasses.add(new ContainedClassImpl(this, file.getCanonicalPath().substring(len + 1)));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                                }
                            }
                        }
                        break;
                    }
                case JAR_LOCAL:
                    {
                        JarFile jarFile = null;
                        try {
                            jarFile = new JarFile(new File(url.toURI()));
                            add(jarFile);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        } finally {
                            if (jarFile != null) try {
                                jarFile.close();
                            } catch (IOException ignored) {
                            }
                        }
                        break;
                    }
                case JAR_REMOTE:
                    {
                        try {
                            JarFile jarFile = ((JarURLConnection) new URL("jar", "", url + "!/").openConnection()).getJarFile();
                            add(jarFile);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        break;
                    }
            }
        }
        return containedClasses;
    }

    private void add(JarFile jarFile) throws IOException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement().getName();
            if (entry.endsWith(".class")) containedClasses.add(new ContainedClassImpl(this, entry));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerImpl container = (ContainerImpl) o;
        return url.equals(container.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    URL getURL(ContainedClassImpl containedClass) {
        String containerURL = url.toExternalForm();
        switch(containerType) {
            case DIR:
                try {
                    return new URL(containerURL + (containerURL.endsWith("/") ? "" : "/") + containedClass.getPath());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            case JAR_LOCAL:
            case JAR_REMOTE:
                try {
                    return new URL("jar:" + containerURL + "!/" + containedClass.getPath());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
        }
        throw new AssertionError("Cannot get URL for " + containedClass + " from " + this);
    }
}
