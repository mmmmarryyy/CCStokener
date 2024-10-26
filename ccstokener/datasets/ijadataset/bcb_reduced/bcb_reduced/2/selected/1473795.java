package com.shengyijie.model.position;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class Position {

    Context mContext;

    LocationManager lm;

    TelephonyManager tm;

    private int count;

    private int cid[] = new int[10];

    private int lac[] = new int[10];

    private JSONObject holder;

    public Position(Context context) {
        mContext = context;
        lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public boolean isGPSAvailable() {
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public double lat;

    public double lng;

    public void positionByPhone() {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://www.google.com/loc/json");
            StringEntity se;
            se = new StringEntity("json=" + holder.toString());
            post.setEntity(se);
            HttpResponse resp = client.execute(post);
            HttpEntity he = resp.getEntity();
            InputStreamReader isr = new InputStreamReader(he.getContent());
            BufferedReader br = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();
            String result = br.readLine();
            while (result != null) {
                sb.append(result);
                result = br.readLine();
            }
            JSONObject d = new JSONObject(sb.toString());
            JSONObject location = new JSONObject();
            JSONObject address = new JSONObject();
            location = (JSONObject) d.get("location");
            address = (JSONObject) location.get("address");
        } catch (Exception e) {
        }
    }

    public JSONObject getCellInfo() {
        try {
            tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            GsmCellLocation gcl = (GsmCellLocation) tm.getCellLocation();
            cid[0] = gcl.getCid();
            lac[0] = gcl.getLac();
            int mcc = Integer.valueOf(tm.getNetworkOperator().substring(0, 3));
            int mnc = Integer.valueOf(tm.getNetworkOperator().substring(3, 5));
            List<NeighboringCellInfo> neiList = tm.getNeighboringCellInfo();
            count = neiList.size();
            NeighboringCellInfo info;
            for (int i = 0; i < count; i++) {
                info = neiList.get(i);
                cid[i + 1] = info.getCid();
                lac[i + 1] = info.getLac();
            }
            holder = new JSONObject();
            holder.put("version", "1.1.0");
            holder.put("host", "maps.google.com");
            holder.put("request_address", true);
            JSONArray jarray = new JSONArray();
            JSONObject data;
            for (int i = 0; i < (count > 2 ? 2 : count); i++) {
                data = new JSONObject();
                data.put("cell_id", cid[i]);
                data.put("location_area_code", lac[i]);
                data.put("mobile_country_code", mcc);
                data.put("mobile_network_code", mnc);
                data.put("age", 0);
                jarray.put(data);
            }
            return holder;
        } catch (Exception e) {
            return null;
        }
    }

    public String intToIp(int i) {
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

    public double GetDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double radLat1 = Rad(lat1);
        double radLat2 = Rad(lat2);
        double dlat = radLat1 - radLat2;
        double dlon = Rad(lon1) - Rad(lon2);
        double a = Math.pow((dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow((dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d;
    }

    private static double Rad(double d) {
        return d * Math.PI / 180.0;
    }
}
