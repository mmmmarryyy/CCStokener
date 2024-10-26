package net.stickycode.deploy.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StickyLibrary {

    private final String jarPath;

    private final Collection<String> classes = new ArrayList<String>();

    private final Collection<String> resources = new ArrayList<String>();

    private String mainClass;

    private StickyEmbedder embedder;

    public StickyLibrary(StickyEmbedder embedder, ZipFile zipFile, String jarPath) {
        super();
        this.embedder = embedder;
        this.jarPath = jarPath;
        index(zipFile);
    }

    @Override
    public String toString() {
        return jarPath;
    }

    private void index(ZipFile zipFile) {
        ZipEntry entry = zipFile.getEntry(jarPath);
        try {
            processEntry(zipFile, entry);
            embedder.debug("Found %s classes and %s resources in jar %s", classes.size(), resources.size(), jarPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
        JarInputStream i = new JarInputStream(zipFile.getInputStream(entry));
        processManifest(i);
        JarEntry current = i.getNextJarEntry();
        while (current != null) {
            if (!current.isDirectory()) processName(current.getName());
            i.closeEntry();
            current = i.getNextJarEntry();
        }
    }

    private void processManifest(JarInputStream i) {
        Manifest manifest = i.getManifest();
        if (manifest != null) {
            mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass != null) embedder.debug("Found main class %s for jar %s", mainClass, jarPath);
        }
    }

    private void processName(String name) {
        if (name.endsWith(".class")) addClass(name.substring(0, name.length() - 6).replace('/', '.')); else addResource(name);
    }

    private boolean addResource(String name) {
        embedder.trace("found resource %s", name);
        return resources.add(name);
    }

    private void addClass(String name) {
        embedder.trace("found class %s", name);
        classes.add(name);
    }

    public String getJarPath() {
        return jarPath;
    }

    public Collection<String> getClasses() {
        return classes;
    }

    public Collection<String> getResources() {
        return resources;
    }

    public String getMainClass() {
        return mainClass;
    }

    public InputStream getInputStream(String path) {
        URL url = getJarStream();
        try {
            JarInputStream jar = new JarInputStream(url.openStream());
            try {
                return loadStream(jar, path);
            } finally {
                jar.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL getJarStream() {
        embedder.trace("opening jar /%s", jarPath);
        URL url = embedder.getClass().getResource("/" + jarPath);
        if (url == null) throw new RuntimeException("Where did " + jarPath + " go?");
        return url;
    }

    private InputStream loadStream(JarInputStream jar, String path) throws IOException {
        JarEntry current = jar.getNextJarEntry();
        while (current != null) {
            if (!current.isDirectory()) if (current.getName().equals(path)) return new ByteArrayInputStream(load(path, jar, current));
            jar.closeEntry();
            current = jar.getNextJarEntry();
        }
        return null;
    }

    private byte[] load(String name, JarInputStream in, JarEntry current) throws IOException {
        int size = deriveEntrySize(current);
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
    private int deriveEntrySize(JarEntry current) {
        if (current.getSize() >= Integer.MAX_VALUE) throw new TheUncompressedSizeListedInJarIsGreaterThan2GbWhichSeemsWrong(current.getName(), current.getCompressedSize(), current.getSize());
        int size = (int) current.getSize();
        if (size < 0) size = 2048;
        return size;
    }

    public boolean hasMainClass() {
        return mainClass != null;
    }
}
