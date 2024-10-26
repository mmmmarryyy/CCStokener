package gr.model.thesis;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class EventPublisher {

    String btmac;

    String zone;

    Long time;

    Long lat;

    Long lon;

    Float accuracy;

    public EventPublisher() {
    }

    public void publish(HashMap<String, Object> m) {
        if (m.containsKey("blipsystem.id")) {
            if (((String) m.get("blipsystem.id")).equals("blipsystem.itu")) {
                btmac = (String) m.get("terminal.btmac");
                zone = (String) m.get("zone.current");
                time = (Long) m.get("timestamp");
                lat = null;
                lon = null;
                accuracy = null;
                System.out.println(time.toString());
            }
        } else if (((String) m.get("type")).equals("device.GPSpos")) {
            btmac = (String) m.get("terminal.btmac");
            time = (Long) m.get("timestamp");
            lat = (Long) m.get("currentLatitude");
            lon = (Long) m.get("currentLongitude");
            accuracy = (Float) m.get("accuracy");
            zone = null;
        }
        HashMap<String, Object> event = new HashMap<String, Object>();
        event.put("fusedLocation", "datafusion");
        event.put("terminal.btmac", btmac);
        event.put("current.zone", zone);
        event.put("timestamp", time);
        event.put("currentLatitude", lat);
        event.put("currentLongitude", lon);
        event.put("accuracy", accuracy);
        System.out.println(event.toString());
        JSONObject jsonEvent = new JSONObject(event);
        Map<String, Object> frm = new HashMap<String, Object>();
        frm.put("event", jsonEvent.toString());
        System.out.println(frm.toString());
        try {
            URL url = new URL("http://test-pub.appspot.com/androidtest");
            this.fetch(url, HTTPMethod.POST, this.toFormEncoded(frm), new WithResponse() {

                @Override
                public void doWithResponse(String payload) throws Exception {
                    System.out.println("http response: " + payload);
                }
            });
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public interface WithResponse {

        void doWithResponse(String payload) throws Exception;
    }

    public enum HTTPMethod {

        POST, GET, DELETE, PUT
    }

    public void fetch(URL url, HTTPMethod method, String payload, WithResponse wr) throws IOException {
        System.out.println("fetchin' " + url.toString() + " with GAE fetch service");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setReadTimeout(10000);
            connection.setRequestMethod(method.name());
            System.out.println(method.name().toString());
            connection.setRequestProperty("Connection", "close");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (wr != null) {
                connection.setDoOutput(true);
            }
            connection.connect();
            System.out.println(connection.toString());
            if (payload != null) {
                OutputStream out = null;
                OutputStreamWriter outWriter = null;
                try {
                    out = connection.getOutputStream();
                    outWriter = new OutputStreamWriter(out, "UTF-8");
                    outWriter.write(payload);
                    System.out.println(out.toString());
                } finally {
                    close(outWriter);
                    close(out);
                }
            }
            if (wr != null) {
                InputStream in = null;
                InputStreamReader reader = null;
                StringBuilder sb = new StringBuilder();
                try {
                    in = connection.getInputStream();
                    reader = new InputStreamReader(in);
                    BufferedReader bufReader = new BufferedReader(reader);
                    String line;
                    while ((line = bufReader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    System.out.println(line);
                } finally {
                    close(reader);
                    close(in);
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected String toFormEncoded(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder();
        boolean f = true;
        for (String k : m.keySet()) {
            if (f) {
                f = !f;
            } else {
                sb.append("&");
            }
            sb.append(k).append('=');
            Object o = m.get(k);
            String s = o.toString();
            try {
                sb.append(URLEncoder.encode(s, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                System.out.println("UTF-8 encoding not supported?!?!?!?");
            }
        }
        return sb.toString();
    }
}
