package gullsview;

import java.io.*;
import java.net.*;
import java.util.*;

public class Packer {

    private Console console;

    private java.util.Map<String, Set<String>> restrictions;

    private List<Map> maps;

    private Map world;

    private List<String> constraints;

    private String secchunk;

    public Packer(Console console) throws Exception {
        this.console = console;
        this.restrictions = new HashMap<String, Set<String>>();
        this.maps = new ArrayList<Map>();
        this.addRestrictedEntry("FC", "gullsview/FileSystemImpl.class");
        this.addRestrictedEntry("BT", "gullsview/Jsr082Locator.class");
        this.addRestrictedEntry("LAPI", "gullsview/Jsr179Locator.class");
        this.addRestrictedEntry("M3G", "gullsview/M3gMapCanvas.class");
        this.addRestrictedEntry("M3G", "gullsview/TextureCache.class");
        this.addRestrictedEntry("M3G", "pointer2.png");
        this.addRestrictedEntry("M3G", "compass.png");
        this.addRestrictedEntry("BTS", "locator.bts");
        this.addRestrictedEntry("BTS", "gullsview/BtsLocator.class");
        this.addRestrictedEntry("HGE100", "gullsview/Hge100Locator.class");
        this.world = new Map();
        this.processWorldMap(this.world);
        this.maps.add(this.world);
        this.constraints = new ArrayList<String>();
        this.secchunk = this.getIp();
        if (this.secchunk == null) this.secchunk = "";
    }

    public void addRestrictedEntry(String constraint, String path) {
        Set<String> paths = this.restrictions.get(constraint);
        if (paths == null) {
            paths = new HashSet<String>();
            this.restrictions.put(constraint, paths);
        }
        paths.add(path);
    }

    public boolean isRestricted(String path) {
        for (Set<String> entries : this.restrictions.values()) {
            if (entries.contains(path)) return true;
        }
        return false;
    }

    public boolean isAllowed(String path) {
        for (String constraint : this.constraints) {
            if ((this.restrictions.get(constraint)).contains(path)) return true;
        }
        return false;
    }

    private void usage() {
        this.console.printSeparator();
        for (int i = 0; i < 6; i++) this.console.printRes("usage-" + i);
        this.console.printSeparator();
        for (int i = 0; i < 16; i++) this.console.printRes("overview-" + i);
        this.console.printSeparator();
    }

    public void run() throws Exception {
        this.usage();
        if (this.console.inputBoolean("enable-fc", null, false)) this.constraints.add("FC");
        if (this.console.inputBoolean("enable-bt", null, false)) this.constraints.add("BT");
        if (this.console.inputBoolean("enable-lapi", null, false)) this.constraints.add("LAPI");
        boolean experimental = this.console.inputBoolean("enable-experimental", null, false);
        if (experimental) {
            if (this.console.inputBoolean("enable-m3g", null, false)) this.constraints.add("M3G");
            if (this.console.inputBoolean("enable-bts", null, false)) this.constraints.add("BTS");
            if (this.console.inputBoolean("enable-hge100", null, false)) this.constraints.add("HGE100");
        }
        int count = this.console.inputInt("map-count", null, 1);
        for (int i = 0; i < count; i++) {
            this.console.printSeparator();
            this.console.print(this.console.r("map-no-params") + " " + (i + 1));
            this.console.printSeparator();
            Map map = new Map();
            this.processMap(map, i);
            this.maps.add(map);
        }
        String path = (new File(".")).getCanonicalPath();
        for (; ; ) {
            path = this.console.inputString("output-path", null, path);
            try {
                File file = new File(path);
                if (!file.exists()) {
                    this.console.errorRes("error-not-exist");
                } else if (!file.isDirectory()) {
                    this.console.errorRes("error-not-directory");
                } else {
                    break;
                }
            } catch (Exception e) {
                this.console.errorRes("error-incorrect-path", e);
            }
        }
        String name = this.console.inputString("output-name", null, "GullsView");
        this.console.printSeparator();
        this.console.printRes("start");
        this.console.printSeparator();
        String outputPath = this.filterJars(path, name);
        int externalDataCount = this.pushMapsData(new FileDumper() {

            private FileOutputStream fis;

            public void next(String path) throws IOException {
                this.close();
                File file = new File(path);
                File dir = file.getParentFile();
                dir.mkdirs();
                this.fis = new FileOutputStream(file);
            }

            public void write(byte[] buffer, int offset, int length) throws IOException {
                this.fis.write(buffer, offset, length);
            }

            public void close() throws IOException {
                if (this.fis != null) {
                    this.fis.flush();
                    this.fis.close();
                    this.fis = null;
                }
            }
        }, outputPath + "_DATA", false);
        this.console.printSeparator();
        this.console.printRes("finish");
        this.console.printSeparator();
        if (externalDataCount > 0) {
            this.console.printRes("copy-data-along");
            this.console.printSeparator();
        }
    }

    public void close() {
        this.console.close();
    }

    private String filterJars(String path, String name) throws IOException {
        String pathPrefix = path + "/" + name;
        String pathSuffix = "";
        for (int i = 0; i < this.constraints.size(); i++) pathSuffix += "_" + this.constraints.get(i);
        this.console.print(this.console.r("writing-output-start") + ": " + pathPrefix + pathSuffix + ".{jar|jad}");
        this.filterJar(path, name + pathSuffix, (pathSuffix.length() > 0) ? pathSuffix.substring(1) : "none");
        this.console.print(this.console.r("writing-output-finish") + ": " + pathPrefix + pathSuffix + ".{jar|jad}");
        return pathPrefix + pathSuffix;
    }

    private void filterJar(String path, final String name, final String extensions) throws IOException {
        JarFilter.Filter filter = new JarFilter.Filter() {

            public boolean processEntry(String name) {
                Packer.this.console.print(Packer.this.console.r("processing-entry") + ": " + name);
                return Packer.this.isRestricted(name) ? Packer.this.isAllowed(name) : true;
            }

            public void processManifest(java.util.Map<String, String> values) {
                Packer.this.console.printRes("processing-manifest");
                String descId = "MIDlet-Description";
                String desc = values.get(descId);
                if (desc != null) values.put(descId, desc + " (extensions: " + extensions + ")");
                values.put("MIDlet-Name", name);
            }

            public void addEntries(FileDumper fd) throws IOException {
                fd.next("data/maps");
                Packer.this.writeMaps(fd);
                Packer.this.writeResourceImage(fd, "data/world/0_0", "/world/0_0");
                Packer.this.writeResourceImage(fd, "data/world/0_1", "/world/0_1");
                Packer.this.writeResourceImage(fd, "data/world/1_0", "/world/1_0");
                Packer.this.writeResourceImage(fd, "data/world/1_1", "/world/1_1");
                Packer.this.pushMapsData(fd, "data", true);
            }
        };
        InputStream in = (this.getClass()).getResourceAsStream("/GullsView.mjar");
        String jarFileName = path + "/" + name + ".jar";
        OutputStream out = new FileOutputStream(jarFileName);
        JarFilter jf = new JarFilter(in, out, filter);
        java.util.Map<String, String> jad = jf.run();
        File jar = new File(jarFileName);
        jad.put("MIDlet-Jar-URL", name + ".jar");
        jad.put("MIDlet-Jar-Size", String.valueOf(jar.length()));
        FileOutputStream fos = new FileOutputStream(path + "/" + name + ".jad");
        jf.writeManifest(jad, fos);
        fos.flush();
        fos.close();
    }

    private String getIp() {
        try {
            URL url = new URL("http://whatismyip.org");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String ip = br.readLine();
            br.close();
            return ((ip != null) && (ip.length() < 20)) ? ip : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void processMap(Map map, int index) {
        map.name = this.inputString(index, "name", "");
        map.title = this.inputString(index, "title", "");
        map.vendor = this.inputString(index, "vendor", "");
        map.secchunk = this.secchunk;
        map.scale = this.inputInt(index, "scale", 0);
        map.segment = this.inputInt(index, "segment", 256);
        while (map.xcount <= 0) map.xcount = this.inputInt(index, "xcount", 1);
        ;
        while (map.ycount <= 0) map.ycount = this.inputInt(index, "ycount", 1);
        ;
        map.mercator = this.inputBoolean(index, "mercator", true);
        if (map.mercator) {
            map.segoffsetx = this.inputInt(index, "segoffsetx", 0);
            map.segoffsety = this.inputInt(index, "segoffsety", 0);
        } else {
            map.bax = 0;
            map.bay = 0;
            map.bbx = map.segment * map.xcount;
            map.bby = 0;
            map.bcx = 0;
            map.bcy = map.segment * map.ycount;
            map.balat = this.inputCoord(index, "lt-lat", 0);
            map.balon = this.inputCoord(index, "lt-lon", 0);
            map.bblat = this.inputCoord(index, "rt-lat", 0);
            map.bblon = this.inputCoord(index, "rt-lon", 0);
            map.bclat = this.inputCoord(index, "lb-lat", 0);
            map.bclon = this.inputCoord(index, "lb-lon", 0);
        }
        map.deflat = this.inputCoord(index, "lat", (map.balat + map.bclat) / 2);
        map.deflon = this.inputCoord(index, "lon", (map.balon + map.bblon) / 2);
        this.processMapData(map, index);
    }

    private void processMapData(Map map, int index) {
        try {
            map.dataDir = (new File(".")).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (; ; ) {
            map.dataDir = this.inputString(index, "data-dir", map.dataDir);
            File dir = new File(map.dataDir);
            if (dir.exists() && dir.isDirectory()) break;
            this.console.errorRes("error-not-directory");
        }
        map.dataFormat = this.inputString(index, "data-format", "{0}_{1}.png");
        map.dataIncluded = this.constraints.contains("FC") ? this.inputBoolean(index, "data-included", true) : true;
    }

    private void processWorldMap(Map map) {
        map.name = "world";
        map.title = this.console.r("world");
        map.vendor = "";
        map.secchunk = "";
        map.scale = 1;
        map.segment = 256;
        map.xcount = 2;
        map.ycount = 2;
        map.deflat = 0;
        map.deflon = 0;
        map.mercator = true;
        map.segoffsetx = 0;
        map.segoffsety = 0;
    }

    private void writeMaps(FileDumper fd) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.writeMaps(baos);
        byte[] buffer = baos.toByteArray();
        fd.write(buffer, 0, buffer.length);
    }

    private void writeMaps(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(maps.size());
        for (Map map : maps) map.save(dos);
        dos.flush();
    }

    private void writeResourceImage(FileDumper fd, String path, String resource) throws IOException {
        this.console.print(this.console.r("writing-resource-entry") + ": " + resource + " => " + path);
        fd.next(path);
        InputStream is = (this.getClass()).getResourceAsStream(resource);
        if (is == null) throw new IOException("Cannot find resource \"" + resource + "\"");
        byte[] buffer = new byte[1024];
        int count;
        while ((count = is.read(buffer, 0, buffer.length)) > 0) fd.write(buffer, 0, count);
    }

    private int pushMapsData(FileDumper fd, String path, boolean inner) throws IOException {
        int ret = 0;
        for (Map map : this.maps) {
            if (this.pushMapData(map, fd, path, inner)) ret++;
        }
        return ret;
    }

    private boolean pushMapData(Map map, FileDumper fd, String path, boolean inner) throws IOException {
        if (map == this.world) return false;
        if (map.dataIncluded != inner) return false;
        String prefix = path + "/" + map.name;
        for (int y = 0; y < map.ycount; y++) {
            for (int x = 0; x < map.xcount; x++) {
                String name = java.text.MessageFormat.format(map.dataFormat, new Object[] { new Integer(x), new Integer(y) });
                File file = new File(new File(map.dataDir), name);
                if (!file.exists()) {
                    this.console.fatalError(this.console.r("error-file-not-exist") + ": \"" + file.getCanonicalPath() + "\"");
                    throw new IOException("File does not exist: " + file);
                }
                if (!file.isFile()) {
                    this.console.fatalError(this.console.r("error-not-file") + ": \"" + file.getCanonicalPath() + "\"");
                    throw new IOException("Path is not file: " + file);
                }
                if (inner) {
                    this.console.print(this.console.r("adding-segment-file-to-archive") + ": " + file.getCanonicalPath());
                } else {
                    this.console.print(this.console.r("adding-segment-file-to-dir") + ": " + prefix + "/" + name);
                }
                fd.next(prefix + "/" + name);
                FileInputStream fis = new FileInputStream(file);
                fd.pump(fis);
                fis.close();
            }
        }
        return true;
    }

    private String inputString(int index, String id, String def) {
        String ret;
        for (; ; ) {
            ret = this.console.inputString("map-" + index + "-" + id, "map-" + id, def);
            ret = ret.trim();
            if (ret.length() > 0) break;
            this.console.errorRes("error-empty");
        }
        return ret;
    }

    private int inputInt(int index, String id, int def) {
        return this.console.inputInt("map-" + index + "-" + id, "map-" + id, def);
    }

    private boolean inputBoolean(int index, String id, boolean def) {
        return this.console.inputBoolean("map-" + index + "-" + id, "map-" + id, def);
    }

    private double inputCoord(int index, String id, double def) {
        for (; ; ) {
            String str = this.inputString(index, id, this.formatCoord(def));
            try {
                return this.parseCoord(str);
            } catch (Exception e) {
                this.console.errorRes("error-not-coord");
            }
        }
    }

    private String formatCoord(double coord) {
        int sgn = coord < 0 ? -1 : 1;
        coord = Math.abs(coord);
        int deg = (int) Math.floor(coord);
        double mrest = (coord - deg) * 60;
        int min = (int) Math.floor(mrest);
        double srest = (mrest - min) * 60;
        int sec = (int) Math.floor(srest);
        int msec = (int) Math.floor((srest - sec) * 1000);
        String smsec = String.valueOf(msec);
        while (smsec.length() < 3) smsec = "0" + smsec;
        return (sgn * deg) + "*" + min + "#" + sec + "." + smsec;
    }

    private double parseCoord(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
        }
        String sdeg = null;
        String smin = null;
        String ssec = null;
        int asterisk = str.indexOf('*');
        if (asterisk < 0) {
            sdeg = str;
        } else {
            sdeg = str.substring(0, asterisk);
            String srest = str.substring(asterisk + 1);
            int pound = srest.indexOf('#');
            if (pound < 0) {
                smin = srest;
            } else {
                smin = srest.substring(0, pound);
                ssec = srest.substring(pound + 1);
            }
        }
        double deg = Double.parseDouble(sdeg);
        double min = Double.parseDouble(smin);
        double sec = Double.parseDouble(ssec);
        return deg + (min / 60) + (sec / 3600);
    }
}
