package marla.ide.resource;

import marla.ide.gui.Domain;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import marla.ide.problem.MarlaException;

/**
 * Checks if maRla should be updated
 * @author Ryan Morehart
 */
public class Updater implements Runnable {

    /**
	 * URL to download updater from
	 */
    private final String updateURL;

    /**
	 * Becomes true when maRla exits
	 */
    private static boolean hasExited = false;

    /**
	 * Creates a new updater pointed at the given update server 
	 * @param updateLocation URL which contains the current revision number
	 */
    private Updater(String updateLocation) {
        updateURL = updateLocation;
    }

    /**
	 * Tell updater that maRla has exited and can be updated
	 */
    public static void notifyExit() {
        hasExited = true;
    }

    /**
	 * Checks for and updates components of maRla if possible
	 * @return true if an update is available (download will start in background
	 *		automatically and launch when maRla exits)
	 */
    public static boolean checkForUpdates() {
        String latestRev = Configuration.fetchSettingFromServer("REV");
        if (latestRev == null) {
            System.out.println("Unable to check for updates");
            return false;
        }
        int rev = Integer.parseInt(latestRev);
        if (rev <= Integer.parseInt(BuildInfo.revisionNumber)) {
            System.out.println("maRla is up-to-date");
            return false;
        }
        return true;
    }

    /**
	 * Gets the give file and returns the path to it
	 * @param urlLocation URL to fetch
	 * @return path to downloaded file
	 */
    private static String fetchFile(String urlLocation) {
        try {
            URL url = new URL(urlLocation);
            URLConnection conn = url.openConnection();
            File tempFile = File.createTempFile("marla", ".jar");
            OutputStream os = new FileOutputStream(tempFile);
            IOUtils.copy(conn.getInputStream(), os);
            return tempFile.getAbsolutePath();
        } catch (IOException ex) {
            throw new MarlaException("Unable to fetch file '" + urlLocation + "' from server", ex);
        }
    }

    /**
	 * Downloads update file and runs it once maRla exits
	 */
    @Override
    public void run() {
        try {
            String updateFile = fetchFile(updateURL);
            while (!hasExited) Thread.sleep(3000);
            Thread.sleep(10);
            runUpdateJar(updateFile);
        } catch (MarlaException ex) {
            Domain.logger.add(ex);
        } catch (InterruptedException ex) {
            Domain.logger.add(ex);
        }
    }

    /**
	 * Runs update jar, if applicable
	 */
    public void runUpdateJar(String updateJar) {
        try {
            System.out.print("Begining maRla update...");
            ProcessBuilder proc = new ProcessBuilder("java -jar " + updateJar);
            proc.start();
            new File(updateJar).delete();
            System.out.println("complete");
        } catch (IOException ex) {
            Domain.logger.add(ex);
            ex.printStackTrace();
        }
    }
}
