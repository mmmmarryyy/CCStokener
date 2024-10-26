package org.uweschmidt.wiimote.whiteboard.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.swing.JOptionPane;
import org.uweschmidt.wiimote.whiteboard.WiimoteWhiteboard;

public class UpdateNotifier {

    public static void checkForUpdate(String version) {
        try {
            URL url = new URL(WiimoteWhiteboard.getProperty("updateURL"));
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            final String current = in.readLine();
            if (compare(version, current)) showUpdateNotification(version, current);
            in.close();
        } catch (Exception e) {
        }
    }

    private static void showUpdateNotification(String program, String current) throws Exception {
        String question = Util.getResourceMap(UpdateNotifier.class).getString("updateQuestion", WiimoteWhiteboard.getProperty("id"));
        String title = Util.getResourceMap(UpdateNotifier.class).getString("updateTitle", WiimoteWhiteboard.getProperty("id"), current);
        int response = JOptionPane.showConfirmDialog(null, question, title, JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            BareBonesBrowserLaunch.openURL(WiimoteWhiteboard.getProperty("homepage"));
        }
    }

    private static boolean compare(String program, String current) throws Exception {
        String[] psplit = program.split("\\."), csplit = current.split("\\.");
        int[] p = new int[3], c = new int[3];
        for (int i = 0; i < 3; i++) {
            p[i] = Integer.valueOf(psplit[i]);
            c[i] = Integer.valueOf(csplit[i]);
        }
        if (c[0] > p[0]) return true;
        if (c[0] == p[0]) {
            if (c[1] > p[1]) return true;
            if (c[1] == p[1]) {
                if (c[2] > p[2]) return true;
            }
        }
        return false;
    }
}
