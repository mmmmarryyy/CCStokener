package ch.bfh.CityExplorer.Application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.android.maps.GeoPoint;

public class Route {

    private GeoPoint src;

    private GeoPoint dest;

    public Route(GeoPoint src, GeoPoint dest) {
        this.src = src;
        this.dest = dest;
    }

    public JSONObject GetJSON() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        double srcLat = src.getLatitudeE6() / 1E6;
        double srcLng = src.getLongitudeE6() / 1E6;
        double destLat = dest.getLatitudeE6() / 1E6;
        double destLng = dest.getLongitudeE6() / 1E6;
        String url = String.format("http://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&sensor=false&mode=walking", srcLat, srcLng, destLat, destLng);
        request.setURI(new URI(url));
        HttpResponse response = client.execute(request);
        BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuffer sb = new StringBuffer("");
        String line = "";
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        in.close();
        JSONObject json = new JSONObject(sb.toString());
        String status = json.getString("status");
        if (status.equals("OVER_QUERY_LIMIT")) {
            throw new GoogleException("Zuviele Abfragen auf google ausgef�hrt");
        }
        return json;
    }

    public RouteInfo getRouteInfo() throws Exception {
        JSONArray jsonArray = GetJSON().getJSONArray("routes");
        jsonArray = jsonArray.getJSONObject(0).getJSONArray("legs");
        JSONObject leg = jsonArray.getJSONObject(0);
        JSONObject jsonObject = leg.getJSONObject("distance");
        String distance = jsonObject.getString("text");
        jsonObject = leg.getJSONObject("duration");
        String duration = jsonObject.getString("text");
        return new RouteInfo(duration, distance);
    }

    public List<GeoPoint> getRoutePoints() throws Exception {
        List<GeoPoint> points = new ArrayList<GeoPoint>();
        JSONArray jsonArray = GetJSON().getJSONArray("routes");
        jsonArray = jsonArray.getJSONObject(0).getJSONArray("legs");
        JSONObject leg = jsonArray.getJSONObject(0);
        JSONArray steps = leg.getJSONArray("steps");
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.getJSONObject(i);
            JSONObject dest = step.getJSONObject("polyline");
            String polyline = dest.getString("points");
            List<GeoPoint> temp = decodePolyline(polyline);
            points.addAll(temp);
        }
        return points;
    }

    public static List<GeoPoint> decodePolyline(String poly) {
        int len = poly.length();
        int index = 0;
        int lat = 0;
        int lng = 0;
        List<GeoPoint> decoded = new ArrayList<GeoPoint>();
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            decoded.add(new GeoPoint(lat * 10, lng * 10));
        }
        return decoded;
    }
}
