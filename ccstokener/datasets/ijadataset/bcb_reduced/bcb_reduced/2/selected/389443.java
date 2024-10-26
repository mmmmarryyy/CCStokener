package tristero.update;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import tristero.util.*;

public class AutoUpdate {

    public static void main(String[] args) throws Exception {
        if (args[1].endsWith(".zip")) updateZip(args[0], args[1], args[2]); else updateManifest(args[0], args[1], args[2]);
    }

    public static boolean detectSwarmcast() {
        try {
            Properties props = System.getProperties();
            props.setProperty("proxyHost", "localhost");
            props.setProperty("proxyPort", "8001");
            System.setProperties(props);
            URL url = new URL("http://proxyapi.swarmcast.net/api/js/ping.js");
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            if (line == null) return false;
            return line.equals("pong=true");
        } catch (Exception e) {
            System.err.println("Error contacting Swarmcast: " + e);
            return false;
        }
    }

    public static void updateList(String base, String manifest, String dest) throws Exception {
        if (!base.endsWith("/")) base = base + "/";
        if (!dest.endsWith("/")) dest = dest + "/";
        File df = new File(dest);
        if (!df.exists()) df.mkdirs();
        System.err.println("base: " + base + " dest: " + dest);
        InputStream is = fetch(base, manifest);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        while (line != null) {
            System.err.println("read line: " + line);
            if (!line.equals("")) download(base, line, dest);
            line = br.readLine();
        }
    }

    public static void updateManifest(String base, String manifest, String dest) throws Exception {
        System.err.println("Updating manifest " + base + " " + manifest + " " + dest);
        System.err.println("Detecting swarmcast: " + detectSwarmcast());
        if (!base.endsWith("/")) base = base + "/";
        if (!dest.endsWith("/")) dest = dest + "/";
        File df = new File(dest);
        if (!df.exists()) df.mkdirs();
        Properties props = new Properties();
        InputStream is = fetch(base, manifest);
        props.load(is);
        Enumeration iterator = props.propertyNames();
        while (iterator.hasMoreElements()) {
            String file = (String) iterator.nextElement();
            String newdest = props.getProperty(file);
            System.err.println("file: " + file + " newdest: " + newdest);
            List l = StringUtils.split(file, ".");
            String ext = "";
            if (l.size() > 1) ext = ((String) l.get(1)).toLowerCase();
            System.err.println("ext: " + ext);
            Properties ids = null;
            boolean skip = false;
            String id = null;
            l = StringUtils.split(file, "!");
            if (l.size() == 2) {
                id = (String) l.get(0);
                file = (String) l.get(1);
                if (ids == null) {
                    ids = new Properties();
                    try {
                        ids.load(new FileInputStream("ids.properties"));
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
                String oldid = ids.getProperty(file);
                if (oldid != null && oldid.equals(id)) skip = true;
            }
            if (!skip) {
                try {
                    if (ext.equals("zip")) {
                        if (newdest.equals(".")) updateZip(base, file, dest); else updateZip(base, file, dest + newdest);
                    } else if (ext.equals("lst")) {
                        if (newdest.equals(".")) updateList(base, file, dest); else updateList(base, file, dest + newdest);
                    } else {
                        if (newdest.equals(".")) download(base, file, dest); else download(base, file, dest + newdest);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (id != null) {
                if (ids == null) {
                    ids = new Properties();
                    try {
                        ids.load(new FileInputStream("ids.properties"));
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
                ids.setProperty(file, id);
                try {
                    ids.save(new FileOutputStream("ids.properties"), "");
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }
    }

    public static void updateZip(String base, String file, String dest) throws Exception {
        System.err.println("Updating zip " + base + " " + file + " " + dest);
        File zipFile = File.createTempFile("autoUpdate", ".zip");
        FileOutputStream fos = new FileOutputStream(zipFile);
        File f = new File(dest);
        if (!f.exists()) f.mkdirs();
        InputStream is = fetch(base, file);
        Conduit.pump(is, fos);
        fos.close();
        is = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry = zis.getNextEntry();
        while (entry != null) {
            try {
                unzip(zis, entry, f);
            } catch (Exception e) {
                System.err.println("Error unzipping: " + e);
            }
            entry = zis.getNextEntry();
        }
    }

    public static void unzip(ZipInputStream zis, ZipEntry entry, File f) throws Exception {
        System.err.println("Unzipping " + entry.getName());
        if (entry.isDirectory()) return;
        File df = new File(f, entry.getName());
        df.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(df);
        Conduit.pump(zis, fos);
    }

    public static InputStream fetch(String base, String file) throws Exception {
        URL url = new URL(base + file);
        System.err.println("fetching " + url);
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        return is;
    }

    public static void download(String base, String file, String dest) throws Exception {
        InputStream is = fetch(base, file);
        File f = new File(dest + "/" + file);
        if (!f.exists()) f.getParentFile().mkdirs();
        System.err.println("Writing " + f);
        OutputStream os = new FileOutputStream(f);
        System.err.println("pumping " + file);
        Conduit.pump(is, os);
        System.err.println("done pumping " + file);
    }
}
