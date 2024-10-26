package com.busfm.net;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import com.busfm.util.Constants;
import com.busfm.util.LogUtil;
import com.busfm.util.PrefUtil;
import com.busfm.util.Utilities;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * <p>
 * Title:NetWorkHelper
 * </p>
 * <p>
 * Description: Mainly used to control NetWork Acess and so on...
 * </p>
 * <p>
 * Copyright (c) 2011 www.bus.fm Inc. All rights reserved.
 * </p>
 * <p>
 * Company: bus.fm
 * </p>
 * 
 * 
 * @author jingguo0@gmail.com
 * 
 */
public class NetWorkHelper {

    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private static final String _3GWAP = "3gwap";

    private static final String _2GWAP = "uniwap";

    private NetWorkHelper() {
    }

    public static DefaultHttpClient createDefaultHttpClient() {
        return createHttpClient(30000, Integer.MAX_VALUE);
    }

    public static DefaultHttpClient createHttpClient(int connectTimeout, int soTimeout) {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("http", sf, 80));
        HttpParams params = new BasicHttpParams();
        if (isMobileActive()) {
            String proxyHost = android.net.Proxy.getDefaultHost();
            HttpHost proxy = null;
            int proxyPort = android.net.Proxy.getDefaultPort();
            if (proxyHost != null) {
                proxy = new HttpHost(proxyHost, proxyPort);
                params.setParameter(ConnRouteParams.DEFAULT_PROXY, proxy);
            }
        }
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpConnectionParams.setConnectionTimeout(params, connectTimeout);
        HttpConnectionParams.setSoTimeout(params, soTimeout);
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, schemeRegistry);
        DefaultHttpClient client = new DefaultHttpClient(ccm, params);
        return client;
    }

    public static BufferedInputStream performRequest(String url) {
        if (Utilities.isEmpty(url) || !NetWorkHelper.isNetworkAvailable()) {
            return null;
        }
        return getHTTPConnection(url);
    }

    public static boolean isMobileActive() {
        ConnectivityManager connectivityManager = (ConnectivityManager) PrefUtil.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if ((null != networkInfo) && (ConnectivityManager.TYPE_MOBILE == networkInfo.getType())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否是2G/3G网络且非联通3GWAP 及 2gwap 
     * @return
     */
    public static boolean isMobileActiveNotUnicom() {
        ConnectivityManager connectivityManager = (ConnectivityManager) PrefUtil.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if ((null != networkInfo)) {
            if (ConnectivityManager.TYPE_MOBILE == networkInfo.getType()) {
                if (_2GWAP.equals(networkInfo.getExtraInfo()) || _3GWAP.equals(networkInfo.getExtraInfo())) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isWifiWorking() {
        ConnectivityManager connectivityManager = (ConnectivityManager) PrefUtil.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if ((null != networkInfo) && (ConnectivityManager.TYPE_WIFI == networkInfo.getType())) {
            return true;
        }
        return false;
    }

    public static boolean isNetworkAvailable() {
        boolean result = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) PrefUtil.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (null != networkInfo) {
            result = networkInfo.isAvailable();
        }
        return result;
    }

    private static BufferedInputStream getHTTPConnection(String sUrl) {
        URL url = null;
        BufferedInputStream bis = null;
        try {
            url = new URL(sUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.connect();
            String encoding = connection.getContentEncoding();
            if (!Utilities.isEmpty(encoding) && "gzip".equalsIgnoreCase(encoding)) {
                bis = new BufferedInputStream(new GZIPInputStream(connection.getInputStream()), IO_BUFFER_SIZE);
            } else if (!Utilities.isEmpty(encoding) && "deflate".equalsIgnoreCase(encoding)) {
                bis = new BufferedInputStream(new InflaterInputStream(connection.getInputStream(), new Inflater(true)), IO_BUFFER_SIZE);
            } else {
                bis = new BufferedInputStream(connection.getInputStream(), IO_BUFFER_SIZE);
            }
        } catch (Exception e) {
            LogUtil.e(Constants.TAG, e.getMessage());
        }
        return bis;
    }
}
