package com.clouds.aic.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.clouds.aic.Constant;
import com.clouds.aic.activity.DesktopActivity;
import com.clouds.aic.activity.LoginActivity;

public class LoginController {

    LoginActivity activity = null;

    public LoginController(LoginActivity loginActivity) {
        activity = loginActivity;
    }

    public boolean login(String username, String password) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("https://mt0-app.cloud.cm/rpc/json");
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("m", "login"));
        nameValuePairs.add(new BasicNameValuePair("c", "User"));
        nameValuePairs.add(new BasicNameValuePair("password", password));
        nameValuePairs.add(new BasicNameValuePair("alias", username));
        String result = "";
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            result = EntityUtils.toString(response.getEntity());
            String info[] = result.split(",");
            String info1 = info[0].trim();
            String loginResult[] = info1.split(":");
            String lResult = loginResult[1];
            Header[] acturalHeaders = response.getAllHeaders();
            String sessionId = substractSessionId(acturalHeaders);
            Log.d("SessionId in the Header:", sessionId);
            Log.d("what2:", "what2");
            if (lResult.equals("0")) {
                Intent i = new Intent(activity, DesktopActivity.class);
                i.putExtra(Constant.USERNAME_KEY, username);
                i.putExtra(Constant.PHP_SESSION_ID, sessionId);
                activity.startActivity(i);
            } else if (lResult.equals("-1")) {
                return false;
            }
            Log.d("Cloud Debug", lResult);
        } catch (java.net.UnknownHostException e) {
            Toast.makeText(activity, "NetWork problem", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        Log.d("MSG", result);
        return true;
    }

    private String substractSessionId(Header headers[]) {
        String result = "";
        for (Header h : headers) {
            Log.d("Header", h.getName() + " " + h.getValue());
            String name = h.getName();
            String value = h.getValue();
            if (name.trim().toLowerCase().contains("set-cookie")) {
                String[] pairs = value.split(";");
                for (String pair : pairs) {
                    String[] p = pair.split("=");
                    if (p.length != 2) continue;
                    if (p[0].trim().toLowerCase().equals("phpsessid")) {
                        result = p[1];
                    }
                }
            }
        }
        return result;
    }
}
