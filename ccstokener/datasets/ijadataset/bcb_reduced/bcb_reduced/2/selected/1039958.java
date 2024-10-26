package com.pinkdroid.networking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import android.util.Log;

public class NetworkEngine {

    private static final int MAX_CONNECTIONS = 20;

    private static final String DEFAULT_KEY_STRING_PREFIX = "jm_";

    private static final String DEFAULT_KEY_STRING_SUFFIX = "_encrypt";

    private static final int SOCKET_TIMEOUT = 10000;

    private static final int THREAD_TIMEOUT = 15000;

    private static final String BOUNDARY = "-----------------------------2009487413965697991066076391";

    private static final String ENDLINE = "\r\n";

    public static final String GATEWAY_SERVICE_URL = "JmServiceUrl";

    public static final String SYNC = "/sync";

    public static final String RECEIVE = "/receive";

    public static final String LOAD_CLIENT_SETTINGS = "/loadClientSettings";

    public static final String CHECK_VERSION = "/checkVersion";

    public static final String REGISTER = "/register";

    public static final String UPLOAD_FILE = "/upload/file";

    public static final String MEDIA_STORE = "/image";

    public static final String DOWNLOAD = "/download";

    public static final String DEFAULT_PROTOCOL = "1.0.0";

    private static final String ANDROID_EXT = ".apk";

    private static int HTTP_PORT = 80;

    private static int HTTPS_PORT = 443;

    private static NetworkEngine instance = null;

    protected ThreadSafeClientConnManager connectionManager;

    protected NetworkEngine() {
        HttpParams connParams = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(connParams, MAX_CONNECTIONS);
        ConnManagerParams.setTimeout(connParams, THREAD_TIMEOUT);
        HttpProtocolParams.setUseExpectContinue(connParams, false);
        HttpConnectionParams.setStaleCheckingEnabled(connParams, false);
        HttpConnectionParams.setTcpNoDelay(connParams, true);
        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), HTTP_PORT));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), HTTPS_PORT));
        connectionManager = new ThreadSafeClientConnManager(connParams, schReg);
    }

    protected HttpClient getHttpClient(int timeout) {
        HttpParams connParams = new BasicHttpParams();
        if (timeout < 0) timeout = SOCKET_TIMEOUT;
        ConnManagerParams.setMaxTotalConnections(connParams, MAX_CONNECTIONS);
        ConnPerRoute connPerRoute = new ConnPerRoute() {

            @Override
            public int getMaxForRoute(HttpRoute route) {
                return 5;
            }
        };
        ConnManagerParams.setMaxConnectionsPerRoute(connParams, connPerRoute);
        ConnManagerParams.setTimeout(connParams, THREAD_TIMEOUT);
        HttpProtocolParams.setUseExpectContinue(connParams, false);
        HttpConnectionParams.setStaleCheckingEnabled(connParams, false);
        HttpConnectionParams.setTcpNoDelay(connParams, true);
        HttpProtocolParams.setVersion(connParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(connParams, HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(connParams, false);
        HttpConnectionParams.setSoTimeout(connParams, timeout);
        return new DefaultHttpClient(connectionManager, connParams);
    }

    public static NetworkEngine getInstance() {
        if (instance == null) {
            instance = new NetworkEngine();
        }
        return instance;
    }

    public static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    public HttpResponse postRaw(String url, Map<String, String> headers, byte[] data, int timeout) throws ClientProtocolException, IOException {
        HttpPost httppost = new HttpPost(url);
        for (String key : headers.keySet()) {
            httppost.addHeader(key, headers.get(key));
            System.out.println("HEADER TO BE POSTED: " + key + "   " + headers.get(key));
        }
        HttpEntity sendentity = new ByteArrayEntity(data);
        httppost.setEntity(sendentity);
        HttpClient httpclient = getHttpClient(timeout);
        return httpclient.execute(httppost);
    }

    public HttpResponse postUrlencoded(String url, Map<String, String> headers, Map<String, String> params, int timeout) throws ClientProtocolException, IOException {
        ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
        if (params != null) for (String key : params.keySet()) parameters.add(new BasicNameValuePair(key, params.get(key)));
        if (headers == null) headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        return postRaw(url, headers, URLEncodedUtils.format(parameters, HTTP.DEFAULT_CONTENT_CHARSET).getBytes(), timeout);
    }

    public byte[] postUnencrypted(String url, Map<String, String> headers, Map<String, String> params, int timeout) throws Exception {
        HttpResponse response;
        response = postUrlencoded(url, headers, params, timeout);
        if (response == null) throw new Exception("Null response");
        if (response.getStatusLine().getStatusCode() != 200) throw new Exception("Server return code " + response.getStatusLine().getStatusCode());
        headers.clear();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outstream);
        return outstream.toByteArray();
    }

    public byte[] postMultipartForm(String path, Map<String, String> headers, Map<String, String> params, byte[] data, int timeout) throws Exception {
        String url = path;
        headers.put("Content-Type", "multipart/form-data; boundary=" + BOUNDARY.substring(2));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write((BOUNDARY + ENDLINE).getBytes());
        for (String k : params.keySet()) {
            String msg = "Content-disposition: form-data; name=\"" + k + "\"" + ENDLINE + ENDLINE;
            msg += params.get(k) + ENDLINE;
            msg += BOUNDARY + ENDLINE;
            output.write(msg.getBytes());
        }
        String filename = "anyname.jpeg";
        String msg = "Content-disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + ENDLINE;
        msg += "Content-Type: image/jpeg" + ENDLINE + ENDLINE;
        output.write(msg.getBytes());
        output.write(data);
        output.write((ENDLINE + BOUNDARY + "--" + ENDLINE).getBytes());
        byte[] tmp = output.toByteArray();
        HttpResponse response;
        response = postRaw(url, headers, tmp, timeout);
        if (response == null) throw new Exception("Null response");
        headers.clear();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        output.reset();
        response.getEntity().writeTo(output);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception("Server return code " + response.getStatusLine().getStatusCode());
        }
        return output.toByteArray();
    }

    public byte[] getRaw(String url, Map<String, String> headers, Map<String, String> params, int timeout) throws Exception {
        if (params != null && !url.contains("?")) {
            ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
            for (String key : params.keySet()) parameters.add(new BasicNameValuePair(key, params.get(key)));
            url += "?" + URLEncodedUtils.format(parameters, HTTP.DEFAULT_CONTENT_CHARSET);
        } else if (params != null && url.contains("?")) {
            ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
            for (String key : params.keySet()) parameters.add(new BasicNameValuePair(key, params.get(key)));
            url += "&" + URLEncodedUtils.format(parameters, HTTP.DEFAULT_CONTENT_CHARSET);
        }
        HttpGet httpget = new HttpGet(url);
        if (headers != null) {
            for (String key : headers.keySet()) httpget.addHeader(key, headers.get(key));
        }
        HttpClient httpclient = getHttpClient(timeout);
        HttpResponse response = httpclient.execute(httpget);
        if (response == null) throw new Exception("Null response");
        if (response.getStatusLine().getStatusCode() != 200) throw new Exception("Server return code " + response.getStatusLine().getStatusCode());
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outstream);
        return outstream.toByteArray();
    }

    public byte[] getRawPair(String url, Map<String, String> headers, ArrayList<NameValuePair> parameters, int timeout) throws Exception {
        if (parameters != null && !url.contains("?")) {
            url += "?" + URLEncodedUtils.format(parameters, HTTP.DEFAULT_CONTENT_CHARSET);
        } else if (parameters != null && url.contains("?")) {
            url += "&" + URLEncodedUtils.format(parameters, HTTP.DEFAULT_CONTENT_CHARSET);
        }
        HttpGet httpget = new HttpGet(url);
        if (headers != null) {
            for (String key : headers.keySet()) httpget.addHeader(key, headers.get(key));
        }
        System.out.println("URL " + url);
        HttpClient httpclient = getHttpClient(timeout);
        HttpResponse response = httpclient.execute(httpget);
        System.out.println("URL Executed");
        if (response == null) throw new Exception("Null response");
        if (response.getStatusLine().getStatusCode() != 200) throw new Exception("Server return code " + response.getStatusLine().getStatusCode() + " for url " + url);
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outstream);
        return outstream.toByteArray();
    }

    public byte[] downloadImage(String url, Map<String, String> headers, Map<String, String> params, int timeout) throws Exception {
        return getRaw(url, headers, params, timeout);
    }
}
