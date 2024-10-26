package spidr.export;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import org.apache.axis.client.*;
import org.apache.log4j.*;
import wdc.dbaccess.*;
import wdc.settings.*;
import wdc.utils.*;

public class SyncClient {

    public static final int REQUESTS_PER_ITERATION = 50;

    private static Logger log = Logger.getLogger(SyncClient.class);

    /**
   * Constructs object to browse mail on server 'host' for user 'user' with
   * password 'passwd'
   *
   * @throws Exception
   */
    public SyncClient() throws Exception {
        super();
    }

    /**
   * Returns a list of requests
   *
   * @throws Exception
   * @return Vector
   */
    public static Vector getListOfRequests() throws Exception {
        Connection con = null;
        Vector reqList = new Vector();
        try {
            con = ConnectionPool.getConnection("users");
            Statement stmt = con.createStatement();
            {
                String sqlStr = "SELECT id, site, tableName, stn, elem, dayIdFrom, dayIdTo, sampling, user" + " FROM sync_log" + " WHERE status=\"wait\"" + " LIMIT " + REQUESTS_PER_ITERATION;
                ResultSet rs = stmt.executeQuery(sqlStr);
                while (rs.next()) {
                    HashMap req = new HashMap();
                    req.put("id", new Integer(rs.getInt(1)));
                    req.put("site", rs.getString(2));
                    req.put("table", rs.getString(3));
                    req.put("stn", rs.getString(4));
                    req.put("element", rs.getString(5));
                    DateInterval dateInterval = new DateInterval(new WDCDay(rs.getInt(6)), new WDCDay(rs.getInt(7)));
                    dateInterval.correctBoundDates();
                    req.put("dateInterval", dateInterval);
                    req.put("sampling", new Integer(rs.getInt(8)));
                    req.put("user", rs.getString(9));
                    reqList.add(req);
                }
            }
            stmt.close();
            return reqList;
        } catch (Exception e) {
            throw new Exception("getListOfRequests@MailSendData: problem to get list of sync. requests: " + e);
        } finally {
            ConnectionPool.releaseConnection(con);
        }
    }

    /**
   * Updates status for record identified by 'id' and having status 'oldStatus'
   * @param id Record identifier
   * @param oldStatus Old status
   * @param newStatus New status
   * @return true if successfully
   * @throws Exception
   */
    public static boolean updateStatus(int id, String oldStatus, String newStatus) throws Exception {
        return updateStatus(id, oldStatus, newStatus, false, "");
    }

    /**
   * Updates status for record identified by 'id' and having status 'oldStatus' and sets completeTime
   * @param id Record identifier
   * @param oldStatus Old status
   * @param newStatus New status
   * @param setCompleteTime if true sets completeTime value
   * @param comment Any comments
   * @return true if successfully
   * @throws Exception
   */
    public static boolean updateStatus(int id, String oldStatus, String newStatus, boolean setCompleteTime, String comment) throws Exception {
        if (comment == null) {
            comment = "";
        }
        String curTime;
        {
            GregorianCalendar clndr = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            curTime = df.format(clndr.getTime());
        }
        Connection con = null;
        try {
            con = ConnectionPool.getConnection("users");
            Statement stmt = con.createStatement();
            String sqlStr = "UPDATE sync_log SET status=\"" + newStatus + "\"" + ((setCompleteTime) ? (", completeTime=\"" + curTime + "\"") : "") + ", comment=\"" + comment + "\"" + " WHERE id=" + id + " AND status=\"" + oldStatus + "\"";
            int num = stmt.executeUpdate(sqlStr);
            stmt.close();
            return (num > 0);
        } catch (Exception e) {
            throw new Exception("updateStatus@MailSendData: problem to update status: " + e);
        } finally {
            ConnectionPool.releaseConnection(con);
        }
    }

    /**
   * Makes SPIDR web service call
   *
   * @param siteUrl - web service URL
   * @param login - web service login
   * @param password - web service password
   * @param table - data table (e.g. KPAP)
   * @param station - station code (e.g. BC840)
   * @param element - parameter, may be null for all (e.g. kp)
   * @param dayFrom - Day ID formatted as YYYYMMDD
   * @param dayTo String
   * @param format - export format, may be "ascii", "IIWG", "WDC", default is
   *   "raw" for serialized spidr.datamode.DataSequenceSet
   * @param sampling - data time step in min, if not default (minimal for the
   *   data)
   * @param filePath String
   * @return fileName � file name of the data file recieved from web service
   * @throws Exception
   */
    public static Vector webService(String siteUrl, String login, String password, String table, String station, String element, String dayFrom, String dayTo, String filePath) throws Exception {
        Service service = new Service();
        Call call = (Call) service.createCall();
        if (login != null) {
            call.setUsername(login);
            if (password != null) {
                call.setPassword(password);
            }
            System.err.println("Info: authentication user=" + login + " passwd=" + password + " at " + siteUrl);
        }
        call.setTargetEndpointAddress(new URL(siteUrl));
        call.setOperationName("syncData");
        Vector exportList = (Vector) call.invoke(new Object[] { table, station, element, dayFrom, dayTo });
        if (exportList != null) {
            for (int k = 0; k < exportList.size(); k++) {
                HashMap exportDescr = (HashMap) exportList.get(k);
                String url = (String) exportDescr.get("fileName");
                log.debug("result URL is " + url);
                String fileName = null;
                URL dataurl = new URL(url);
                String filePart = dataurl.getFile();
                if (filePart == null) {
                    throw new Exception("Error: file part in the data URL is null");
                } else {
                    fileName = filePart.substring(filePart.lastIndexOf("/") < 0 ? 0 : filePart.lastIndexOf("/") + 1);
                    if (filePath != null) {
                        fileName = filePath + fileName;
                    }
                    log.debug("local file name is " + fileName);
                }
                FileOutputStream fos = new FileOutputStream(fileName);
                if (fos == null) {
                    throw new Exception("Error: file output stream is null");
                }
                InputStream strm = dataurl.openStream();
                if (strm == null) {
                    throw new Exception("Error: data input stream is null");
                } else {
                    int c;
                    while ((c = strm.read()) != -1) {
                        fos.write(c);
                    }
                }
                strm.close();
                fos.close();
                File file = new File(fileName);
                exportDescr.put("fileName", file.getCanonicalPath());
            }
        }
        return exportList;
    }

    /**
   * Synchronizes SPIDR databases
   *
   * @throws Exception
   */
    public void synchronize() throws Exception {
        String exportPath = null;
        try {
            exportPath = Settings.get("mail.exportPath");
        } catch (Exception e) {
        }
        if (exportPath == null) {
            exportPath = "";
        }
        Vector requests = getListOfRequests();
        if (requests == null || requests.size() == 0) {
            log.info("No synchronization requests found");
            return;
        } else {
            log.info("Found " + requests.size() + " synchronization requests");
        }
        for (int reqno = 0; reqno < requests.size() && reqno < REQUESTS_PER_ITERATION; reqno++) {
            HashMap req = (HashMap) requests.elementAt(reqno);
            int reqID = ((Integer) req.get("id")).intValue();
            String site = (String) req.get("site");
            String table = (String) req.get("table");
            String stn = (String) req.get("stn");
            String element = (String) req.get("element");
            String spidrUser = (String) req.get("user");
            DateInterval dateInterval = (DateInterval) req.get("dateInterval");
            int sampling = ((Integer) req.get("sampling")).intValue();
            log.info("Request id=" + reqID + " from user " + spidrUser + " to site " + site + " table " + table + " station " + stn + " element " + element + " date interval " + dateInterval + " sampling " + sampling);
            String siteUrl = Settings.get("sites." + site + ".metadata");
            String siteDescr = Settings.get("sites." + site + ".description");
            String siteUser = Settings.get("sites." + site + ".user");
            String sitePassword = Settings.get("sites." + site + ".password");
            if (siteUrl == null) {
                log.error("URL is not defined for the site: " + site);
                updateStatus(reqID, "wait", "error", true, "unknown site");
                continue;
            } else {
                log.debug("Get data from site " + siteUrl + " (" + siteDescr + ") for user " + siteUser);
            }
            updateStatus(reqID, "wait", "process", true, "");
            Vector exportList = null;
            try {
                exportList = webService(siteUrl, siteUser, sitePassword, table, stn, element, "" + dateInterval.getDateFrom().getDayId(), "" + dateInterval.getDateFrom().getDayId(), exportPath);
            } catch (Exception e) {
                log.error("Can't call web service for request ID=" + reqID + " : " + e);
                updateStatus(reqID, "process", "error", true, "ws error: " + e.toString().replace('"', '\''));
                continue;
            }
            String link = Settings.get("mail.wsUrl");
            String user = Settings.get("mail.wsUser");
            String passwd = Settings.get("mail.wsPassword");
            String senderKey = spidrUser;
            if (exportList == null || exportList.size() == 0) {
                updateStatus(reqID, "process", "done", true, "no data");
                continue;
            }
            for (int k = 0; k < exportList.size(); k++) {
                HashMap exportDescr = (HashMap) exportList.get(k);
                String dataType = (String) exportDescr.get("dataType");
                String fileName = (String) exportDescr.get("fileName");
                String fileParams = (String) exportDescr.get("fileParams");
                try {
                    log.info("Load to SPIDR node '" + link + "' user '" + user + "' data file '" + fileName + "' table '" + table + "' format '" + dataType + "'" + " with options '" + fileParams + "'" + " as '" + senderKey + "'");
                    String wsLog = FileClient.webService(link, user, passwd, senderKey, "sync", table, dataType, fileName, fileParams, false);
                    log.info("SPIDR node '" + link + "' returned: " + wsLog);
                } catch (Exception e) {
                    log.error("SPIDR node '" + link + "' error: '" + e + "'");
                    updateStatus(reqID, "process", "error", true, "upload error: " + e.toString().replace('"', '\''));
                } finally {
                    try {
                        File ftmp = new File(fileName);
                        ftmp.deleteOnExit();
                    } catch (Exception e) {
                    }
                }
            }
            updateStatus(reqID, "process", "done", true, "");
        }
        log.info("Synchronization complete");
    }

    public static void main(String[] args) {
        System.out.println("Activate settings");
        try {
            if (args.length == 0) {
                System.err.println("Settings base config-file is required.");
                return;
            }
            Settings.getInstance().load(args[0]);
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        System.err.println("SyncClient started  ...");
        try {
            SyncClient syncClient = new SyncClient();
            syncClient.synchronize();
        } catch (Exception e) {
            System.err.println("Error in SyncClient main(): " + e);
            System.exit(1);
        }
        System.err.println("SyncClient finished.");
    }
}
