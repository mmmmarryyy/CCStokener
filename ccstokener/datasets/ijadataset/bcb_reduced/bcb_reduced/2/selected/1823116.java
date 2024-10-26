package me.Trackit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class UserMap extends MapActivity {

    /** Called when the activity is first created. */
    List<Overlay> mapOverlays;

    Drawable drawable, drawable2;

    MItemizedOverlay itemizedOverlay, itemizedOverlay2;

    OverlayItem overlayitem;

    GeoPoint geoPoint;

    MapView mapView;

    MapController mController;

    double lat = 30.060047;

    double lng = 31.305294;

    Handler mHandler = new Handler();

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    double blatitude, blongitude = Double.NaN;

    String result;

    InputStream is = null;

    StringBuilder sb = null;

    String result2 = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        MyApp uid = (MyApp) getApplicationContext();
        result = uid.getStringValue();
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.setFocusableInTouchMode(true);
        mapView.displayZoomControls(true);
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.bus_icon);
        itemizedOverlay = new MItemizedOverlay(drawable);
        drawable2 = this.getResources().getDrawable(R.drawable.coolred_small);
        itemizedOverlay2 = new MItemizedOverlay(drawable2);
        String provider = LocationManager.GPS_PROVIDER;
        LocationManager locationManager;
        String context = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) getSystemService(context);
        Location location = locationManager.getLastKnownLocation(provider);
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(15000);
                        itemizedOverlay.removeOverlay(overlayitem);
                        mapOverlays.remove(overlayitem);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                requestcoord();
                            }
                        });
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }

    private void requestcoord() {
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new name_value("uid", result));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://www.gotrackit.net/server/getbuscoord.php");
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection" + e.toString());
        }
        try {
            int r = -1;
            StringBuffer reader = new StringBuffer();
            while ((r = is.read()) != -1) reader.append((char) r);
            String tagOpenlat = "<`latitude`>";
            String tagCloselat = "<`/latitude`>";
            String tagOpenlng = "<`longitude`>";
            String tagCloselng = "<`/longitude`>";
            if (reader.indexOf(tagOpenlat) != -1) {
                int start = reader.indexOf(tagOpenlat) + tagOpenlat.length();
                int end = reader.indexOf(tagCloselat);
                String value = reader.substring(start, end);
                blatitude = Double.parseDouble(value);
            }
            if (reader.indexOf(tagOpenlng) != -1) {
                int start = reader.indexOf(tagOpenlng) + tagOpenlng.length();
                int end = reader.indexOf(tagCloselng);
                String value = reader.substring(start, end);
                blongitude = Double.parseDouble(value);
            }
            is.close();
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        String bus = "latitude = " + blatitude + "\nlongitude = " + blongitude;
        Toast toast = Toast.makeText(context, bus, duration);
        toast.show();
        mController = mapView.getController();
        geoPoint = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
        overlayitem = new OverlayItem(geoPoint, "Me", "");
        itemizedOverlay2.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay2);
        GeoPoint point = new GeoPoint((int) (blatitude * 1E6), (int) (blongitude * 1E6));
        overlayitem = new OverlayItem(point, "Bus", "");
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
        mController.animateTo(point);
        mController.setCenter(point);
        mController.setZoom(17);
    }

    private void updateWithNewLocation(Location location) {
        String latLongString;
        if (location != null) {
            latLongString = "Lat:" + lat + "\nLong:" + lng;
        } else {
            latLongString = "No location found";
        }
    }

    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            updateWithNewLocation(location);
        }

        public void onProviderDisabled(String provider) {
            updateWithNewLocation(null);
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
}
