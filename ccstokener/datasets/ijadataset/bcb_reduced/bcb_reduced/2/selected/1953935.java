package edu.usu.cosl.aggregatord;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import au.id.jericho.lib.html.*;
import edu.usu.cosl.microformats.EventGeneric;

public class MicroformatHarvester extends DBThread {

    private static final String QUERY_ENTRIES_NEEDING_HARVEST = "SELECT entries.id, entries.permalink, age(now(), harvested_at) FROM watched_pages " + "INNER JOIN entries ON watched_pages.entry_id = entries.id " + "WHERE feed_id = 0 AND (harvested_at IS NULL OR (age(now(), harvested_at) > interval '24:00:00' AND has_microformats = 't'))";

    private static final double DEFAULT_HARVEST_INTERVAL = 20;

    private static double dHarvestInterval = DEFAULT_HARVEST_INTERVAL;

    private static final String[] asTestPages = { "http://fundyfilm.ca/calendar", "http://finetoothcog.com/site/stolen_bikes", "http://www.clinicalpsychologyarena.com/resources/conferences.asp", "http://laughingsquid.com/laughing-squid-10th-anniversary-party", "http://jhtc.org", "http://www.gore-tex.com/remote/Satellite?c=fabrics_content_c&cid=1162322807952&pagename=goretex_en_US%2Ffabrics_content_c%2FKnowWhatsInsideDetail", "https://www.urbanbody.com/information/contact-us", "http://www.newbury-college.ac.uk/home/default.asp", "http://07.pagesd.info/ardeche/agenda.aspx", "http://www.comtec-ars.com/press-releases", "http://austin.adactio.com/" };

    private class EntryInfo {

        int nEntryID;

        String sLink;

        boolean bKnownToContainMicroformats;

        public EntryInfo(int nEntryID, String sLink, boolean bKnownToContainMicroformats) {
            this.nEntryID = nEntryID;
            this.sLink = sLink;
            this.bKnownToContainMicroformats = bKnownToContainMicroformats;
        }
    }

    public MicroformatHarvester() {
    }

    private void harvestEntries() throws SQLException, ClassNotFoundException {
        Connection cn = getConnection();
        Vector<EntryInfo> vEntries = getEntriesNeedingHarvest(cn);
        if (vEntries.size() > 0) Logger.info("Harvesting pages for microformats: " + vEntries.size());
        for (ListIterator<EntryInfo> liEntries = vEntries.listIterator(); liEntries.hasNext(); ) {
            EntryInfo entry = liEntries.next();
            try {
                jerichoParse(cn, entry.sLink, entry.nEntryID, entry.bKnownToContainMicroformats);
            } catch (Exception e) {
                Logger.error("Error harvesting microformats from: " + entry.sLink);
            }
        }
        cn.close();
    }

    public void run() {
        try {
            while (!bStop) {
                harvestEntries();
                if (!bStop) Thread.sleep((long) (dHarvestInterval * 1000));
            }
        } catch (Exception e) {
            Logger.error("Error in microformat harvester: " + e);
        }
    }

    private Vector<EntryInfo> getEntriesNeedingHarvest(Connection cn) {
        Statement stGetEntries = null;
        ResultSet rsEntries = null;
        try {
            stGetEntries = cn.createStatement();
            Vector<EntryInfo> vEntries = new Vector<EntryInfo>();
            rsEntries = stGetEntries.executeQuery(QUERY_ENTRIES_NEEDING_HARVEST);
            while (rsEntries.next()) {
                vEntries.add(new EntryInfo(rsEntries.getInt(1), rsEntries.getString(2), rsEntries.getBoolean(3)));
            }
            rsEntries.close();
            stGetEntries.close();
            return vEntries;
        } catch (SQLException e) {
            Logger.error("tcb1: " + e);
            Logger.error(e.getNextException());
            try {
                if (rsEntries != null) rsEntries.close();
                if (stGetEntries != null) {
                    stGetEntries.close();
                    stGetEntries = null;
                }
                if (cn != null) {
                    cn.close();
                    cn = null;
                }
            } catch (SQLException e2) {
            }
            return null;
        }
    }

    public String fileName(String sUrl, int nFile) {
        String localFile = null;
        try {
            URL url = new URL(sUrl);
            StringTokenizer st = new StringTokenizer(url.getFile(), "/");
            while (st.hasMoreTokens()) localFile = st.nextToken();
        } catch (Exception e) {
            Logger.error(e);
        }
        return nFile + "-" + (localFile != null && localFile.length() > 6 ? localFile.substring(0, 5) : "") + ".txt";
    }

    public String copyUrl(String sUrl, String sFileName) {
        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(sUrl);
            InputStream is = url.openStream();
            FileOutputStream fos = null;
            fos = new FileOutputStream("c:\\temp\\mf\\" + sFileName);
            int oneChar, count = 0;
            while ((oneChar = is.read()) != -1) {
                fos.write(oneChar);
                sb.append((char) oneChar);
                count++;
            }
            is.close();
            fos.close();
        } catch (Exception e) {
            Logger.error(e);
        }
        return sb.toString();
    }

    private int getEntryID(Connection cn, String sURI) throws SQLException {
        PreparedStatement st = cn.prepareStatement("SELECT id FROM entries WHERE feed_id = 0 AND permalink = ?");
        st.setString(1, sURI);
        ResultSet rs = st.executeQuery();
        int nEntryID = 0;
        if (rs.next()) nEntryID = rs.getInt(1); else {
            rs.close();
            if (sURI.endsWith("/")) {
                st.setString(1, sURI.substring(0, sURI.length() - 1));
                rs = st.executeQuery();
                if (rs.next()) nEntryID = rs.getInt(1);
            } else {
                st.setString(1, sURI + "/");
                rs = st.executeQuery();
                if (rs.next()) nEntryID = rs.getInt(1);
            }
        }
        rs.close();
        st.close();
        return nEntryID;
    }

    private Element getSubElement(Element element, String sClass) {
        List lChildren = element.findAllStartTags("class", sClass, false);
        Iterator iChildren = lChildren.iterator();
        if (iChildren.hasNext()) {
            return ((StartTag) iChildren.next()).getElement();
        }
        return null;
    }

    private String getSubElementText(Element element, String sClass) {
        Element child = getSubElement(element, sClass);
        return child == null ? null : child.extractText();
    }

    private String getSubElementAttribute(Element element, String sClass, String sAttr) {
        Element child = getSubElement(element, sClass);
        return child == null ? null : child.getAttributeValue(sAttr);
    }

    private String getChildTagAttribute(Element element, String sTagName, String sAttr) {
        List lChildren = element.findAllStartTags(sTagName);
        Iterator iChildren = lChildren.iterator();
        if (iChildren.hasNext()) {
            StartTag child = (StartTag) iChildren.next();
            return child.getAttributeValue(sAttr);
        } else return null;
    }

    private String parseFormattedDate(String sDate, String sParsePattern, String sFormatPattern) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(sParsePattern);
            Date parsedDate = dateFormat.parse(sDate);
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(parsedDate);
            int nHour = cal.get(Calendar.HOUR_OF_DAY);
            if (nHour == 0) dateFormat.applyPattern("yyyy-MM-dd"); else dateFormat.applyPattern(sFormatPattern);
            return dateFormat.format(parsedDate);
        } catch (ParseException e) {
            return null;
        }
    }

    private String parseDate(String sDate) {
        String sNormalizedDate = parseFormattedDate(sDate, "yyyyMMdd'T'HHmmssZ", "dd MMMM yyyy hh:mm a");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyyMMdd'T'HHmmZ", "dd MMMM yyyy hh:mm a");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyyMMdd'T'HHmm'Z'", "dd MMMM yyyy hh:mm a");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyyMMdd'T'HHmmss", "dd MMMM yyyy hh:mm a");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyyMMdd'T'HHmm", "dd MMMM yyyy hh:mm a");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyy-MM-dd", "yyyy-MM-dd");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyyMMddssZ", "yyyy-MM-dd");
        if (sNormalizedDate == null) sNormalizedDate = parseFormattedDate(sDate, "yyyyMMdd", "yyyy-MM-dd");
        return (sNormalizedDate == null) ? sDate : sNormalizedDate;
    }

    private String getDateFromElement(Element event, String sClass) {
        Element date = getSubElement(event, sClass);
        if (date == null) return null;
        String sDate = date.getAttributeValue("title");
        if (sDate == null) sDate = date.extractText();
        return parseDate(sDate);
    }

    private double jerichoParse(Connection cn, String sPage, int nEntryID, boolean bKnownToContainMicroformats) throws IOException, SQLException {
        Logger.info("Harvesting for microformats: " + sPage);
        Timestamp time = Harvester.currentTime();
        au.id.jericho.lib.html.Source source = new Source(new URL(sPage));
        source.setLogWriter(new OutputStreamWriter(System.err));
        List lEvents = source.findAllStartTags("class", "vevent", false);
        PreparedStatement stUpdateEntryInfo = cn.prepareStatement("UPDATE watched_pages SET harvested_at = ?, has_microformats = ? WHERE entry_id = ?");
        int nNewEvents = 0;
        MicroformatDBManager mdm = MicroformatDBManager.create(cn);
        for (Iterator iEvents = lEvents.iterator(); iEvents.hasNext(); ) {
            EventGeneric mfEvent = new EventGeneric();
            Element event = ((StartTag) iEvents.next()).getElement();
            String sSummary = getSubElementText(event, "summary");
            if (sSummary != null) {
                if (sSummary.length() > 200) {
                    Logger.error("Event name truncated: " + sSummary);
                    sSummary = sSummary.substring(0, 197) + "...";
                }
                mfEvent.setName(sSummary);
            }
            String sDescription = getSubElementText(event, "description");
            if (sDescription != null) mfEvent.setDescription(sDescription);
            String sDuration = getSubElementText(event, "duration");
            if (sDuration != null) mfEvent.setDuration(sDuration);
            String sEndDate = getDateFromElement(event, "dtend");
            String sStartDate = getDateFromElement(event, "dtstart");
            if (sStartDate != null) {
                if (sStartDate != null && sStartDate.length() < 3 && sEndDate != null && sEndDate.length() > 3 && sEndDate.contains(" ")) {
                    int nStartDate = 0;
                    try {
                        nStartDate = Integer.parseInt(sStartDate);
                    } catch (Exception e) {
                    }
                    if (nStartDate != 0) sStartDate = nStartDate + sEndDate.substring(sEndDate.indexOf(' '), sEndDate.length());
                }
            }
            if (sStartDate != null) {
                mfEvent.setBegins(sStartDate);
            }
            if (sEndDate != null) {
                mfEvent.setEnds(sEndDate);
            }
            String sLocation = null;
            Element location = getSubElement(event, "location");
            if (location != null) {
                sLocation = location.extractText();
                if (sLocation == null || sLocation.length() == 0) sLocation = location.getAttributeValue("title");
            }
            if (sLocation != null) mfEvent.setLocation(sLocation);
            String sURL = getSubElementAttribute(event, "url", "href");
            if (sURL == null) {
                Element summary = getSubElement(event, "summary");
                if (summary != null) {
                    sURL = getChildTagAttribute(summary, "a", "href");
                }
            }
            if (sURL != null && !sURL.startsWith("http")) {
                try {
                    if (sURL.startsWith("/")) sURL = "http://" + new URL(sPage).getHost() + sURL; else if (!sURL.startsWith("http://")) sURL = sPage + sURL;
                } catch (MalformedURLException e) {
                    sURL = sPage + sURL;
                }
            }
            if (sURL != null) mfEvent.addLink(sURL, "");
            try {
                if (mdm.addEventToDB(mfEvent, nEntryID)) {
                    nNewEvents++;
                }
            } catch (Exception e) {
                Logger.error("Error adding event to database.");
                Logger.error(mfEvent.toString());
                Logger.error(e);
            }
        }
        mdm.close();
        stUpdateEntryInfo.setTimestamp(1, Harvester.currentTime());
        stUpdateEntryInfo.setBoolean(2, bKnownToContainMicroformats ? true : lEvents.size() > 0);
        stUpdateEntryInfo.setInt(3, nEntryID);
        stUpdateEntryInfo.execute();
        if (nNewEvents > 0) Logger.info("Harvested events from " + sPage + ": " + nNewEvents);
        return Harvester.secondsSince(time);
    }

    public static void main(String[] args) {
        try {
            Logger.setLogToConsole(true);
            Logger.setLogLevel(10);
            MicroformatHarvester mfHarvester = new MicroformatHarvester();
            Connection cn = getConnection();
            for (int nPage = 0; nPage < asTestPages.length; nPage++) {
                int nEntryID = mfHarvester.getEntryID(cn, asTestPages[nPage]);
                nEntryID = 1;
                mfHarvester.jerichoParse(cn, asTestPages[nPage], nEntryID, false);
            }
            cn.close();
        } catch (Exception e) {
            Logger.error(e);
        }
    }
}
