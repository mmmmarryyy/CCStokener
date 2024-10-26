package org.snipsnap.util;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 *
 * @author Matthias L. Jugel
 * @version $Id: Checksum.java 867 2003-05-23 10:47:26Z stephan $
 */
public class Checksum {

    private String id;

    private Map checksums;

    public Checksum(File file) throws IOException {
        load(file);
    }

    public Checksum(URL url) throws IOException {
        load(url);
    }

    public Checksum(String id) {
        this(id, new HashMap());
    }

    public Checksum(String id, Map init) {
        this.id = id;
        this.checksums = init;
    }

    public String getId() {
        return id;
    }

    public void add(String file, Long checksum) {
        checksums.put(file, checksum);
    }

    public Long get(String file) {
        return (Long) checksums.get(file);
    }

    public Set compareChanged(Checksum other) {
        Set result = new TreeSet();
        Iterator it = checksums.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (!checksums.get(key).equals(other.get(key))) {
                result.add(key);
            }
        }
        return result;
    }

    public Set getFileNames() {
        return checksums.keySet();
    }

    public Set compareUnchanged(Checksum other) {
        Set result = new TreeSet();
        Iterator it = checksums.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (checksums.get(key).equals(other.get(key))) {
                result.add(key);
            }
        }
        return result;
    }

    public void store(File file) throws IOException {
        Iterator it = checksums.keySet().iterator();
        Properties save = new Properties();
        while (it.hasNext()) {
            String name = (String) it.next();
            save.setProperty(name, Long.toHexString(((Long) checksums.get(name)).longValue()));
        }
        OutputStream out = new FileOutputStream(file);
        save.setProperty("ID", id);
        save.store(out, "checksums for " + id);
        out.close();
    }

    public void load(File file) throws IOException {
        load(new FileInputStream(file));
    }

    public void load(URL url) throws IOException {
        load(url.openStream());
    }

    private void load(InputStream in) throws IOException {
        checksums = new HashMap();
        Properties load = new Properties();
        load.load(in);
        Iterator it = load.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (!"ID".equals(key)) {
                checksums.put(key, new Long(Long.parseLong(load.getProperty(key), 16)));
            }
        }
        id = load.getProperty("ID");
    }

    public String toString() {
        return "Checksum[id=" + id + ", " + checksums.toString() + "]";
    }
}
