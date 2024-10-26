package org.xmlvm.demo.java.photovm.net;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Helper class for making HTTP requests.
 * 
 */
public class HTTPRequest {

    private static DefaultHttpClient client = new DefaultHttpClient();

    public static String get(String url) {
        HttpGet method = new HttpGet(url);
        try {
            HttpResponse response = client.execute(method);
            int returnCode = response.getStatusLine().getStatusCode();
            if ((returnCode >= 200) && (returnCode < 300)) {
                return method.getResponseBodyAsString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
