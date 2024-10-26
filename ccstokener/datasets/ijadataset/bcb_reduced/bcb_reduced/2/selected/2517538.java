package com.isaacwaller.digg;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.util.Log;

public class DiggInterface {

    public static final String userAgent = "DiggAndroid/1.0 (compatible; MSIE 6.0; Windows NT 5.0)";

    public static final String APP_ID = "http%3A%2F%2Fcode.google.com%2Fp%2Fdiggandroid";

    public static String TAG = "DiggInterface";

    public static void reportError(Exception e) {
        Log.e(TAG, "Exception: " + e);
        e.printStackTrace();
    }

    public static JSONObject makeRequest(String url) throws URISyntaxException, ClientProtocolException, IOException, JSONException {
        return makeRequest(url, "");
    }

    public static JSONObject makeRequest(String url, String query) throws URISyntaxException, ClientProtocolException, IOException, JSONException {
        URI u = new URI("http://services.digg.com/" + url + "?type=json&appkey=" + APP_ID + query);
        Log.v(TAG, "Reqesting URI: " + u);
        HttpClient client = new DefaultHttpClient();
        HttpGet r = new org.apache.http.client.methods.HttpGet(u);
        r.setHeader("User-Agent", userAgent);
        HttpResponse p = client.execute(r);
        InputStream reader = p.getEntity().getContent();
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = reader.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return new JSONObject(new JSONTokener(out.toString()));
    }
}
