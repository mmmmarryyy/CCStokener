package com.myzuku.getmylocation.pro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import com.google.android.maps.GeoPoint;

public class ReverseGeocodingHelper {

    /**
	 * Log's TAG.
	 */
    private static final String TAG = "LocateMe";

    /**
	 * {@link StatusLine} HTTP status code when no server error has occurred.
	 */
    private static final int HTTP_STATUS_OK = 200;

    /**
	 * Shared buffer used by {@link #getUrlContent(String)} when reading results
	 * from an API request.
	 */
    private static byte[] sBuffer = new byte[512];

    /**
	 * User-agent string to use when making requests. Should be filled using
	 * {@link #prepareUserAgent(Context)} before making any other calls.
	 */
    private static String sUserAgent = null;

    /**
	 * Thrown when there were problems contacting the remote API server, either
	 * because of a network error, or the server returned a bad status code.
	 */
    @SuppressWarnings("serial")
    public static class ApiException extends Exception {

        public ApiException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public ApiException(String detailMessage) {
            super(detailMessage);
        }
    }

    /**
	 * Thrown when there were problems parsing the response to an API call,
	 * either because the response was empty, or it was malformed.
	 */
    @SuppressWarnings("serial")
    public static class ParseException extends Exception {

        public ParseException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    /**
	 * Prepare the internal User-Agent string for use.
	 * 
	 * @param userAgent
	 *            The user agent string used by the application to identify
	 *            itself
	 */
    public static void prepareUserAgent(String userAgent) {
        sUserAgent = userAgent;
    }

    /**
	 * Get the address from a latitude/longitude pair in {@link GeoPoint}
	 * 
	 * @param point
	 *            The latitude/longitude pair.
	 * @return The address detail.
	 * @throws ApiException
	 *             If any connection or server error occurs.
	 * @throws ParseException
	 *             If there are problems parsing the response.
	 */
    public static ReverseGeocodingResult getGeocodingResult(GeoPoint point) {
        String lat = Double.toString(point.getLatitudeE6() / 1e6);
        String lng = Double.toString(point.getLongitudeE6() / 1e6);
        String url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&sensor=true";
        ReverseGeocodingResult geocodingResult = new ReverseGeocodingResult();
        try {
            String content = getUrlContent(url);
            JSONObject response = new JSONObject(content);
            String statusString = response.getString("status");
            if (!statusString.equalsIgnoreCase("OK")) throw new Exception("Geocoding JSON result status is not OK.");
            JSONArray resultList = response.getJSONArray("results");
            JSONObject firstResult = resultList.getJSONObject(0);
            String formatted_address = firstResult.getString("formatted_address");
            geocodingResult.setAddress(formatted_address);
        } catch (Exception e) {
            Log.e(TAG, "Error in geocoding", e);
        }
        return geocodingResult;
    }

    /**
	 * Get address from a latitude/longitude pair using Android's built-in
	 * geocoding mechanism.
	 * 
	 * @param context
	 *            The application's context
	 * @param point
	 *            The latitude/longitude pair
	 * @return Address details
	 */
    public static ReverseGeocodingResult getGeocodingResult(Context context, GeoPoint point) {
        ReverseGeocodingResult geocodingResult = new ReverseGeocodingResult();
        Geocoder geocoder = new Geocoder(context);
        try {
            List<Address> addresses = geocoder.getFromLocation(point.getLatitudeE6() / 1e6, point.getLongitudeE6() / 1e6, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressLine = "";
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressLine += address.getAddressLine(i) + "\n";
                }
                addressLine += address.getAddressLine(address.getMaxAddressLineIndex());
                geocodingResult.setAddress(addressLine);
            } else {
                geocodingResult.setAddress(null);
            }
        } catch (IOException e) {
            Log.e(TAG, "An error happened in geocoding", e);
        }
        return geocodingResult;
    }

    /**
	 * Pull the raw text content of the given URL. This call blocks until the
	 * operation has completed, and is synchronized because it uses a shared
	 * buffer {@link #sBuffer}.
	 * 
	 * @param url
	 *            The exact URL to request.
	 * 
	 * @return The raw content returned by the server.
	 * @throws ApiException
	 *             If any connection or server error occurs.
	 */
    protected static synchronized String getUrlContent(String url) throws ApiException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HTTP_STATUS_OK) {
                throw new ApiException("Invalid response from server: " + status.toString());
            }
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            int readBytes = 0;
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            return new String(content.toByteArray());
        } catch (IOException e) {
            throw new ApiException("Problem communicating with API", e);
        }
    }
}
