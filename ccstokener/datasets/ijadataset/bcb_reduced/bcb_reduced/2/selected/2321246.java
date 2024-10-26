package com.angis.fx.activity.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.widget.Toast;
import com.angis.fx.util.GeoLatLng;

public class GSMService extends Service {

    private TelephonyManager tm;

    private double mLat;

    private double mLon;

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        openGSM();
    }

    public GeoLatLng getGeoLatLon() {
        return new GeoLatLng(mLat, mLon);
    }

    public void returnGeoLatLon() {
        Intent intent = new Intent("sendgsm");
        intent.putExtra("geolatlon", new GeoLatLng(mLat, mLon));
        this.sendBroadcast(intent);
    }

    private void openGSM() {
        try {
            GsmCellLocation gcl = (GsmCellLocation) tm.getCellLocation();
            int cid = gcl.getCid();
            int lac = gcl.getLac();
            int mcc = Integer.valueOf(tm.getNetworkOperator().substring(0, 3));
            int mnc = Integer.valueOf(tm.getNetworkOperator().substring(3, 5));
            JSONObject holder = new JSONObject();
            holder.put("version", "1.1.0");
            holder.put("host", "maps.google.com");
            holder.put("request_address", true);
            JSONArray array = new JSONArray();
            JSONObject data = new JSONObject();
            data.put("cell_id", cid);
            data.put("location_area_code", lac);
            data.put("mobile_country_code", mcc);
            data.put("mobile_network_code", mnc);
            array.put(data);
            holder.put("cell_towers", array);
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://www.google.com/loc/json");
            StringEntity se = new StringEntity(holder.toString());
            post.setEntity(se);
            HttpResponse resp = client.execute(post);
            HttpEntity entity = resp.getEntity();
            BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
            StringBuffer sb = new StringBuffer();
            String result = br.readLine();
            while (result != null) {
                sb.append(result);
                result = br.readLine();
            }
            mLat = Double.parseDouble(sb.toString().split(":")[2].split(",")[0]);
            mLon = Double.parseDouble(sb.toString().split(":")[3].split(",")[0]);
        } catch (Exception e) {
        }
        returnGeoLatLon();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
