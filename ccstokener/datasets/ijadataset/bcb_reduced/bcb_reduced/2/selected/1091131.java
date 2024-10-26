package com.rlsoftwares.util;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import javax.swing.JOptionPane;

/**
 *
 * @author Rodrigo
 */
public class BrowserControl {

    private static final String WIN_ID = "Windows";

    private static final String WIN_PATH = "rundll32";

    private static final String WIN_FLAG = "url.dll,FileProtocolHandler";

    private static final String UNIX_PATH = "netscape";

    private static final String UNIX_FLAG = "-remote openURL";

    /**
     * Display a file in the system browser.  If you want to display a
     * file, you must include the absolute path name.
     *
     * @param parent frame parent to show error message box
     * @param url the file's url (the url must start with either "http://" or
     * "file://").
     */
    public static void displayURL(Component parent, String url) {
        boolean windows = isWindowsPlatform();
        String cmd = null;
        try {
            if (windows) {
                cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
                Process p = Runtime.getRuntime().exec(cmd);
            } else {
                cmd = UNIX_PATH + " " + UNIX_FLAG + "(" + url + ")";
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    int exitCode = p.waitFor();
                    if (exitCode != 0) {
                        cmd = UNIX_PATH + " " + url;
                        p = Runtime.getRuntime().exec(cmd);
                    }
                } catch (InterruptedException x) {
                    JOptionPane.showMessageDialog(parent, "Error bringing up browser, url=" + url, "error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (IOException x) {
            JOptionPane.showMessageDialog(parent, "Could not invoke browser, url=" + url, "error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Try to determine whether this application is running under Windows
     * or some other platform by examing the "os.name" property.
     *
     * @return true if this application is running under a Windows OS
     */
    private static boolean isWindowsPlatform() {
        String os = System.getProperty("os.name");
        if (os != null && os.startsWith(WIN_ID)) return true; else return false;
    }

    public static void postUrl(String targetUrl, HashMap<String, String> parameters) {
        try {
            String data = "";
            for (String key : parameters.keySet()) {
                data += (data.length() > 0) ? "&" : "";
                data += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(parameters.get(key), "UTF-8");
            }
            URL url = new URL(targetUrl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            wr.close();
            rd.close();
        } catch (Exception e) {
        }
    }
}
