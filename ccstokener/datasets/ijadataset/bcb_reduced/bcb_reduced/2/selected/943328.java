package net.sf.gridarta.updater;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import net.sf.gridarta.MainControl;
import net.sf.gridarta.gui.dialog.prefs.NetPreferences;
import net.sf.gridarta.utils.ActionBuilderUtils;
import net.sf.gridarta.utils.Exiter;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class handles updating the map editor.
 * @author <a href="mailto:cher@riedquat.de">Christian.Hujer</a>
 * @fixme the updater fails on windows, the user is notified of this but still
 * it isn't nice. The updater should be a separate application.
 * @todo move the updater to JAPI
 */
public class Updater implements Runnable {

    /**
     * Action Builder to create Actions.
     */
    @NotNull
    private static final ActionBuilder ACTION_BUILDER = ActionBuilderFactory.getInstance().getActionBuilder("net.sf.gridarta");

    /**
     * Logger.
     */
    @NotNull
    private static final Category log = Logger.getLogger(Updater.class);

    /**
     * Preferences.
     */
    @NotNull
    private static final Preferences preferences = Preferences.userNodeForPackage(MainControl.class);

    /**
     * Preferences key for last update.
     */
    @NotNull
    public static final String LAST_UPDATE_KEY = "UpdateTimestamp";

    /**
     * The parentComponent to show dialogs on.
     */
    @Nullable
    private final Component parentComponent;

    /**
     * The {@link Exiter} for terminating the application.
     */
    @NotNull
    private final Exiter exiter;

    /**
     * The file to update.
     */
    @NotNull
    private final String updateFileName;

    /**
     * Buffer size.
     */
    private static final int BUF_SIZE = 4096;

    /**
     * Create a new instance.
     * @param parentComponent the parent component to show dialogs on
     * @param exiter the exiter for terminating the application
     * @param updateFileName the file to update
     */
    public Updater(@Nullable final Component parentComponent, @NotNull final Exiter exiter, @NotNull final String updateFileName) {
        this.parentComponent = parentComponent;
        this.exiter = exiter;
        this.updateFileName = updateFileName;
        if (parentComponent != null) {
            parentComponent.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            final String propUrl = ACTION_BUILDER.getString("update.url");
            if (propUrl == null) {
                return;
            }
            final InputStream pin = openStream(propUrl);
            try {
                final ResourceBundle updateBundle = new PropertyResourceBundle(pin);
                final String downloadUrl = updateBundle.getString("update.url");
                if (downloadUrl == null) {
                    ACTION_BUILDER.showMessageDialog(parentComponent, "updateError", "invalid server response: update.url is missing");
                    return;
                }
                final VersionInfo update = new VersionInfo(updateBundle, "update");
                VersionInfo active = VersionInfo.UNAVAILABLE;
                try {
                    active = new VersionInfo(ResourceBundle.getBundle("build"), "build");
                } catch (final MissingResourceException e) {
                    ACTION_BUILDER.showMessageDialog(parentComponent, "updateActiveVersionUnavailable");
                }
                preferences.putLong(LAST_UPDATE_KEY, System.currentTimeMillis());
                if (active == null || update.isNewerThan(active)) {
                    if (askIfUserWantsUpdate(active, update, propUrl, downloadUrl)) {
                        downloadAndInstallUpdate(downloadUrl);
                    }
                } else {
                    noNewUpdate(active, update, propUrl, downloadUrl);
                }
            } finally {
                pin.close();
            }
        } catch (final UnknownHostException e) {
            ACTION_BUILDER.showMessageDialog(parentComponent, "updateError", e.getLocalizedMessage());
        } catch (final IOException e) {
            ACTION_BUILDER.showMessageDialog(parentComponent, "updateError", e);
        } finally {
            if (parentComponent != null) {
                parentComponent.setEnabled(true);
            }
        }
    }

    /**
     * Ask the user whether he wants to update.
     * @param active VersionInfo of currently installed version
     * @param update VersionInfo of available update version
     * @param propUrl URL where properties were downloaded from
     * @param downloadUrl URL where the update would be downloaded from
     * @return <code>true</code> if the user chose that he wants to update,
     *         otherwise <code>false</code>
     */
    private boolean askIfUserWantsUpdate(@Nullable final VersionInfo active, @NotNull final VersionInfo update, @NotNull final String propUrl, @NotNull final String downloadUrl) {
        return ACTION_BUILDER.showConfirmDialog(parentComponent, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, "updateAvailable", updateFileName, active == null ? "?" : active.version, update.version, active == null ? "?" : active.developer, update.developer, active == null ? "?" : active.timestamp, update.timestamp, propUrl, downloadUrl) == JOptionPane.YES_OPTION;
    }

    /**
     * Tell the user there is no update.
     * @param active VersionInfo of currently installed version
     * @param update VersionInfo of available update version
     * @param propUrl URL where properties were downloaded from
     * @param downloadUrl URL where the update would be downloaded from
     */
    private void noNewUpdate(@Nullable final VersionInfo active, @NotNull final VersionInfo update, @NotNull final String propUrl, @NotNull final String downloadUrl) {
        ACTION_BUILDER.showMessageDialog(parentComponent, "updateUnavailable", active == null ? "?" : active.version, update.version, active == null ? "?" : active.developer, update.developer, active == null ? "?" : active.timestamp, update.timestamp, propUrl, downloadUrl);
    }

    /**
     * Download and install an update.
     * @param url URL to get update from
     */
    private void downloadAndInstallUpdate(@NotNull final String url) {
        final File download = new File(updateFileName + ".tmp");
        final File backup = new File(updateFileName + ".bak");
        final File orig = new File(updateFileName);
        try {
            final InputStream in = openStream(url);
            try {
                final OutputStream out = new FileOutputStream(download);
                try {
                    final byte[] buf = new byte[BUF_SIZE];
                    while (true) {
                        final int bytesRead = in.read(buf);
                        if (bytesRead == -1) {
                            break;
                        }
                        out.write(buf, 0, bytesRead);
                    }
                    out.close();
                    if (!orig.renameTo(backup)) {
                        ACTION_BUILDER.showMessageDialog(parentComponent, "updateFailedNoBackup", updateFileName);
                    } else if (!download.renameTo(orig)) {
                        backup.renameTo(orig);
                        ACTION_BUILDER.showMessageDialog(parentComponent, "updateFailedNoDownload");
                    } else {
                        ACTION_BUILDER.showMessageDialog(parentComponent, "updateRestart", updateFileName);
                        exiter.doExit(0);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (final InterruptedIOException e) {
            ACTION_BUILDER.showMessageDialog(parentComponent, "updateAborted");
        } catch (final Exception e) {
            log.warn("updateError", e);
            ACTION_BUILDER.showMessageDialog(parentComponent, "updateError", e);
        }
    }

    /**
     * Opens an InputStream on a URL.
     * @param url the URL to open InputStream on
     * @return the InputStream for URL
     * @throws IOException in case of I/O problems
     */
    @NotNull
    private InputStream openStream(@NotNull final String url) throws IOException {
        final Proxy proxy = NetPreferences.getProxy();
        final URLConnection con = new URL(url).openConnection(proxy);
        final ProgressMonitorInputStream stream = new ProgressMonitorInputStream(parentComponent, ActionBuilderUtils.getString(ACTION_BUILDER, "updateProgress.title"), con.getInputStream());
        final ProgressMonitor monitor = stream.getProgressMonitor();
        monitor.setMaximum(con.getContentLength());
        monitor.setNote(ActionBuilderUtils.getString(ACTION_BUILDER, "updateProgress"));
        monitor.setMillisToDecideToPopup(10);
        monitor.setMillisToPopup(10);
        return stream;
    }

    /**
     * Class for holding version information and quickly comparing it.
     */
    private static class VersionInfo {

        /**
         * Update information: Version of update version, usually the build
         * number.
         */
        @NotNull
        private final String version;

        /**
         * Update information: Time stamp of update version.
         */
        @NotNull
        private final String timestamp;

        /**
         * Update information: Developer that created the update version.
         */
        @NotNull
        private final String developer;

        /**
         * Special Version "unavailable".
         */
        @NotNull
        private static final VersionInfo UNAVAILABLE = new VersionInfo();

        /**
         * Private constructor used for unavailable versions.
         */
        private VersionInfo() {
            this("unavailable", "unavailable", "unavailable");
        }

        /**
         * Private constructor to map the strings.
         * @param version the version
         * @param timestamp the timestamp
         * @param developer the developer
         */
        private VersionInfo(@NotNull final String version, @NotNull final String timestamp, @NotNull final String developer) {
            this.version = version;
            this.timestamp = timestamp;
            this.developer = developer;
        }

        /**
         * Create update information from a ResourceBundle. The ResourceBundle
         * should have Strings for <var>prefix</var> + <code>.number</code>,
         * <code>.tstamp</code> and <code>.developer</code>.
         * @param bundle ResourceBundle to create update information from
         * @param prefix Prefix for update information within the resource
         * bundle
         */
        VersionInfo(@NotNull final ResourceBundle bundle, @NotNull final String prefix) {
            this(bundle.getString(prefix + ".number"), bundle.getString(prefix + ".tstamp"), bundle.getString(prefix + ".developer"));
        }

        /**
         * Check whether this version is newer than another version.
         * @param other Other version information to compare to
         * @return <code>true</code> if this version is newer than
         *         <var>other</var>, otherwise <code>false</code>
         */
        @SuppressWarnings("ObjectEquality")
        boolean isNewerThan(@NotNull final VersionInfo other) {
            return this != UNAVAILABLE && (other == UNAVAILABLE || timestamp.compareTo(other.timestamp) > 0);
        }
    }
}
