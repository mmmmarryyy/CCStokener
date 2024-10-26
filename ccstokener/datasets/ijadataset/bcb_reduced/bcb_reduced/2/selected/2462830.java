package de.tudresden.inf.rn.mobilis.android.xhunt.dvb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import android.util.Log;

public class DVBMonitor {

    String stationId;

    ArrayList<DVBData> dvbData;

    /** The TAG for the Log. */
    private static final String TAG = "DVBMonitor";

    public DVBMonitor(String stationId) {
        if (stationId == null) stationId = "hbf";
        this.stationId = stationId;
        String prefix = "http://widgets.vvo-online.de/abfahrtsmonitor/Abfahrten.do?ort=Dresden&hst=";
        String suffix = "&vm=stadtbus&vm=strassenbahn&vm=s-bahn";
        String request = prefix + stationId + suffix;
        String resp = sendRequest(request);
        dvbData = processData(resp);
    }

    public ArrayList<DVBData> getDVBData() {
        return dvbData;
    }

    public static String sendRequest(String urlstring) {
        URL url;
        String line;
        Log.i("DVBMonitor", "Please wait while receiving data from dvb...");
        try {
            url = new URL(urlstring);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            if ((line = in.readLine()) != null) {
                return line;
            } else {
                return null;
            }
        } catch (Exception ex) {
            Log.e("DVBMonitor", ex.toString() + " while sending request to dvb");
            return null;
        }
    }

    public static ArrayList<DVBData> processData(String response) {
        if (response == null) {
            Log.e("DVBMonitor", "Failure while receiving data");
            return null;
        } else {
            if (response.equals("[]")) {
                Log.e(TAG, "No data");
                return null;
            } else {
                response = response.replaceAll("&quot;", "");
                response = response.replaceAll("\"", "");
                response = response.replaceAll("&#196;", "�");
                response = response.replaceAll("&#214;", "�");
                response = response.replaceAll("&#220;", "�");
                response = response.replaceAll("&#228;", "�");
                response = response.replaceAll("&#246;", "�");
                response = response.replaceAll("&#252;", "�");
                response = response.replaceAll("&#223;", "ss");
                response = response.replaceAll("\\[", "");
                response = response.replaceAll("\\]", "");
                String[] segs = response.split(Pattern.quote(","));
                int count = segs.length / 3;
                ArrayList<DVBData> list = new ArrayList<DVBData>();
                for (int i = 0; i < count; i++) {
                    list.add(new DVBData(segs[(i * 3)], segs[(i * 3) + 1], segs[(i * 3) + 2]));
                }
                return list;
            }
        }
    }
}
