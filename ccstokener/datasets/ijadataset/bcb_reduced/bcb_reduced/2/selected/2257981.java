package com.rhythm.commons.net;

import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author Michael J. Lee @ Synergy Energy Holdings, LLC
 */
public class NetUtil {

    private NetUtil() {
    }

    public static NetResponse ping(String urlAddress, int timeout) {
        URL url = null;
        URLConnection connection = null;
        NetResponse response = null;
        try {
            url = new URL(urlAddress);
            response = NetResponse.start(url, timeout);
            connection = url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            response.stop(true);
            return response;
        } catch (Exception ex) {
            if (response != null) response.addException(ex); else {
                response = NetResponse.start(null, timeout);
                response.stop(false);
                response.addException(ex);
            }
        }
        return response;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
