package de.mordred.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import de.mordred.Loader;
import de.mordred.Main;

public class VersionCheck {

    public static void checkUpdate() {
        String result = postVersionToWebsite();
        int comparison = Version.VERSION.compareToIgnoreCase(result);
        if (comparison < 0) {
            String message = "<html>Es ist eine neuere Version(" + result + ") verfügbar<br>" + "http://jphysikum.googlecode.com</html>";
            ImageIcon ico = Loader.getImageIcon("Doctor.png");
            JOptionPane.showMessageDialog(Main.getHauptfenster(), message, "Update", JOptionPane.INFORMATION_MESSAGE, ico);
        }
    }

    public static String postVersionToWebsite() {
        try {
            String data = "version=version";
            URL url = new URL("http://jphysikum.lima-city.de/version.php");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                return line;
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
