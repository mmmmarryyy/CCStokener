package org.maestroframework.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HTTPUtils {

    public static URLConnection openConnection(String urlString, String username, String password) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String userpassword = username + ":" + password;
        String encodedAuthorization = StringUtils.encodeBase64(userpassword);
        connection.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        return connection;
    }
}
