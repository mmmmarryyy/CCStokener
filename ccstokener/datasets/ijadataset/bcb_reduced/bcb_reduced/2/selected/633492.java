package com.example.android.locrec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

/**
 * メイン画面
 */
public class MainActivity extends MapActivity {

    protected static final String TAG = "MainActivity";

    private MapView mMapView;

    MapController mapController;

    PositionOverlay positionOverlay;

    private IGPSService gpsService = null;

    protected Calendar actionHistoryFrom;

    private Context mApplicationContext;

    private Handler mHandler = new Handler();

    private static final int CONNECT = 0;

    private static final int DISCONNECT = 1;

    private static final int MENU_APPPREFERENCES = Menu.FIRST;

    private static final int MENU_ACTION_HISTORY = Menu.FIRST + 1;

    private static final int MENU_END_ACTION_HISTORY = Menu.FIRST + 2;

    private Handler mProgressHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CONNECT) {
                setProgressBarIndeterminateVisibility(true);
            } else {
                setProgressBarIndeterminateVisibility(false);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        mMapView = (MapView) findViewById(R.id.myMapView);
        mapController = mMapView.getController();
        mMapView.setSatellite(false);
        mMapView.setStreetView(false);
        mMapView.setBuiltInZoomControls(true);
        positionOverlay = new PositionOverlay();
        List<Overlay> overlays = mMapView.getOverlays();
        overlays.add(positionOverlay);
        mapController.setZoom(17);
        Intent intent = new Intent(IGPSService.class.getName());
        bindService(intent, gpsServiceConn, BIND_AUTO_CREATE);
        mApplicationContext = this.getApplicationContext();
    }

    private ServiceConnection gpsServiceConn = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected() called");
            gpsService = IGPSService.Stub.asInterface(service);
            try {
                gpsService.registerUpdateLocationCallback(updateLocationCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisonnected() called");
            try {
                gpsService.unreigsterUpdateLocationCallback(updateLocationCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private IUpdateLocationCallback updateLocationCallback = new IUpdateLocationCallback.Stub() {

        public void updateLocation(double latitude, double longitude) throws RemoteException {
            positionOverlay.setCurrentLocation(latitude, longitude);
            Double geoLat = latitude * 1E6;
            Double geoLng = longitude * 1E6;
            GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
            mapController.animateTo(point);
            Log.i(TAG, "upateLocation:" + latitude + ":" + longitude);
        }
    };

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (positionOverlay.isDisplayActionHistory()) {
            menu.add(0, MENU_APPPREFERENCES, Menu.NONE, R.string.menu_apppreferences).setIcon(android.R.drawable.ic_menu_preferences);
            menu.add(0, MENU_END_ACTION_HISTORY, Menu.NONE, R.string.menu_end_action_history).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            menu.add(0, MENU_APPPREFERENCES, Menu.NONE, R.string.menu_apppreferences).setIcon(android.R.drawable.ic_menu_preferences);
            menu.add(0, MENU_ACTION_HISTORY, Menu.NONE, R.string.menu_action_history).setIcon(android.R.drawable.ic_menu_myplaces);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
            case MENU_APPPREFERENCES:
                Intent intent = new Intent(this, AppPreferences.class);
                startActivity(intent);
                return true;
            case MENU_ACTION_HISTORY:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.menu_action_history);
                builder.setItems(R.array.action_history_entries, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        int[] intary = getResources().getIntArray(R.array.action_history_entryvalues);
                        int mHours = intary[which];
                        final TimeZone utc = TimeZone.getTimeZone("UTC");
                        actionHistoryFrom = Calendar.getInstance(utc);
                        actionHistoryFrom.add(Calendar.HOUR_OF_DAY, mHours * -1);
                        getActionHistory();
                    }
                });
                builder.show();
                return true;
            case MENU_END_ACTION_HISTORY:
                positionOverlay.setDisplayActionHistory(false);
                mMapView.invalidate();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            gpsService.stopService();
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            unbindService(gpsServiceConn);
        }
    }

    protected void getActionHistory() {
        Log.i(TAG, "getActionHistory() called");
        Thread updateThread = new Thread(null, backgroundRefresh, "refresh_actionHistory");
        updateThread.start();
    }

    private Runnable backgroundRefresh = new Runnable() {

        public void run() {
            mProgressHandler.sendMessage(mProgressHandler.obtainMessage(CONNECT));
            doDrawActionHistory();
            mProgressHandler.sendMessage(mProgressHandler.obtainMessage(DISCONNECT));
        }
    };

    /**
     * InputStreamからStringへ変換する
     */
    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    private void doDrawActionHistory() {
        String serverUrl = AppPreferences.getServerUrl(this.getApplicationContext());
        String loginId = AppPreferences.getLoginId(this.getApplicationContext());
        Date fromDate = actionHistoryFrom.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d-kk:mm");
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(timeZone);
        String dateBeginStr = sdf.format(fromDate);
        Calendar now = Calendar.getInstance(timeZone);
        Date toDate = now.getTime();
        String dateEnd = sdf.format(toDate);
        try {
            String locationFeed = "http://" + serverUrl + "/api?loginid=" + loginId + "&datebegin=" + dateBeginStr + "&dateend=" + dateEnd;
            Log.i(TAG, locationFeed);
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
            HttpConnectionParams.setSoTimeout(params, 2 * 1000);
            HttpClient objHttp = new DefaultHttpClient(params);
            HttpGet objGet = new HttpGet(locationFeed);
            HttpResponse objResponse = objHttp.execute(objGet);
            if (objResponse.getStatusLine().getStatusCode() == 200) {
                InputStream objStream = objResponse.getEntity().getContent();
                String jsonString = convertStreamToString(objStream);
                Type type = new TypeToken<List<GPSRecord>>() {
                }.getType();
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                Gson gson = gsonBuilder.create();
                List<GPSRecord> locationList = gson.fromJson(jsonString, type);
                if (locationList.size() > 1) {
                    positionOverlay.setActionHistory(locationList);
                    positionOverlay.setDisplayActionHistory(true);
                    final GPSRecord locr = locationList.get(0);
                    mHandler.post(new Runnable() {

                        public void run() {
                            Double geoLat = locr.getLatitude() * 1E6;
                            Double geoLng = locr.getLongitude() * 1E6;
                            GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
                            mapController.animateTo(point);
                            mMapView.invalidate();
                            Toast.makeText(mApplicationContext, R.string.history_found, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {

                        public void run() {
                            Toast.makeText(mApplicationContext, R.string.history_not_found, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        } catch (SocketTimeoutException e) {
            mHandler.post(new Runnable() {

                public void run() {
                    Toast.makeText(mApplicationContext, R.string.cannot_connect_server, Toast.LENGTH_LONG).show();
                }
            });
        } catch (IOException e) {
            mHandler.post(new Runnable() {

                public void run() {
                    Toast.makeText(mApplicationContext, R.string.io_error, Toast.LENGTH_LONG).show();
                }
            });
        } finally {
        }
    }
}
