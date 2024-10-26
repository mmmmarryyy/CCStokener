package net.sourceforge.keepassj2me.packer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import javax.swing.JOptionPane;

/**
 * MidletPacker - pack custom midlet
 * 
 * @author Stepan Strelets
 *
 */
public class MidletPacker {

    private Config conf;

    MidletPacker(Config conf) {
        this.conf = conf;
    }

    public void pack() throws Exception {
        File srcJar = new File(conf.getSourceJar());
        if (!srcJar.exists()) throw new Exception("Source JAR not exists");
        if (!srcJar.isFile()) throw new Exception("Source JAR not is file");
        if (!srcJar.canRead()) throw new Exception("Source JAR not readable");
        File dstJar = new File(conf.getTargetJar());
        if (dstJar.exists()) {
            if (JOptionPane.showConfirmDialog(null, "Target JAR exists, overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION) != JOptionPane.OK_OPTION) return;
            if (!dstJar.isFile()) throw new Exception("Target JAR not is file");
            if (!dstJar.canWrite()) throw new Exception("Target JAR not writable");
        }
        ;
        JarFile src = new JarFile(srcJar);
        JarOutputStream dst = new JarOutputStream(new FileOutputStream(dstJar));
        byte buffer[] = new byte[1024];
        int bytesRead;
        boolean resourcePackEnabled = conf.getResourcesPackEnable();
        String iconsPackDir = "/res/packs/" + conf.getIconsPackName() + "/";
        String logoPackDir = "/res/packs/" + conf.getLogoPackName() + "/";
        Enumeration<JarEntry> entries = src.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            InputStream is;
            if (resourcePackEnabled && name.equals("images/icon.png")) {
                entry = new JarEntry(name);
                is = this.getResourceInputStream(logoPackDir + name.substring(7));
            } else if (resourcePackEnabled && name.startsWith("images/")) {
                entry = new JarEntry(name);
                is = this.getResourceInputStream(iconsPackDir + name.substring(7));
            } else {
                is = src.getInputStream(entry);
            }
            ;
            dst.putNextEntry(entry);
            while ((bytesRead = is.read(buffer)) != -1) {
                dst.write(buffer, 0, bytesRead);
            }
            ;
        }
        ;
        src.close();
        String kdb, name, names = "";
        Vector<String> already = new Vector<String>();
        int i = 0;
        while ((kdb = conf.getSourceKdb(i)) != null) {
            File srcKdb = new File(kdb);
            if (!srcKdb.exists()) throw new Exception("Source KDB '" + kdb + "' not exists");
            if (!srcKdb.isFile()) throw new Exception("Source KDB '" + kdb + "' not is file");
            if (!srcKdb.canRead()) throw new Exception("Source KDB '" + kdb + "' not readable");
            name = srcKdb.getName();
            JarEntry entry = new JarEntry("kdb/" + i);
            dst.putNextEntry(entry);
            names += name + "\n";
            already.add(name);
            FileInputStream kdbStream = new FileInputStream(srcKdb);
            while ((bytesRead = kdbStream.read(buffer)) != -1) {
                dst.write(buffer, 0, bytesRead);
            }
            ;
            ++i;
        }
        ;
        if (names.length() > 0) {
            JarEntry lsentry = new JarEntry("kdb/ls");
            dst.putNextEntry(lsentry);
            byte[] names_bytes = names.getBytes("UTF-8");
            dst.write(names_bytes, 0, names_bytes.length);
        }
        ;
        dst.close();
    }

    private InputStream getResourceInputStream(String path) throws IOException {
        URL url = getClass().getResource(path);
        if (url != null) return url.openStream(); else return new FileInputStream(new File("." + path));
    }
}
