package net.jetrix.services;

import java.awt.TrayIcon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import net.jetrix.SystrayManager;
import net.jetrix.config.ServerConfig;

/**
 * Service checking the availability of a new release.
 *
 * @since 0.2
 * 
 * @author Emmanuel Bourg
 * @version $Revision: 794 $, $Date: 2009-02-17 14:08:39 -0500 (Tue, 17 Feb 2009) $
 */
public class VersionService extends CronService {

    /** The latest stable version known. */
    private static String latestVersion;

    public VersionService() {
        setPattern("0 0 * * * *");
    }

    public String getName() {
        return "Version Checker";
    }

    protected void run() {
        updateLatestVersion();
        if (isNewVersionAvailable()) {
            String message = "A new version is available (" + VersionService.getLatestVersion() + "), download it on http://jetrix.sf.net now!";
            log.warning(message);
            SystrayManager.notify(message, TrayIcon.MessageType.INFO);
        }
    }

    /**
     * Return the version of the latest release. The version of the last stable
     * release is stored on the Jetrix site (http://jetrix.sf.net/version.php),
     * this file is build dynamically everyday and reuse the version specified
     * in the project.properties file.
     */
    private static String fetchLatestVersion() {
        String version = null;
        try {
            URL url = new URL("http://jetrix.sourceforge.net/version.php");
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                version = in.readLine();
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to check the availability of a new version", e);
        }
        return version;
    }

    /**
     * Update the latest stable version known.
     */
    public static void updateLatestVersion() {
        latestVersion = fetchLatestVersion();
    }

    /**
     * Return the latest stable version known.
     */
    public static String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Check if the latest stable version is more recent than the current version.
     */
    public static boolean isNewVersionAvailable() {
        return latestVersion != null && ServerConfig.VERSION.compareTo(latestVersion) < 0;
    }
}
