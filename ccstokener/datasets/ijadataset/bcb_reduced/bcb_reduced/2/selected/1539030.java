package org.sqsh;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Simple utility class to determine the jsqsh version and build date.
 * This class expects a file called build.properties to be located at the
 * root of the jsqsh.jar (well, technically ANY jar). If that file doesn't
 * exist, hilarity will ensue.
 */
public class Version {

    private static final String VERSION_FILE = "build.properties";

    private static String version = "UNKNOWN";

    private static String buildDate = "UNKNOWN";

    private static boolean isInit = false;

    /**
     * A bean is needed to expose the version as a jsqsh variable.
     */
    public static class Bean {

        public String getVersion() {
            return Version.getVersion();
        }

        public void setVersion(String version) {
            throw new UnsupportedOperationException("Cannot set version");
        }

        public String getBuildDate() {
            return Version.getBuildDate();
        }

        public void setBuildDate(String date) {
            throw new UnsupportedOperationException("Cannot set build date");
        }
    }

    /**
     * @return The jsqsh version number or "UNKNOWN" if the version is
     * not known.
     */
    public static String getVersion() {
        if (!isInit) init();
        return version;
    }

    /**
     * @return The jsqsh build date or "UNKNOWN" if the build date is not
     *   known.
     */
    public static String getBuildDate() {
        if (!isInit) init();
        return buildDate;
    }

    private static synchronized void init() {
        if (!isInit) {
            URL url = Version.class.getClassLoader().getResource(VERSION_FILE);
            if (url == null) {
                System.err.println("Could not locate " + VERSION_FILE + " in jsqsh.jar. Unable to determine version");
                return;
            }
            try {
                Properties props = new Properties();
                InputStream in = url.openStream();
                props.load(in);
                in.close();
                version = props.getProperty("build.version");
                buildDate = props.getProperty("build.date");
            } catch (Throwable e) {
                System.err.println("Failed to read " + VERSION_FILE + " in jsqsh.jar: " + e.getMessage() + ". Cannot determine version number");
            }
            isInit = true;
        }
    }
}
