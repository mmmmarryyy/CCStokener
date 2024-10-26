package net.sylvek.where;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import com.google.android.maps.GeoPoint;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.List;

public class WhereUtils {

    private static final String TINYLINK_URI = "http://tinyurl.com/api-create.php?url=";

    private WhereUtils() {
    }

    public static void sentNotification(Context context, String message) {
        Notification notification = new Notification(R.drawable.mylocation, message, System.currentTimeMillis());
        Intent i = new Intent(context, Where.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, i, Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.setLatestEventInfo(context, context.getText(R.string.app_name), message, pending);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Where.UID, notification);
    }

    public static String getCurrentStaticLocationUrl(Context context, Location location) {
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        return SylvekClient.STATIC_WEB_MAP + "?pos=" + latitude + "," + longitude;
    }

    public static Location getLastKnownLocation(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String provider = lm.getBestProvider(UpdateLocationService.getCriteria(), false);
        if (provider != null) {
            return lm.getLastKnownLocation(provider);
        }
        return null;
    }

    public static String getAddress(Context context, Location location) {
        Geocoder gc = new Geocoder(context);
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        List<Address> address = null;
        try {
            address = gc.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            Log.e("get.address", "unable to get address", e);
            return "";
        }
        if (address == null || address.size() == 0) {
            Log.w("get.address", "unable to parse address");
            return "";
        }
        Address a = address.get(0);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.getMaxAddressLineIndex(); i++) {
            b.append(a.getAddressLine(i));
            b.append(" ");
        }
        return b.toString();
    }

    public static String getTinyLink(String url) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(TINYLINK_URI + URLEncoder.encode(url));
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity());
        }
        return url;
    }

    public static Drawable getToCache(Context context, String uid) {
        return Drawable.createFromPath(context.getFilesDir() + "/" + uid);
    }

    public static boolean storeToCache(Context context, String uid, Bitmap bitmap) {
        boolean result = false;
        try {
            FileOutputStream file = context.openFileOutput(uid, Context.MODE_PRIVATE);
            result = bitmap.compress(CompressFormat.JPEG, 100, file);
            file.flush();
            file.close();
        } catch (FileNotFoundException e) {
            Log.e("store.photo", "error during openFileOutput", e);
        } catch (IOException e) {
            Log.e("store.photo", "error during flush & close", e);
        }
        return result;
    }

    public static Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e("photo.get", e.getMessage());
        }
        return bm;
    }

    private static int EARTH_RADIUS_KM = 6371;

    public static int MILLION = 1000000;

    /**
     * Computes the distance in kilometers between two points on Earth.
     * 
     * @param lat1 Latitude of the first point
     * @param lon1 Longitude of the first point
     * @param lat2 Latitude of the second point
     * @param lon2 Longitude of the second point
     * @return Distance between the two points in kilometers.
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        return Math.acos(Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)) * EARTH_RADIUS_KM;
    }

    /**
     * Computes the distance in kilometers between two points on Earth.
     * 
     * @param p1 First point
     * @param p2 Second point
     * @return Distance between the two points in kilometers.
     */
    public static double distanceKm(GeoPoint p1, GeoPoint p2) {
        double lat1 = p1.getLatitudeE6() / (double) MILLION;
        double lon1 = p1.getLongitudeE6() / (double) MILLION;
        double lat2 = p2.getLatitudeE6() / (double) MILLION;
        double lon2 = p2.getLongitudeE6() / (double) MILLION;
        return distanceKm(lat1, lon1, lat2, lon2);
    }

    public static String displayDistanceKm(String t, GeoPoint p1, GeoPoint p2) {
        if (p1 == null || p2 == null) {
            return t;
        }
        StringBuilder toDisplay = new StringBuilder(t);
        toDisplay.append(" : ");
        toDisplay.append(new DecimalFormat("0.00").format(distanceKm(p1, p2)));
        toDisplay.append(" km");
        return toDisplay.toString();
    }
}
