package de.radis.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import de.radis.util.Helper;

public class CheckForUpdates {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
	 * This method asynchronously checks for an update for RadiS if the update
	 * check fails,
	 */
    public static void check() {
        executor.submit(new UpdateChecker());
    }

    private static class UpdateChecker implements Runnable {

        @Override
        public void run() {
            if (Helper.getXmlHandler().getCheckUpdates()) {
                BufferedReader br;
                InputStreamReader isr;
                try {
                    isr = new InputStreamReader(CheckForUpdates.class.getClassLoader().getResource("Resources/update.upd").openStream());
                    br = new BufferedReader(isr);
                    String currentVersion = br.readLine();
                    URL url = new URL("http://radis.sf.net/update.upd");
                    isr = new InputStreamReader(url.openStream());
                    br = new BufferedReader(isr);
                    String serverVersion = br.readLine();
                    if (Integer.parseInt(currentVersion) < Integer.parseInt(serverVersion)) showDialog();
                } catch (IOException e) {
                    Helper.log().warn("Update check failed. Exception: ", e);
                } catch (Exception e) {
                    Helper.log().error("Update check failed in an unexpected way. Exception: ", e);
                }
            }
        }

        private void showDialog() {
            int result = JOptionPane.showConfirmDialog(null, "There is a newer version of RadiS available. Do you want to open the download page?", "Question", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    String theUrl = "http://sourceforge.net/project/platformdownload.php?group_id=269346";
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + theUrl);
                } catch (Exception e) {
                    Helper.log().error("Could not open default browser! Exception: ", e);
                }
            }
        }
    }
}
