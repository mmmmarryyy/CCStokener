package com.shengyijie.model.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;
import com.shengyijie.activity.share.ShareConstant;
import com.shengyijie.context.ContextApplication;
import com.shengyijie.model.object.baseobject.User;
import com.shengyijie.util.Utility;

/** @title: HttpConnect DRIVER 1.0 
 * @description: HttpConnect DRIVER 1.0 
 * @copyright: Dippo (c) 2011.06 
 * @company: Diipo 
 * @author liukun 
 * @version 1.0 
 */
public class HttpConnectApi {

    public static String CENTER_SERVICE_URL = "http://iphone.shengyijie.net/center_service/index";

    public static String USER_SERVICE_URL = "http://iphone.shengyijie.net/user_service/index";

    public static String KEY = "$%^&*shengyijie.net!@#";

    public HttpClient httpclient;

    private static HttpConnectApi httpConnect;

    public int maxTime = 15000;

    public HttpConnectApi() {
    }

    public static HttpConnectApi getInstance() {
        if (httpConnect == null) {
            httpConnect = new HttpConnectApi();
        }
        return httpConnect;
    }

    public HttpResponse executeHttpRequest(HttpRequestBase httpRequest) {
        HttpResponse response = null;
        try {
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, maxTime);
            HttpConnectionParams.setSoTimeout(httpParams, maxTime);
            httpclient = new DefaultHttpClient(httpParams);
            response = httpclient.execute(httpRequest);
            maxTime = 15000;
        } catch (Exception e) {
        }
        return response;
    }

    public HttpGet createHttpGet(String url, NameValuePair... nameValuePairs) {
        String query = URLEncodedUtils.format(stripNulls(nameValuePairs), HTTP.UTF_8);
        HttpGet httpGet = new HttpGet(url + "?" + query);
        return httpGet;
    }

    public HttpPost createHttpPost(String url, NameValuePair... nameValuePairs) {
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(stripNulls(nameValuePairs)));
        } catch (Exception e) {
        }
        return httpPost;
    }

    private List<NameValuePair> stripNulls(NameValuePair... nameValuePairs) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (int i = 0; i < nameValuePairs.length; i++) {
            NameValuePair param = nameValuePairs[i];
            if (param.getValue() != null) {
                params.add(param);
            }
        }
        return params;
    }

    public HttpResponse getRenren(String token) {
        try {
            HttpPost httpPost = new HttpPost("https://graph.renren.com/oauth/token");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
            nameValuePairs.add(new BasicNameValuePair("code", token));
            nameValuePairs.add(new BasicNameValuePair("client_id", ShareConstant.RENREN_API_KEY));
            nameValuePairs.add(new BasicNameValuePair("client_secret", ShareConstant.RENREN_API_SECRET));
            nameValuePairs.add(new BasicNameValuePair("redirect_uri", "http://graph.renren.com/oauth/login_success.html"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse regiSter(String email, String password, String mobile, String user_name, String contacter, int industry, int com_type, int user_type) {
        try {
            HttpPost httpPost = new HttpPost(USER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("email", email);
            params.put("mobile", mobile);
            params.put("password", password);
            params.put("industry", industry);
            params.put("com_type", com_type);
            params.put("user_name", user_name);
            params.put("contacter", contacter);
            params.put("user_type", user_type);
            JSONObject json = new JSONObject();
            json.put("method", "regiSter");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "regiSter: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getCheckEmail(String email) {
        try {
            HttpPost httpPost = new HttpPost(USER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("email", email);
            JSONObject json = new JSONObject();
            json.put("method", "getCheckEmail");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getCheckEmail: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse userKey(Context context) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("os", 1);
            params.put("lan", "android");
            params.put("key", Utility.getIMEI(context));
            JSONObject json = new JSONObject();
            json.put("method", "userKey");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "userKey: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse doLogin(String email, String password) {
        try {
            HttpPost httpPost = new HttpPost(USER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("email", email);
            params.put("password", password);
            JSONObject json = new JSONObject();
            json.put("method", "doLogin");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "doLogin: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getLoginUser(String sessionID) {
        try {
            HttpPost httpPost = new HttpPost(USER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getLogIn");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getLogIn: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getFindManage(String sessionID, String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("limit", limit);
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getFindManage");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getFindManage: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getNewsType(String sessionID) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getNewsType");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getNewsType: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getMessageDetail(String sessionID, String id) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("mageid", id);
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getOneManage");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getOneManage: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getAttentionProject(String sessionID, String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("limit", limit);
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getFindProject");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getFindProject: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getFindIndustry(int id, String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("industry", id);
            params.put("limit", limit);
            JSONObject json = new JSONObject();
            json.put("method", "getFindIndustry");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getFindIndustry: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getRecommendedList(String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("limit", limit);
            JSONObject json = new JSONObject();
            json.put("method", "getStatuList");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getStatuList: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getUserProjectDetail() {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("sessionid", ContextApplication.user.getSession_ID());
            JSONObject json = new JSONObject();
            json.put("method", "getOneProject");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getOneProject: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getOneProjectDetail(int id) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("sessionid", ContextApplication.user.getSession_ID());
            params.put("pro_id", id);
            JSONObject json = new JSONObject();
            json.put("method", "getOneProject");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getOneProject: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getProjectDetail(int id) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("pro_id", id);
            JSONObject json = new JSONObject();
            json.put("method", "getProjectOne");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getProjectOne: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getProjectPic(int id) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("pro_id", id);
            JSONObject json = new JSONObject();
            json.put("method", "getProjectPic");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getProjectPic: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse guestBook(String id, String guestname, String mobile, String email, String address, String content, User user) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("proid", id);
            params.put("guestname", guestname);
            params.put("mobile", mobile);
            params.put("email", email);
            params.put("address", address);
            params.put("content", content);
            if (ContextApplication.isUserLogin && ContextApplication.user != null) params.put("userid", user.getUserID()); else {
                params.put("userid", 0);
            }
            JSONObject json = new JSONObject();
            json.put("method", "guesbook");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "guesbook: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse attentionPro(int id, User user) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("pro_id", id);
            if (ContextApplication.isUserLogin && ContextApplication.user != null) params.put("sessionid", ContextApplication.user.getSession_ID());
            JSONObject json = new JSONObject();
            json.put("method", "proction");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "proction: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getMessageProject(String sessionID, String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("limit", limit);
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getFindWord");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getFindWord: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getAbout(int type) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("type", type);
            ;
            JSONObject json = new JSONObject();
            json.put("method", "aboutConfig");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "aboutConfig: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse searchProject(String keyword, int industry, int city, String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("keyword", keyword);
            params.put("industry", industry);
            params.put("city", city);
            params.put("limit", limit);
            JSONObject json = new JSONObject();
            json.put("method", "soKeyWord");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "soKeyWord: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getOneAdver(int id) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("adverid", id);
            JSONObject json = new JSONObject();
            json.put("method", "getOneAdver");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getOneAdver: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse getFindGuseBook(String sessionID, String limit) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("limit", limit);
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "getFindGuseBook");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "getFindGuseBook: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }

    public HttpResponse setGuseBook(String sessionID, String id) {
        try {
            HttpPost httpPost = new HttpPost(CENTER_SERVICE_URL);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            JSONObject params = new JSONObject();
            params.put("bokid", id);
            params.put("sessionid", sessionID);
            JSONObject json = new JSONObject();
            json.put("method", "setGuseBook");
            json.put("key", KEY);
            json.put("params", params);
            nameValuePairs.add(new BasicNameValuePair("service", json.toString()));
            Log.e(ContextApplication.TAG, "setGuseBook: " + json.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
        }
        return null;
    }
}
