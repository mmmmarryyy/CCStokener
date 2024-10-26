package net.stickycode.deploy.bootstrap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class StickyClassLoader extends ClassLoader {

    private final StickyEmbedder embedder;

    private StickyEmbeddedUrlStreamHandler urlFactory;

    public StickyClassLoader(ClassLoader parent, StickyEmbedder stickyEmbedder) {
        super(parent);
        this.embedder = stickyEmbedder;
        urlFactory = new StickyEmbeddedUrlStreamHandler(stickyEmbedder);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        embedder.trace("Looking up class %s", name);
        for (StickyLibrary j : embedder.getLibraries()) {
            if (j.getClasses().contains(name)) {
                embedder.debug("loading %s from %s", j, name);
                return loadClass(j, name);
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        embedder.trace("Looking up resource %s", name);
        for (StickyLibrary j : embedder.getLibraries()) {
            if (j.getResources().contains(name)) {
                URL url = urlFactory.createResourceUrl(name, j);
                embedder.debug("define url %s for %s in %s", url, name, j);
                return url;
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        LinkedList<URL> list = new LinkedList<URL>();
        embedder.trace("Looking up resources %s", name);
        for (StickyLibrary j : embedder.getLibraries()) {
            if (j.getResources().contains(name)) {
                URL url = urlFactory.createResourceUrl(name, j);
                embedder.debug("define url %s for %s in %s", url, name, j);
                list.add(url);
            }
        }
        return Collections.enumeration(list);
    }

    private Class<?> loadClass(StickyLibrary j, String name) {
        URL url = embedder.getClass().getResource("/" + j.getJarPath());
        try {
            JarInputStream jar = new JarInputStream(url.openStream());
            return loadClass(jar, name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> loadClass(JarInputStream i, String name) throws IOException {
        String searchFor = name.replace('.', '/') + ".class";
        JarEntry current = i.getNextJarEntry();
        while (current != null) {
            if (!current.isDirectory()) if (current.getName().equals(searchFor)) return load(name, i, current);
            i.closeEntry();
            current = i.getNextJarEntry();
        }
        return null;
    }

    private Class<?> load(String name, JarInputStream i, JarEntry current) throws IOException {
        byte[] b = copy(i, current);
        return defineClass(name, b, 0, b.length);
    }

    protected byte[] copy(InputStream in, JarEntry current) throws IOException {
        int size = deriveClassSize(current);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        byte[] buf = new byte[2048];
        while (baos.size() < (Integer.MAX_VALUE - 2048)) {
            int len = in.read(buf);
            if (len < 0) return baos.toByteArray();
            baos.write(buf, 0, len);
        }
        throw new TheEntryBeingLoadedWasBiggerThan2GWhichSeemsWrong(current.getName(), baos.size(), current.getCompressedSize(), current.getSize());
    }

    /**
   * Return the size of the class so that the byte array output stream is optimally sized and no copies are needed.
   *
   * If the jar is dodgy and does not have proper sizes for the classes then return 2048 which is a reasonably guess for the average
   * class.
   */
    private int deriveClassSize(JarEntry current) {
        if (current.getSize() >= Integer.MAX_VALUE) throw new TheUncompressedSizeListedInJarIsGreaterThan2GbWhichSeemsWrong(current.getName(), current.getCompressedSize(), current.getSize());
        int size = (int) current.getSize();
        if (size < 0) size = 2048;
        return size;
    }
}
