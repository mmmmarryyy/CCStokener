package com.realsnake.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

/**
 * @author wjsrkddnr
 *
 */
public class EarthquakeService extends Service {

    private Timer updateTimer = null;

    private TimerTask refreshTask = null;

    private float minimumMagnitude = 0f;

    private boolean autoUpdate = false;

    public static final String NEW_EARTHQUAKE_FOUND = "New_Earthquake_Found";

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        this.updateTimer = new Timer(this.getString(R.string.task_name));
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        SharedPreferences prefs = this.getSharedPreferences(Preferences.USER_PREFERENCE, Activity.MODE_PRIVATE);
        int minMagIndex = prefs.getInt(Preferences.PREF_MIN_MAG, 0);
        if (minMagIndex < 0) minMagIndex = 0;
        int freqIndex = prefs.getInt(Preferences.PREF_UPDATE_FREQ, 0);
        if (freqIndex < 0) freqIndex = 0;
        this.autoUpdate = prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);
        Resources r = this.getResources();
        int[] minMagValues = r.getIntArray(R.array.magnitude);
        this.minimumMagnitude = minMagValues[minMagIndex];
        int[] freqValues = r.getIntArray(R.array.update_freq_values);
        int updateFreq = freqValues[freqIndex];
        if (this.refreshTask != null) {
            Log.d("EarthquakeService.onStart()", "TimerTask(Frequently refreshEarthquakes() run) canceled!");
            this.refreshTask.cancel();
            this.refreshTask = null;
        }
        if (this.autoUpdate) {
            Log.d("EarthquakeService.onStart()", "New TimerTask object created, frequently refreshEarthquakes() run!");
            this.updateTimer.scheduleAtFixedRate(this.refreshTask = new TimerTask() {

                public void run() {
                    refreshEarthquakes();
                }
            }, 0, updateFreq * 60 * 1000);
        } else {
            Log.d("EarthquakeService.onStart()", "refreshEarthquakes() run!");
            this.refreshEarthquakes();
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * 지진 Feed에 접속해 가져온 XML을 통해 지진 정보(날짜, 진도, 링크, 위치)를 추출한 후, addNewQuake 메서드에 전달한다.
     */
    private void doRefreshEarthquakes() {
        Log.d("EarthquakeService.doRefreshEarthquakes()", "Getting feed from Earthquake RSS!");
        URL url = null;
        String quakeFeed = this.getString(R.string.quake_feed);
        HttpURLConnection httpConnection = null;
        try {
            url = new URL(quakeFeed);
            URLConnection connection = url.openConnection();
            httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList nl = docEle.getElementsByTagName("entry");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element entry = (Element) nl.item(i);
                        Element title = (Element) entry.getElementsByTagName("title").item(0);
                        Element g = (Element) entry.getElementsByTagName("georss:point").item(0);
                        Element when = (Element) entry.getElementsByTagName("updated").item(0);
                        Element link = (Element) entry.getElementsByTagName("link").item(0);
                        String details = title.getFirstChild().getNodeValue();
                        String magnitudeString = details.split(" ")[1];
                        int end = magnitudeString.length() - 1;
                        double magnitude = Double.parseDouble(magnitudeString.substring(0, end));
                        String linkString = link.getAttribute("href");
                        String point = g.getFirstChild().getNodeValue();
                        String dt = when.getFirstChild().getNodeValue();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
                        Date qdate = new GregorianCalendar(0, 0, 0).getTime();
                        try {
                            qdate = sdf.parse(dt);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        String[] locations = point.split(" ");
                        Location location = new Location("dummyGPS");
                        location.setLatitude(Double.parseDouble(locations[0]));
                        location.setLongitude(Double.parseDouble(locations[1]));
                        details = details.split(",")[1].trim();
                        Quake quake = new Quake(qdate, details, location, magnitude, linkString);
                        this.addNewQuake(quake);
                    }
                }
                in.close();
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
            if (httpConnection != null) httpConnection.disconnect();
        }
    }

    /**
     * 새 지진 정보를 ArrayList(earthquakeList)에 추가한 후, ArrayAdapter(aa)에 통지한다.
     * => 애플리케이션의 ContentResolver를 이용해 각각의 새로운 지진 정보를 공급자에 삽입한다.
     * 
     * @param quake
     */
    private void addNewQuake(Quake quake) {
        String w = EarthquakeProvider.KEY_DATE + "=" + quake.getDate().getTime();
        Cursor c = this.getContentResolver().query(EarthquakeProvider.CONTENT_URI, null, w, null, null);
        int dbCount = c.getCount();
        c.close();
        if (dbCount == 0) {
            ContentValues values = new ContentValues();
            values.put(EarthquakeProvider.KEY_DATE, quake.getDate().getTime());
            values.put(EarthquakeProvider.KEY_DETAILS, quake.getDetails());
            double lat = quake.getLocation().getLatitude();
            double lng = quake.getLocation().getLongitude();
            values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
            values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
            values.put(EarthquakeProvider.KEY_LINK, quake.getLink());
            values.put(EarthquakeProvider.KEY_MAGNITUDE, quake.getMagnitude());
            this.getContentResolver().insert(EarthquakeProvider.CONTENT_URI, values);
            Log.d("EarthquakeService.addNewQuake()", "New earthquake(" + quake.toString() + ") added at EarthquakeProvider!");
            if (quake.getMagnitude() > this.minimumMagnitude) {
                this.announceNewQuake(quake);
            }
        }
    }

    /**
     * 새로운 지진이 발견될 때마다 새로운 인텐트를 방송한다.
     * 
     * @param quake
     */
    private void announceNewQuake(Quake quake) {
        NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification newEarthquakeNotification = new Notification();
        newEarthquakeNotification.icon = R.drawable.icon;
        newEarthquakeNotification.tickerText = this.getString(R.string.new_ticker);
        newEarthquakeNotification.when = System.currentTimeMillis();
        Intent earthquakeActivityIntent = new Intent(this, Earthquake.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, earthquakeActivityIntent, 0);
        String expandedTitle = "M:" + quake.getMagnitude() + " " + quake.getDetails();
        String expandedText = quake.getDate().toString();
        newEarthquakeNotification.setLatestEventInfo(this, expandedTitle, expandedText, pi);
        double vibrateLength = 100 * Math.exp(0.53 * quake.getMagnitude());
        long[] vibrate = { 100, 100, (long) vibrateLength };
        newEarthquakeNotification.vibrate = vibrate;
        int color = 0;
        if (quake.getMagnitude() < 5) {
            color = Color.GREEN;
        } else if (quake.getMagnitude() < 6) {
            color = Color.YELLOW;
        } else {
            color = Color.RED;
        }
        newEarthquakeNotification.ledARGB = color;
        newEarthquakeNotification.ledOffMS = (int) vibrateLength;
        newEarthquakeNotification.ledOnMS = (int) vibrateLength;
        newEarthquakeNotification.flags = newEarthquakeNotification.flags | Notification.FLAG_SHOW_LIGHTS;
        nm.notify(NOTIFICATION_ID, newEarthquakeNotification);
        Intent intent = new Intent(NEW_EARTHQUAKE_FOUND);
        intent.putExtra("date", quake.getDate().getTime());
        intent.putExtra("details", quake.getDetails());
        intent.putExtra("longitude", quake.getLocation().getLongitude());
        intent.putExtra("latitude", quake.getLocation().getLatitude());
        intent.putExtra("magnitude", quake.getMagnitude());
        Log.d("EarthquakeService.announceNewQuake()", "New notification added and send broadcast!");
        this.sendBroadcast(intent);
    }

    /**
     * doRefreshEarthquakes()를 백그라운드 서비스(쓰레드)로 실행하기
     */
    private void refreshEarthquakes() {
        Thread updateThread = new Thread(null, this.backgroundRefresh, "refresh_earthquake");
        updateThread.start();
        Log.d("EarthquakeService.refreshEarthquakes()", "doRefreshEarthquakes() run with child-thread! New thread created, Thread ID=" + updateThread.getId() + ", Thread name=" + updateThread.getName());
    }

    private Runnable backgroundRefresh = new Runnable() {

        @Override
        public void run() {
            Log.d("EarthquakeService.backgroundRefresh.run()", "doRefreshEarthquakes() run!");
            doRefreshEarthquakes();
        }
    };
}
