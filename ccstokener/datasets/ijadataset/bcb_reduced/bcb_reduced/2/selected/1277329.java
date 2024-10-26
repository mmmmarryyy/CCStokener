package de.yaams.launcher.another;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import javax.swing.JOptionPane;
import de.yaams.launcher.gui.YException;
import de.yaams.launcher.gui.YMessagesDialog;
import de.yaams.launcher.gui.YProgressWindowRepeat;

/**
 * @author Nebli
 * 
 */
public class SystemHelper {

    /** 
	 * Check the basics, if the system support the action for this path
	 * 
	 * @param a
	 * @param path
	 * @return true, support it/user select ignore, false, don't support it
	 */
    public static boolean basics(final Action a, final String path) {
        try {
            YMessagesDialog errors = new YMessagesDialog(I18N.t("Aufruf von {0} nicht möglich", path), "action" + a.toString());
            if (!Desktop.isDesktopSupported()) {
                errors.add(I18N.t("Java Desktop Funktion wird vom System nicht untersützt."), YMessagesDialog.INFO);
            }
            final Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(a)) {
                errors.add(I18N.t("{0}-Aktion wird nicht unterstützt.", a), YMessagesDialog.INFO);
            }
            return errors.showOk();
        } catch (final Throwable t) {
            YException.error("Can not run system action " + a + " for " + path, t);
        }
        return true;
    }

    /**
	 * Open the url with the default browser
	 * 
	 * @param url
	 */
    public static void openUrl(final String url) {
        if (!basics(Action.BROWSE, url)) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (final Throwable t) {
            YException.error("Can not open url " + url, t);
        }
    }

    /**
	 * Open the file with the default viewer
	 * 
	 * @param file
	 */
    public static void viewFile(final File file) {
        if (!basics(Action.OPEN, file.getAbsolutePath())) {
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (final Throwable t) {
            YException.error("Can not view " + file, t);
        }
    }

    /**
	 * Open the file with the default viewer
	 * 
	 * @param file
	 */
    public static void editFile(final File file) {
        if (!basics(Action.EDIT, file.getAbsolutePath())) {
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (final Throwable t) {
            YException.error("Can not edit " + file, t);
        }
    }

    /**
	 * Send feeback to yaams
	 * 
	 * @param data
	 */
    public static void sendData(final HashMap<String, String> data) {
        YProgressWindowRepeat y = new YProgressWindowRepeat(I18N.t("Send Data to yaams.de"));
        try {
            final StringBuffer send = new StringBuffer("1=1");
            for (final String key : data.keySet()) {
                send.append("&");
                send.append(key);
                send.append("=");
                send.append(URLEncoder.encode(data.get(key), "UTF-8"));
            }
            final URL url = new URL("http://www.rpg-studio.de/libraries/abttools/yaamsFeedback.php");
            final URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            final OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(send.toString());
            wr.flush();
            final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            final StringBuffer erg = new StringBuffer("");
            while ((line = rd.readLine()) != null) {
                erg.append(line);
            }
            JOptionPane.showMessageDialog(null, erg.toString(), I18N.t("Feedback"), JOptionPane.INFORMATION_MESSAGE);
            wr.close();
            rd.close();
        } catch (final Throwable t) {
            YException.error("Can not send feedback to http://www.rpg-studio.de/libraries/abttools/yaamsFeedback.php", t);
        }
        y.close();
    }
}
