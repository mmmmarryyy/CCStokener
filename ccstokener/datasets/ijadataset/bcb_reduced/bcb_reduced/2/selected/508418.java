package com.cnc.mediaconnect.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class JSonCommon {

    public static final String URL_LOGIN = "http://192.168.1.169:8083/mapi/login?format=xml&email={0}&password={1}&submit=Submit+Query";

    public static JSONObject getJSONfromURL(String url) {
        InputStream is = null;
        String result = "";
        JSONObject jArray = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(url);
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
        }
        try {
            InputStreamReader input = new InputStreamReader(is, "iso-8859-1");
            BufferedReader reader = new BufferedReader(input, 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
        } catch (Exception e) {
        }
        try {
            jArray = new JSONObject(result);
        } catch (JSONException e) {
        }
        return jArray;
    }

    public static String[] login(String username, String password) {
        String out[] = new String[2];
        out[0] = null;
        out[1] = null;
        JSONObject json = JSonCommon.getJSONfromURL("http://192.168.1.169:8083/mapi/login?format=xml&email=admin%40cnc.com.vn&password=123456&submit=Submit+Query");
        System.out.println(json);
        return out;
    }
}
