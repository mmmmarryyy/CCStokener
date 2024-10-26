package com.cirnoworks.spk;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Cloudee
 * 
 */
public class JarToSPK extends SPKWriter {

    private final JarFile jar;

    public JarToSPK(URL jarURL, long seed) throws IOException {
        super(seed);
        URL url = new URL("jar:" + jarURL + "!/");
        JarURLConnection juc = (JarURLConnection) url.openConnection();
        jar = juc.getJarFile();
    }

    @Override
    protected InputStream getInputStream(String base, String name) throws IOException {
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String entryName = base + name;
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
        JarEntry entry = jar.getJarEntry(entryName);
        return jar.getInputStream(entry);
    }

    @Override
    protected void checkSource(String base) throws IOException {
        if (base.equals("") || base.equals("/")) {
            return;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        JarEntry entry = jar.getJarEntry(base);
        if (entry == null || !entry.isDirectory()) {
            throw new IOException(base);
        }
    }

    @Override
    protected void listFile(String base, NameFilter filter, List<FileEntry> files) throws IOException {
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            if (name.startsWith(base) && (filter == null || filter.accept(name))) {
                name = name.substring(base.length());
                System.out.println("LIST " + name);
                FileEntry fe = new FileEntry(name);
                files.add(fe);
            }
        }
    }
}
