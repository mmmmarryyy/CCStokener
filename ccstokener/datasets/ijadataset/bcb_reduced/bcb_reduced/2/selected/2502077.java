package com.sunavi.wifi;

import java.io.IOException;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

public class wifi extends Activity {

    /** Called when the activity is first created. */
    WifiManager mainWifi;

    WifiReceiver receiverWifi;

    List<ScanResult> wifiList;

    TextView textview;

    StringBuilder sb = new StringBuilder();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        textview = (TextView) findViewById(R.id.textView1);
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
    }

    public boolean onKeyUp(int KeyCode, KeyEvent envent) {
        if (KeyCode == KeyEvent.KEYCODE_0) onDestroy(); else super.onKeyUp(KeyCode, envent);
        return true;
    }

    public void onDestroy() {
        Log.e("wifi", "onDestroy");
        super.onDestroy();
    }

    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            wifiList = mainWifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++) {
                Log.e("wifi", wifiList.get(i).toString());
            }
            HttpPost httpRequest = new HttpPost("http://www.google.com/loc/json");
            JSONObject holder = new JSONObject();
            JSONArray array = new JSONArray();
            try {
                holder.put("version", "1.1.0");
                holder.put("host", "maps.google.com");
                holder.put("request_address", true);
                for (int i = 0; i < wifiList.size(); i++) {
                    JSONObject current_data = new JSONObject();
                    current_data.put("mac_address", wifiList.get(i).BSSID);
                    current_data.put("ssid", wifiList.get(i).SSID);
                    current_data.put("signal_strength", wifiList.get(i).level);
                    array.put(current_data);
                }
                holder.put("wifi_towers", array);
                Log.e("wifi", holder.toString());
                StringEntity se = new StringEntity(holder.toString());
                httpRequest.setEntity(se);
                HttpResponse resp = new DefaultHttpClient().execute(httpRequest);
                if (resp.getStatusLine().getStatusCode() == 200) {
                    String strResult = EntityUtils.toString(resp.getEntity());
                    textview.setText(strResult);
                }
            } catch (JSONException e) {
                textview.setText(e.getMessage().toString());
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                textview.setText(e.getMessage().toString());
                e.printStackTrace();
            } catch (IOException e) {
                textview.setText(e.getMessage().toString());
                e.printStackTrace();
            } catch (Exception e) {
                textview.setText(e.getMessage().toString());
                e.printStackTrace();
            }
        }
    }
}
