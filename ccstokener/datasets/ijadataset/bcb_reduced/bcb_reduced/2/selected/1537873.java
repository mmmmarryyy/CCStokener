package squirrel.DeaddropDroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.ci.geo.route.Road;
import org.ci.geo.route.RoadProvider;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class ShowRoute extends ShowMap implements LocationListener {

    /**
	 * De route.
	 */
    private Road mRoad;

    private Bundle extras;

    private Bundle deaddrop;

    private Location location;

    private Boolean newRoute;

    private static final String TAG = DeaddropDroid.TAG;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extras = getIntent().getExtras();
        newRoute = true;
        final int deaddropID = extras.getInt(DeaddropDB.KEY_ID);
        deaddrop = db.getDeaddrop(deaddropID).getBundle("0");
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            Toast.makeText(getApplicationContext(), R.string.awaiting_location, Toast.LENGTH_LONG).show();
            location = new Location("");
        } else showRoute();
    }

    @Override
    public void onLocationChanged(final Location location) {
        super.onLocationChanged(location);
        this.location = location;
        showRoute();
    }

    /**
	 * Toont de route op de kaart, indien beschikbaar.
	 */
    public void showRoute() {
        new Thread() {

            @Override
            public void run() {
                if (!haveLocation) return;
                final double fromLat = location.getLatitude();
                final double fromLon = location.getLongitude();
                final double toLat = Double.parseDouble(deaddrop.getString(DeaddropDB.KEY_DEADDROP_LAT));
                final double toLon = Double.parseDouble(deaddrop.getString(DeaddropDB.KEY_DEADDROP_LON));
                final String url = RoadProvider.getUrl(fromLat, fromLon, toLat, toLon);
                final PushbackInputStream pis = new PushbackInputStream(getConnection(url));
                try {
                    final int b = pis.read();
                    if (b == -1) {
                        Log.v(TAG, "Geen route beschikbaar!");
                        mRoad = RoadProvider.getRoute(pis);
                        mHandler.sendEmptyMessage(1);
                        return;
                    }
                    pis.unread(b);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                mRoad = RoadProvider.getRoute(pis);
                mHandler.sendEmptyMessage(0);
            }
        }.start();
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(final android.os.Message msg) {
            if (msg.what == 1) Toast.makeText(getApplicationContext(), R.string.toast_no_route, Toast.LENGTH_SHORT).show(); else {
                final TextView textView = (TextView) findViewById(R.id.description);
                textView.setText(mRoad.mName + " " + mRoad.mDescription);
                textView.setVisibility(View.VISIBLE);
                final MapOverlay mapOverlay = new MapOverlay(mRoad, mapView);
                mapOverlays.add(mapOverlay);
                mapView.invalidate();
                if (newRoute) {
                    final ArrayList<GeoPoint> points = mapOverlay.mPoints;
                    int minLat = 360000000;
                    int maxLat = 0;
                    int minLong = 180000000;
                    int maxLong = -180000000;
                    for (final GeoPoint p : points) {
                        int l = p.getLatitudeE6();
                        if (minLat > l) minLat = l;
                        if (maxLat < l) maxLat = l;
                        l = p.getLongitudeE6();
                        if (minLong > l) minLong = l;
                        if (maxLong < l) maxLong = l;
                    }
                    final double latSpan = maxLat - minLat;
                    final double longSpan = maxLong - minLong;
                    final double x = mapView.getWidth();
                    final double y = mapView.getHeight();
                    final int xZoom = (int) (1 + (Math.log(360 * 1e6 * x / longSpan) - Math.log(256)) / Math.log(2));
                    final int yZoom = (int) (1 + (Math.log(360 * 1e6 * y / latSpan) - Math.log(256)) / Math.log(2));
                    if (xZoom < yZoom) mapController.setZoom(xZoom); else mapController.setZoom(yZoom);
                    mapController.animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLong + minLong) / 2));
                    newRoute = false;
                    mapStatus.putBoolean("centered", false);
                }
            }
        }

        ;
    };

    /**
	 * Maakt een verbinding met de URL, en geeft een InputStream als resultaat.
	 * 
	 * @param url
	 *            de URL.
	 * @return de InputStream vanwaar de data is te downloaden.
	 */
    private InputStream getConnection(final String url) {
        InputStream is = null;
        try {
            final URLConnection conn = new URL(url).openConnection();
            is = conn.getInputStream();
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    /**
	 * Detecteerd of de kaart is verschoven.
	 */
    @Override
    public void onPanEvent() {
        if (haveLocation) {
            final MapOverlay mapOverlay = new MapOverlay(mRoad, mapView);
            mapOverlays.add(mapOverlay);
            mapView.invalidate();
        }
        super.onPanEvent();
    }

    /**
	 * Detecterd of kaart is in- of uitgezoomd.
	 */
    @Override
    public void onZoomEvent() {
        if (haveLocation) {
            final MapOverlay mapOverlay = new MapOverlay(mRoad, mapView);
            mapOverlays.add(mapOverlay);
            mapView.invalidate();
        }
        super.onZoomEvent();
    }

    /**
	 * Vereiste van Google. Laten we eens eerlijk zijn.
	 */
    @Override
    protected boolean isRouteDisplayed() {
        return true;
    }
}

/**
 * De overlay die het allemaal moet tonen.
 */
class MapOverlay extends com.google.android.maps.Overlay {

    Road mRoad;

    ArrayList<GeoPoint> mPoints;

    /**
	 * Tekent de route op de kaart.
	 * 
	 * @param road
	 *            De route.
	 * @param mv
	 *            Een MapView.
	 */
    public MapOverlay(final Road road, final MapView mv) {
        if (road == null) return;
        if (road.mRoute == null) return;
        mRoad = road;
        mPoints = new ArrayList<GeoPoint>();
        if (road.mRoute.length > 0) for (int i = 0; i < road.mRoute.length; i++) mPoints.add(new GeoPoint((int) (road.mRoute[i][1] * 1e6), (int) (road.mRoute[i][0] * 1e6)));
    }

    @Override
    public boolean draw(final Canvas canvas, final MapView mv, final boolean shadow, final long when) {
        super.draw(canvas, mv, shadow);
        drawPath(mv, canvas);
        return true;
    }

    /**
	 * Tekent een stap van de route.
	 * 
	 * @param mv
	 *            Een MapView.
	 * @param canvas
	 *            Een Canvas.
	 */
    public void drawPath(final MapView mv, final Canvas canvas) {
        int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
        final Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        for (int i = 0; i < mPoints.size(); i++) {
            final Point point = new Point();
            mv.getProjection().toPixels(mPoints.get(i), point);
            x2 = point.x;
            y2 = point.y;
            if (i > 0) canvas.drawLine(x1, y1, x2, y2, paint);
            x1 = x2;
            y1 = y2;
        }
    }
}
