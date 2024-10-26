package com.michael.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

/**
 * HTTP接口调用
 * @author zhanghongze
 * 
 */
public class HttpUtil {

    public static final String ERROR_START = "ErrorResponse:";

    public static String TICKET = null;

    private static final String DOMAIN = ".youxigu.com";

    private static final String TICKET_NAME = "ticket";

    private static HttpParams httpParams;

    static {
        httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 20 * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, 20 * 1000);
        HttpConnectionParams.setSocketBufferSize(httpParams, 8192);
        HttpClientParams.setCookiePolicy(httpParams, CookiePolicy.BROWSER_COMPATIBILITY);
    }

    /**
	 * post请求
	 * 
	 * @param url
	 * @param paramsMap
	 * @return
	 */
    public static String post(String url, Map<String, String> paramsMap) {
        String result = null;
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Iterator<String> iterator = paramsMap.keySet().iterator(); iterator.hasNext(); ) {
            String name = iterator.next();
            String value = paramsMap.get(name);
            params.add(new BasicNameValuePair(name, value));
        }
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        if (HttpUtil.TICKET != null) {
            CookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(TICKET_NAME, HttpUtil.TICKET);
            cookie.setDomain(DOMAIN);
            cookie.setPath("/");
            cookieStore.addCookie(cookie);
            cookie.getDomain();
            client.setCookieStore(cookieStore);
        }
        try {
            post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = ERROR_START + response.getStatusLine().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        Log.d("HTTP_POST", url);
        Log.d("HTTP_POST_result", result);
        return result;
    }

    /**
	 * get请求
	 * 
	 * @param url
	 * @return
	 */
    public static String get(String url) {
        String result = null;
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        if (HttpUtil.TICKET != null) {
            CookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie(TICKET_NAME, HttpUtil.TICKET);
            cookie.setDomain(DOMAIN);
            cookie.setPath("/");
            cookieStore.addCookie(cookie);
            cookie.getDomain();
            client.setCookieStore(cookieStore);
        }
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = ERROR_START + response.getStatusLine().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        Log.d("HTTP_GET", url);
        Log.d("HTTP_GET_result", result);
        return result;
    }

    /**
	 * 获取网络图片
	 * 
	 * @param url
	 * @param image
	 * @return
	 */
    public static boolean getWebImage(String url, ImageView image) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.connect();
            is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bitmap != null) {
            image.setImageBitmap(bitmap);
            return true;
        }
        return false;
    }

    /**
	 * 设置网络图片
	 * 
	 * @param url
	 * @param image
	 * @return
	 */
    public static boolean getWebImage2(String url, ImageView image) {
        boolean result = false;
        HttpGet get = new HttpGet(url);
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                if (response.getEntity().isStreaming()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(response.getEntity().getContent());
                    image.setImageBitmap(bitmap);
                    result = true;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        return result;
    }
}
