package com.ever365.open.qq;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Map;
import net.gqu.utils.FileCopyUtils;
import net.gqu.utils.JSONUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class QQInfoClient {

    private String appid;

    private String appkey;

    private String appname;

    private String openip;

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public void setAppkey(String appkey) {
        this.appkey = appkey;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setOpenip(String openip) {
        this.openip = openip;
    }

    public Map<String, Object> getCurrentUserInfo(String openid, String openkey) {
        if (openid == null || openkey == null) {
            return null;
        }
        try {
            String url = "http://" + openip + "/user/info?openid=" + openid + "&openkey=" + openkey + "&appid=" + appid + "&appkey=" + appkey + "&appname=" + URLEncoder.encode(appname, "UTF-8");
            System.out.println(url);
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse hr = httpclient.execute(httpget);
            String rawString = FileCopyUtils.copyToString(new InputStreamReader(hr.getEntity().getContent(), "UTF-8"));
            JSONObject jso = new JSONObject(rawString);
            System.out.println(jso.toString());
            return JSONUtils.jsonObjectToMap(jso);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
