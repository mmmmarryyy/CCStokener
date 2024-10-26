package de.ios.framework.basic;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class ZipClassLoader extends ClassLoader {

    Hashtable cache = new Hashtable();

    Hashtable dataCache = new Hashtable();

    Vector zipFiles = new Vector();

    public ZipClassLoader() {
    }

    public void addArchive(ZipInputStream zi) throws ZipException, IOException {
        ZipEntry ze;
        int len;
        byte buffer[] = new byte[10240];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        do {
            os.reset();
            ze = zi.getNextEntry();
            if (ze != null) {
                while ((len = zi.read(buffer, 0, buffer.length)) >= 0) os.write(buffer, 0, len);
                dataCache.put(ze.getName(), os.toByteArray());
            }
        } while (ze != null);
    }

    /**
     * Convidence-methode to add a archive from url.
     * Remark: With jdk 2, the URLClassloader from the package 
     * java.net can be used.
     */
    public void addArchive(URL url) throws ZipException, IOException {
        addArchive(new ZipInputStream(url.openStream()));
    }

    public void addZipFile(String archiveName) throws ZipException, IOException {
        ZipFile zf = new ZipFile(archiveName);
        zipFiles.addElement(zf);
    }

    /**
    * Try to load an entry from a ZipFile.
    */
    private byte[] loadData(ZipFile zipFile, String name) {
        try {
            ZipEntry entry = zipFile.getEntry(name);
            if (entry == null) return null;
            InputStream istream = zipFile.getInputStream(entry);
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int countBytes = 0;
            while (countBytes < entry.getSize()) {
                int len = istream.read(buf);
                if (len > 0) {
                    countBytes += len;
                    ostream.write(buf, 0, len);
                }
            }
            ostream.flush();
            ostream.close();
            istream.close();
            return ostream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
    * Try to load an entry the internal caches and the added zipfiles.
    */
    private byte[] loadData(String name) {
        byte data[] = null;
        try {
            data = (byte[]) dataCache.get(name);
            Enumeration zfe = zipFiles.elements();
            while (zfe.hasMoreElements() && data == null) data = loadData((ZipFile) zfe.nextElement(), name);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return data;
    }

    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = (Class) cache.get(name);
        if (c == null) {
            byte data[] = loadData(name.replace('.', '/') + ".class");
            if (data == null) return findSystemClass(name);
            c = defineClass(data, 0, data.length);
            cache.put(name, c);
        }
        if (resolve) resolveClass(c);
        return c;
    }

    public InputStream getResourceAsStream(String name) {
        byte data[] = loadData(name);
        if (data == null) return getSystemResourceAsStream(name);
        return new ByteArrayInputStream(data);
    }
}
