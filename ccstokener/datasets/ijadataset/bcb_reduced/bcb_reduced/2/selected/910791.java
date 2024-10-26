package com.realdolmen.sf.mapface.geocoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.realdolmen.sf.mapface.exception.NoGoogleKeyException;
import com.realdolmen.sf.mapface.model.LatLng;
import com.realdolmen.sf.mapface.utils.PropertiesHolder;

/**
 * The Class Geocoder.
 */
public class Geocoder {

    /** The google key. */
    private String googleKey;

    /**
	 * Instantiates a new geocoder. If no google key is provided, a NoGoogleKeyException will be thrown.
	 * 
	 * @param googleKey the google key
	 * 
	 * @throws NoGoogleKeyException the no google key exception
	 */
    public Geocoder(String googleKey) throws NoGoogleKeyException {
        if (googleKey != null && googleKey.length() > 0) {
            this.googleKey = googleKey;
        } else {
            throw new NoGoogleKeyException();
        }
    }

    /**
	 * Geocode an address.
	 * 
	 * @param address the address
	 * @param city the city
	 * @param country the country
	 * 
	 * @return the lat lng
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public LatLng geocode(String address, String city, String country) throws IOException {
        LatLng result;
        if ((city == null || city.equals("")) || (address == null || address.equals(""))) {
            result = null;
        } else {
            try {
                result = geocodeGoogle(address, city, country);
            } catch (IOException ex) {
                throw new IOException("geocoding failed");
            }
        }
        return result;
    }

    /**
	 * This method makes the call to the Google Geocoder service.
	 * 
	 * @param address the address
	 * @param city the city
	 * @param country the country
	 * 
	 * @return the lat lng
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    protected LatLng geocodeGoogle(String address, String city, String country) throws IOException {
        BufferedReader input = null;
        LatLng result = null;
        try {
            URL url = new URL(buildGooglePath(address, city, country));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            input = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = "";
            line = input.readLine();
            if (line != null && line.length() != 0) {
                String[] coords = org.apache.commons.lang.StringUtils.split(line, ",");
                if (coords[0].trim().equals("200")) {
                    result = new LatLng(Double.valueOf(coords[2]), Double.valueOf(coords[3]), Integer.valueOf(coords[0]), Integer.valueOf(coords[1]));
                } else {
                    result = new LatLng();
                    result.setStatusCode(Integer.valueOf(coords[0]));
                }
            }
        } catch (IOException ex) {
            throw new IOException("geocoding failed");
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return result;
    }

    /**
	 * Builds the url to call the service.
	 * 
	 * @param address the address
	 * @param city the city
	 * @param country the country
	 * 
	 * @return the string
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    protected String buildGooglePath(String address, String city, String country) throws IOException {
        if (country == null) {
            country = "";
        }
        String q = address.replace(' ', '+') + "+" + city.replace(' ', '+') + "+" + country.replace(' ', '+');
        StringBuffer sb = new StringBuffer();
        String service = PropertiesHolder.getGoogleGeocoderServiceURL();
        sb.append(service);
        sb.append("q=" + q);
        sb.append("&output=csv&sensor=false");
        sb.append("&key=" + googleKey);
        return sb.toString();
    }

    /**
	 * Gets the google key.
	 * 
	 * @return the google key
	 */
    public String getGoogleKey() {
        return this.googleKey;
    }
}
