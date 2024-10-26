package com.example.android.locrec;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class GPSService extends Service {

    private static final String TAG = GPSService.class.getName();

    private LocationManager mLocationManager;

    private Context mApplicationContext;

    private Calendar mLastUpdateTime;

    private Handler mHandler;

    final RemoteCallbackList<IUpdateLocationCallback> callbackList = new RemoteCallbackList<IUpdateLocationCallback>();

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called");
        initGPSDevice();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart() called");
    }

    private void initGPSDevice() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider == null) {
            return;
        }
        mHandler = new Handler(Looper.getMainLooper());
        mApplicationContext = this.getApplicationContext();
        mLastUpdateTime = Calendar.getInstance();
        Location location = mLocationManager.getLastKnownLocation(provider);
        updateLocation(location);
        mLocationManager.requestLocationUpdates(provider, 1 * 60 * 1000, 50, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            int freq = AppPreferences.getUpdateFreq(mApplicationContext);
            Calendar tmpTime = mLastUpdateTime;
            tmpTime.add(Calendar.SECOND, freq);
            Calendar currentTime = Calendar.getInstance();
            if (currentTime.compareTo(tmpTime) > 0) {
                updateLocation(location);
                mLastUpdateTime = Calendar.getInstance();
            }
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    class MyRunnable implements Runnable {

        Location mLocation;

        public MyRunnable(Location location) {
            mLocation = location;
        }

        @Override
        public void run() {
            updateLocation2(mLocation);
        }
    }

    private void updateLocation(Location location) {
        MyRunnable myRunnable = new MyRunnable(location);
        Thread updateThread = new Thread(null, myRunnable, "refresh_updateLocation");
        updateThread.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (IGPSService.class.getName().equals(intent.getAction())) {
            return gpsServiceIf;
        }
        return null;
    }

    private IGPSService.Stub gpsServiceIf = new IGPSService.Stub() {

        public void stopService() throws RemoteException {
            mLocationManager.removeUpdates(locationListener);
        }

        public void registerUpdateLocationCallback(IUpdateLocationCallback cb) throws RemoteException {
            if (cb != null) {
                callbackList.register(cb);
            }
        }

        public void unreigsterUpdateLocationCallback(IUpdateLocationCallback cb) throws RemoteException {
            if (cb != null) {
                callbackList.register(cb);
            }
        }
    };

    private void updateLocation2(Location location) {
        Log.i(TAG, "updateLocation2");
        if (location == null) return;
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        updateLocationCallbackFunc(lat, lon);
        Log.d(TAG, "location(" + lat + "," + lon + ")");
        String serverUrl = AppPreferences.getServerUrl(this.getApplicationContext());
        if (serverUrl.length() == 0) return;
        String loginId = AppPreferences.getLoginId(this.getApplicationContext());
        String postUrl = "http://" + serverUrl + "/api";
        Log.i(TAG, postUrl);
        HttpParams params = new BasicHttpParams();
        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpConnectionParams.setConnectionTimeout(params, 5000);
        HttpConnectionParams.setSoTimeout(params, 3000);
        HttpClient httpclient = new DefaultHttpClient(params);
        HttpPost httppost = new HttpPost(postUrl);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("loginid", loginId));
            nameValuePairs.add(new BasicNameValuePair("latitude", Double.toString(lat)));
            nameValuePairs.add(new BasicNameValuePair("longitude", Double.toString(lon)));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httppost);
            response.getEntity();
        } catch (SocketTimeoutException e) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, R.string.cannot_connect_server, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        } catch (IOException e) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    Context context = getApplicationContext();
                    Toast toast = Toast.makeText(context, R.string.io_error, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        } finally {
        }
    }

    private void updateLocationCallbackFunc(double latitude, double longitude) {
        int n = callbackList.beginBroadcast();
        try {
            for (int i = 0; i < n; i++) {
                callbackList.getBroadcastItem(i).updateLocation(latitude, longitude);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:" + e.getMessage());
        }
        callbackList.finishBroadcast();
    }
}
