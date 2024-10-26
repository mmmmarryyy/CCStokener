package com.bazaaroid.mobile.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.*;
import org.apache.http.impl.client.DefaultHttpClient;

public class DefaultActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGetRequest = new HttpGet("http://www.google.com/");
        String line = "", responseString = "";
        try {
            HttpResponse response = client.execute(httpGetRequest);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                while ((line = br.readLine()) != null) {
                    responseString += line;
                }
                br.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tv.setText(responseString);
        setContentView(tv);
    }
}
