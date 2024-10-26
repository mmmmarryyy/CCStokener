package prajna.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import prajna.data.GeoCoord;
import prajna.data.Location;

/**
 * Implementation of a GeoLocator which uses the Google lookup service. This
 * class requires a key to be set before using it, and has a limited number of
 * queries it will respond to in a day. See the Google Maps API for more
 * information.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class GoogleLocator extends GeoLocator {

    private static String key = "ABQIAAAAv_t34sZlMJhW7HagRAJj9RRiRPk7b96qT_AcyZAUnbnn80mx9RRCpKGIqR-hgpZPKNQ-2h8B9lw8gQ";

    private String googleUrlStr = "http://maps.google.com/maps/geo?output=csv&q=";

    private boolean estimate = false;

    private HashMap<String, GeoCoord> cache = new HashMap<String, GeoCoord>();

    /**
     * Create a GoogleLocator with the estimate flag set to false
     */
    public GoogleLocator() {
    }

    /**
     * Create a GoogleLocator with the specified estimate flag
     * 
     * @param doEstimate the estimate flag
     */
    public GoogleLocator(boolean doEstimate) {
        estimate = doEstimate;
    }

    /**
     * Request the geographic coordinate from the Google Map server.
     * 
     * @param locName Location name.
     * @return the coordinate for the given location
     */
    @Override
    public GeoCoord getCoordinate(String locName) {
        GeoCoord coord = cache.get(locName);
        if (coord == null) {
            String[] values = { "404", "", "", "" };
            String urlString = googleUrlStr + locName.trim().replaceAll("\\s+", "+") + "&key=" + key;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()));
                String line = reader.readLine();
                if (line != null) {
                    values = line.split(",");
                }
                reader.close();
            } catch (IOException exc) {
                throw new RuntimeException("Cannot parse stream from " + urlString, exc);
            }
            int status = Integer.parseInt(values[0]);
            if (status == 200) {
                double lat = Double.parseDouble(values[2]);
                double lon = Double.parseDouble(values[3]);
                coord = new GeoCoord(lat, lon);
            } else if (estimate) {
                int spcInx = locName.indexOf(" ");
                if (spcInx != -1) {
                    coord = getCoordinate(locName.substring(spcInx + 1));
                }
            }
            cache.put(locName, coord);
        }
        return coord;
    }

    /**
     * Return whether the locator allows estimation
     * 
     * @return whether the locator allows estimation
     */
    @Override
    public boolean isEstimateFlag() {
        return estimate;
    }

    /**
     * Look up the shape associated with the specified Location
     * 
     * @param loc the specified location object
     * @return The geographic shape representing the specified location
     */
    @Override
    public GeoShape lookup(Location loc) {
        GeoShape shape = null;
        if (loc.getCenter() == null && loc.getName().length() > 0) {
            shape = lookup(loc.getName());
        } else {
            shape = new GeoMarker(loc.getCenter());
        }
        return shape;
    }

    /**
     * Look up the shape associated with the specified Location by name
     * 
     * @param locName the specified location name
     * @return The geographic shape representing the specified location
     */
    @Override
    public GeoShape lookup(String locName) {
        GeoCoord coord = cache.get(locName);
        if (coord == null) {
            coord = getCoordinate(locName);
        }
        return new GeoMarker(coord);
    }

    /**
     * Look up the shape associated with the specified Location by name and
     * type
     * 
     * @param locName the specified location name
     * @param type the type of location the name represents
     * @return The geographic shape representing the specified location
     */
    @Override
    public GeoShape lookup(String locName, String type) {
        return lookup(locName);
    }

    /**
     * Set the flag indicating whether to allow estimation within the locator.
     * 
     * @param estimateFlag whether the locator allows estimation
     */
    @Override
    public void setEstimateFlag(boolean estimateFlag) {
        estimate = estimateFlag;
    }

    /**
     * Set the Google Map API key
     * 
     * @param keyString the Google Map API key
     */
    public static void setKey(String keyString) {
        key = keyString;
    }
}
