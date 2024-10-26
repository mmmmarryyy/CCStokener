package com.danielestevez.kokoroid.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.util.Log;

public class GS1ServicesHelper {

    private static final String TAG = "HttpService";

    public void sampleAccess() {
        Log.d(TAG, "Button httpServiceBtn pressed");
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("LoginPanel_ScriptManager_HiddenField", ""));
        nameValuePairs.add(new BasicNameValuePair("__VIEWSTATE", "/wEPDwUJNzEwNzI5ODEyD2QWAgIBD2QWBmYPDxYCHgdWaXNpYmxlaGRkAgEPZBYCAgMPZBYCAgMPPCsACgEADxYCHhJEZXN0aW5hdGlvblBhZ2VVcmwFDn4vRGVmYXVsdC5hc3B4ZGQCAw9kFgJmD2QWBgIHDw8WAh4EVGV4dAUGU2VhcmNoZGQCCw8PFgIfAGhkZAINDw8WAh8AaGRkZD4oib5c9LoitBSeYav+99x1qxZb"));
        nameValuePairs.add(new BasicNameValuePair("__EVENTVALIDATION", "/wEWBwLh4JaVCwK10vUrAtfxrfMFAtLawdoCAorY9vsFAvy10s8DApD0z+sF447IiKCYMAJZSVFXA/Uf9Pp5P6E="));
        nameValuePairs.add(new BasicNameValuePair("rblGTIN", "party"));
        nameValuePairs.add(new BasicNameValuePair("txtRequestGTIN", "6111035000027"));
        nameValuePairs.add(new BasicNameValuePair("Realm", "All"));
        nameValuePairs.add(new BasicNameValuePair("btnSubmitGTIN", "Search"));
        postData("http://gepir.gs1.org/V31/xx/gtin.aspx?Lang=en-US", nameValuePairs);
    }

    public void postData(String url, List<NameValuePair> nameValuePairs) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                Log.i(TAG, "HEADER: " + headers[i].getName() + " - " + headers[i].getValue());
            }
            InputStream is = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line = "";
            String gtinCode = null;
            while ((line = reader.readLine()) != null) {
                System.out.println("Parsing line... " + line);
                if (line.contains("<html xmlns:fn")) {
                    gtinCode = line.substring(line.indexOf("GLN:") + 165, line.indexOf("GLN:") + 176);
                    System.out.println("OUT: " + gtinCode);
                    break;
                }
            }
            Log.i(TAG, "OK");
        } catch (ClientProtocolException e) {
            Log.e(TAG, "ClientProtocolException ", e);
        } catch (IOException e) {
            Log.e(TAG, "HTTP Not Available", e);
        }
    }
}
