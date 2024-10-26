package com.cejing.ex.pmf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

public class NetworkManager {

    public static final String TAG = "NetworkManager   ";

    public enum Response {

        SUCCESS, FAIL, TIMEOUT
    }

    private Context context;

    private boolean connectionStatus = true;

    private HttpURLConnection conn;

    public NetworkManager(Context context) {
        this.context = context;
    }

    public Boolean getConnectionStatus() {
        ConnectivityManager cm = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) return false;
        connectionStatus = cm.getActiveNetworkInfo().isConnectedOrConnecting();
        return connectionStatus;
    }

    public InputStream getStream(String uri) {
        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            return null;
        }
        conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
        try {
            return conn.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public InputStream doPost(String URI, String data) {
        URL url = null;
        conn = null;
        try {
            url = new URL(URI);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/xml");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            Log.d(TAG + "Flow   doPost   ///", "Encode = " + writer.getEncoding() + "\n XML = \n" + data + "\n");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK || conn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.d(TAG + "Flow   doPost   ///", "[doPost] Successfully Posted");
                Log.d(TAG + "Flow", "[doPost] from Post / show = " + conn.getResponseCode() + " message = " + conn.getResponseMessage());
                return conn.getInputStream();
            } else {
                Log.d(TAG + "Flow", "[doPost] Failed to Post / Err = " + conn.getResponseCode() + " message = " + conn.getResponseMessage());
            }
        } catch (MalformedURLException mfe) {
            mfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public String doPostObject(String URI, String data) {
        URL url = null;
        conn = null;
        try {
            url = new URL(URI);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/xml");
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            Log.d(TAG + "Flow   doPostObject   ///", "Encode = " + writer.getEncoding() + "\n XML = \n" + data + "\n");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK || conn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                Log.d(TAG + "Flow   doPostObject   ///", "[doPost] Successfully Posted");
                Log.d(TAG + "Flow", "[doPostObject] from Post / show = " + conn.getResponseCode() + " message = " + conn.getResponseMessage());
                return conn.getResponseMessage();
            } else {
                Log.d(TAG + "Flow", "[doPostObject] Failed to Post / Err = " + conn.getResponseCode() + " message = " + conn.getResponseMessage());
            }
        } catch (MalformedURLException mfe) {
            mfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "error";
    }
}
