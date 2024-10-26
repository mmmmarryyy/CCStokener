package jp.ac.tokai.et.lifemode2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.util.Log;

public class HGpsCompass implements IGpsCompassSource {

    private static final int CONNECT_TIMEOUT = 1000;

    private static final int SOCKET_TIMEOUT = 1000;

    private final String url;

    public HGpsCompass(String url) {
        this.url = url;
    }

    @Override
    public double getCompass() {
        InputStream in = null;
        double result = 0;
        try {
            in = connectURL(this.url + "/compass");
            try {
                String aStr = "";
                BufferedReader inbr = new BufferedReader(new InputStreamReader(in));
                aStr = inbr.readLine();
                Pattern pat = Pattern.compile("[ ,]+");
                String[] strs = pat.split(aStr);
                result = Double.valueOf(strs[1]);
                in.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (IOException e) {
            Log.i(LOG_TAG, "Failed to obtain compass over network", e);
        }
        return result;
    }

    @Override
    public TLaLo getGps() {
        InputStream in = null;
        TLaLo lalo = null;
        try {
            in = connectURL(this.url + "/gps");
            try {
                String aStr = "";
                BufferedReader inbr = new BufferedReader(new InputStreamReader(in));
                aStr = inbr.readLine();
                Pattern pat = Pattern.compile("[ ,]+");
                String[] strs = pat.split(aStr);
                lalo = new TLaLo();
                lalo.latitude = Double.valueOf(strs[1]);
                lalo.longitude = Double.valueOf(strs[3]);
                in.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (IOException e) {
            Log.i(LOG_TAG, "Failed to obtain gps over network", e);
        }
        return lalo;
    }

    private InputStream connectURL(String aurl) throws IOException {
        InputStream in = null;
        int response = -1;
        URL url = new URL(aurl);
        URLConnection conn = url.openConnection();
        if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection.");
        HttpURLConnection httpConn = (HttpURLConnection) conn;
        response = getResponse(httpConn);
        if (response == HttpURLConnection.HTTP_OK) {
            in = httpConn.getInputStream();
        } else throw new IOException("Response Code: " + response);
        return in;
    }

    private int getResponse(HttpURLConnection httpConn) throws ProtocolException, IOException {
        int response = -1;
        httpConn.setAllowUserInteraction(false);
        httpConn.setConnectTimeout(CONNECT_TIMEOUT);
        httpConn.setReadTimeout(SOCKET_TIMEOUT);
        httpConn.setInstanceFollowRedirects(true);
        httpConn.setRequestMethod("GET");
        httpConn.connect();
        response = httpConn.getResponseCode();
        return response;
    }
}
