package org.gtugs.service;

import org.gtugs.domain.Point;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author jasonacooper@google.com (Jason Cooper)
 */
public class GoogleMapsService implements MapsService {

    protected String key;

    public GoogleMapsService() {
        this(null);
    }

    public GoogleMapsService(String key) {
        this.key = key;
    }

    public Point getCoordinates(String location, String country) {
        return getCoordinates(location, null, country);
    }

    public Point getCoordinates(String city, String state, String country) {
        return getCoordinates(null, city, state, country);
    }

    public Point getCoordinates(String address, String city, String state, String country) {
        StringBuilder queryString = new StringBuilder();
        StringBuilder urlString = new StringBuilder();
        StringBuilder response = new StringBuilder();
        if (address != null) {
            queryString.append(address.trim().replaceAll(" ", "+"));
            queryString.append("+");
        }
        if (city != null) {
            queryString.append(city.trim().replaceAll(" ", "+"));
            queryString.append("+");
        }
        if (state != null) {
            queryString.append(state.trim().replaceAll(" ", "+"));
            queryString.append("+");
        }
        if (country != null) {
            queryString.append(country.replaceAll(" ", "+"));
        }
        urlString.append("http://maps.google.com/maps/geo?key=");
        urlString.append(key);
        urlString.append("&sensor=false&output=json&oe=utf8&q=");
        urlString.append(queryString.toString());
        try {
            URL url = new URL(urlString.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JSONObject root = (JSONObject) JSONValue.parse(response.toString());
            JSONObject placemark = (JSONObject) ((JSONArray) root.get("Placemark")).get(0);
            JSONArray coordinates = (JSONArray) ((JSONObject) placemark.get("Point")).get("coordinates");
            Point point = new Point();
            point.setLatitude((Double) coordinates.get(1));
            point.setLongitude((Double) coordinates.get(0));
            return point;
        } catch (MalformedURLException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    public void setKey(String key) {
        this.key = key;
    }
}
