package spidr.dbaccess;

import java.sql.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;
import org.apache.axis.client.*;
import org.apache.log4j.*;
import wdc.dbaccess.ConnectionPool;
import wdc.dbaccess.ApiException;
import wdc.settings.*;
import wdc.utils.*;
import spidr.datamodel.*;

public class LocalApi {

    private Logger log = Logger.getLogger(LocalApi.class);

    static final String SQL_LOAD_META_ELEMENTS = "SELECT CONCAT(elements_descr.element,\"@\",elements_descr.elemTable) AS elemKey, elements_descr.*, " + "min_yrMon*100+1 AS dateFrom, max_yrMon*100+31 AS dateTo " + "FROM elements_descr LEFT JOIN elements_periods ON " + "(elements_descr.elemTable=elements_periods.dataTable AND elements_descr.element=elements_periods.param) " + "ORDER BY elemTable, element";

    static final String SQL_LOAD_META_STATIONS = "SELECT stations_descr.*, " + "min_yrMon*100+1 AS dateFrom, max_yrMon*100+31 AS dateTo " + "FROM stations_descr LEFT JOIN stations_periods ON " + "(stations_descr.dataTable=stations_periods.dataTable AND stations_descr.stn=stations_periods.param) " + "ORDER BY dataTable, stName";

    public static final int MONTHLY_SAMPLING = spidr.dbaccess.DBAccess.MONTHLY_SAMPLING;

    public static final int YEARLY_SAMPLING = spidr.dbaccess.DBAccess.YEARLY_SAMPLING;

    private WDCTable metaElements = null;

    private int indElemElement;

    private int indElemTable;

    private int indElemDescription;

    private int indElemMultiplier;

    private int indElemMissingValue;

    private int indElemOutputFormat;

    private int indElemUnits;

    private int indElemDateFrom;

    private int indElemDateTo;

    private WDCTable metaStations = null;

    private int indStatStation;

    private int indStatTable;

    private int indStatDescription;

    private int indStatLat;

    private int indStatLon;

    private int indStatDateFrom;

    private int indStatDateTo;

    /** Constructs object with database properties extracted from file
   */
    public LocalApi() throws ApiException {
        loadMetadata();
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Loads metadata from the database
   */
    private void loadMetadata() throws ApiException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = ConnectionPool.getConnection("metadata");
            stmt = con.createStatement();
            metaElements = Metadata.getDataSet("spidrApi_meta_elements", stmt, SQL_LOAD_META_ELEMENTS);
            indElemElement = metaElements.getColumnIndex("element");
            indElemTable = metaElements.getColumnIndex("elemTable");
            indElemDescription = metaElements.getColumnIndex("description");
            indElemMultiplier = metaElements.getColumnIndex("multiplier");
            indElemMissingValue = metaElements.getColumnIndex("missingValue");
            indElemOutputFormat = metaElements.getColumnIndex("outputFormat");
            indElemUnits = metaElements.getColumnIndex("units");
            indElemDateFrom = metaElements.getColumnIndex("dateFrom");
            indElemDateTo = metaElements.getColumnIndex("dateTo");
            metaStations = Metadata.getDataSet("spidrApi_meta_stations", stmt, SQL_LOAD_META_STATIONS);
            indStatStation = metaStations.getColumnIndex("stn");
            indStatTable = metaStations.getColumnIndex("dataTable");
            indStatDescription = metaStations.getColumnIndex("stName");
            indStatLat = metaStations.getColumnIndex("lat");
            indStatLon = metaStations.getColumnIndex("lon");
            indStatDateFrom = metaStations.getColumnIndex("dateFrom");
            indStatDateTo = metaStations.getColumnIndex("dateTo");
        } catch (Exception e) {
            throw new ApiException("Can not load metadata: " + e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }
            ConnectionPool.releaseConnection(con);
        }
    }

    /** Creates and returns value for given day and hour.
   * @param key The parameter has the following structure "[element]@[table]" or "[element]@[table]@[station]"
   * @param curDate The date to be considered
   * @param hour The hour of interest [0..23]
   * @return value for the hour
   */
    public float getHourlyValue(String key, WDCDay curDate, int hour) throws ApiException {
        if (hour < 0 || hour > 23) {
            throw new ApiException("The hour out of range [0..23]: hour=" + hour);
        }
        float[] data = getData(key, curDate, 60);
        return (data != null) ? data[hour] : Float.NaN;
    }

    /** Creates and returns value for given day and hour.
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param curDate The date to be considered
   * @param hour The hour of interest [0..23]
   * @return value for the hour
   */
    public float getHourlyValue(String table, String element, WDCDay curDate, int hour) throws ApiException {
        if (hour < 0 || hour > 23) {
            throw new ApiException("The hour out of range [0..23]: hour=" + hour);
        }
        float[] data = getData(table, element, null, new DateInterval(curDate, curDate), 60);
        return (data != null) ? data[hour] : Float.NaN;
    }

    /** Creates and returns value for given day and hour.
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param stn The station code
   * @param curDate The date to be considered
   * @param hour The hour of interest [0..23]
   * @return value for the hour
   */
    public float getHourlyValue(String table, String element, String stn, WDCDay curDate, int hour) throws ApiException {
        if (hour < 0 || hour > 23) {
            throw new ApiException("The hour out of range [0..23]: hour=" + hour);
        }
        float[] data = getData(table, element, stn, new DateInterval(curDate, curDate), 60);
        return (data != null) ? data[hour] : Float.NaN;
    }

    /** Creates and returns daily average value
   * @param key The parameter has the following structure "[element]@[table]" or "[element]@[table]@[station]"
   * @param curDate The date to be considered
   * @return value for the day
   */
    public float getDailyValue(String key, WDCDay curDate) throws ApiException {
        float[] data = getData(key, curDate, 1440);
        return (data != null) ? data[0] : Float.NaN;
    }

    /** Creates and returns daily average value
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param curDate The date to be considered
   * @return value for the day
   */
    public float getDailyValue(String table, String element, WDCDay curDate) throws ApiException {
        float[] data = getData(table, element, null, new DateInterval(curDate, curDate), 1440);
        return (data != null) ? data[0] : Float.NaN;
    }

    /** Creates and returns daily average value
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param stn The station code
   * @param curDate The date to be considered
   * @return value for the day
   */
    public float getDailyValue(String table, String element, String stn, WDCDay curDate) throws ApiException {
        float[] data = getData(table, element, stn, new DateInterval(curDate, curDate), 1440);
        return (data != null) ? data[0] : Float.NaN;
    }

    /** Creates and returns monthly average value
   * @param key The parameter has the following structure "[element]@[table]" or "[element]@[table]@[station]"
   * @param curDate The date to be considered
   * @return value for the day
   */
    public float getMonthlyValue(String key, WDCDay curDate) throws ApiException {
        float[] data = getData(key, curDate, this.MONTHLY_SAMPLING);
        return (data != null) ? data[0] : Float.NaN;
    }

    /** Creates and returns monthly average value
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param curDate The date to be considered
   * @return value for the day
   */
    public float getMonthlyValue(String table, String element, WDCDay curDate) throws ApiException {
        float[] data = getData(table, element, null, new DateInterval(curDate, curDate), this.MONTHLY_SAMPLING);
        return (data != null) ? data[0] : Float.NaN;
    }

    /** Creates and returns monthly average value
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param stn The station code
   * @param curDate The date to be considered
   * @return value for the day
   */
    public float getMonthlyValue(String table, String element, String stn, WDCDay curDate) throws ApiException {
        float[] data = getData(table, element, stn, new DateInterval(curDate, curDate), this.MONTHLY_SAMPLING);
        return (data != null) ? data[0] : Float.NaN;
    }

    /** Creates a sequence of samples for the given element at the date interval.
   * @param key The parameter has the following structure "[element]@[table]" or "[element]@[table]@[station]"
   * @param curDate The date to be considered
   * @param sampling The sampling of the data to be received in minutes [0,1440],
   *                 MONTHLY_SAMPLING, YEARLY_SAMPLING, 0 is min found sampling (without scaling)
   * @return float array or null if no data were found (Float.NaN for missing values)
   */
    public float[] getData(String key, WDCDay curDate, int sampling) throws ApiException {
        if (key == null) {
            throw new ApiException("The key is undefined");
        }
        key = key.trim();
        int ind = key.indexOf('@');
        if (ind == -1 || ind == key.length() - 1) {
            throw new ApiException("The key does not contain table");
        }
        if (ind == 0) {
            throw new ApiException("The key does not contain element");
        }
        int ind2 = key.indexOf('@', ind + 1);
        String element = key.substring(0, ind);
        String stn = null;
        String table = null;
        if (ind2 != -1 && ind2 != key.length() - 1) {
            table = key.substring(ind + 1, ind2);
            stn = key.substring(ind2 + 1);
        } else {
            table = key.substring(ind + 1);
        }
        return getData(table, element, stn, new DateInterval(curDate, curDate), sampling);
    }

    /** Creates a sequence of samples for the given element at the date interval.
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param curDate The date to be considered
   * @param sampling The sampling of the data to be received in minutes [0,1440],
   *                 MONTHLY_SAMPLING, YEARLY_SAMPLING, 0 is min found sampling (without scaling)
   * @return float array or null if no data were found (Float.NaN for missing values)
   */
    public float[] getData(String table, String element, WDCDay curDate, int sampling) throws ApiException {
        return getData(table, element, null, new DateInterval(curDate, curDate), sampling);
    }

    /** Creates a sequence of samples for the given element at the date interval.
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param stn The station code
   * @param curDate The date to be considered
   * @param sampling The sampling of the data to be received in minutes [0,1440],
   *                 MONTHLY_SAMPLING, YEARLY_SAMPLING, 0 is min found sampling (without scaling)
   * @return float array or null if no data were found (Float.NaN for missing values)
   */
    public float[] getData(String table, String element, String stn, WDCDay curDate, int sampling) throws ApiException {
        return getData(table, element, stn, new DateInterval(curDate, curDate), sampling);
    }

    /** Creates a sequence of samples for the given element at the date interval.
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @param stn The station code
   * @param dateInterval The time period of data to be received
   * @param sampling The sampling of the data to be received in minutes [0,1440],
   *                 MONTHLY_SAMPLING, YEARLY_SAMPLING, 0 is min found sampling (without scaling)
   * @return float array or null if no data were found (Float.NaN for missing values)
   */
    public float[] getData(String table, String element, String stn, DateInterval dateInterval, int sampling) throws ApiException {
        DataDescription descr = new DataDescription(table, element);
        Station station = (stn != null) ? (new Station(stn, table, stn)) : null;
        if (metaElements == null) {
            throw new ApiException("Metadata required to get data");
        }
        int ind = metaElements.findRow(indElemTable, table, indElemElement, element);
        if (ind != -1) {
            try {
                descr.setMultiplier(Float.parseFloat((String) metaElements.getValueAt(ind, indElemMultiplier)));
            } catch (NumberFormatException e) {
            }
            try {
                descr.setMissingValue(Float.parseFloat((String) metaElements.getValueAt(ind, indElemMissingValue)));
            } catch (NumberFormatException e) {
            }
        }
        float missingValue = descr.getMissingValue();
        Vector dsList = getData(descr, station, dateInterval, sampling);
        if (dsList == null || dsList.size() == 0) {
            return null;
        }
        DataSequence ds = (DataSequence) dsList.get(0);
        if (ds == null || ds.size() == 0) {
            return null;
        }
        if (ds.get(0) instanceof MultidayData) {
            float[] res = new float[ds.size()];
            for (int i = 0; i < res.length; i++) {
                res[i] = ((MultidayData) ds.get(i)).getData()[0];
            }
            return res;
        }
        int samplesPerDay = ((DailyData) ds.get(0)).getData().length;
        float[] data = new float[dateInterval.numDays() * samplesPerDay];
        for (int k = 0; k < data.length; k++) {
            data[k] = Float.NaN;
        }
        for (int k = 0; k < ds.size(); k++) {
            DailyData dd = (DailyData) ds.get(k);
            if (dd.getData().length != samplesPerDay) {
                throw new ApiException("The method can not be used to receive data with flowing sampling");
            }
            float[] curData = dd.getData();
            int daysFromStart = dateInterval.getDateFrom().daysUntil(new WDCDay(dd.getDayId()));
            for (int i = 0; i < curData.length; i++) {
                data[(daysFromStart - 1) * samplesPerDay + i] = (curData[i] != missingValue) ? curData[i] : Float.NaN;
            }
        }
        return data;
    }

    /** Creates a sequence of samples for the given element at the date interval (only for constant samping).
   * @param table The table name (Ex. GOES, KPAP, etc)
   * @param element The element name
   * @return DataDescription object
   */
    public DataDescription getDescription(String table, String element) throws ApiException {
        DataDescription descr = new DataDescription(table, element);
        if (metaElements == null) {
            throw new ApiException("Metadata required to get data");
        }
        int ind = metaElements.findRow(indElemTable, table, indElemElement, element);
        if (ind != -1) {
            try {
                descr.setMultiplier(Float.parseFloat((String) metaElements.getValueAt(ind, indElemMultiplier)));
            } catch (NumberFormatException e) {
            }
            try {
                descr.setMissingValue(Float.parseFloat((String) metaElements.getValueAt(ind, indElemMissingValue)));
            } catch (NumberFormatException e) {
            }
            descr.setOutputFormat((String) metaElements.getValueAt(ind, indElemOutputFormat));
            descr.setElemDescr((String) metaElements.getValueAt(ind, indElemDescription));
            descr.setUnits((String) metaElements.getValueAt(ind, indElemUnits));
        }
        return descr;
    }

    /** Creates a sequence of samples for the given element at the date interval (only for constant samping).
   * @param descr The description of the data to be received
   * @param station The recording station to be consider
   * @param dateInterval The time period of data to be received
   * @param sampling The sampling of the data to be received in minutes [0,1440], 0 is min found sampling (without scaling)
   * @return Vector of a sequences of daily data sets
   */
    public Vector getData(DataDescription descr, Station station, DateInterval dateInterval, int sampling) throws ApiException {
        Connection con = null;
        Statement stmt = null;
        String table = (descr != null) ? descr.getTable() : null;
        Vector dsList = new Vector();
        try {
            String wsflag = Settings.get(table + ".useWebService");
            if ("yes".equals(wsflag) || "true".equals(wsflag)) {
                String serviceUrl = Settings.get(table + ".dataServiceUrl");
                String serviceUser = Settings.get(table + ".dataServiceUser");
                String servicePassword = Settings.get(table + ".dataServicePassword");
                Call call = (Call) (new Service()).createCall();
                call.setTargetEndpointAddress(serviceUrl);
                call.setOperationName("getData");
                if (serviceUser != null) {
                    call.setUsername(serviceUser);
                    if (servicePassword != null) {
                        call.setPassword(servicePassword);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Service " + serviceUrl + " authentication user=" + serviceUser + " passwd=" + servicePassword + " call method getData" + " for table " + table + " station " + ((station != null) ? station.getStn() : "") + " element " + ((descr != null && descr.getElement() != null) ? descr.getElement() : "") + " dateFrom " + dateInterval.getDateFrom().getDayId() + " dateTo " + dateInterval.getDateTo().getDayId() + " sampling " + sampling);
                }
                String dssUrl = (String) call.invoke(new Object[] { table, ((station != null) ? station.getStn() : ""), ((descr != null && descr.getElement() != null) ? descr.getElement() : ""), "" + dateInterval.getDateFrom().getDayId(), "" + dateInterval.getDateTo().getDayId(), "", "" + sampling });
                if (log.isDebugEnabled()) {
                    log.debug("Service return url '" + dssUrl + "'");
                }
                if (dssUrl != null && !"".equals(dssUrl)) {
                    URL dataurl = new URL(dssUrl);
                    DataSequenceSet dsstmp = readDataSet(dataurl.openStream());
                    if (dsstmp != null && dsstmp.size() > 0) {
                        dsList.addAll(dsstmp);
                        if (log.isDebugEnabled()) {
                            log.debug("Data set list size is " + dsstmp.size());
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Data set list is empty");
                        }
                    }
                }
            } else {
                con = ConnectionPool.getConnection(table);
                stmt = con.createStatement();
                String className = Settings.get(table + ".classGetter");
                if (className == null) {
                    throw new ApiException("Undefined classGetter field for table '" + table + "'");
                }
                dsList = ((DBAccess) Class.forName(className).newInstance()).getDataSequence(stmt, descr, station, dateInterval, sampling);
            }
            return dsList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException("Data are not available: " + e.toString());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception e) {
            }
            ConnectionPool.releaseConnection(con);
        }
    }

    /** Gets all stations from metadata
   * @return Vector of station objects (class Station) and null if no stations were found
   */
    public Vector getStations() throws ApiException {
        return getStations("ALL", null);
    }

    /** Gets stations from metadata for given table
   * @param table The data table (like GOES, GEOM, IONO, etc) or ALL for all tables
   * @return Vector of station objects (class Station) and null if no stations were found
   */
    public Vector getStations(String table) throws ApiException {
        return getStations(table, null);
    }

    /** Gets all stations from metadata for given data interval
   * @param dateInterval The time period of data to be received
   * @return Vector of station objects (class Station) and null if no stations were found
   */
    public Vector getStations(DateInterval dateInterval) throws ApiException {
        return getStations(null, dateInterval);
    }

    /** Gets staions from metadata for given table and date interval
   * @param table The data table (like GOES, GEOM, IONO, etc) or ALL for all tables
   * @param dateInterval The time period of data to be received
   * @return Vector of station objects (class Station). It could be empty
   */
    public Vector getStations(String table, DateInterval dateInterval) throws ApiException {
        if (dateInterval != null && !dateInterval.isValid()) {
            throw new ApiException("Wrong dateInterval: " + dateInterval);
        }
        if (table == null) {
            table = "ALL";
        }
        Vector stations = new Vector();
        int[] inds = null;
        if (table.equals("ALL")) {
            inds = new int[metaStations.getRowCount()];
            for (int k = 0; k < inds.length; k++) {
                inds[k] = k;
            }
        } else {
            inds = metaStations.findAllRows(indStatTable, table);
        }
        for (int ind = 0; inds != null && ind < inds.length; ind++) {
            int nRow = inds[ind];
            String stn = (String) metaStations.getValueAt(nRow, indStatStation);
            if (stn.endsWith("_hr")) {
                stn = stn.substring(0, stn.length() - 3);
            } else if (stn.endsWith("_min")) {
                stn = stn.substring(0, stn.length() - 4);
            }
            String curTable = (String) metaStations.getValueAt(nRow, indStatTable);
            String name = (String) metaStations.getValueAt(nRow, indStatDescription);
            float lat = Station.BADVALUE;
            float lon = Station.BADVALUE;
            try {
                lat = Float.parseFloat((String) metaStations.getValueAt(nRow, indStatLat));
                lon = Float.parseFloat((String) metaStations.getValueAt(nRow, indStatLon));
            } catch (Exception e) {
            }
            int dayIdFrom = 0;
            int dayIdTo = 0;
            try {
                dayIdFrom = Integer.parseInt((String) metaStations.getValueAt(nRow, indStatDateFrom));
                dayIdTo = Integer.parseInt((String) metaStations.getValueAt(nRow, indStatDateTo));
                System.out.println((String) metaStations.getValueAt(nRow, indStatStation) + ", " + dayIdFrom + " - " + dayIdTo);
            } catch (Exception e) {
            }
            DateInterval curDateInterval = new DateInterval(new WDCDay(dayIdFrom), new WDCDay(dayIdTo));
            curDateInterval.correctBoundDates();
            if (dateInterval != null && !curDateInterval.isValid()) {
                continue;
            }
            if (dateInterval != null && !dateInterval.isIntersection(curDateInterval)) {
                continue;
            }
            Station curStation = new Station(stn, curTable, name, lat, lon);
            if (stations.indexOf(curStation) == -1) {
                stations.add(curStation);
            }
        }
        return stations;
    }

    /** Writes the object into a file.
   * @param filename The file name to write data
   */
    public static void writeDataSet(DataSequenceSet dss, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        GZIPOutputStream gzos = new GZIPOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(gzos);
        oos.writeObject(dss);
        oos.flush();
        oos.close();
    }

    /** Reads DataSequenceSet object from the stream.
   * @param strm The input stream object
   * @return DataSequenceSet object
   */
    public static DataSequenceSet readDataSet(InputStream is) throws IOException, StreamCorruptedException, ClassNotFoundException {
        GZIPInputStream gzis = new GZIPInputStream(is);
        ObjectInputStream ois = new ObjectInputStream(gzis);
        DataSequenceSet dss = (DataSequenceSet) ois.readObject();
        ois.close();
        gzis.close();
        return dss;
    }

    /** Used to test the class
   */
    public static void main(String[] args) {
        System.out.println("Activate settings: " + args[0]);
        try {
            if (args.length == 0) {
                System.out.println("No parameters: base conf-file required.");
                return;
            }
            Settings.getInstance().load(args[0]);
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        System.out.println("Start");
        long curTime = (new java.util.Date()).getTime();
        try {
            LocalApi api = new LocalApi();
            DataDescription descr = new DataDescription("Iono", "foE");
            descr.setMissingValue(9999f);
            Vector test = api.getData(descr, new Station("AA343", "", ""), new DateInterval(new WDCDay(19970124), new WDCDay(19971231)), 60);
            Vector stns = api.getStations("Iono", new DateInterval(new WDCDay(19970124), new WDCDay(19971231)));
            for (int i = 0; i < stns.size(); i++) {
                Station stn = (Station) stns.get(i);
                System.out.println(stn.getStn() + ", " + stn.getName());
            }
            DataSequence dss = (DataSequence) test.get(0);
            for (int i = 0; i < dss.size(); i++) {
                DailyData dd = (DailyData) dss.get(i);
                float[] data = dd.getData();
                for (int j = 0; j < data.length; j++) {
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        curTime = (new java.util.Date()).getTime() - curTime;
        System.out.println("Finish. Time: " + curTime / 1000f + " sec.");
    }

    private void jbInit() throws Exception {
    }
}
