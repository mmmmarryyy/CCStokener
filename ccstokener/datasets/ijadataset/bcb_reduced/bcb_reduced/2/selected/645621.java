package at.the.gogo.parkoid.util.webservices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import at.the.gogo.parkoid.models.GeoCodeResult;
import at.the.gogo.parkoid.util.Util;

public class YahooGeocoding {

    public static int readTimeOut = 120000;

    public static int connectTimeOut = 10000;

    private static final String USER_AGENT = "geonames-webservice-client-1.0.6";

    private static final String YAHOO_API_BASE_URL = "http://where.yahooapis.com/geocode?q=%1$s,+%2$s&flags=J&gflags=R&appid=";

    private static final String YAHOO_API_KEY = "";

    private static InputStream connect(final String url) throws IOException {
        InputStream in = null;
        try {
            final URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(YahooGeocoding.connectTimeOut);
            conn.setReadTimeout(YahooGeocoding.readTimeOut);
            conn.setRequestProperty("User-Agent", YahooGeocoding.USER_AGENT);
            in = conn.getInputStream();
            return in;
        } catch (final IOException e) {
            Util.d("problems connecting to geonames url " + url + "Exception:" + e);
        }
        return in;
    }

    public static String webGetString(final String url) {
        try {
            String line;
            final StringBuilder sb = new StringBuilder();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(connect(url)));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (final Exception e) {
            Util.d(e.toString());
        }
        return null;
    }

    /**
     * Yahoo reversegeocoding
     * 
     * @param latitude
     * @param longitude
     * @return
     */
    public static GeoCodeResult reverseGeoCode(final double latitude, final double longitude) {
        final String urlStr = String.format(YahooGeocoding.YAHOO_API_BASE_URL, String.valueOf(latitude), String.valueOf(longitude)) + YahooGeocoding.YAHOO_API_KEY;
        final String jsonStr = webGetString(urlStr);
        return extractGeoCodeResult(jsonStr);
    }

    private static GeoCodeResult extractGeoCodeResult(final String yahooAnswer) {
        GeoCodeResult result = null;
        try {
            if ((yahooAnswer != null) && (yahooAnswer.length() > 0)) {
                JSONObject json = new JSONObject(yahooAnswer);
                json = json.getJSONObject("ResultSet");
                final JSONArray jsonArray = json.getJSONArray("Results");
                final int nrOfEntries = jsonArray.length();
                if (nrOfEntries > 0) {
                    result = new GeoCodeResult();
                    final JSONObject addressObject = jsonArray.getJSONObject(0);
                    result.setLine1(addressObject.getString("line1"));
                    result.setLine2(addressObject.getString("line2"));
                    result.setLine3(addressObject.getString("line3"));
                    result.setLine4(addressObject.getString("line4"));
                    result.setCountry(addressObject.getString("country"));
                    result.setCity(addressObject.getString("city"));
                }
            }
        } catch (final Exception x) {
            Util.d("Yahoo Response not parsable...");
        }
        return result;
    }
}
