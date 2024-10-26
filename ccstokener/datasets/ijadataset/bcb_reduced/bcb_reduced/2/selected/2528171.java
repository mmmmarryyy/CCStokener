package br.net.woodstock.rockframework.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class ClassLoaderUtils {

    public static final String FILE_PREFIX = "file:";

    public static final String FTP_PREFIX = "ftp:";

    public static final String HTTP_PREFIX = "http:";

    public static final String JAR_PREFIX = "jar:";

    public static final String VFSFILE_PREFIX = "vfsfile:";

    public static final String VFSZIP_PREFIX = "vfszip:";

    public static final char JAR_SEPARATOR = '!';

    public static final char PROTOCOL_SEPARATOR = ':';

    private ClassLoaderUtils() {
    }

    public static URL getResource(final String name) {
        return ClassLoaderUtils.getResource(Thread.currentThread().getContextClassLoader(), name);
    }

    public static URL getResource(final ClassLoader classLoader, final String name) {
        return classLoader.getResource(name);
    }

    public static Collection<URL> getResources(final String name) throws IOException {
        return ClassLoaderUtils.getResources(Thread.currentThread().getContextClassLoader(), name);
    }

    public static Collection<URL> getResources(final ClassLoader classLoader, final String name) throws IOException {
        Enumeration<URL> urls = classLoader.getResources(name);
        return CollectionUtils.toCollection(urls);
    }

    public static InputStream getResourceAsStream(final String name) throws URISyntaxException, IOException {
        return ClassLoaderUtils.getResourceAsStream(Thread.currentThread().getContextClassLoader(), name);
    }

    public static InputStream getResourceAsStream(final ClassLoader classLoader, final String name) throws URISyntaxException, IOException {
        URL url = classLoader.getResource(name);
        if (url != null) {
            InputStream inputStream = ClassLoaderUtils.getInputStream(url, name);
            return inputStream;
        }
        return null;
    }

    public static Collection<InputStream> getResourcesAsStream(final String name) throws URISyntaxException, IOException {
        return ClassLoaderUtils.getResourcesAsStream(Thread.currentThread().getContextClassLoader(), name);
    }

    public static Collection<InputStream> getResourcesAsStream(final ClassLoader classLoader, final String name) throws URISyntaxException, IOException {
        Collection<URL> urls = ClassLoaderUtils.getResources(classLoader, name);
        Collection<InputStream> collection = new LinkedList<InputStream>();
        if (urls != null) {
            for (URL url : urls) {
                InputStream inputStream = ClassLoaderUtils.getInputStream(url, name);
                if (inputStream != null) {
                    collection.add(inputStream);
                }
            }
        }
        return collection;
    }

    public static URI getURI(final URL url) throws URISyntaxException {
        String urlString = url.toString();
        URI uri = null;
        if (urlString.startsWith(ClassLoaderUtils.JAR_PREFIX)) {
            int start = urlString.indexOf(ClassLoaderUtils.FILE_PREFIX);
            int end = urlString.indexOf(ClassLoaderUtils.JAR_SEPARATOR);
            String s = urlString.substring(start, end);
            uri = new URI(s);
        } else if (urlString.startsWith(ClassLoaderUtils.VFSZIP_PREFIX)) {
            int start = urlString.indexOf(ClassLoaderUtils.PROTOCOL_SEPARATOR) + 1;
            int end = urlString.indexOf(ClassLoaderUtils.JAR_SEPARATOR);
            String s = ClassLoaderUtils.FILE_PREFIX + urlString.substring(start, end);
            uri = new URI(s);
        } else if (urlString.startsWith(ClassLoaderUtils.VFSFILE_PREFIX)) {
            String s = urlString.replace(ClassLoaderUtils.VFSFILE_PREFIX, ClassLoaderUtils.FILE_PREFIX);
            uri = new URI(s);
        } else {
            uri = new URI(urlString);
        }
        return uri;
    }

    public static InputStream getInputStream(final URL url, final String name) throws URISyntaxException, IOException {
        String urlString = url.toString();
        boolean isJar = urlString.startsWith(ClassLoaderUtils.JAR_PREFIX);
        boolean isVSFFile = urlString.startsWith(ClassLoaderUtils.VFSFILE_PREFIX);
        boolean isVSFZip = urlString.startsWith(ClassLoaderUtils.VFSZIP_PREFIX);
        if ((isVSFFile) || (isVSFZip)) {
            InputStream input = url.openStream();
            return input;
        }
        if (isJar) {
            URI uri = ClassLoaderUtils.getURI(url);
            JarFile file = new JarFile(new File(uri));
            JarEntry entry = file.getJarEntry(name);
            InputStream input = file.getInputStream(entry);
            return input;
        }
        InputStream input = url.openStream();
        return input;
    }
}
