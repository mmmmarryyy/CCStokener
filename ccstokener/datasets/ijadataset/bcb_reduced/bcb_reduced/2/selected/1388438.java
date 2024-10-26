package org.hackathon.ashiato;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class PostActivity extends MapActivity implements LocationListener, OnClickListener {

    static final String TAG = "Ashiato";

    static final String API_URI_GET = "http://hackathon-ashiato.appspot.com/ashi/get?";

    static final String API_URI_POST = "http://hackathon-ashiato.appspot.com/ashi/put?";

    LocationManager locationManager;

    Button button;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapView map = new MapView(this, "0f9G_PGB3oPAHZmsHXHUVDgCt2LeBHY_R3d1fKw");
        map.setEnabled(true);
        map.setClickable(true);
        setContentView(map);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
        MyOverlay overlay = new MyOverlay(bmp, new GeoPoint(35656000, 139700000));
        List<Overlay> list = map.getOverlays();
        list.add(overlay);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, this);
    }

    public class DownloadTask extends AsyncTask<String, Integer, Bitmap> {

        private HttpClient mClient;

        private HttpGet mGetMethod;

        private PostActivity mActivity;

        public DownloadTask(PostActivity activity) {
            mActivity = activity;
            mClient = new DefaultHttpClient();
            mGetMethod = new HttpGet();
        }

        Bitmap downloadImage(String uri) {
            try {
                mGetMethod.setURI(new URI(uri));
                HttpResponse resp = mClient.execute(mGetMethod);
                if (resp.getStatusLine().getStatusCode() < 400) {
                    InputStream is = resp.getEntity().getContent();
                    String tmp = convertStreamToString(is);
                    Log.d(TAG, "hoge" + tmp);
                    is.close();
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public String convertStreamToString(InputStream is) throws IOException {
            if (is != null) {
                StringBuilder sb = new StringBuilder();
                String line;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                } finally {
                    is.close();
                }
                return sb.toString();
            } else {
                return "";
            }
        }

        private Bitmap createBitmap(InputStream is) {
            return BitmapFactory.decodeStream(is);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String uri = params[0];
            return downloadImage(uri);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
        }
    }

    long last = 0;

    public void onLocationChanged(Location location) {
        if (location != null) {
            if (System.currentTimeMillis() - last > 5000) {
                String request = API_URI_POST + "email=kazunori279@gmail.com&lat=" + String.valueOf(location.getLatitude()) + "&lng=" + String.valueOf(location.getLongitude());
                Log.v(TAG, "Send " + request);
                DownloadTask task = new DownloadTask(this);
                task.execute(request);
                last = System.currentTimeMillis();
            }
        }
    }

    public void onProviderDisabled(String provider) {
        Log.v("gps", "onProviderDisabled");
    }

    public void onProviderEnabled(String provider) {
        Log.v("gps", "onProviderEnabled");
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.v("gps", "onStatusChanged");
    }

    /**
	 * 
	 */
    private String getGPSLocationString(Location location) {
        String s;
        if (location == null) {
            s = "Location[unknown]\n";
        } else {
            s = String.format("%f,%f,%d,%s\n", location.getLatitude(), location.getLongitude(), location.getTime(), location.getProvider());
        }
        return s;
    }

    @Override
    public void onClick(View v) {
        String request = API_URI_GET + "email=kazunori279@gmail.com";
        Log.v(TAG, "Send " + request);
        DownloadTask task = new DownloadTask(this);
        task.execute(request);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
