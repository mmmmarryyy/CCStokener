package net.sourceforge.processdash;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/** This class reads a collection of settings from a URL, and adds them to
 * a user's dashboard Settings file.
 * 
 * This class normally would be run during the installation process, to
 * optionally tweak dashboard configuration settings.
 */
public class MergeSettings {

    public static void main(String[] args) {
        if (args.length == 2) {
            String url = args[0];
            String destDir = args[1];
            try {
                merge(url, destDir);
            } catch (Exception e) {
            }
        }
        System.exit(0);
    }

    private static void merge(String url, String destDir) throws Exception {
        if ("none".equals(url)) return;
        Properties propsIn = new Properties();
        propsIn.load(new URL(url).openStream());
        if (propsIn.isEmpty()) return;
        File destFile = new File(destDir, getSettingsFilename());
        Properties orig = new Properties();
        try {
            FileInputStream origIn = new FileInputStream(destFile);
            orig.load(origIn);
            origIn.close();
        } catch (Exception e) {
        }
        Properties propsOut = new Properties();
        for (Iterator i = propsIn.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String) e.getKey();
            if (!key.startsWith(PROP_PREFIX)) continue;
            String settingName = key.substring(PROP_PREFIX.length());
            String value = (String) e.getValue();
            if (!orig.containsKey(settingName)) propsOut.put(settingName, value);
        }
        if (propsOut.isEmpty()) return;
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile, true)));
        out.newLine();
        out.newLine();
        for (Iterator i = propsOut.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            out.write((String) e.getKey());
            out.write("=");
            out.write((String) e.getValue());
            out.newLine();
        }
        out.close();
    }

    private static final String getSettingsFilename() {
        if (System.getProperty("os.name").toUpperCase().startsWith("WIN")) return "pspdash.ini"; else return ".pspdash";
    }

    private static final String PROP_PREFIX = "pspdash.";
}
