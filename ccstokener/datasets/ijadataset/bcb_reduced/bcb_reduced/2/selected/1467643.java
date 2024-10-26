package org.robocup.msl.refbox.applications;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class VersionInfo {

    private static final String PRODUCT_ID = "RefBox";

    private static final String LEAGUE_ID = "MSL";

    private static final String YEAR = "2010";

    private static final String VERSION_DEFAULT = "2010-1.35";

    private static final String VERSION_KEY = "version";

    private static final String DATE = "2010-06-05";

    private VersionInfo() {
    }

    private static String getAttributeValue(final String attributeId, final String defaultValue) {
        URL url;
        try {
            url = new URL("jar:" + VersionInfo.class.getResource(VersionInfo.class.getSimpleName() + ".class").getPath().split("!")[0] + "!/");
            JarURLConnection jarConnection;
            jarConnection = (JarURLConnection) url.openConnection();
            final Manifest manifest = jarConnection.getManifest();
            final Attributes attributes = manifest.getMainAttributes();
            final String value = attributes.getValue(attributeId);
            if (value != null) {
                return value;
            }
        } catch (final MalformedURLException e) {
            return defaultValue;
        } catch (final IOException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    public static String product() {
        return VersionInfo.PRODUCT_ID;
    }

    public static String league() {
        return VersionInfo.LEAGUE_ID;
    }

    public static String year() {
        return VersionInfo.YEAR;
    }

    public static String version() {
        return getAttributeValue(VersionInfo.VERSION_KEY, VersionInfo.VERSION_DEFAULT);
    }

    public static String date() {
        return VersionInfo.DATE;
    }
}
