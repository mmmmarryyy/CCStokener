package com.google.gdata.client.authn.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple client for making http requests and returning the response body.
 * Uses {@link java.net.HttpURLConnection} to make http requests.
 *
 * 
 */
public class OAuthHttpClient {

    /**
   * Makes an http request to the input URL, and returns the response body as a
   * string.
   *
   * @param url the url to make the request to
   * @return the response body of the request
   * @throws OAuthException if there was an error making the request
   */
    public String getResponse(URL url) throws OAuthException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (IOException e) {
            throw new OAuthException("Error getting HTTP response", e);
        }
    }
}
