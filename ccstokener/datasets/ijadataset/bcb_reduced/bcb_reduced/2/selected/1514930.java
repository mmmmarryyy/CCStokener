package com.dddforandroid.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import com.dddforandroid.api.R;
import com.dddforandroid.application.ClientAPIActivity;

public class C2DMRegistrationReceiver extends BroadcastReceiver {

    public static final String REGISTRATION_ID = "registration_id";

    public static final String ERROR = "error";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("C2DM", "Registration Receiver called");
        if ("com.google.android.c2dm.intent.REGISTRATION".equals(action)) {
            Log.v("C2DM", "Received registration ID");
            final String registrationId = intent.getStringExtra(REGISTRATION_ID);
            String error = intent.getStringExtra(ERROR);
            Log.v("C2DM", "dmControl: registrationId = " + registrationId + ", error = " + error);
            String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            sendRegistrationIdToServer(deviceId, registrationId);
            saveRegistrationId(context, registrationId);
        }
    }

    private void saveRegistrationId(Context context, String registrationId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = prefs.edit();
        edit.putString(ClientAPIActivity.AUTH, registrationId);
        edit.commit();
    }

    public void sendRegistrationIdToServer(String deviceId, String registrationId) {
        Log.v("C2DM", "Sending registration ID to my application server");
        HttpClient client = SQLiteBackup.authClient;
        HttpPost post = new HttpPost("http://3dforandroid.appspot.com/api/v2/register");
        String responseMessage = null;
        try {
            String jsonString = "{\"deviceid\":\"" + deviceId + "\",\"registrationid\":\"" + registrationId + "\"}";
            StringEntity se = new StringEntity(jsonString);
            se.setContentEncoding(HTTP.UTF_8);
            se.setContentType("application/json");
            post.setEntity(se);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "*/*");
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            InputStream instream;
            instream = entity.getContent();
            responseMessage = read(instream);
            Log.v("postresponse", responseMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String read(InputStream instream) {
        StringBuilder sb = null;
        try {
            sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(instream));
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
