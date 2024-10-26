package org.gnuhpc.overlay;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.gnuhpc.location.LocationUtil;
import org.gnuhpc.location.MyLocationManager;
import org.gnuhpc.location.MyMapView;
import org.gnuhpc.roadroute.Road;
import org.gnuhpc.roadroute.RoadProvider;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class ShowFriendsLocItemizedOverlay extends ShowItemizedOverlay {

    private MyMapView mMapview;

    private Road mRoad;

    private static final String LOG_TAG = "ShowItemizedOverlay";

    public ShowFriendsLocItemizedOverlay(Drawable defaultMarker, Context context, MyMapView mapview) {
        super(defaultMarker, context);
        mContext = context;
        mMapview = mapview;
    }

    public void addGeos(ArrayList<GeoPoint> geoList) {
        clearOverlayItems();
        for (int i = 0; i < geoList.size(); i++) {
            addOverlayItem(new OverlayItem(geoList.get(i), "Geographic Information", geoList.get(i).getLatitudeE6() + "," + geoList.get(i).getLongitudeE6()));
        }
    }

    @Override
    protected boolean onTap(int index) {
        OverlayItem item = mOverlays.get(index);
        Location loc = MyLocationManager.getCurrentLocation();
        String[] splitResult = item.getSnippet().split(",");
        if (loc == null) {
            return false;
        }
        double fromLat = loc.getLatitude();
        double fromLon = loc.getLongitude();
        double toLat = (double) (Long.parseLong(splitResult[0]) / 1e6);
        double toLon = (double) (Long.parseLong(splitResult[1]) / 1e6);
        Log.d(LOG_TAG, fromLat + " " + fromLon + " " + toLat + " " + toLon);
        if (fromLat == 0 && fromLon == 0) {
            return false;
        }
        showRoute(fromLat, fromLon, toLat, toLon);
        return true;
    }

    public void showRoute(double fromLat, double fromLon, double toLat, double toLon) {
        final String url = RoadProvider.getUrl(fromLat, fromLon, toLat, toLon, mContext);
        Log.d(LOG_TAG, fromLat + " " + fromLon + " " + toLat + " " + toLon);
        new Thread() {

            @Override
            public void run() {
                InputStream is = getConnection(url);
                mRoad = RoadProvider.getRoute(is);
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                showRouteHandler.sendEmptyMessage(0);
            }
        }.start();
    }

    private InputStream getConnection(String url) {
        InputStream is = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            is = conn.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    Handler showRouteHandler = new Handler() {

        public void handleMessage(Message msg) {
            RouteOverlay routeOverlay = new RouteOverlay(mRoad, mContext);
            mMapview.removeOverlay(routeOverlay);
            mMapview.addOverlay(routeOverlay);
            int maxZoom = mMapview.getMaxZoomLevel();
            int initZoom = (int) (LocationUtil.getZOOM_FACTOR() * (double) maxZoom);
            mMapview.setZoom(initZoom);
            mMapview.postInvalidate();
            Log.d(LOG_TAG, "draw the route!");
        }

        ;
    };
}
