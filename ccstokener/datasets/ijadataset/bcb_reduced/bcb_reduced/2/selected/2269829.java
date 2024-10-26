package playground.christoph.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Elevationcoder {

    public static GoogleResponse getElevation(String lat, String lon) throws IOException {
        String url = "http://maps.google.com/maps/api/elevation/xml?locations=";
        url = url + String.valueOf(lat);
        url = url + ",";
        url = url + String.valueOf(lon);
        url = url + "&sensor=false";
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String line;
        GoogleResponse googleResponse = new GoogleResponse();
        googleResponse.lat = Double.valueOf(lat);
        googleResponse.lon = Double.valueOf(lon);
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("<status>")) {
                line = line.replace("<status>", "");
                line = line.replace("</status>", "");
                googleResponse.status = line;
                if (!line.toLowerCase().equals("ok")) return googleResponse;
            } else if (line.startsWith("<elevation>")) {
                line = line.replace("<elevation>", "");
                line = line.replace("</elevation>", "");
                googleResponse.elevation = Double.valueOf(line);
                return googleResponse;
            }
        }
        return googleResponse;
    }

    public static class GoogleResponse {

        public String status = null;

        public double lon = Double.NaN;

        public double lat = Double.NaN;

        public double elevation = Double.NaN;

        private GoogleResponse() {
        }

        @Override
        public String toString() {
            return "Latitude: " + lat + ", Longitude: " + lon + ", Elevation: " + elevation;
        }
    }

    public static void main(String[] argv) throws Exception {
        Geocoder.Location zurich = Geocoder.getLocation("Zürich");
        GoogleResponse googleResponse = getElevation(zurich.lat, zurich.lon);
        System.out.println(googleResponse.toString());
    }
}
