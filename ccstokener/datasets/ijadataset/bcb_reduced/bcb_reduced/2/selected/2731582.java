package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.swing.SwingWorker;
import display.Display;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * The Version class handles version checking for iTunesDSM.
 *
 * The class polls a webserver for the most up to date version of iTunesDSM.
 *
 * @author Brian Gibowski brian@brgib.com
 */
public class Version extends SwingWorker<Void, Void> {

    private static Logger log = Logger.getLogger(Version.class.getName());

    private static final int MAJOR_VERSION = 0;

    private static final int MINOR_VERSION = 9;

    private static final int BUG_FIX_VERSION = 6;

    private static final int ITUNES_MAJOR_VERSION = 9;

    private static final int ITUNES_MINOR_VERSION = 0;

    private static final int ITUNES_BUG_FIX_VERSION = 3;

    /**
     * Returns the current Version object of the iTunesDSM program.
     */
    public static final Version ITUNESDSM_VERSION = new Version(MAJOR_VERSION, MINOR_VERSION, BUG_FIX_VERSION);

    private static String ONLINE_FILE_LOCATION = "http://itunesdsm.sourceforge.net/version.dat";

    private static String CURRENT_VERSION_COMMENT = "Current Version:";

    private int majorVersion;

    private int minorVersion;

    private int bugFixVersion;

    private String versionDetails = "";

    private boolean showUpToDateDialog = true;

    /**
     * Creates a new Version object where showUpToDataDialog is set to true, i.e.
     * a dialog box will confirm the user is using the most up to date version
     * of iTunesDSM.
     */
    public Version() {
        this(true);
    }

    /**
     * Creates a new Version object with the option of showing
     * the dialog that confirms the newest version of iTunesDSM.
     *
     * @param showUpToDateDialog
     */
    public Version(boolean showUpToDateDialog) {
        this.showUpToDateDialog = showUpToDateDialog;
    }

    /**
     * Creates a new Version object given the major, minor, and bug
     * fix integers.
     *
     * @param major The major version number.
     *
     * @param minor The minor version number.
     *
     * @param bugfix The bugfix version number.
     */
    public Version(int major, int minor, int bugfix) {
        majorVersion = major;
        minorVersion = minor;
        bugFixVersion = bugfix;
    }

    /**
     * The SwingWorker doInBackground method downloads information on the latest
     * release and compares that with the current release.  If a new version is
     * available the user is able to open the iTunesDSM blog homepage.
     * 
     * @return Boolean Whether or not there is an update to iTunesDSM.
     */
    @Override
    public Void doInBackground() {
        Version v = getLatestITDSMVersion();
        if (v != null) {
            if (!isUpToDate(v)) {
                String message = "A new version of iTunesDSM is available.  Would you like to " + "download it from the iTunesDSM homepage?";
                if (versionDetails.length() > 0) {
                    message += "\nNew Features:\n" + versionDetails;
                }
                int n = Display.newYesNoOptionMessage("New Version " + v.toString() + " Available", message);
                if (n == JOptionPane.YES_OPTION) {
                    Display.openWebPage(Display.HOMEPAGE_URL);
                }
            } else {
                if (showUpToDateDialog) {
                    Display.newInfoMessage(("No new updates"), "This version of iTunesDSM is up to date.");
                }
            }
        } else {
            Display.newWarningMessage("Unable to connect...", "iTunesDSM was unable to connect to the update server." + "  Please check your internet connection or " + "visit the iTunesDSM homepage in the Help menu.");
        }
        return null;
    }

    @Override
    public void done() {
        log.log(Level.FINE, "Finished Checking up to date version");
    }

    /**
     * Returns version details.
     *
     * @return The version details.
     */
    public String getVersionDetails() {
        return versionDetails;
    }

    /**
     * Determines whether this Version object is an newer version than the given
     * olderVersion object.
     *
     * @param onlineVersion The proposed Version object.
     *
     * @return If true, this version is an old version compared to the given
     * version.  Otherwise, this version is the same or newer than the given
     * Version.
     */
    public boolean isUpToDate(Version onlineVersion) {
        int[] thisVersion = { MAJOR_VERSION, MINOR_VERSION, BUG_FIX_VERSION };
        int[] onlineV = { onlineVersion.getMajorVersion(), onlineVersion.getMinorVersion(), onlineVersion.getBugFixVersion() };
        for (int i = 0; i < thisVersion.length; i++) {
            if (thisVersion[i] < onlineV[i]) {
                return false;
            } else if (thisVersion[i] > onlineV[i]) {
                return true;
            }
        }
        return true;
    }

    /**
     * Reads the newest version information from a file on a webserver.
     * 
     * @return The latest version information from a webserver.
     */
    public Version getLatestITDSMVersion() {
        BufferedReader in = null;
        String latestVersion = null;
        Version v = null;
        try {
            URL url = new URL(ONLINE_FILE_LOCATION);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals(CURRENT_VERSION_COMMENT)) {
                    latestVersion = in.readLine();
                    v = parseVersion(latestVersion);
                } else if (inputLine.length() > 0) {
                    versionDetails += inputLine + "\n";
                }
            }
            log.log(Level.FINE, this.getClass().getName() + "\nVersion string parsed successfully");
        } catch (UnknownHostException e) {
            log.log(Level.SEVERE, this.getClass().getName(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                System.out.println("Unable to close BufferedReader object in Version class " + e);
                log.log(Level.SEVERE, this.getClass().getName(), e);
            }
            return v;
        }
    }

    /**
     * Returns the bug fix version.  For example in the
     * X.Y.Z version string, the Z is returned.
     *
     * @return The bug fix integer of a version.  For example, in the
     * version X.Y.Z, Z is returned.
     */
    public int getBugFixVersion() {
        return bugFixVersion;
    }

    /**
     * Sets the bug fix version integer.
     *
     * @param bugFixVersion The bug fix integer to set the version's
     * bug fix integer to.
     */
    public void setBugFixVersion(int bugFixVersion) {
        this.bugFixVersion = bugFixVersion;
    }

    /**
     * Returns the version's major version integer.  In a version string
     * X.Y.Z, X is returned.
     *
     * @return The version's major version integer.  In a version string
     * X.Y.Z, X is returned.
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Sets the verion's major version integer.
     *
     * @param majorVersion The major versions integer to set the version to.
     * In the version string X.Y.Z, X will be set to majorVersion.
     *
     */
    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * Returns the version's minor version integer.  In the version
     * string X.Y.Z, Y is returned.
     *
     * @return The version's minor version integer.  In the version string
     * X.Y.Z, Y is returned.
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Sets the version's minor version number.  In the version
     * string X.Y.Z, Y is set to the given integer.
     *
     * @param minorVersion The integer to set the version's
     * minor version integer to.
     */
    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * Tests whether or not to the version object is equal to another
     * version object.
     *
     * @param testVersion The version object to test against.
     *
     * @return Whether or not the two version objects are equal.
     */
    public boolean equals(Version testVersion) {
        if (this == null || testVersion == null) {
            throw new NullPointerException("Version is null");
        }
        if (majorVersion == testVersion.getMajorVersion() && minorVersion == testVersion.getMinorVersion() && bugFixVersion == testVersion.getBugFixVersion()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parses a version string.  A valid version String is of the form
     * X.Y.Z where X,Y, and Z are positive integers seperated by a '.'.
     *
     * @param versionString The string to parse the version numbers from.
     *
     * @return The Version object parsed by the given string.
     */
    public static Version parseVersion(String versionString) {
        log.log(Level.FINE, "parseVersion called with string:  " + versionString);
        if (versionString == null) {
            NullPointerException e = new NullPointerException("Version string to parse is null");
            log.log(Level.SEVERE, Version.class.getName(), e);
            throw e;
        }
        if (versionString.length() == 0) {
            IllegalArgumentException e = new IllegalArgumentException("" + "The version string to parse do not contain any characters" + " to parse");
            log.log(Level.SEVERE, Version.class.getName(), e);
            throw e;
        }
        String[] versions = versionString.split("\\.");
        if (versions.length != 3) {
            IllegalArgumentException e = new IllegalArgumentException("The version string was not " + "properly formatted i.e. X.Y.Z");
            log.log(Level.SEVERE, Version.class.getName(), e);
            throw e;
        }
        int major;
        int minor;
        int bug;
        try {
            major = Integer.parseInt(versions[0]);
            minor = Integer.parseInt(versions[1]);
            bug = Integer.parseInt(versions[2]);
            if (major < 0 || minor < 0 || bug < 0) {
                IllegalArgumentException e = new IllegalArgumentException("The versions number parsed were negative numbers");
                log.log(Level.SEVERE, Version.class.getName(), e);
                throw e;
            }
            log.log(Level.FINE, "Parsed version is " + new Version(major, minor, bug).toString());
            return new Version(major, minor, bug);
        } catch (NumberFormatException e) {
            log.log(Level.SEVERE, Version.class.getName(), e);
            throw e;
        }
    }

    /**
     * Returns a formatted String object of the version object.
     *
     * @return A formatted String object of the version object.
     */
    @Override
    public String toString() {
        return majorVersion + "." + minorVersion + "." + bugFixVersion;
    }

    public static void main(String[] args) {
    }
}
