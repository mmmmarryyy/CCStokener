package com.googlecode.ipinfodb.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Denis Migol
 * 
 */
public class WebClientImpl implements WebClient {

    @Override
    public String getContent(final URL url) {
        String ret = null;
        try {
            final URLConnection conn = url.openConnection();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            final StringBuffer response = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            ret = response.toString();
        } catch (final SocketTimeoutException e) {
        } catch (final Exception e) {
        }
        return ret;
    }
}
