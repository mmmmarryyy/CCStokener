package com.antrou.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.antrou.constants.AZConstants;

public class ConnUtil {

    private static final String TAG = "ConnUtil";

    /**
	 * 远程调用后台服务（登录）
	 * @param jsonObject JSON参数
	 * @param OPCode 操作代码
	 * @return JSON
	 */
    public static String connRemote(JSONObject jsonObject, String OPCode) {
        String retSrc = "";
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(AZConstants.validateURL);
            HttpParams httpParams = new BasicHttpParams();
            List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
            nameValuePair.add(new BasicNameValuePair(AZConstants.ACTION_TYPE, OPCode));
            nameValuePair.add(new BasicNameValuePair(AZConstants.PARAM, jsonObject.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
            httpPost.setParams(httpParams);
            HttpResponse response = httpClient.execute(httpPost);
            retSrc = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return retSrc;
    }

    /**
	 * 远程调用后台服务
	 * @param jsonObject  JSON参数
	 * @param OPCode 操作类型
	 * @param nameValuePair 其他参数
	 * @return
	 */
    public static String connRemote(JSONObject jsonObject, String OPCode, List<NameValuePair> nameValuePair) {
        String retSrc = "";
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(AZConstants.validateURL);
            HttpParams httpParams = new BasicHttpParams();
            nameValuePair.add(new BasicNameValuePair(AZConstants.ACTION_TYPE, OPCode));
            nameValuePair.add(new BasicNameValuePair(AZConstants.PARAM, jsonObject.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
            httpPost.setParams(httpParams);
            HttpResponse response = httpClient.execute(httpPost);
            retSrc = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return retSrc;
    }

    /**
	 * 判断是否有可用网络
	 * @param activity
	 * @return
	 */
    public static boolean hasInternet(Activity activity) {
        ConnectivityManager manager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return false;
        }
        if (info.isRoaming()) {
            return true;
        }
        return true;
    }
}
