package com.william.lifetraxer.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import android.util.Log;

public class HttpHelper {

    private static String TAG = "HttpHelper";

    private static String sessionId = null;

    private static String URI = "http://192.168.1.113:8080/LifeTraxerServerNoEngine/dispatcher";

    private static String request(HttpPost httpRequest, List<NameValuePair> params) {
        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            List<Cookie> list = httpClient.getCookieStore().getCookies();
            for (Cookie cookie : list) {
                Log.d(cookie.getName(), cookie.getValue());
                sessionId = cookie.getValue();
            }
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                Log.d(TAG, "strResult=" + strResult);
                return strResult;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String request(String url) {
        try {
            HttpGet httpRequest = new HttpGet(url);
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                Log.d(TAG, "strResult=" + strResult);
                return strResult;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String login(String username, String password) {
        HttpPost post = new HttpPost(URI);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("request_type", "log_handler"));
        params.add(new BasicNameValuePair("log_handle_type", "login"));
        params.add(new BasicNameValuePair("log_username", username));
        params.add(new BasicNameValuePair("log_password", password));
        String result = request(post, params);
        return result;
    }

    public static String registerUser(String username, String password) {
        HttpPost post = new HttpPost(URI);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("request_type", "user_handler"));
        params.add(new BasicNameValuePair("user_handle_type", "insert_user"));
        params.add(new BasicNameValuePair("user_username", username));
        params.add(new BasicNameValuePair("user_password", password));
        String result = request(post, params);
        return result;
    }

    public static String registerInfo(String username, String gender, String birthday, String phone, String email) {
        HttpPost post = new HttpPost(URI);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("request_type", "info_handler"));
        params.add(new BasicNameValuePair("info_handle_type", "insert_info"));
        params.add(new BasicNameValuePair("info_username", username));
        params.add(new BasicNameValuePair("info_gender", gender));
        params.add(new BasicNameValuePair("info_birthday", birthday));
        params.add(new BasicNameValuePair("info_phone", phone));
        params.add(new BasicNameValuePair("info_email", email));
        String result = request(post, params);
        return result;
    }
}
