package com.whale.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import android.util.Log;

public class NetworkUtil {

    private static final String TAG = "NetworkTool";

    private static final int MAXTRYCNT = 3;

    private static DefaultHttpClient client = null;

    private static HttpContext localContext = null;

    static {
        init();
    }

    private static void init() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "utf-8");
        params.setBooleanParameter("http.protocol.expect-continue", false);
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        client = new DefaultHttpClient(cm, params);
        HttpParams httpParams = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
        HttpConnectionParams.setSoTimeout(httpParams, 3000);
        client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        CookieStore cookieStore = new BasicCookieStore();
        localContext = new BasicHttpContext();
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    private static String readByteStream(InputStream input, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, encoding), 8192);
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        reader.close();
        return sb.toString();
    }

    private static void setPostParam(Map<String, String> paramMap, List<BasicNameValuePair> paramList) {
        java.util.Iterator<Map.Entry<String, String>> it = paramMap.entrySet().iterator();
        Map.Entry<String, String> entry = null;
        while (it.hasNext()) {
            entry = it.next();
            paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
    }

    private static List<BasicNameValuePair> convertMap2List(Map<String, String> paramMap) {
        List<BasicNameValuePair> paramList = new ArrayList<BasicNameValuePair>();
        java.util.Iterator<Map.Entry<String, String>> it = paramMap.entrySet().iterator();
        Map.Entry<String, String> entry = null;
        while (it.hasNext()) {
            entry = it.next();
            paramList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return paramList;
    }

    private static HttpRequestBase getExecuteMethod(String method, String url, Map<String, String> paramMap, File file) throws UnsupportedEncodingException {
        HttpRequestBase base = null;
        if (method.equals("get")) {
            base = new HttpGet(url);
        } else if (method.equals("post")) {
            base = new HttpPost(url);
            HttpEntity entity = null;
            List<BasicNameValuePair> paramList = new ArrayList<BasicNameValuePair>();
            if (null != file) {
                paramList = convertMap2List(paramMap);
                entity = createMultipartEntity("imageFile", file, paramList);
            } else if (null != paramMap) {
                setPostParam(paramMap, paramList);
                entity = new UrlEncodedFormEntity(paramList, HTTP.UTF_8);
            }
            ((HttpEntityEnclosingRequestBase) base).setEntity(entity);
        } else if (method.equals("put")) {
            base = new HttpPut(url);
            List<BasicNameValuePair> paramList = new ArrayList<BasicNameValuePair>();
            setPostParam(paramMap, paramList);
            ((HttpEntityEnclosingRequestBase) base).setEntity(new UrlEncodedFormEntity(paramList, HTTP.UTF_8));
        } else if (method.equals("delete")) {
        } else if (method.equals("trace")) {
        } else {
            base = new HttpPost(url);
        }
        return base;
    }

    public static String send(String method, String url, Map<String, String> paramMap, File file, String encoding) throws HttpServerStatusException {
        Log.i(TAG, "url:" + url);
        boolean bVisitOK = false;
        int tryCnt = 0;
        String result = "";
        while (!bVisitOK && (tryCnt++ < MAXTRYCNT)) {
            try {
                HttpRequestBase base = getExecuteMethod(method, url, paramMap, file);
                HttpResponse response = client.execute(base, localContext);
                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = readByteStream(entity.getContent(), encoding);
                    entity.consumeContent();
                }
                if (status == 200) {
                    return result;
                } else {
                    throw new HttpServerStatusException(status, result);
                }
            } catch (HttpServerStatusException e) {
                throw e;
            } catch (IllegalStateException e) {
                bVisitOK = false;
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                bVisitOK = false;
                Log.e(TAG, e.toString());
            }
        }
        return result;
    }

    public static String get(String url, Map<String, String> paramMap) throws HttpServerStatusException {
        return send("get", url, paramMap, null, "utf-8");
    }

    public static String post(String url, Map<String, String> paramMap) throws HttpServerStatusException {
        return send("post", url, paramMap, null, "utf-8");
    }

    public static HttpEntity sendHE(String method, String url, Map<String, String> paramMap, String encoding) throws HttpServerStatusException {
        Log.i(TAG, "url:" + url);
        boolean bVisitOK = false;
        int tryCnt = 0;
        while (!bVisitOK && (tryCnt++ < MAXTRYCNT)) {
            try {
                HttpRequestBase base = getExecuteMethod(method, url, paramMap, null);
                HttpResponse response = client.execute(base, localContext);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        return entity;
                    }
                } else {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        entity.consumeContent();
                    }
                    throw new HttpServerStatusException(status, "");
                }
            } catch (HttpServerStatusException e) {
                throw e;
            } catch (IllegalStateException e) {
                bVisitOK = false;
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                bVisitOK = false;
                Log.e(TAG, e.toString());
            }
        }
        return null;
    }

    public static String httpPost(String url, Map<String, String> paramMap, String encoding) {
        try {
            return send("post", url, paramMap, null, encoding);
        } catch (HttpServerStatusException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String httpPost(String url, Map<String, String> paramMap, File file, String encoding) {
        try {
            return send("post", url, paramMap, file, encoding);
        } catch (HttpServerStatusException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String httpPut(String url, Map<String, String> paramMap, String encoding) {
        try {
            return send("put", url, paramMap, null, encoding);
        } catch (HttpServerStatusException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String httpGet(String url, Map<String, String> paramMap, String encoding) {
        try {
            return send("get", url, paramMap, null, encoding);
        } catch (HttpServerStatusException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String httpDelete(String url, Map<String, String> paramMap, String encoding) {
        return "";
    }

    public static String httpTrace(String url, Map<String, String> paramMap, String encoding) {
        return "";
    }

    /**
     * 普通http post访问url
     * 
     * @param url
     * @return 一般则返回200后的内容，可能trycnt后仍存在连接问题等返回不全或空字符串(由网络情况定)，不可能返回null
     */
    public static String httpPostURL(String url, String encoding) {
        try {
            return send("post", url, null, null, encoding);
        } catch (HttpServerStatusException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 普通http get访问url
     * 
     * @param url
     * @return 一般则返回200后的内容，可能trycnt后仍存在连接问题等返回不全或空字符串(由网络情况定)，不可能返回null
     */
    public static String httpGetURL(String url, String encoding) {
        try {
            return send("get", url, null, null, encoding);
        } catch (HttpServerStatusException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static HttpEntity httpGetURLHE(String url) {
        HttpEntity he = null;
        try {
            he = sendHE("get", url, null, "noneed");
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return he;
    }

    public static List<Cookie> getBBLCookies() {
        CookieStore cs = (CookieStore) localContext.getAttribute(ClientContext.COOKIE_STORE);
        return cs.getCookies();
    }

    /**
     * 创建可带一个File的MultipartEntity
     * 
     * @param filename
     * @param file
     * @param postParams
     * @return 带文件和其他参数的Entity
     * @throws UnsupportedEncodingException
     */
    private static MultipartEntity createMultipartEntity(String filename, File file, List<BasicNameValuePair> postParams) throws UnsupportedEncodingException {
        MultipartEntity entity = new MultipartEntity();
        entity.addPart(filename, new FileBody(file));
        for (BasicNameValuePair param : postParams) {
            entity.addPart(param.getName(), new StringBody(param.getValue()));
        }
        return entity;
    }
}
