package org.andolphin.client.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.andolphin.client.Andolphin;
import org.andolphin.client.Result;
import org.andolphin.client.ResultHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class RemoteAccess {

    private static final String EXPIRE = "expire";

    private static final String AUTH_TOKEN = "auth_token";

    private static final String PASSWORD = "password";

    private static final String USERNAME = "username";

    private static final String TAG = RemoteAccess.class.getName();

    private static final String EMAIL = "email";

    private RemoteAccess() {
    }

    public static void login(final String username, final String password, final ResultHandler rh) {
        HttpPost request = new HttpPost(Andolphin.URL.LOGIN);
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair(USERNAME, username));
        qparams.add(new BasicNameValuePair(PASSWORD, password));
        try {
            request.setEntity(new UrlEncodedFormEntity(qparams, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, StringUtils.digMessage(e), e);
        }
        executeRequest(request, rh);
    }

    public static void regsiter(final String username, final String password, final String email, final ResultHandler rh) {
        HttpPost request = new HttpPost(Andolphin.URL.REGSITER);
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair(USERNAME, username));
        qparams.add(new BasicNameValuePair(PASSWORD, password));
        qparams.add(new BasicNameValuePair(EMAIL, email));
        try {
            request.setEntity(new UrlEncodedFormEntity(qparams, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, StringUtils.digMessage(e), e);
        }
        executeRequest(request, rh);
    }

    /**
     * 执行请求！
     * 
     * @param request 请求
     * @param handler 结果的handler
     */
    public static void executeRequest(final HttpRequestBase request, final ResultHandler rh) {
        Handler handler = rh.getHandler();
        Context ctx = rh.getContext();
        SharedPreferences prefs = ctx.getSharedPreferences(Andolphin.Prefs.ANDOLPHIN, Context.MODE_WORLD_READABLE);
        String username = prefs.getString(Andolphin.Prefs.USERNAME, "");
        String authToken = prefs.getString(Andolphin.Prefs.AUTH_TOKEN, "");
        request.setHeader(USERNAME, username);
        request.setHeader(AUTH_TOKEN, authToken);
        DefaultHttpClient client = new DefaultHttpClient();
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();
        try {
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                String resultText = StringUtils.readStringFromStream(entity.getContent());
                try {
                    JSONObject json = new JSONObject(resultText);
                    Result result = Result.createFromJSONObject(json);
                    sendMessage(handler, message, bundle, result);
                } catch (JSONException e) {
                    Log.e(TAG, StringUtils.digMessage(e), e);
                    Result result = Result.DATA_ERROR;
                    result.setMessage(StringUtils.digMessage(e));
                    sendMessage(handler, message, bundle, result);
                }
            } else {
                Log.e(TAG, "Http Response StatusCode : " + status.getStatusCode());
                sendMessage(handler, message, bundle, Result.NETWORK_ERROR);
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, StringUtils.digMessage(e), e);
            sendMessage(handler, message, bundle, Result.NETWORK_ERROR);
        } catch (IOException e) {
            Log.e(TAG, StringUtils.digMessage(e), e);
            sendMessage(handler, message, bundle, Result.NETWORK_ERROR);
        } catch (Exception e) {
            Log.e(TAG, StringUtils.digMessage(e), e);
            sendMessage(handler, message, bundle, Result.UNKNOWN_ERROR);
        }
    }

    /**
     * 发送消息
     * 
     * @param handler Hanlder
     * @param result 结果
     * @param bundle Bundle
     * @param message Message
     */
    private static void sendMessage(Handler handler, Message message, Bundle bundle, Result result) {
        bundle.putSerializable(Result.KEY, result);
        message.setData(bundle);
        message.what = result.getCode();
        handler.sendMessage(message);
    }
}
