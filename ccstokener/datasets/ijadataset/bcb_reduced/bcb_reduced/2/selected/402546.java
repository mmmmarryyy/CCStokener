package com.android.taobao.sessionkey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;
import com.android.taobao.cache.CachePool;
import com.android.taobao.common.Common;
import com.android.taobao.common.Util;

/**
 * 获得用户SessionKey
 * 
 * @author Bobby.Jin
 * 
 */
public class GetSessionKey {

    private static String doGetForSessionKey(String authCode) throws Exception {
        String sessionKey = "";
        HttpClient hc = new DefaultHttpClient();
        HttpGet hg = new HttpGet(Common.TEST_SESSION_HOST + Common.TEST_SESSION_PARAM + authCode);
        HttpResponse hr = hc.execute(hg);
        BufferedReader br = new BufferedReader(new InputStreamReader(hr.getEntity().getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        String result = sb.toString();
        Log.i("sessionKeyMessages", result);
        Map<String, String> map = Util.handleURLParameters(result);
        sessionKey = map.get(Common.TOP_SESSION);
        String topParameters = map.get(Common.TOP_PARAMETERS);
        String decTopParameters = Util.decodeBase64(topParameters);
        Log.i("base64", decTopParameters);
        map = Util.handleURLParameters(decTopParameters);
        Log.i("nick", map.get(Common.VISITOR_NICK));
        CachePool.put(Common.VISITOR_NICK, map.get(Common.VISITOR_NICK));
        return sessionKey;
    }

    /**
	 * To Get SessionKey
	 * 
	 * @param authCode
	 * @return
	 */
    public static String getSessionKey(String authCode) {
        String sessionKey = "";
        try {
            sessionKey = doGetForSessionKey(authCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sessionKey;
    }
}
