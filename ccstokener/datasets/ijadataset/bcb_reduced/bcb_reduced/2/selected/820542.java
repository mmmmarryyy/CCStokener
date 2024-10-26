package com.juryrig.couchweather;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.GregorianCalendar;
import javax.swing.ProgressMonitor;
import org.svenson.JSONParser;
import org.svenson.tokenize.InputStreamSource;

/**
 *   CouchWeather - view weather stored in a CouchDb by
 *   Per Ejeklint's (http://www.ejeklint.se) excellent Mac-based program:
 *   WLoggerDaemon ( http://github.com/ejeklint/WLoggerDaemon)
 *   Copyright (C) 2009 - Robert A. Yetman
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.

 *   This program is distributed in the hope that it will be useful, but
 *   WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *   General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
public class WeatherDB {

    private Calendar oldest = null;

    private Calendar oldestLoaded = null;

    private boolean isLoading = false;

    private String dbURL;

    private String dbName;

    private final String deviceStatusDocName = "device_status";

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final TreeMap<String, Conditions> historicalData = new TreeMap<String, Conditions>();

    ;

    @SuppressWarnings("unchecked")
    public WeatherDB(Settings settings) {
        settings.getDbPollSecs();
        dbURL = new String("http://" + settings.getDbHost() + ":" + settings.getDbPort() + "/");
        dbName = settings.getDbName();
        boolean connected = false;
        try {
            List l = getURLList("_all_dbs");
            if (l != null && l.contains(dbName)) {
                connected = true;
            }
        } catch (IOException ioe) {
            System.err.println("Error connecting to couchdb: " + ioe.getLocalizedMessage());
        }
        if (connected == false) {
            System.err.println("Unable to connect to couchdb at " + dbURL + " or database " + dbName + " doesn't exist");
            System.exit(-1);
        }
    }

    public boolean haveData() {
        return historicalData != null;
    }

    @SuppressWarnings("unchecked")
    private List getURLList(String request) throws IOException {
        List list = null;
        try {
            URL url = new URL(dbURL + request);
            URLConnection conn = url.openConnection();
            conn.connect();
            JSONParser parser = JSONParser.defaultJSONParser();
            InputStreamSource stream = new InputStreamSource(conn.getInputStream(), true);
            list = parser.parse(List.class, stream);
            stream.destroy();
        } catch (MalformedURLException mue) {
            System.err.println("Internal malformed url Exception: " + mue);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Map getURLMap(String request) throws IOException {
        Map map = null;
        try {
            URL url = new URL(dbURL + request);
            URLConnection conn = url.openConnection();
            conn.connect();
            JSONParser parser = JSONParser.defaultJSONParser();
            InputStreamSource stream = new InputStreamSource(conn.getInputStream(), true);
            map = parser.parse(Map.class, stream);
            stream.destroy();
        } catch (MalformedURLException mue) {
            System.err.println("Internal malformed url Exception: " + mue);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public DeviceStatus getDeviceStatus() {
        DeviceStatus status = null;
        try {
            Map doc = getURLMap(dbName + "/" + this.deviceStatusDocName);
            if (doc != null) {
                String deviceTime = (String) doc.get("baseStationTime");
                GregorianCalendar statusTime = new GregorianCalendar();
                if (deviceTime != null) {
                    try {
                        statusTime.setTime(format.parse(deviceTime));
                        status = new DeviceStatus();
                        status.setBaseStationTime(statusTime);
                        status.setBaseBatteryLevel((String) doc.get("baseBatteryLevel"));
                        status.setBaseExternalPower((String) doc.get("baseHasExternalPower"));
                        status.setBaseHasRadioSync((String) doc.get("baseHasRadioSync"));
                        status.setTemperatureBatterySensor0((String) doc.get("tempBatteryLevelSensor_0"));
                        status.setTemperatureBatterySensor1((String) doc.get("tempBatteryLevelSensor_1"));
                        status.setWindBatteryLevel((String) doc.get("windBatteryLevel"));
                    } catch (ParseException e) {
                        status = null;
                    }
                }
            }
        } catch (IOException e1) {
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    public Conditions getCurrentConditions() {
        Conditions conditions = null;
        try {
            Map docs = getURLMap(dbName + "/_all_docs?limit=1&descending=true&endkey_docid=1900&startkey_docid=9999&include_docs=true");
            ArrayList rows = (ArrayList) docs.get("rows");
            Map current = (Map) rows.get(0);
            String id = (String) current.get("id");
            conditions = parseConditions(id, (HashMap) current.get("doc"));
            addToHistory(id, conditions);
        } catch (IOException e) {
        }
        return conditions;
    }

    @SuppressWarnings("unchecked")
    Conditions parseConditions(String id, Map doc) {
        Conditions conditions = null;
        if (id != null && id.length() > 0 && doc != null) {
            GregorianCalendar now = new GregorianCalendar();
            try {
                now.setTime(format.parse(id));
                conditions = new Conditions();
                conditions.setCurrentTime(now);
                conditions.setForecast((String) doc.get("fc_str"));
                conditions.setHumidityIndoors((Number) doc.get("h_in"));
                conditions.setHumidityOutdoors((Number) doc.get("h_out"));
                conditions.setPressureRelative((Number) doc.get("p_rel"));
                conditions.setPressureRelativeForecast((Number) doc.get("p_rel_fc"));
                conditions.setPressureAbsolute((Number) doc.get("p_abs"));
                conditions.setPressureAbsoluteForecast((Number) doc.get("p_abs_fc"));
                conditions.setRain1hours((Number) doc.get("r_1h"));
                conditions.setRain24hours((Number) doc.get("r_24h"));
                conditions.setTemperatureDew((Number) doc.get("t_dew"));
                conditions.setTemperatureDewReported((Number) doc.get("t_dew_reported"));
                conditions.setTemperatureIndoors((Number) doc.get("t_in"));
                conditions.setTemperatureOutdoors((Number) doc.get("t_out"));
                conditions.setWindAverage((Number) doc.get("w_av"));
                conditions.setWindChill((Number) doc.get("w_chill"));
                conditions.setWindDirection((Number) doc.get("w_dir"));
                conditions.setWindGust((Number) doc.get("w_gust"));
            } catch (NumberFormatException e) {
                conditions = null;
            } catch (ParseException e) {
                conditions = null;
            }
        }
        return conditions;
    }

    private Conditions addToHistory(String id, Conditions conditions) {
        if (conditions != null && historicalData != null) {
            historicalData.put(id, conditions);
            Calendar curr = conditions.getCurrentTime();
            if (oldest == null) {
                oldest = curr;
            } else if (oldest.after(curr)) {
                oldest = curr;
            }
        }
        return conditions;
    }

    @SuppressWarnings("unchecked")
    public void loadHistory(int nrDays) {
        this.isLoading = true;
        ProgressMonitor monitor = new ProgressMonitor(null, "Loading Weather History", null, 0, 0);
        Map database;
        try {
            int count = 0;
            ArrayList<String> ids = new ArrayList<String>();
            Calendar cal = null;
            if (nrDays > 0 && nrDays < 9999) {
                cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -1 * nrDays);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
            }
            if (cal == null || oldestLoaded == null || cal.before(this.oldest)) {
                String startDate = (cal == null) ? "1900-01-01 00:01" : AbstractTrendPane.formatDate(cal);
                String endDate = (oldestLoaded == null) ? "9999-12-31 23:59" : AbstractTrendPane.formatDate(oldestLoaded);
                database = getURLMap(dbName + "/_all_docs?descending=false&endkey_docid=" + reformatDateURL(endDate) + "&startkey_docid=" + reformatDateURL(startDate));
                if (database != null) {
                    Object list = database.get("rows");
                    if (list instanceof ArrayList) {
                        monitor.setMaximum(((ArrayList) list).size());
                        for (Object o : (ArrayList) list) {
                            if (o instanceof HashMap) {
                                HashMap doc = (HashMap) o;
                                String id = (String) doc.get("id");
                                if (id != null) {
                                    ids.add(id);
                                } else {
                                    monitor.setProgress(++count);
                                }
                            }
                        }
                    }
                }
                for (String id : ids) {
                    Map doc = getURLMap(dbName + "/" + reformatDateURL(id));
                    if (doc != null) {
                        monitor.setProgress(++count);
                        Conditions conditions = parseConditions(id, doc);
                        addToHistory(id, conditions);
                    }
                    if (monitor.isCanceled()) {
                        this.oldestLoaded = null;
                        break;
                    }
                }
                if (!monitor.isCanceled() && ids.size() > 0) {
                    try {
                        oldestLoaded = Calendar.getInstance();
                        oldestLoaded.setTime(format.parse(ids.get(0)));
                    } catch (ParseException e) {
                        oldestLoaded = null;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error retrieving historical data:" + e.getLocalizedMessage());
            historicalData.clear();
        }
        monitor.close();
        isLoading = false;
    }

    /**
	 * for queries to work they have to be correct http
	 * so replace the space and colon in the date with the http equivalents
	 */
    private String reformatDateURL(String date) {
        return date.replace(" ", "%20").replace(":", "%3A");
    }

    public Calendar getOldestDate() {
        return oldest;
    }

    public <T> Collection<Conditions> getHistoricalData() {
        Collection<Conditions> cond = Collections.synchronizedCollection(historicalData.values());
        return cond;
    }

    public boolean isLoading() {
        return this.isLoading;
    }

    public String getOldestLoadedString() {
        return (oldestLoaded == null) ? "None" : AbstractTrendPane.formatDate(oldestLoaded);
    }
}
