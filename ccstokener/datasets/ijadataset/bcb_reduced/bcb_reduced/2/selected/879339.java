package org.cvj.mirror;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class will contain information about the version of this
 * release of CvJMirror, as well as utility methods associated with
 * the version.
 * 
 * @author Charl van Jaarsveldt
 */
public class Version implements Comparable {

    public static String TAG_FINAL_STRING = "";

    public static String TAG_ALPHA_STRING = "Alpha";

    public static String TAG_BETA_STRING = "Beta";

    public static int TAG_FINAL = 3;

    public static int TAG_BETA = 2;

    public static int TAG_ALPHA = 1;

    private static int MAJOR = 1;

    private static int MINOR = 3;

    private static int TAG = TAG_FINAL;

    private static Version latest = null;

    private int major, minor, tag;

    /**
     * Creates a new <code>Version</code> instance with the current
     * version of CvJMirror.
     */
    public Version() {
        this(MAJOR, MINOR, TAG);
    }

    /**
     * Creates a new <code>Version</code> instance with the given 
     * "final" version.
     * @param major
     * @param minor
     */
    public Version(int major, int minor) {
        this(major, minor, TAG_FINAL);
    }

    /**
     * Creates a new <code>Version</code> instance with the given version.
     * @param major
     * @param minor
     * @param tag Can be Alpha, beta, or nothing.
     */
    public Version(int major, int minor, int tag) {
        this.major = major;
        this.minor = minor;
        this.tag = tag;
    }

    /**
     * 
     * @return The Major version number.
     */
    public int getMajor() {
        return major;
    }

    /**
     * 
     * @return The Minor version number.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * 
     * @return The version tag (Alpha Beta or blank for final.
     */
    public int getTag() {
        return tag;
    }

    /**
     * 
     * @return A string representation of the Version.
     */
    @Override
    public String toString() {
        String s = major + "." + minor;
        if (tag == TAG_BETA) {
            s = s + " Beta";
        } else if (tag == TAG_ALPHA) {
            s = s + " Alpha";
        }
        return s;
    }

    /**
     * This fetches the latest released version of CvJMirror from 
     * the sourceforge website.
     * @return The latest version of CvJMirror
     */
    public static Version getLatestVersion() {
        if (latest != null) return latest;
        try {
            URL url = new URL("http://cvjmirror.sourceforge.net/version.txt");
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            int iMajor = Integer.parseInt(in.readLine().trim());
            int iMinor = Integer.parseInt(in.readLine().trim());
            int iTag = 0;
            try {
                iTag = Integer.parseInt(in.readLine().trim());
            } catch (Exception ex) {
            }
            latest = new Version(iMajor, iMinor, iTag);
        } catch (MalformedURLException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return latest;
    }

    /**
     * Checks if the currently running instance of CvJMirror is the latest
     * version according to the latest release info on Sourceforge.
     * @return <code>true</code> if a new version is available.
     */
    public static boolean isNewVersionAvailable() {
        boolean newerAvailable = false;
        Version current = new Version();
        getLatestVersion();
        if (current.compareTo(latest) < 0) {
            newerAvailable = true;
        }
        return newerAvailable;
    }

    /**
     * Compares this version with the given one.
     * @param version
     * @return 0 if equal, -1 if smaller, +1 if bigger.
     */
    public int compareTo(Object version) {
        if (!(version instanceof Version)) {
            return -1;
        }
        Version ver = (Version) version;
        if (major > ver.getMajor()) {
            return +1;
        }
        if (major < ver.getMajor()) {
            return -1;
        }
        if (minor > ver.getMinor()) {
            return +1;
        }
        if (minor < ver.getMinor()) {
            return -1;
        }
        if (tag > ver.getTag()) {
            return +1;
        }
        if (tag < ver.getTag()) {
            return -1;
        }
        return 0;
    }

    /**
     * Main method of <code>Version</code> - it runs a few tests to see if
     * everything works.
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("=== Testing compareTo ===");
        Version current = new Version(2, 2);
        System.out.println("2.2 compared to 2.3       = " + current.compareTo(new Version(2, 3)));
        System.out.println("2.2 compared to 2.1       = " + current.compareTo(new Version(2, 1)));
        System.out.println("2.2 compared to 2.2       = " + current.compareTo(new Version(2, 2)));
        System.out.println("2.2 compared to 1.2       = " + current.compareTo(new Version(1, 2)));
        System.out.println("2.2 compared to 3.2       = " + current.compareTo(new Version(3, 2)));
        System.out.println("2.2 compared to 2.2 Alpha = " + current.compareTo(new Version(2, 2, TAG_ALPHA)));
        System.out.println("2.2 compared to 2.2 Beta  = " + current.compareTo(new Version(2, 2, TAG_BETA)));
        System.out.println("=== Checking available version ===");
        System.out.println(Version.isNewVersionAvailable());
    }
}
