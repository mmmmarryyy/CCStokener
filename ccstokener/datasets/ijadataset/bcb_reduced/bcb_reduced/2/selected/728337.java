package edu.drexel.sd0910.ece01.aqmon.weather;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Makes an HTTP Request to the following URL: http://api.wunderground.com/auto
 * /wui/geo/GeoLookupXML/index.xml?query=lat,lon
 * 
 * @author Kyle O'Connor
 * 
 */
public class WeatherUndergroundHTTPRequest {

    private static final String QUERY_URL = "http://api.wunderground.com/auto/wui/geo/GeoLookupXML/index.xml?query=";

    private static final String COMMA = ",";

    /**
	 * Default constructor.
	 */
    public WeatherUndergroundHTTPRequest() {
        super();
    }

    public static InputStream get(String latStr, String lonStr) throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append(QUERY_URL);
        buf.append(latStr);
        buf.append(COMMA);
        buf.append(lonStr);
        String urlStr = buf.toString();
        return readFrom(urlStr, null);
    }

    public static InputStream get(String urlStr) throws Exception {
        return readFrom(urlStr, null);
    }

    public static InputStream post(String urlStr, String post) throws Exception {
        return readFrom(urlStr, post);
    }

    private static InputStream readFrom(String urlStr, String postStr) throws MalformedURLException, IOException {
        URLConnection conn = new URL(urlStr).openConnection();
        conn.setDoInput(true);
        if (postStr != null && postStr.length() > 0) {
            conn.setDoOutput(true);
            DataOutputStream output = new DataOutputStream(conn.getOutputStream());
            output.writeBytes(postStr);
            output.flush();
            output.close();
        }
        return conn.getInputStream();
    }
}
