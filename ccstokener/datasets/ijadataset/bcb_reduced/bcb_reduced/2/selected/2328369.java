package com.bbn.wild.server.component.source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import com.bbn.wild.server.location.CivicLocation;
import com.bbn.wild.server.location.LocationRequest;
import com.bbn.wild.server.location.LocationResponse;
import com.bbn.wild.server.location.Measurement;
import com.bbn.wild.server.location.WifiMeasurement;
import com.bbn.wild.server.location.Circle;
import com.bbn.wild.server.component.Component;
import com.bbn.wild.server.component.SynchronousResponder;
import com.bbn.wild.server.exception.UnregisteredComponentException;
import org.json.*;

public class GearsLocationSource implements SynchronousResponder {

    private static final String DEFAULT_URI = "http://www.google.com/loc/json";

    private static final String URI_PARAM = "com.bbn.wild.server.component.source.gears.uri";

    private String gearsServerUri = DEFAULT_URI;

    public GearsLocationSource() {
    }

    public LocationResponse getResponse(LocationRequest lrq) throws UnregisteredComponentException {
        LocationResponse lrs = lrq.createResponse();
        try {
            String rqs, rss;
            rqs = encodeGearsRequest(lrq);
            URL url = new URL(this.gearsServerUri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/json");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(rqs);
            wr.flush();
            BufferedReader rd;
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            rss = "";
            String line;
            while ((line = rd.readLine()) != null) {
                rss += line;
            }
            rd.close();
            decodeGearsResponse(rss, lrs);
        } catch (Exception e) {
            e.printStackTrace();
            lrs.setError("Error querying Gears");
        }
        return lrs;
    }

    public static String encodeGearsRequest(LocationRequest lrq) {
        String rqs = "{ \"version\" : \"1.1.0\", \"host\" : \"_null_.localdomain\", \"request_address\" : true ";
        if (lrq.getMeasurements() != null) {
            Measurement[] meas = lrq.getMeasurements();
            boolean gotWifi = false;
            for (int i = 0; i < meas.length && !gotWifi; ++i) gotWifi = (meas[i] instanceof WifiMeasurement);
            if (gotWifi) {
                rqs += ", \"wifi_towers\" : [ ";
                int wifiCount = 0;
                for (int i = 0; i < meas.length; ++i) {
                    if (!(meas[i] instanceof WifiMeasurement)) continue;
                    WifiMeasurement wfm = (WifiMeasurement) meas[i];
                    if (wifiCount > 0) rqs += ", ";
                    wifiCount++;
                    rqs += "{";
                    if (wfm.getBssid() != null) {
                        rqs += "\"mac_address\" : \"" + wfm.getBssid() + "\", ";
                    }
                    if (wfm.getChannel() != -1) {
                        rqs += "\"channel\" : \"" + wfm.getChannel() + "\", ";
                    }
                    if (wfm.getSsid() != null) {
                        rqs += "\"ssid\" : \"" + wfm.getSsid() + "\", ";
                    }
                    if (wfm.getRssi() != -1) {
                        rqs += "\"signal_strength\" : \"" + wfm.getRssi() + "\", ";
                    }
                    if (wfm.getSnr() != -1) {
                        rqs += "\"signal_to_noise\" : \"" + wfm.getSnr() + "\", ";
                    }
                    rqs += "\"foo\": \"bar\"";
                    rqs += "}";
                }
                rqs += "] }";
            } else {
                rqs += "}";
            }
        } else {
            rqs += "}";
        }
        System.err.println(rqs);
        return rqs;
    }

    public static void decodeGearsResponse(String gearsResponse, LocationResponse lrs) throws Exception {
        JSONObject jsrs = new JSONObject(gearsResponse);
        if (jsrs == null) return;
        if (jsrs.get("location") != null) {
            JSONObject loc = jsrs.getJSONObject("location");
            if (loc.has("latitude") && loc.has("longitude")) {
                int radius = (loc.has("accuracy")) ? loc.getInt("accuracy") : 0;
                lrs.setGeodetic(new Circle(loc.getDouble("latitude"), loc.getDouble("longitude"), radius));
            }
            if (loc.has("address")) {
                CivicLocation civic = new CivicLocation();
                JSONObject addr = loc.getJSONObject("address");
                if (addr.has("country_code")) {
                    civic.setCountry(addr.getString("country_code"));
                }
                if (addr.has("region")) {
                    civic.setA1(addr.getString("region"));
                }
                if (addr.has("county")) {
                    civic.setA2(addr.getString("county"));
                }
                if (addr.has("city")) {
                    civic.setA3(addr.getString("city"));
                }
                if (addr.has("postal_code")) {
                    civic.setPC(addr.getString("postal_code"));
                }
                if (addr.has("street")) {
                    civic.setRD(addr.getString("street"));
                }
                if (addr.has("street_number")) {
                    civic.setHNO(addr.getString("street_number"));
                }
                lrs.setCivic(civic);
            }
        }
    }

    public Component createComponent() {
        return new GearsLocationSource();
    }

    public void configure(Properties prop) {
        if (prop.containsKey(URI_PARAM)) this.gearsServerUri = prop.getProperty(URI_PARAM);
    }

    public Component getSuccessor() {
        return null;
    }
}
