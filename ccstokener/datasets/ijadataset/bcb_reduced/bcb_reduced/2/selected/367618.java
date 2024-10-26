package StoMpd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * 
 * @author espenk
 */
public class ConfigHandler {

    private File cfgFile;

    private File bckupFile;

    private String latestv = "";

    private String thisv;

    private static ConfigHandler instance = null;

    private HashMap<String, String> cfgMap = new HashMap<String, String>();

    private HashMap<String, String> cfgNew = new HashMap<String, String>();

    private HashMap<String, String> padMap = new HashMap<String, String>();

    protected ConfigHandler() {
        if (cfgMap.isEmpty()) {
            readConfig();
            if (!cfgFile.exists()) {
                createFiles();
            }
            cfgMap.put("superpilot", "no");
            initPads("default");
            thisv = readTextFromJar("/StoMpd/resources/stompd.current");
        }
    }

    public static ConfigHandler getInstance() {
        if (instance == null) {
            instance = new ConfigHandler();
        }
        return instance;
    }

    protected boolean isUpToDate() {
        return (latestv.equals(thisv));
    }

    protected String latestVersion() {
        if (latestv.isEmpty()) {
            latestv = getVersionFile();
        }
        return latestv;
    }

    protected String thisVersion() {
        return thisv;
    }

    public boolean isUserAdmin(String s) {
        if (cfgMap.get("useradmin").contains(s)) {
            return true;
        }
        return false;
    }

    public boolean singleuser() {
        if (cfgMap.get("singleuser").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    public boolean normalization() {
        if (cfgMap.get("normalization").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    public boolean globaladmin() {
        if (cfgMap.get("singleuser").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    protected boolean isMpdLocal() {
        if (cfgMap.get("mpdserver").equals("localhost")) {
            return true;
        }
        if (cfgMap.get("mpdserver").equals("127.0.0.1")) {
            return true;
        }
        return false;
    }

    public String mpdserver() {
        return cfgMap.get("mpdserver");
    }

    public int mpdport() {
        return Integer.parseInt(cfgMap.get("mpdport"));
    }

    public int mpdvol() {
        return Integer.parseInt(cfgMap.get("mpdvol"));
    }

    protected int mpdfade() {
        return Integer.parseInt(cfgMap.get("mpdfade"));
    }

    public int mixrampdb() {
        return Integer.parseInt(cfgMap.get("mixrampdb"));
    }

    public String mixrampdelay() {
        if (!has("mixrampdelay")) {
            cfgMap.put("mixrampdelay", "0");
        }
        return cfgMap.get("mixrampdelay");
    }

    public void setXfade(Boolean b) {
        if (b) {
            cfgMap.put("xfadeon", "yes");
            return;
        }
        cfgMap.put("xfadeon", "no");
    }

    protected int superlimit() {
        return Integer.parseInt(cfgMap.get("superlimit"));
    }

    public int xfadeval() {
        return Integer.parseInt(cfgMap.get("xfadeval"));
    }

    protected int maxmem() {
        return Integer.parseInt(cfgMap.get("maxmem"));
    }

    public boolean verboselog() {
        if (cfgMap.get("verboselog").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    public boolean fadestop() {
        if (cfgMap.get("fadestop").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    public boolean guivolume() {
        if (cfgMap.get("guivolume").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    public boolean jogwheel() {
        if (cfgMap.get("jogwheel").equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    public Boolean has(String s) {
        return (cfgMap.containsKey(s));
    }

    public Boolean is(String s) {
        if (has(s)) {
            if (get(s).equals("yes")) {
                return true;
            }
            if (get(s).equals("on")) {
                return true;
            }
        }
        return false;
    }

    public String get(String s) {
        if (cfgMap.containsKey(s)) {
            return cfgMap.get(s);
        }
        return "";
    }

    public void set(String k, String v) {
        cfgMap.put(k, v);
    }

    protected final void initPads(String usr) {
        readPadConfig(usr);
        setPads();
    }

    protected String padMapValue(String s) {
        return padMap.get(s);
    }

    private void readPadConfig(String userName) {
        File padfile = new File(cfgMap.get("padsdir") + userName + ".pad");
        if (!padfile.exists()) {
            padfile = new File(cfgMap.get("padsdir") + "default.pad");
        }
        String[] part = new String[2];
        try {
            BufferedReader br = new BufferedReader(new FileReader(padfile));
            String str;
            while ((str = br.readLine()) != null) {
                if (!str.isEmpty() && !str.startsWith("#")) {
                    part = str.split(" ", 2);
                    padMap.put(part[0], part[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void setPads() {
        for (int i = 1; i < 13; i++) {
            if (!padMap.containsKey("pad" + i + "title")) {
                if (padMap.containsKey("pad" + i + "file")) {
                    padMap.put("pad" + i + "title", padMap.get("pad" + i + "file"));
                } else {
                    padMap.put("pad" + i + "title", "-");
                }
            }
        }
    }

    protected void writeConfig() {
        cfgNew.putAll(cfgMap);
        TreeMap tmNew = new TreeMap(cfgNew);
        try {
            if (!cfgFile.exists()) {
                System.out.println("Creating new config file.");
            } else {
                if (cfgFile.renameTo(bckupFile)) {
                    System.out.println("Old config file saved as backup.");
                }
            }
            FileWriter write = new FileWriter(cfgFile, true);
            PrintWriter text = new PrintWriter(write);
            while (tmNew.size() > 0) {
                text.println(tmNew.firstKey().toString().toLowerCase() + " " + tmNew.get(tmNew.firstKey()).toString());
                tmNew.remove(tmNew.firstKey());
            }
            text.flush();
            write.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return;
        }
    }

    private void readConfig() {
        String home = System.getProperty("user.home");
        cfgMap.put("guivolume", "no");
        cfgMap.put("mpdserver", "localhost");
        cfgMap.put("mpdport", "6600");
        cfgMap.put("mpdvol", "100");
        cfgMap.put("mpdfade", "60");
        cfgMap.put("replaygain", "track");
        cfgMap.put("useradmin", "default");
        cfgMap.put("usrpassrequired", "no");
        cfgMap.put("singleuser", "no");
        cfgMap.put("verboselog", "no");
        cfgMap.put("autoplay", "yes");
        cfgMap.put("autorefill", "no");
        cfgMap.put("fadestop", "no");
        cfgMap.put("xfadeon", "yes");
        cfgMap.put("xfadeval", "4");
        cfgMap.put("mixrampdb", "-20");
        cfgMap.put("mixrampdelay", "nan");
        cfgMap.put("superlimit", "20");
        cfgMap.put("maxmem", "50");
        cfgMap.put("homedir", home + File.separator + ".stompd" + File.separator);
        cfgMap.put("coverdir", home + File.separator + ".stompd" + File.separator + "cover" + File.separator);
        cfgMap.put("logdir", home + File.separator + ".stompd" + File.separator + "logs" + File.separator);
        cfgMap.put("padsdir", home + File.separator + ".stompd" + File.separator + "pads" + File.separator);
        cfgMap.put("userdir", home + File.separator + ".stompd" + File.separator + "users" + File.separator);
        cfgMap.put("columns", "file Artist Title Track Album Date Genre Time Comment");
        cfgMap.put("stdcolumns", "file Artist Title Album Time");
        cfgFile = new File(home + File.separator + ".stompd" + File.separator + "stompd.conf");
        if (!cfgFile.exists()) {
            writeConfig();
            return;
        }
        bckupFile = new File(cfgFile.toString() + ".old");
        String[] part = new String[2];
        try {
            BufferedReader br = new BufferedReader(new FileReader(cfgFile));
            String str;
            while ((str = br.readLine()) != null) {
                if (!str.isEmpty() && !str.startsWith("#")) {
                    part = str.split(" ", 2);
                    if (part.length > 1) {
                        cfgMap.put(part[0].toLowerCase(), part[1]);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createFiles() {
        String home = System.getProperty("user.home");
        Boolean success = (new File(home + File.separator + ".stompd" + File.separator + "users" + File.separator + "default")).mkdirs();
        success = (new File(home + File.separator + ".stompd" + File.separator + "pads" + File.separator + "sounds").mkdirs());
        success = (new File(home + File.separator + ".stompd" + File.separator + "logs").mkdir());
        success = (new File(home + File.separator + ".stompd" + File.separator + "cover").mkdir());
        if (success) {
            System.out.println("StoMpd directories created in " + home);
            writeConfig();
        }
    }

    private String getVersionFile() {
        String serverfile = "unknown";
        try {
            URL url;
            URLConnection urlConn;
            BufferedReader dis;
            url = new URL("http://www.stompd.org/stompd.current");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            serverfile = dis.readLine();
            dis.close();
        } catch (MalformedURLException mue) {
            serverfile = mue.getLocalizedMessage();
        } catch (IOException ioe) {
            serverfile = ioe.getLocalizedMessage();
        }
        return serverfile;
    }

    public static String readTextFromJar(String s) {
        InputStream is = null;
        BufferedReader br = null;
        String line = null;
        try {
            is = RandomInfo.class.getResourceAsStream(s);
            br = new BufferedReader(new InputStreamReader(is));
            line = br.readLine();
            if (br != null) {
                br.close();
            }
            if (is != null) {
                is.close();
            }
        } catch (Exception e) {
        }
        return line;
    }
}
