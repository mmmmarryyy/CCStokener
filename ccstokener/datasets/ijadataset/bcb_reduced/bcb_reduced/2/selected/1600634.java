package com.trollworks.ttk.utility;

import com.trollworks.ttk.preferences.Preferences;
import com.trollworks.ttk.text.Version;
import com.trollworks.ttk.widgets.WindowUtils;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;

/** Provides a background check for updates. */
public class UpdateChecker implements Runnable {

    private static String MSG_CHECKING;

    private static String MSG_UP_TO_DATE;

    private static String MSG_OUT_OF_DATE;

    private static String MSG_UPDATE_TITLE;

    private static String MSG_IGNORE_TITLE;

    private static final String MODULE = "Updates";

    private static final String LAST_VERSION_KEY = "LastVersionSeen";

    private static boolean NEW_VERSION_AVAILABLE = false;

    private static String RESULT;

    private static String UPDATE_URL;

    private String mProductKey;

    private String mCheckURL;

    private int mMode;

    static {
        LocalizedMessages.initialize(UpdateChecker.class);
        RESULT = MSG_CHECKING;
    }

    /**
	 * Initiates a check for updates.
	 * 
	 * @param productKey The product key to check for.
	 * @param checkURL The URL to use for checking whether a new version is available.
	 * @param updateURL The URL to use when going to the update site.
	 */
    public static void check(String productKey, String checkURL, String updateURL) {
        Thread thread = new Thread(new UpdateChecker(productKey, checkURL), UpdateChecker.class.getSimpleName());
        UPDATE_URL = updateURL;
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    /** @return Whether a new version is available. */
    public static boolean isNewVersionAvailable() {
        return NEW_VERSION_AVAILABLE;
    }

    /** @return The result. */
    public static String getResult() {
        return RESULT;
    }

    /** Go to the update location on the web, if a new version is available. */
    public static void goToUpdate() {
        if (NEW_VERSION_AVAILABLE && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(UPDATE_URL));
            } catch (Exception exception) {
                WindowUtils.showError(null, exception.getMessage());
            }
        }
    }

    private UpdateChecker(String productKey, String checkURL) {
        mProductKey = productKey;
        mCheckURL = checkURL;
    }

    @Override
    public void run() {
        if (mMode == 0) {
            long currentVersion = Version.extractVersion(App.getVersion());
            if (currentVersion == 0) {
                mMode = 2;
                RESULT = MSG_UP_TO_DATE;
                return;
            }
            long versionAvailable = currentVersion;
            mMode = 2;
            try {
                StringBuilder buffer = new StringBuilder(mCheckURL);
                try {
                    NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                    if (!ni.isLoopback()) {
                        if (ni.isUp()) {
                            if (!ni.isVirtual()) {
                                buffer.append('?');
                                byte[] macAddress = ni.getHardwareAddress();
                                for (byte one : macAddress) {
                                    buffer.append(Integer.toHexString(one >>> 4 & 0xF));
                                    buffer.append(Integer.toHexString(one & 0xF));
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                }
                URL url = new URL(buffer.toString());
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = in.readLine();
                while (line != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line, "\t");
                    if (tokenizer.hasMoreTokens()) {
                        try {
                            if (tokenizer.nextToken().equalsIgnoreCase(mProductKey)) {
                                String token = tokenizer.nextToken();
                                long version = Version.extractVersion(token);
                                if (version > versionAvailable) {
                                    versionAvailable = version;
                                }
                            }
                        } catch (Exception exception) {
                        }
                    }
                    line = in.readLine();
                }
            } catch (Exception exception) {
            }
            if (versionAvailable > currentVersion) {
                Preferences prefs = Preferences.getInstance();
                String humanReadableVersion = Version.getHumanReadableVersion(versionAvailable);
                NEW_VERSION_AVAILABLE = true;
                RESULT = MessageFormat.format(MSG_OUT_OF_DATE, humanReadableVersion);
                if (versionAvailable > Version.extractVersion(prefs.getStringValue(MODULE, LAST_VERSION_KEY, App.getVersion()))) {
                    prefs.setValue(MODULE, LAST_VERSION_KEY, humanReadableVersion);
                    prefs.save();
                    mMode = 1;
                    EventQueue.invokeLater(this);
                    return;
                }
            } else {
                RESULT = MSG_UP_TO_DATE;
            }
        } else if (mMode == 1) {
            if (App.isNotificationAllowed()) {
                String result = getResult();
                mMode = 2;
                if (WindowUtils.showConfirmDialog(null, result, MSG_UPDATE_TITLE, JOptionPane.OK_CANCEL_OPTION, new String[] { MSG_UPDATE_TITLE, MSG_IGNORE_TITLE }, MSG_UPDATE_TITLE) == JOptionPane.OK_OPTION) {
                    goToUpdate();
                }
            } else {
                DelayedTask.schedule(this, 250);
            }
        }
    }
}
