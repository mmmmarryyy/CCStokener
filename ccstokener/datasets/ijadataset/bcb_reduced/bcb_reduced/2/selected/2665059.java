package com.goldgewicht.francois.google.wave.extensions.robots.drmaps.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * Basic HTTP helper.
 * 
 * @author francois.goldgewicht@gmail.com (Francois Goldgewicht)
 */
public class HttpHelper {

    private static final Logger log = Logger.getLogger(HttpHelper.class.getName());

    /**
	 * Sends a synchronous HTTP GET request and returns the associated HTTP response content
	 * @param url the destination of the request
	 * @return the content of the response
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
    public static String sendSynchronousHttpGetRequest(String url) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder result = new StringBuilder();
        try {
            String inputLine = null;
            while ((inputLine = reader.readLine()) != null) {
                result.append(inputLine);
            }
        } catch (Exception e) {
            log.warning(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }
        return result.toString();
    }

    /**
	 * URL-encodes a string using UTF-8 encoding.
	 * @param string the string to URL-encode.
	 * @return the URL-encoded string
	 * @throws UnsupportedEncodingException
	 */
    public static String urlEncode(String string) throws UnsupportedEncodingException {
        return URLEncoder.encode(string, "UTF-8");
    }
}
