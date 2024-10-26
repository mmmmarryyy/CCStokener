package org.anrc.poi;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.IBinder;

public class PoiListService extends Service {

    private Timer updateTimer;

    public static final String NEW_POI_FOUND = "New_Poi_Found";

    private Notification newPoiNotification;

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onStart(Intent intent, int startId) {
        SharedPreferences prefs = getSharedPreferences(PoiPreferences.USER_PREFERENCES, Activity.MODE_PRIVATE);
        int freqIndex = prefs.getInt(PoiPreferences.PREF_UPDATE_FREQ, 0);
        if (freqIndex < 0) {
            freqIndex = 0;
        }
        boolean autoUpdate = prefs.getBoolean(PoiPreferences.PREF_AUTO_UPDATE, false);
        Resources r = getResources();
        int[] freqValues = r.getIntArray(R.array.update_freq_values);
        int updateFreq = freqValues[freqIndex];
        updateTimer.cancel();
        if (autoUpdate) {
            updateTimer = new Timer("poiUpdates");
            updateTimer.scheduleAtFixedRate(doRefresh, 0, updateFreq * 60 * 1000);
        } else refreshPois();
    }

    ;

    private TimerTask doRefresh = new TimerTask() {

        public void run() {
            refreshPois();
        }
    };

    @Override
    public void onCreate() {
        updateTimer = new Timer("poiUpdates");
        int icon = R.drawable.bubble;
        String tickerText = "Nowe POI";
        long when = System.currentTimeMillis();
        newPoiNotification = new Notification(icon, tickerText, when);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void addNewPoi(Poi _poi) {
        ContentResolver cr = getContentResolver();
        String w = PoiProvider.KEY_LOCATION_LAT + " = " + _poi.getLocation().getLatitude() + " and " + PoiProvider.KEY_LOCATION_LNG + " = " + _poi.getLocation().getLongitude();
        Cursor c = cr.query(PoiProvider.CONTENT_URI, null, w, null, null);
        int dbCount = c.getCount();
        if (dbCount == 0) {
            ContentValues values = new ContentValues();
            values.put(PoiProvider.KEY_NAME, _poi.getName());
            double lat = _poi.getLocation().getLatitude();
            double lng = _poi.getLocation().getLongitude();
            values.put(PoiProvider.KEY_LOCATION_LAT, lat);
            values.put(PoiProvider.KEY_LOCATION_LNG, lng);
            cr.insert(PoiProvider.CONTENT_URI, values);
            announceNewPoi(_poi);
        }
        c.close();
    }

    private void announceNewPoi(Poi _poi) {
        String svcName = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager;
        notificationManager = (NotificationManager) getSystemService(svcName);
        Context context = getApplicationContext();
        String expandedTitle = _poi.getName();
        String expandedText = "Szer:" + _poi.getLocation().getLatitude() + " Dl: " + _poi.getLocation().getLongitude();
        Intent startActivityIntent = new Intent(this, Poi.class);
        PendingIntent launchIntent = PendingIntent.getActivity(context, 0, startActivityIntent, 0);
        newPoiNotification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
        newPoiNotification.when = java.lang.System.currentTimeMillis();
        notificationManager.notify(NOTIFICATION_ID, newPoiNotification);
        Intent intent = new Intent(NEW_POI_FOUND);
        intent.putExtra("name", _poi.getName());
        intent.putExtra("longitude", _poi.getLocation().getLongitude());
        intent.putExtra("latitude", _poi.getLocation().getLatitude());
        sendBroadcast(intent);
    }

    private void refreshPois() {
        Thread updateThread = new Thread(null, backgroundRefresh, "refresh_poi");
        updateThread.start();
    }

    private Runnable backgroundRefresh = new Runnable() {

        public void run() {
            doRefreshPois();
        }
    };

    private void doRefreshPois() {
        URL url;
        try {
            SharedPreferences prefs;
            prefs = getSharedPreferences(MainPreferences.USER_PREFERENCES, Activity.MODE_PRIVATE);
            String poiFeed = prefs.getString(PoiPreferences.PREF_UPDATE_URL, "http://poi-radar.googlecode.com/files/dolny_slask.kml");
            url = new URL(poiFeed);
            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf;
                dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("Placemark");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element Placemark = (Element) nl.item(i);
                        Element name = (Element) Placemark.getElementsByTagName("name").item(0);
                        Element g = (Element) Placemark.getElementsByTagName("Point").item(0);
                        Element coordinates = (Element) g.getElementsByTagName("coordinates").item(0);
                        String nameString = name.getFirstChild().getNodeValue();
                        String point = coordinates.getFirstChild().getNodeValue();
                        String[] location = point.split(",");
                        Location l = new Location("dummyGPS");
                        l.setLongitude(Double.parseDouble(location[0]));
                        l.setLatitude(Double.parseDouble(location[1]));
                        Poi poi = new Poi(nameString, l);
                        addNewPoi(poi);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
        }
    }
}
