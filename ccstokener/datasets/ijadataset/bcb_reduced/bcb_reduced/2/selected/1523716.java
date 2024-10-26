package org.libreplan.business.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * It contains the current version of project and implements of singleton
 * pattern. <br />
 * It also has a cached value with information about last project version
 * published. It checks the last version against a URL.
 *
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class VersionInformation {

    private static final Log LOG = LogFactory.getLog(VersionInformation.class);

    /**
     * URL with a text file only with last number version of LibrePlan
     */
    private static final String LIBREPLAN_VERSION_URL = "http://libreplan.org/VERSION";

    private static final String LIBREPLAN_USAGE_STATS_PARAM = "stats";

    private static final String LIBREPLAN_VERSION_PARAM = "version";

    /**
     * Delay to wait till we check the URL again
     */
    private static final long DELAY_TO_CHECK_URL = 24 * 60 * 60 * 1000;

    private static final VersionInformation singleton = new VersionInformation();

    private String projectVersion;

    private boolean newVersionCached = false;

    private Date lastVersionCachedDate = new Date();

    private VersionInformation() {
    }

    private void loadNewVersionFromURL(boolean allowToGatherUsageStatsEnabled) {
        lastVersionCachedDate = new Date();
        try {
            URL url = getURL(allowToGatherUsageStatsEnabled);
            String lastVersion = (new BufferedReader(new InputStreamReader(url.openStream()))).readLine();
            if (projectVersion != null && lastVersion != null) {
                newVersionCached = !projectVersion.equals(lastVersion);
            }
        } catch (MalformedURLException e) {
            LOG.warn("Problems generating URL to check LibrePlan version. MalformedURLException: " + e.getMessage());
        } catch (IOException e) {
            LOG.info("Could not check LibrePlan version information from " + LIBREPLAN_VERSION_URL + ". IOException: " + e.getMessage());
        }
    }

    private URL getURL(boolean allowToGatherUsageStatsEnabled) throws MalformedURLException {
        String url = LIBREPLAN_VERSION_URL;
        if (allowToGatherUsageStatsEnabled) {
            url += "?" + LIBREPLAN_USAGE_STATS_PARAM + "=1";
            url += "&" + LIBREPLAN_VERSION_PARAM + "=" + projectVersion;
        }
        return new URL(url);
    }

    public static VersionInformation getInstance() {
        return singleton;
    }

    /**
     * It returns the current version of the project for retrieval at any place.
     */
    public static String getVersion() {
        return singleton.getProjectVersion();
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String argVersion) {
        projectVersion = argVersion;
        loadNewVersionFromURL(false);
    }

    /**
     * Returns true if a new version of the project is published.
     *
     * @param allowToGatherUsageStatsEnabled
     *            If true LibrePlan developers will process the requests to check
     *            the new versions to generate usages statistics
     */
    public static boolean isNewVersionAvailable(boolean allowToGatherUsageStatsEnabled) {
        return singleton.checkIsNewVersionAvailable(allowToGatherUsageStatsEnabled);
    }

    /**
     * If there is a new version already detected, it doesn't check it again.
     * Otherwise, during one day it returns the cached value. And it checks it
     * again after that time.
     */
    private boolean checkIsNewVersionAvailable(boolean allowToGatherUsageStatsEnabled) {
        if (!newVersionCached) {
            long oneDayLater = lastVersionCachedDate.getTime() + DELAY_TO_CHECK_URL;
            if (oneDayLater < new Date().getTime()) {
                loadNewVersionFromURL(allowToGatherUsageStatsEnabled);
            }
        }
        return newVersionCached;
    }
}
