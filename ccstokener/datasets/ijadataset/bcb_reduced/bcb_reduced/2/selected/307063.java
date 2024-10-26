package com.xlg.common.network;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import com.xlg.base.BaseApp;
import com.xlg.beans.UserBean;
import com.xlg.common.utils.CacheUtil;
import com.xlg.common.utils.CommonUtil;
import com.xlg.common.utils.LogUtil;

/***
 * 网络请求类
 */
public class RequestNetwork {

    private static final String NET_SERVER_URL = "http://122.11.46.195:30006/android.action";

    public static final String NETWORK_ERROR_MSG = "无网络连接";

    private static final RequestNetwork netService = new RequestNetwork();

    private RequestNetwork() {
    }

    public static RequestNetwork getInstance() {
        return netService;
    }

    /**
	 * 请求服务器数据
	 * 
	 * @param context
	 * @param requestJson
	 *            请求json
	 */
    public String getNetRequestData(Context context, String requestJson) {
        InputStream is = null;
        HttpClient client = null;
        try {
            client = getHttpClient();
            HttpPost post = new HttpPost(NET_SERVER_URL);
            byte[] jsonByte = requestJson.getBytes("UTF-8");
            is = new ByteArrayInputStream(jsonByte);
            InputStreamEntity entity = new InputStreamEntity(is, jsonByte.length);
            post.setEntity(entity);
            post.addHeader("authorization", getUserData());
            post.addHeader("token", RequestJson.getPhoneToken());
            post.addHeader("version", "android2.0");
            post.addHeader("width", String.valueOf(BaseApp.getDisplayWidth()));
            post.addHeader("viewplace", "android");
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (final Throwable e) {
            LogUtil.warn(e.toString());
        } finally {
            CommonUtil.closeInputStream(is);
            if (client != null) client.getConnectionManager().shutdown();
        }
        return null;
    }

    /**
	 * 从网络上读取数据
	 */
    public void loadData(String url, ResponseCallback responseCallBack) {
        HttpClient client = null;
        try {
            client = getHttpClient();
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            responseCallBack.callback(response);
        } catch (Throwable e) {
            LogUtil.error(e);
        } finally {
            if (client != null) client.getConnectionManager().shutdown();
        }
    }

    /**
	 * 整理http请求客户端
	 */
    private HttpClient getHttpClient() throws Throwable {
        if (!checkNetIsAvailable()) throw new Throwable(NETWORK_ERROR_MSG);
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
        HttpConnectionParams.setSoTimeout(httpParams, 15000);
        if (checkGPRS_WAP()) {
            httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(Proxy.getDefaultHost(), Proxy.getDefaultPort(), "http"));
        }
        return new DefaultHttpClient(httpParams);
    }

    /**
	 * 检查手机网络是否可用 true：可用 false:不可用
	 */
    public boolean checkNetIsAvailable() {
        ConnectivityManager cm = (ConnectivityManager) BaseApp.getBaseApp().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isAvailable() || !info.isConnected() || info.getState() != NetworkInfo.State.CONNECTED) return false;
        return true;
    }

    /**
	 * 检查手机网络连接类型是否为GPRS WAP
	 */
    private boolean checkGPRS_WAP() {
        if (!checkNetIsAvailable()) return false;
        NetworkInfo info = ((ConnectivityManager) BaseApp.getBaseApp().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (ConnectivityManager.TYPE_WIFI == info.getType()) return false;
        if (ConnectivityManager.TYPE_MOBILE != info.getType()) return false;
        return StringUtils.isNotBlank(Proxy.getDefaultHost());
    }

    /**
	 * 从网络上读取图片
	 */
    public Bitmap loadImage(String imageUrl) {
        HttpClient client = null;
        HttpGet get = new HttpGet(imageUrl);
        InputStream is = null;
        try {
            client = getHttpClient();
            HttpResponse response = client.execute(get);
            is = response.getEntity().getContent();
            return BitmapFactory.decodeStream(is);
        } catch (Throwable e) {
            LogUtil.error(e);
            return null;
        } finally {
            CommonUtil.closeInputStream(is);
            if (client != null) client.getConnectionManager().shutdown();
        }
    }

    /**
	 * 获取用户信息
	 */
    private String getUserData() throws Throwable {
        UserBean user = CacheUtil.readLoginData();
        LogUtil.info("user = " + user);
        if (user == null) return StringUtils.EMPTY;
        return user.userName + "xianleju" + user.passWord;
    }

    /**
	 * 网络强求响应回调
	 */
    public interface ResponseCallback {

        void callback(HttpResponse response) throws Throwable;
    }
}
