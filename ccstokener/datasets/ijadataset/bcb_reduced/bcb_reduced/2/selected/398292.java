package at.fhj.itm.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;
import at.fhj.itm.model.Point;
import at.fhj.itm.model.Trip;
import at.fhj.itm.model.Waypoint;

public class GoogleUtilImpl implements GoogleUtil {

    private final Logger logger = Logger.getLogger(GoogleUtilImpl.class);

    /**
	 * Calls the Google Maps Direction API.
	 * 
	 * @param trip
	 *            Trip containing the waypoints to calculate the route
	 * @return JSONObject with the data by Google
	 * @throws IOException
	 */
    @Override
    public JSONObject getJsonObj(Trip trip, List<Waypoint> stops) throws IOException {
        JSONObject jsonObject = null;
        try {
            List<String> filtered = new LinkedList<String>();
            filtered.add(trip.getWaypoint().getFromLocation().getCity().replaceAll("\\W+", "+"));
            filtered.add(trip.getWaypoint().getToLocation().getCity().replaceAll("\\W+", "+"));
            for (Waypoint w : stops) {
                String from = w.getFromLocation().getCity().replaceAll("\\W+", "+");
                if (!filtered.contains(from)) filtered.add(from);
                String to = w.getToLocation().getCity().replaceAll("\\W+", "+");
                if (!filtered.contains(to)) filtered.add(to);
            }
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://maps.google.com/maps/api/directions/json");
            urlBuilder.append("?origin=").append(filtered.get(0));
            urlBuilder.append("&destination=").append(filtered.get(1));
            if (filtered.size() > 2) {
                urlBuilder.append("&waypoints=");
                for (int i = 2; i < filtered.size() - 1; i++) urlBuilder.append(filtered.get(i)).append("|");
                urlBuilder.append(filtered.get(filtered.size() - 1));
            }
            urlBuilder.append("&sensor=false");
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urlBuilder.toString()).openStream()));
            StringBuilder answerBuilder = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) answerBuilder.append(inputLine);
            in.close();
            jsonObject = new JSONObject(answerBuilder.toString());
        } catch (JSONException e) {
            logger.error("Problem with initializing JSONObject", e);
        }
        return jsonObject;
    }

    /**
	 * adds the points of the JSONObject to the trip
	 * 
	 * @param Trip
	 *            the trip where the points should be saved in
	 * @param JSONObject
	 *            the jsonObject where the points are saved in
	 * @return Trip with points
	 * @throws JSONException
	 */
    @Override
    public Trip loadGoogleData(Trip trip, JSONObject json) throws IOException, JSONException {
        List<Point> points = new ArrayList<Point>();
        if ("OK".equals(json.getString("status"))) {
            JSONArray routes = json.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");
                int order = 1;
                for (int l = 0; l < legs.length(); l++) {
                    JSONObject leg = legs.getJSONObject(l);
                    JSONArray steps = leg.getJSONArray("steps");
                    if (steps.length() < 0) continue;
                    for (int s = 0; s < steps.length(); s++) {
                        JSONObject step = steps.getJSONObject(s);
                        JSONObject poly = step.getJSONObject("polyline");
                        if (!points.isEmpty()) order = points.get(points.size() - 1).getOrder();
                        List<Point> polyline = decode(poly.getString("points"), trip.getId(), order);
                        int avgDuration = Math.round(step.getJSONObject("duration").getInt("value") / (float) polyline.size());
                        for (Point point : polyline) {
                            point.setDuration(avgDuration);
                            points.add(point);
                        }
                    }
                }
                trip.setPoints(points);
                trip.setCopyright(route.getString("copyrights"));
            }
        } else {
            throw new IOException("Status = " + json.getString("status"));
        }
        return trip;
    }

    /**
	 * decodes a string to a list of points.
	 * 
	 * @param encoded
	 *            -> the string which should be decoded
	 * @param tripId
	 *            -> the id of the Trip
	 * @param order
	 *            -> the number of the first point in the string, so that more
	 *            than one string can be decoded to one trip
	 * @return List of points
	 */
    private List<Point> decode(String encoded, int tripId, int order) {
        List<Point> poly = new ArrayList<Point>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            order++;
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            Point p = new Point(order, ((double) lng) / 1E5, ((double) lat) / 1E5, 0, tripId);
            poly.add(p);
        }
        return poly;
    }
}
