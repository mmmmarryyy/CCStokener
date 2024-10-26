package phex.update;

import java.io.*;
import java.net.*;
import java.util.*;
import phex.common.*;
import phex.event.*;
import phex.utils.*;

/**
 *
 * @todo The UpdateChecker stays this way even after integrating build numbers.
 * The main reason for this is that we like to display changes on an update check.
 * Because we don't like to download a complete change log and only display changes
 * of new builds a more complex algorithm would be needed (maybe on webserver side).
 * Also we like the user to be able to select if he only likes to see final releases
 * or also beta versions.
 * @author Gregor Koukkoullis
 *
 */
public class UpdateChecker {

    private static final String RELEASE_VERSION_KEY = "releaseVersion";

    private static final String BETA_VERSION_KEY = "betaVersion";

    private static final long ONE_WEEK_MILLIS = 1000 * 60 * 1440 * 7;

    private static final String versionURL = "http://phex.kouk.de/version.properties";

    private String releaseVersion;

    private String betaVersion;

    /**
     * A possible exception that might occure during the update check.
     */
    private Throwable updateCheckError;

    private UpdateNotificationListener listener;

    /**
     * Creates a new UpdateChecker and starts the update check prozess in a new
     * thread. The call returns immediatly and prozess the update check in the
     * background.
     * @param updateListener the listener that gets notified about a update.
     */
    public static void checkForUpdates(UpdateNotificationListener updateListener) {
        UpdateChecker checker = new UpdateChecker(updateListener);
        checker.checkForUpdate();
    }

    public UpdateChecker(UpdateNotificationListener updateListener) {
        listener = updateListener;
    }

    public void setShowUpdateNotification(boolean state) {
        if (ServiceManager.sCfg.showUpdateNotification != state) {
            ServiceManager.sCfg.showUpdateNotification = state;
            ServiceManager.sCfg.save();
        }
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getBetaVersion() {
        return betaVersion;
    }

    /**
     * Returns a possible Throwable that could be thrown during the update check
     * or null if no error was caught.
     * @return a possible Throwable that could be thrown during the update check
     * or null if no error was caught.
     */
    public Throwable getUpdateCheckError() {
        return updateCheckError;
    }

    /**
     * Checks for updates in a new thread. The method returns immediatly.
     */
    public void checkForUpdate() {
        if (ServiceManager.sCfg.lastUpdateCheckTime > System.currentTimeMillis() - ONE_WEEK_MILLIS) {
            return;
        }
        Thread thread = new Thread(ThreadTracking.rootThreadGroup, new UpdateCheckRunner(), "UpdateCheckRunner");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Checks for updates in a new thread. The method waits till the check is
     * finished.
     */
    public void checkForUpdateAndWait() {
        if (ServiceManager.sCfg.lastUpdateCheckTime > System.currentTimeMillis() - ONE_WEEK_MILLIS) {
            return;
        }
        Thread thread = new Thread(ThreadTracking.rootThreadGroup, new UpdateCheckRunner(), "UpdateCheckRunner");
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException exp) {
            Logger.logWarning(exp);
        }
    }

    private void fireUpdateNotification() {
        listener.updateNotification(this);
    }

    private class UpdateCheckRunner implements Runnable {

        public void run() {
            try {
                performUpdateCheck();
            } catch (Throwable exp) {
                updateCheckError = exp;
            }
        }

        public void performUpdateCheck() {
            URL url;
            Properties props;
            try {
                url = new URL(versionURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches(false);
                connection.setRequestProperty("User-Agent", Environment.getPhexVendor());
                InputStream inStream = connection.getInputStream();
                props = new Properties();
                props.load(inStream);
            } catch (java.net.MalformedURLException exp) {
                updateCheckError = exp;
                Logger.logError(exp);
                throw new RuntimeException();
            } catch (UnknownHostException exp) {
                updateCheckError = exp;
                return;
            } catch (SocketException exp) {
                updateCheckError = exp;
                return;
            } catch (IOException exp) {
                updateCheckError = exp;
                Logger.logWarning(exp);
                return;
            }
            ServiceManager.sCfg.lastUpdateCheckTime = System.currentTimeMillis();
            if (!ServiceManager.sCfg.showUpdateNotification) {
                ServiceManager.sCfg.save();
                return;
            }
            releaseVersion = props.getProperty(RELEASE_VERSION_KEY, "0");
            if (ServiceManager.sCfg.showBetaUpdateNotification) {
                betaVersion = props.getProperty(BETA_VERSION_KEY, "0");
            } else {
                betaVersion = "0";
            }
            int releaseCompare = VersionUtils.compare(releaseVersion, VersionUtils.getProgramVersion());
            int betaCompare = VersionUtils.compare(betaVersion, VersionUtils.getProgramVersion());
            if (releaseCompare <= 0 && betaCompare <= 0) {
                ServiceManager.sCfg.save();
                return;
            }
            releaseCompare = VersionUtils.compare(releaseVersion, ServiceManager.sCfg.lastUpdateCheckVersion);
            betaCompare = VersionUtils.compare(betaVersion, ServiceManager.sCfg.lastBetaUpdateCheckVersion);
            int verDiff = VersionUtils.compare(betaVersion, releaseVersion);
            boolean triggerUpdateNotification = false;
            if (releaseCompare > 0) {
                ServiceManager.sCfg.lastUpdateCheckVersion = releaseVersion;
                triggerUpdateNotification = true;
            }
            if (betaCompare > 0) {
                ServiceManager.sCfg.lastBetaUpdateCheckVersion = betaVersion;
                triggerUpdateNotification = true;
            }
            if (verDiff > 0) {
                releaseVersion = null;
            } else {
                betaVersion = null;
            }
            ServiceManager.sCfg.save();
            if (triggerUpdateNotification) {
                fireUpdateNotification();
            }
        }
    }
}
