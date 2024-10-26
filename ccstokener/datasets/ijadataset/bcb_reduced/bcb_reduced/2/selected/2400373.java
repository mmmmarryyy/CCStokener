package bahamontes;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 *
 * @author Onno Kluyt
 */
public class Track implements Serializable {

    /**
     * Creates a new instance of Track
     */
    public Track() {
        propertyChange = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChange.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChange.removePropertyChangeListener(l);
    }

    private void normalize() {
        if (firstTime) {
            firstTime = false;
            calculateMaxMin();
            if (hasTime) {
                uncorrectedStartTime = startTime;
                uncorrectedDate = date;
                if (timezone == 100) {
                    timezone = timezoneOffset(theTrack.get(0).getLatitude(), theTrack.get(0).getLongitude());
                }
                String tzName;
                if (timezone < 0) {
                    tzName = String.format("GMT" + "%d", timezone);
                } else {
                    tzName = String.format("GMT" + "+%d", timezone);
                }
                TimeZone tz = TimeZone.getTimeZone(tzName);
                year = Integer.parseInt(date.substring(0, date.indexOf('-')));
                int month = Integer.parseInt(date.substring(date.indexOf('-') + 1, date.lastIndexOf('-')));
                int day = Integer.parseInt(date.substring(date.lastIndexOf('-') + 1, date.length()));
                int hour = Integer.parseInt(startTime.substring(0, startTime.indexOf(':')));
                int minute = Integer.parseInt(startTime.substring(startTime.indexOf(':') + 1, startTime.lastIndexOf(':')));
                int second = Integer.parseInt(startTime.substring(startTime.lastIndexOf(':') + 1, startTime.length()));
                calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
                if (tz.inDaylightTime(calendar.getTime())) {
                    timezone++;
                }
                calendar.add(Calendar.HOUR, timezone);
                date = Utils.dateToString(calendar.getTime());
                startTime = Utils.timeToString(calendar.getTime());
                rideDateTime = date + " " + startTime;
            }
        }
        double dist = 0.0f;
        int i = 0;
        for (TrackPoint tp : theTrack) {
            tp.setLatitudeNormalized((tp.getLatitude() - minLatitude));
            tp.setLongitudeNormalized((tp.getLongitude() - minLongitude));
            tp.setAltitudeNormalized((tp.getAltitude() - minAltitude));
            if (!hasDistance && i > 0) {
                dist += calcLatLonDistance(i - 1, i);
                tp.setDistance((float) dist);
            }
            i++;
        }
        if (!hasDistance) {
            distance = String.valueOf(dist);
            hasDistance = true;
        }
    }

    public void smooth() {
        if (theTrack == null) {
            return;
        }
        long diff = 0L;
        for (int i = 1; i < theTrack.size(); i++) {
            TrackPoint p0 = theTrack.get(i - 1);
            TrackPoint p1 = theTrack.get(i);
            if (p1.getSpeed() < (float) minSpeed) {
                p1.setSpeedSmoothed(p0.getSpeedSmoothed());
            } else {
                p1.setSpeedSmoothed(p1.getSpeed());
            }
            if (p1.getCadence() < minCadence) {
                p1.setCadenceSmoothed(p0.getCadenceSmoothed());
            } else {
                p1.setCadenceSmoothed(p1.getCadence());
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean s) {
        saved = s;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean d) {
        dirty = d;
    }

    public boolean isMetric() {
        return metric;
    }

    public ArrayList<TrackPoint> getTrack() {
        return theTrack;
    }

    public void setTrack(ArrayList<TrackPoint> list) {
        theTrack = list;
        normalize();
        if (hasTime) {
            calculateMaxSpeed();
        }
        if (hasHR) {
            calculateMaxHR();
        }
        calculateTotalAscent();
        smooth();
        if (town.equals("")) {
            determineNearestTown();
        }
        loaded = true;
    }

    public String getDateTimeStamp() {
        return dateTimeStamp;
    }

    public void setDateTimeStamp(String s) {
        dateTimeStamp = s;
    }

    public float getMaxLatitude() {
        return maxLatitude;
    }

    public float getMinLatitude() {
        return minLatitude;
    }

    public float getMaxLongitude() {
        return maxLongitude;
    }

    public float getMinLongitude() {
        return minLongitude;
    }

    public float getMaxAltitude() {
        return maxLatitude;
    }

    public float getMinAltitude() {
        return maxLatitude;
    }

    public long getTimeBetweenPoints(int first, int second) {
        String s1 = theTrack.get(first).getTime();
        String date1 = getDatePortion(s1);
        long time1 = Utils.timeInSeconds(s1, true);
        String s2 = theTrack.get(second).getTime();
        String date2 = getDatePortion(s2);
        long time2 = Utils.timeInSeconds(s2, true);
        if (date1.equals(date2)) {
            return time2 - time1;
        } else {
            long midnight = 24 * 3600;
            return (midnight - time1) + time2;
        }
    }

    public float getDistanceBetweenPoints(int first, int second) {
        return theTrack.get(second).getDistance() - theTrack.get(first).getDistance();
    }

    public float getAverageLatitude() {
        return (minLatitude + maxLatitude) / 2.0f;
    }

    public float getAverageLongitude() {
        return (minLongitude + maxLongitude) / 2.0f;
    }

    public String getRideSpeed() {
        return String.format("%.2f", averageSpeed);
    }

    public float getRideSpeedAsFloat() {
        return averageSpeed;
    }

    public String getRideMaxSpeed() {
        return String.format("%.2f", maximumSpeed);
    }

    public float getRideMaxSpeedAsFloat() {
        return maximumSpeed;
    }

    public String getTotalAscent() {
        return String.format("%.0f", totalAscent);
    }

    public float getTotalAscentAsFloat() {
        return totalAscent;
    }

    public void setElevation(String elevation) {
        totalAscent = Float.valueOf(elevation);
    }

    public void setMinCadence(int v) {
        minCadence = v;
    }

    public void setMinSpeed(int v) {
        minSpeed = v;
    }

    public void setAvgSpeed(String s) {
        averageSpeed = Float.parseFloat(s);
    }

    public void setMaxSpeed(String s) {
        maximumSpeed = Float.parseFloat(s);
    }

    public void setRideDistance(String d) {
        distance = d;
    }

    public String getRideDistance() {
        return distance;
    }

    public float getRideDistanceAsFloat() {
        return Float.valueOf(distance);
    }

    public void setRideCadence(String c) {
        cadence = c;
    }

    public String getRideCadence() {
        return cadence;
    }

    public int getRideCadenceAsInt() {
        return Integer.parseInt(cadence);
    }

    public void setAvgHR(String c) {
        avgHR = c;
    }

    public String getAvgHR() {
        return avgHR;
    }

    public String getMaxHR() {
        return maxHR;
    }

    public void setMaxHR(String hr) {
        maxHR = hr;
    }

    public void setRideDuration(String d) {
        duration = d;
    }

    public String getRideDuration() {
        return duration;
    }

    public void setDurationInSeconds(String d) {
        durationInSeconds = Integer.parseInt(d);
        setRideDuration(Utils.secondsToString(durationInSeconds));
    }

    public int getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setRideDate(String d) {
        date = d;
    }

    public String getUncorrectedRideDate() {
        return uncorrectedDate;
    }

    public String getRideDate() {
        return date;
    }

    public String getUncorrectedRideStartTime() {
        return uncorrectedStartTime;
    }

    public String getRideStartTime() {
        return startTime;
    }

    public void setRideStartTime(String t) {
        startTime = t;
    }

    public void setRideName(String n) {
        name = n;
        dirty = true;
    }

    public String getRideName() {
        return name;
    }

    public void setBike(String s) {
        bike = s;
    }

    public String getBike() {
        return bike;
    }

    public void setRideDateTime(String s) {
        rideDateTime = s;
    }

    public String getRideDateTime() {
        return rideDateTime;
    }

    public boolean hasHR() {
        return hasHR;
    }

    public void setHasHR(boolean v) {
        hasHR = v;
    }

    public Boolean hasCadence() {
        return hasCadence;
    }

    public void setHasCadence(boolean v) {
        hasCadence = v;
    }

    public Boolean hasTime() {
        return hasTime;
    }

    public void setHasTime(boolean v) {
        hasTime = v;
    }

    public void setHasDistance(boolean v) {
        hasDistance = v;
    }

    public boolean hasDistance() {
        return hasDistance;
    }

    public boolean hasGPS() {
        return hasGPSData;
    }

    public boolean hasLoaded() {
        return loaded;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean l) {
        loading = l;
    }

    public void setGPS(boolean gps) {
        hasGPSData = gps;
        if (!gps || calendar == null) {
            year = Utils.getYear(date);
            int month = Utils.getMonth(date);
            int day = Utils.getDay(date);
            calendar = new GregorianCalendar(year, month - 1, day);
        }
    }

    public void setTimeZone(int tz) {
        timezone = tz;
    }

    public int getTimeZone() {
        return timezone;
    }

    public String getNearestTown() {
        return town;
    }

    public void setTown(String t) {
        town = t;
    }

    public String getYear() {
        return Integer.toString(year);
    }

    public GregorianCalendar getCalendar() {
        return calendar;
    }

    public boolean isLastAdded() {
        return lastAdded;
    }

    public void setLastAdded(boolean b) {
        lastAdded = b;
    }

    public double getMapDiameter() {
        return mapDiameter;
    }

    public void setID(int i) {
        id = i;
    }

    public int getID() {
        return id;
    }

    private String getDatePortion(String s) {
        return s.substring(0, 10);
    }

    private void calculateMaxHR() {
        int max = 0;
        for (TrackPoint p : theTrack) {
            if (max < p.getHR()) {
                max = p.getHR();
            }
        }
        maxHR = Integer.toString(max);
    }

    private void calculateTotalAscent() {
        float totAscent = 0.0f;
        float previous = theTrack.get(0).getAltitude();
        for (TrackPoint point : theTrack) {
            float current = point.getAltitude();
            if (previous < current) {
                totAscent += (current - previous);
            }
            previous = current;
        }
        totalAscent = totAscent;
    }

    private void calculateMaxSpeed() {
        float maxSpeed = 0.0f;
        long progressedTime = 0L;
        float progressedDistance = 0.0f;
        boolean recheckSpeeds = false;
        for (int i = 0; i < theTrack.size() - 1; i++) {
            TrackPoint p1 = theTrack.get(i);
            TrackPoint p2 = theTrack.get(i + 1);
            float speed = 0.0f;
            boolean timegap = false;
            long time = getTimeBetweenPoints(i, i + 1);
            float d = getDistanceBetweenPoints(i, i + 1);
            if (i > 0) {
                if (d == 0f) {
                    time = 0L;
                } else {
                    if (time > 30L) {
                        time = 9L;
                        timegap = true;
                    }
                }
            }
            progressedTime += time;
            if (time != 0L && !timegap) {
                speed = (d / (float) (time)) * 3600f;
            } else {
                speed = p1.getSpeed();
            }
            if (speed < p1.getSpeed() * 3 && !timegap) {
                if (speed > maxSpeed) {
                    maxSpeed = speed;
                }
            } else {
                recheckSpeeds = true;
            }
            p2.setSpeed(speed);
            progressedDistance += d;
            p2.setAvgSpeed((progressedDistance / (float) progressedTime) * 3600f);
        }
        maximumSpeed = maxSpeed;
        averageSpeed = (progressedDistance / (float) progressedTime) * 3600f;
        setRideDuration(Utils.parseTime(Long.toString(progressedTime)));
        if (recheckSpeeds) {
            for (int i = 1; i < theTrack.size(); i++) {
                TrackPoint p = theTrack.get(i);
                if (p.getSpeed() > maximumSpeed) {
                    p.setSpeed(theTrack.get(i - 1).getSpeed());
                }
            }
        }
    }

    private void calculateMaxMin() {
        TrackPoint tp = theTrack.get(0);
        minLatitude = tp.getLatitude();
        maxLatitude = tp.getLatitude();
        minLongitude = tp.getLongitude();
        maxLongitude = tp.getLongitude();
        minAltitude = tp.getAltitude();
        maxAltitude = tp.getAltitude();
        for (int i = 1; i < theTrack.size(); i++) {
            tp = theTrack.get(i);
            float lat = tp.getLatitude();
            float lon = tp.getLongitude();
            float alt = tp.getAltitude();
            if (maxLatitude < lat) {
                maxLatitude = lat;
            }
            if (minLatitude > lat) {
                minLatitude = lat;
            }
            if (maxLongitude < lon) {
                maxLongitude = lon;
            }
            if (minLongitude > lon) {
                minLongitude = lon;
            }
            if (minAltitude > alt) {
                minAltitude = alt;
            }
            if (maxAltitude < alt) {
                maxAltitude = alt;
            }
        }
    }

    private double calcLatLonDistance(int one, int two) {
        double latA = theTrack.get(one).getLatitude() * Utils.RAD;
        double lonA = theTrack.get(one).getLongitude() * Utils.RAD;
        double latB = theTrack.get(two).getLatitude() * Utils.RAD;
        double lonB = theTrack.get(two).getLongitude() * Utils.RAD;
        double cosAOB = StrictMath.cos(latA) * StrictMath.cos(latB) * StrictMath.cos(lonB - lonA) + StrictMath.sin(latA) * StrictMath.sin(latB);
        double result = StrictMath.acos(cosAOB) * 6371.009f;
        return result;
    }

    private String grabInformationFromWeb(String query, String infoName) throws Exception {
        String result = "";
        URL url = new URL(query);
        HttpURLConnection request = null;
        request = (HttpURLConnection) url.openConnection();
        if (request != null) {
            InputStream in = url.openStream();
            int c = 0;
            StringBuilder sb = new StringBuilder();
            while ((c = in.read()) != -1) {
                sb = sb.append((char) c);
            }
            String s = sb.toString();
            result = Utils.getTagValue(s, "<" + infoName + ">", "</" + infoName + ">");
            in.close();
        }
        return result;
    }

    private void determineNearestTown() {
        String oldvalue = town;
        float lat = theTrack.get(0).getLatitude();
        float lon = theTrack.get(0).getLongitude();
        geoCodeString = String.format(Locale.US, geoCodeString, lat, lon);
        try {
            town = grabInformationFromWeb(geoCodeString, "name");
            if (!town.equals("")) {
                if (town.indexOf('(') != -1) {
                    town = town.substring(0, town.indexOf('('));
                    dirty = true;
                }
            } else {
                town = "Not available";
            }
        } catch (Exception ex) {
            System.out.println("DetermineNearestTown: " + ex);
            town = "Not available";
        }
        propertyChange.firePropertyChange(NEAREST_TOWN, oldvalue, town);
    }

    private int timezoneOffset(float lat, float lon) {
        int offset = 0;
        timezoneQuery = String.format(Locale.US, timezoneQuery, lat, lon);
        try {
            String result = grabInformationFromWeb(timezoneQuery, "offset");
            if (result != null) {
                offset = Integer.parseInt(result);
            }
        } catch (Exception ex) {
            System.out.println("timezoneOffset: exception: " + ex);
        }
        return offset;
    }

    public static final String NEAREST_TOWN = "Nearest_Town";

    private boolean dirty = false;

    private boolean saved = false;

    private PropertyChangeSupport propertyChange;

    private ArrayList<TrackPoint> theTrack = null;

    private float minLatitude = 0.0f;

    private float minLongitude = 0.0f;

    private float maxLatitude = 0.0f;

    private float maxLongitude = 0.0f;

    private float minAltitude = 0.0f;

    private float maxAltitude = 0.0f;

    private boolean firstTime = true;

    private boolean metric = true;

    private boolean hasTime = true;

    private boolean hasDistance = true;

    private boolean hasCadence = true;

    private boolean hasHR = false;

    private int minCadence = 10;

    private int minSpeed = 5;

    private String geoCodeString = "http://ws.geonames.org/findNearbyPlaceName?lat=%f&lng=%f";

    private String timezoneQuery = "http://www.earthtools.org/timezone/%f/%f";

    private float averageSpeed = 0.0f;

    private float maximumSpeed = 0.0f;

    private float totalAscent = 0.0f;

    private String maxHR = "0";

    private String distance = "0";

    private String duration = "0";

    private int durationInSeconds = 0;

    private String date = "";

    private String uncorrectedDate = "";

    private String startTime = "";

    private String uncorrectedStartTime = "";

    private String rideDateTime = "";

    private int timezone = 100;

    private String cadence = "0";

    private String avgHR = "0";

    private String name = "";

    private String bike = "Maximilian";

    private String town = "";

    private int year = 0;

    private GregorianCalendar calendar = null;

    private boolean lastAdded = false;

    private double mapDiameter = 0;

    private boolean hasGPSData = true;

    private boolean loaded = false;

    private boolean loading = false;

    private int id = 0;

    private String dateTimeStamp = "";
}
