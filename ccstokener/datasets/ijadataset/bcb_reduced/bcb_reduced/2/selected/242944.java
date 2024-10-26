package app;

import util.Util;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Map;
import java.awt.Component;

/**
 * super.defaults is actually jar_defaults. However, if net_defaults 
 * successfully loads then super.defaults become net_defaults. This does
 * not disregaurd jar_defaults (it only overides them) because
 * net_defaults.defaults is jar_defaults.
 *
 * @author Kenroy Granville / Tim Hickey
 * @version "%I%, %G%"
 **/
public class Preferences extends Properties {

    static final int URL_TIMEOUT = 1500;

    public static final String USERNAME = "USERNAME", GROUPNAME = "GROUPNAME", LOCALHOST = "LOCALHOST", MINIMUM_PORT = "MINIMUM_PORT", MAXIMUM_PORT = "MAXIMUM_PORT", RENDEZVOUS_HOST = "RENDEZVOUS_HOST", RENDEZVOUS_PORT = "RENDEZVOUS_PORT", RENDEZVOUS_PW = "RENDEZVOUS_PW", UPDATED_PREFS_URL = "UPDATED_PREFS_URL", PLUGIN_DIR = "PLUGIN_DIR", BUILD = "BUILD", PREFS_FILE_NAME = "GEP";

    static File USERFILE;

    public Preferences() {
        super();
    }

    public Preferences(Preferences defaults) {
        super(defaults);
    }

    protected void setDefaults(final Component parent) {
        Preferences jar_defaults = new Preferences();
        try {
            Enumeration e = GrewpEdit.LOADER.getResources(PREFS_FILE_NAME);
            URL url = null;
            while (e.hasMoreElements()) {
                url = (URL) e.nextElement();
                if (url.getProtocol().equalsIgnoreCase("jar")) {
                    jar_defaults.load(url, parent);
                    final Preferences updated_defaults = new Preferences(jar_defaults);
                    this.defaults = updated_defaults;
                    new Thread(new Runnable() {

                        public void run() {
                            try {
                                URL url = new URL(getProperty(UPDATED_PREFS_URL));
                                updated_defaults.load(url, parent);
                            } catch (Exception e) {
                                Util.error("Problem loading updated preferences off the" + " Internet!", e, parent);
                            }
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            Util.error("Problem loading default preferences!", e, parent);
        }
        setUserPrefsFile();
        try {
            load(getUserPrefsFile(), parent);
        } catch (Exception e) {
            Util.error("Problem loading your default preferences!", e, parent);
        }
    }

    public void load(String file, Component parent) throws Exception {
        if (file == null) return;
        load(new File(file), parent);
    }

    public void load(File file, Component parent) throws Exception {
        if (file == null || !file.exists()) return;
        load(new FileInputStream(file), file.toString(), parent);
    }

    public void load(URL url, Component parent) throws Exception {
        if (url != null) {
            URLConnection con = url.openConnection();
            load(con.getInputStream(), url.toString(), parent);
        }
    }

    public void load(InputStream in, String file, Component parent) throws Exception {
        InputStream stream = new BufferedInputStream(in);
        try {
            super.load(stream);
            stream.close();
        } catch (IOException e) {
            Util.error("Problem loading preferences from:\n " + file, e, parent);
        }
    }

    public boolean store(String file, String header) {
        return (file == null) ? false : store(new File(file), header);
    }

    public boolean store(File file, String header) {
        if (file == null) return false;
        try {
            super.store(new FileOutputStream(file), header);
            return true;
        } catch (Exception e) {
            Util.error("Problem storeing preference to:\n " + file, e, null);
            return false;
        }
    }

    public void setUsername(String user) {
        setProperty(USERNAME, user);
    }

    public void setGroupname(String group) {
        setProperty(GROUPNAME, group);
    }

    public void setLocalhost(String host) {
        setProperty(LOCALHOST, host);
    }

    public void setMinPort(String min) {
        setProperty(MINIMUM_PORT, min);
    }

    public void setMaxPort(String max) {
        setProperty(MAXIMUM_PORT, max);
    }

    public void setRendezvousHost(String host) {
        setProperty(RENDEZVOUS_HOST, host);
    }

    public void setRendezvousPort(String port) {
        setProperty(RENDEZVOUS_PORT, port);
    }

    public void setRendezvousPW(String pw) {
        setProperty(RENDEZVOUS_PW, pw);
    }

    public void setUserPrefsFile() {
        String path = getSysHome();
        USERFILE = new File(path == null ? "" : path, PREFS_FILE_NAME);
    }

    public void setPluginDirectory(String dir) {
        setProperty(PLUGIN_DIR, dir);
    }

    public Preferences getDefaults() {
        return (Preferences) defaults;
    }

    public String getUsername() {
        String name = getProperty(USERNAME);
        name = name == null ? "" : name;
        String sysname = getSysUser();
        return !name.trim().equals("") ? name : sysname != null ? sysname : "user";
    }

    public String getGroupname() {
        return getProperty(GROUPNAME);
    }

    public String getLocalhost() {
        return getProperty(LOCALHOST);
    }

    public String getMinPort() {
        return getProperty(MINIMUM_PORT);
    }

    public String getMaxPort() {
        return getProperty(MAXIMUM_PORT);
    }

    public String getRendezvousHost() {
        return getProperty(RENDEZVOUS_HOST);
    }

    public String getRendezvousPort() {
        return getProperty(RENDEZVOUS_PORT);
    }

    public String getRendezvousPW() {
        return getProperty(RENDEZVOUS_PW);
    }

    public String getUserPrefsFile() {
        if (USERFILE != null) return USERFILE.toString();
        return PREFS_FILE_NAME;
    }

    public String getBuild() {
        return getProperty(BUILD);
    }

    public String getPluginDirectory() {
        return getProperty(PLUGIN_DIR);
    }

    public static String getSysHome() {
        return System.getProperty("user.home");
    }

    public static String getSysUser() {
        return System.getProperty("user.name");
    }

    public String toString() {
        return "Preferences(" + super.toString() + ")";
    }

    public static void main(String[] args) throws Exception {
        Preferences pref2 = new Preferences();
        pref2.setDefaults(null);
        pref2.list(System.out);
        pref2.clear();
        Util.debugln("\ncleared\n");
        pref2.list(System.out);
        pref2.store("test.gep", "testing prefs");
    }
}
