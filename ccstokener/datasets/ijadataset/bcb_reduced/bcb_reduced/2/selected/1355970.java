package com.android.WiFiQualityMeasurement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WiFiQualityMeasurementService extends Service implements LocationListener {

    private String mStringFilePath;

    private File mFile;

    private FileOutputStream mFos;

    private TelephonyManager mTelephonyManager;

    private String mStringLineType;

    private WifiManager mWifiManager;

    private WifiInfo mWifiInfo;

    private ConnectivityManager mConManager;

    private NetworkInfo mNetInfo;

    private String mStringThroughput;

    private String mStringElectricFieldStrength;

    private Calendar mCalendar;

    private String mStringDayTime;

    private String mStringDayTime2;

    private LocationManager mLocationManager;

    private String mStringLatitude;

    private String mStringLongitude;

    private String mStringBssid;

    private String mStringMacAddress;

    private String mStringModel;

    private String mStringSsid;

    private String mStringCareerName;

    private int mInterval = 50;

    private int mTickCount;

    private Timer mTimer;

    public static final String ACTION = "WiFiQualityMeasurement Service";

    @Override
    public void onCreate() {
        super.onCreate();
        mCalendar = Calendar.getInstance();
        mStringFilePath = Environment.getExternalStorageDirectory() + "/" + Integer.toString(mCalendar.get(Calendar.YEAR)) + Integer.toString(mCalendar.get(Calendar.MONTH)) + Integer.toString(mCalendar.get(Calendar.DAY_OF_MONTH)) + Integer.toString(mCalendar.get(Calendar.HOUR_OF_DAY)) + Integer.toString(mCalendar.get(Calendar.MINUTE)) + Integer.toString(mCalendar.get(Calendar.SECOND)) + Integer.toString(mCalendar.get(Calendar.MILLISECOND)) + ".txt";
        Log.d("WiFiQualityMeasurementService", "FilePath = " + mStringFilePath);
        mTimer = null;
        mStringLatitude = "";
        mStringLongitude = "";
        mFile = new File(mStringFilePath);
        if (mFile != null) {
            mFile.getParentFile().mkdir();
        }
        mTimer = new Timer(true);
        final android.os.Handler handler = new android.os.Handler();
        if (mTimer != null && handler != null) {
            mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    handler.post(new Runnable() {

                        public void run() {
                            mTickCount++;
                            if (mTickCount >= mInterval) {
                                getState();
                                sendLog();
                                mTickCount = 0;
                            }
                        }
                    });
                }
            }, 1000, 100);
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager != null) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        getState();
        sendLog();
    }

    @Override
    public void onDestroy() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        if (mTimer != null) {
            mTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void getState() {
        mTelephonyManager = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE));
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        if (mWifiManager != null) {
            mWifiInfo = mWifiManager.getConnectionInfo();
        }
        mCalendar = Calendar.getInstance();
        mConManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConManager != null) {
            mNetInfo = mConManager.getActiveNetworkInfo();
        }
        if (mNetInfo != null) {
            if (mNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                mStringLineType = "WiFi";
                if (mTelephonyManager != null) {
                    mStringCareerName = mTelephonyManager.getNetworkOperatorName();
                }
            } else if (mNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (mTelephonyManager != null) {
                    mStringLineType = "3G";
                    mStringCareerName = mTelephonyManager.getNetworkOperatorName();
                }
            } else {
                mStringLineType = "Other";
                mStringCareerName = "";
            }
        }
        if (mWifiInfo != null) {
            mStringThroughput = Integer.toString(mWifiInfo.getLinkSpeed());
            mStringElectricFieldStrength = Integer.toString(mWifiInfo.getRssi());
            mStringBssid = mWifiInfo.getBSSID();
            mStringMacAddress = mWifiInfo.getMacAddress();
            mStringSsid = mWifiInfo.getSSID();
        }
        if (mCalendar != null) {
            mStringDayTime = (String.format("%02d", mCalendar.get(Calendar.YEAR)) + "/" + String.format("%02d", mCalendar.get(Calendar.MONTH)) + "/" + String.format("%02d", mCalendar.get(Calendar.DAY_OF_MONTH)) + " " + String.format("%02d", mCalendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", mCalendar.get(Calendar.MINUTE)) + ":" + String.format("%02d", mCalendar.get(Calendar.SECOND)) + ":" + String.format("%03d", mCalendar.get(Calendar.MILLISECOND)));
            mStringDayTime2 = (String.format("%02d", mCalendar.get(Calendar.YEAR)) + "/" + String.format("%02d", mCalendar.get(Calendar.MONTH)) + "/" + String.format("%02d", mCalendar.get(Calendar.DAY_OF_MONTH)) + " " + String.format("%02d", mCalendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", mCalendar.get(Calendar.MINUTE)) + ":" + String.format("%02d", mCalendar.get(Calendar.SECOND)));
        }
        mStringModel = Build.MODEL;
    }

    public void saveState() {
        try {
            if (mFile != null) {
                mFos = new FileOutputStream(mFile, true);
            }
            if (mFos != null) {
                OutputStreamWriter osw = new OutputStreamWriter(mFos, "UTF-8");
                if (osw != null) {
                    BufferedWriter bw = new BufferedWriter(osw);
                    if (bw != null) {
                        String str = mStringDayTime + mStringLineType + "\n" + mStringDayTime + mStringThroughput + "\n" + mStringDayTime + mStringElectricFieldStrength + "\n" + mStringDayTime + mStringLatitude + "\n" + mStringDayTime + mStringLongitude + "\n" + mStringDayTime + mStringBssid + "\n" + mStringDayTime + mStringMacAddress + "\n" + mStringDayTime + mStringModel + "\n" + mStringDayTime + mStringSsid + "\n" + mStringDayTime + mStringCareerName + "\n";
                        bw.write(str);
                        bw.flush();
                        bw.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * サーバへログを送信
     */
    private void sendLog() {
        HttpClient hc = new DefaultHttpClient();
        String url = "http://gae-for-android.appspot.com/wifi/upload";
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("time", mStringDayTime));
        params.add(new BasicNameValuePair("lineType", mStringLineType));
        params.add(new BasicNameValuePair("throughput", mStringThroughput));
        params.add(new BasicNameValuePair("strength", mStringElectricFieldStrength));
        params.add(new BasicNameValuePair("latitude", mStringLatitude));
        params.add(new BasicNameValuePair("longitude", mStringLongitude));
        params.add(new BasicNameValuePair("bssId", mStringBssid));
        params.add(new BasicNameValuePair("mcAddr", mStringMacAddress));
        params.add(new BasicNameValuePair("model", mStringModel));
        params.add(new BasicNameValuePair("ssId", mStringSsid));
        try {
            post.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse res = hc.execute(post);
            int status = res.getStatusLine().getStatusCode();
            if (!(status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED)) {
                throw new RuntimeException("Request timeout");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mStringLatitude = (Double.toString(location.getLatitude()));
            mStringLongitude = (Double.toString(location.getLongitude()));
        }
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }
}
