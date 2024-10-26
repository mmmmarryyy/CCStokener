package com.rapidminer.gui.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import com.rapidminer.Version;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

/**
 * This class tries to connect a RapidMiner server and checks if new versions of RapidMiner
 * are available. Saves the current date as last update check date in the user
 * directory. If a new version is available, an info message is shown. Otherwise
 * a failure dialog might be shown.
 * 
 * @author Ingo Mierswa
 *          ingomierswa Exp $
 */
public class CheckForUpdatesThread extends Thread {

    private static final String[] VERSION_URLS = { "http://www.rapid-i.com/versions/rapidminer/version.txt", "http://www.rapid-i.com/versions/yale/version.txt", "http://www-ai.cs.uni-dortmund.de/SOFTWARE/YALE/version.txt" };

    private boolean showFailureDialog = false;

    private MainFrame mainFrame;

    public CheckForUpdatesThread(MainFrame mainFrame, boolean dialog) {
        this.mainFrame = mainFrame;
        this.showFailureDialog = dialog;
    }

    public void run() {
        List<String> remoteVersions = new LinkedList<String>();
        for (String s : VERSION_URLS) {
            URL url = null;
            try {
                url = new URL(s);
            } catch (MalformedURLException e) {
                LogService.getGlobal().log("Cannot create update target url: " + e.getMessage(), LogService.ERROR);
            }
            if (url != null) {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(url.openStream()));
                    String remoteVersion = in.readLine();
                    if ((remoteVersion != null) && (remoteVersion.length() > 0) && (Character.isDigit(remoteVersion.charAt(0)))) {
                        remoteVersions.add(remoteVersion);
                    }
                } catch (IOException e) {
                    LogService.getGlobal().log("Not able to check for updates. Maybe no internet connection.", LogService.WARNING);
                } finally {
                    try {
                        if (in != null) in.close();
                    } catch (IOException e) {
                        throw new Error(e);
                    }
                }
            }
        }
        if (remoteVersions.size() > 0) {
            RapidMinerGUI.saveLastUpdateCheckDate();
        }
        Iterator<String> i = remoteVersions.iterator();
        VersionNumber newestVersion = getVersionNumber(Version.getLongVersion());
        while (i.hasNext()) {
            String remoteVersionString = i.next();
            if (remoteVersionString != null) {
                VersionNumber remoteVersion = getVersionNumber(remoteVersionString);
                if (isNewer(remoteVersion, newestVersion)) {
                    newestVersion = remoteVersion;
                }
            }
        }
        if ((newestVersion != null) && (isNewer(newestVersion, getVersionNumber(Version.getLongVersion())))) {
            JOptionPane.showMessageDialog(mainFrame, "New version of the RapidMiner Community Edition is available:" + Tools.getLineSeparator() + Tools.getLineSeparator() + "          RapidMiner " + newestVersion + Tools.getLineSeparator() + Tools.getLineSeparator() + "Please download it from:" + Tools.getLineSeparator() + "          http://www.rapidminer.com", "New RapidMiner version", JOptionPane.INFORMATION_MESSAGE);
        } else if (showFailureDialog) {
            JOptionPane.showMessageDialog(mainFrame, "No newer versions of the RapidMiner Community Edition available!", "RapidMiner CE is up to date", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private VersionNumber getVersionNumber(String versionString) {
        return new VersionNumber(versionString);
    }

    private boolean isNewer(VersionNumber remoteVersion, VersionNumber newestVersion) {
        return remoteVersion.compareTo(newestVersion) > 0;
    }
}
