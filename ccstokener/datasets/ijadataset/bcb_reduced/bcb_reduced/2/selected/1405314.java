package net.sf.timeslottracker.net;

import java.awt.TrayIcon.MessageType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.sf.timeslottracker.core.Action;
import net.sf.timeslottracker.core.ActionListener;
import net.sf.timeslottracker.core.Configuration;
import net.sf.timeslottracker.core.TimeSlotTracker;
import net.sf.timeslottracker.core.TimeoutTimer;

/**
 * Manages the version info from sf.net
 * 
 * @version File version: $Revision: 800 $, $Date: 2009-05-15 21:53:21 -0400 (Fri, 15 May 2009) $
 * @author Last change: $Author: cnitsa $
 */
public class VersionManager {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String VERSION_REGEXP = "TimeSlotTracker ([a-z,A-Z,0-9,.,_,-]+) released";

    private static final String RELEASES_RSS = "http://sourceforge.net/export/rss2_projfiles.php?group_id=132880";

    private final TimeSlotTracker timeSlotTracker;

    public VersionManager(TimeSlotTracker timeSlotTracker) {
        this.timeSlotTracker = timeSlotTracker;
    }

    /**
   * Gets the last version
   * 
   * @return null if no new version available or version info about it
   */
    public VersionInfo getLastVersion() {
        VersionInfo versionInfo = null;
        try {
            URL url = new URL(RELEASES_RSS);
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(br);
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName();
                    if (versionInfo != null && name.equals("title")) {
                        String versionStr = getTextElement(reader);
                        Pattern pattern = Pattern.compile(VERSION_REGEXP);
                        Matcher matcher = pattern.matcher(versionStr);
                        if (matcher.find()) {
                            versionStr = matcher.group(1);
                        }
                        versionInfo.setVersion(versionStr);
                        continue;
                    }
                    if (versionInfo != null && name.equals("pubDate")) {
                        versionInfo.setReleaseDate(getTextElement(reader));
                        continue;
                    }
                    if (versionInfo != null && name.equals("link")) {
                        versionInfo.setNotesLink(getTextElement(reader));
                        continue;
                    }
                    if (name.equals("item")) {
                        if (versionInfo == null) {
                            versionInfo = new VersionInfo();
                            continue;
                        } else {
                            break;
                        }
                    }
                }
            }
            if (versionInfo != null) {
                versionInfo.setFilesLink("http://sourceforge.net/project/showfiles.php?group_id=132880");
            }
            setStatus(versionInfo);
        } catch (Exception e) {
            timeSlotTracker.errorLog(e);
        }
        return versionInfo;
    }

    /**
   * Install last version auto checker
   */
    public void installUpdater() {
        if (!timeSlotTracker.getConfiguration().getBoolean(Configuration.CHECK_NEW_VERSION_ENABLED, true)) {
            return;
        }
        doCheckIfNeed();
        new TimeoutTimer(timeSlotTracker, "check new version", new ActionListener() {

            @Override
            public void actionPerformed(Action action) {
                doCheckIfNeed();
            }
        }, 24 * 60 * 60, 10);
    }

    private void doCheckIfNeed() {
        Date lastUpdate = null;
        String lastUpdateStr = timeSlotTracker.getConfiguration().getString(Configuration.CHECK_NEW_VERSION_LAST_UPDATE, null);
        if (lastUpdateStr != null) {
            try {
                lastUpdate = DATE_FORMAT.parse(lastUpdateStr);
            } catch (Exception e) {
            }
        }
        if (lastUpdate == null) {
            lastUpdate = new Date(0);
            saveLastUpdate(lastUpdate);
        }
        Integer days = timeSlotTracker.getConfiguration().getInteger(Configuration.CHECK_NEW_VERSION_DAYS, 14);
        Calendar shouldUpdate = Calendar.getInstance();
        shouldUpdate.setTime(lastUpdate);
        shouldUpdate.add(Calendar.DATE, days);
        if (shouldUpdate.getTime().before(new Date())) {
            doCheckVersion(new Date());
        }
    }

    private void saveLastUpdate(Date lastUpdate) {
        timeSlotTracker.getConfiguration().set(Configuration.CHECK_NEW_VERSION_LAST_UPDATE, DATE_FORMAT.format(lastUpdate));
    }

    private void setStatus(VersionInfo lastVersion) {
        boolean isDeveloperVersion = timeSlotTracker.getConfiguration().isDeveloperVersion();
        String currentVersion = timeSlotTracker.getConfiguration().getVersionString();
        if (currentVersion != null) {
            currentVersion = currentVersion.substring(currentVersion.lastIndexOf(" ") + 1, currentVersion.length() - 1);
            currentVersion = currentVersion.replaceAll("_", ".");
        }
        System.out.println("currentVersion = [" + currentVersion + "]");
        System.out.println("lastVersion.getVersion() = [" + lastVersion.getVersion() + "]");
        if (!isDeveloperVersion && currentVersion.compareTo(lastVersion.getVersion()) < 0) {
            lastVersion.setNewVersionAvailable(true);
        } else {
            lastVersion.setNewVersionAvailable(false);
        }
    }

    private String getTextElement(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int next = reader.next();
            if (next == XMLStreamConstants.END_ELEMENT) {
                return null;
            }
            if (next == XMLStreamConstants.CHARACTERS) {
                return reader.getText();
            }
        }
        return null;
    }

    private void doCheckVersion(Date lastUpdate) {
        VersionInfo lastVersion = getLastVersion();
        if (lastVersion != null && lastVersion.isNewVersionAvailable()) {
            StringBuffer stringBuffer = new StringBuffer().append(timeSlotTracker.getString("newVersionDialog.title")).append(": ").append(lastVersion.getVersion()).append("\n").append(timeSlotTracker.getString("newVersionDialog.released")).append(": ").append(lastVersion.getReleaseDate());
            timeSlotTracker.getTrayIconService().showMessage(stringBuffer.toString(), MessageType.INFO);
        }
        saveLastUpdate(lastUpdate);
    }
}
