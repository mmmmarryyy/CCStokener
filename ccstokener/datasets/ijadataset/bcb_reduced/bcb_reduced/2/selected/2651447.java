package org.light.portal.mobile.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import android.util.Log;

public class HttpHelper {

    private static HttpHelper instance = new HttpHelper();

    private static final String tag = "HttpHelper";

    public static HttpHelper getIntance() {
        return instance;
    }

    private HttpClient httpclient;

    private CookieStore cookieStore;

    HttpContext localContext;

    private HttpHelper() {
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0(Linux; U; Android 2.2; MobileApp;)");
        cookieStore = new BasicCookieStore();
        localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public String get(String url) {
        StringBuilder result = null;
        try {
            Log.d(tag, "executing request " + url);
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget, localContext);
            HttpEntity entity = response.getEntity();
            List<Cookie> cookies = cookieStore.getCookies();
            for (int i = 0; i < cookies.size(); i++) {
                Log.d(tag, "Local cookie: " + cookies.get(i));
            }
            if (entity != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                result = new StringBuilder();
                String str;
                while ((str = in.readLine()) != null) {
                    result.append(str);
                }
                in.close();
                Log.d(tag, "return response " + result.toString());
            }
        } catch (Exception e) {
            Log.e(tag, e.getMessage());
        }
        return (result == null) ? null : result.toString();
    }
}
