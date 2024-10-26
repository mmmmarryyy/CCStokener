package cn.chengdu.in.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import cn.chengdu.in.android.config.Config;
import cn.chengdu.in.error.IcdException;
import cn.chengdu.in.error.IcdParseException;
import cn.chengdu.in.parser.Parser;
import cn.chengdu.in.type.IcdType;
import cn.chengdu.in.util.JsonUtil;

/**
 * @author Declan.Z(declan.zhang@gmail.com)
 * @date 2011-2-23
 */
public abstract class AbstractIcdHttp implements IcdHttp {

    private static final String TAG = "AbstractIcdHttp";

    private static final boolean DEBUG = Config.DEBUG;

    public static final int TIMEOUT = 30 * 1000;

    private static final String CLIENT_VERSION_HEADER = "User-Agent";

    private DefaultHttpClient mHttpClient;

    private String mVersion;

    public AbstractIcdHttp(DefaultHttpClient httpClient, String version) {
        this.mHttpClient = httpClient;
        this.mVersion = version;
    }

    @Override
    public HttpGet createHttpGet(String url, NameValuePair... nameValuePairs) {
        if (DEBUG) Log.i(TAG, "creating HttpGet for: " + url);
        String query = URLEncodedUtils.format(stripNulls(nameValuePairs), HTTP.UTF_8);
        HttpGet httpGet = new HttpGet(url + "?" + query);
        httpGet.addHeader(CLIENT_VERSION_HEADER, "android-" + mVersion);
        if (DEBUG) Log.d(TAG, "Created: " + httpGet.getURI());
        return httpGet;
    }

    @Override
    public HttpPost createHttpPost(String url, NameValuePair... nameValuePairs) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader(CLIENT_VERSION_HEADER, "android-" + mVersion);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(stripNulls(nameValuePairs), HTTP.UTF_8));
        } catch (UnsupportedEncodingException e1) {
            throw new IllegalArgumentException("Unable to encode http parameters.");
        }
        return httpPost;
    }

    @Override
    public IcdType doHttpRequest(HttpRequestBase httpRequest, Parser<? extends IcdType> parser) throws IcdParseException, IcdException, IOException {
        HttpResponse resp = executeHttpRequest(httpRequest);
        int statusCode = resp.getStatusLine().getStatusCode();
        switch(statusCode) {
            case 200:
                String content = EntityUtils.toString(resp.getEntity());
                if (DEBUG) Log.d(TAG, "result: " + content);
                return JsonUtil.consume(parser, content);
            default:
                resp.getEntity().consumeContent();
                checkServerStatus();
                throw new IcdException("网络连接错误, 请检查你的网络状态 ");
        }
    }

    public String doHttpRequest(HttpRequestBase httpRequest) throws IcdParseException, IcdException, IOException {
        if (DEBUG) Log.d(TAG, "doHttpRequest : " + httpRequest.getURI());
        HttpResponse resp = executeHttpRequest(httpRequest);
        int statusCode = resp.getStatusLine().getStatusCode();
        switch(statusCode) {
            case 200:
                String content = EntityUtils.toString(resp.getEntity());
                if (DEBUG) Log.d(TAG, "result: " + content);
                return content;
            case 500:
                resp.getEntity().consumeContent();
                throw new IcdException("IN成都服务器正在升级...");
            default:
                resp.getEntity().consumeContent();
                throw new IcdException("网络连接出错了哦... ");
        }
    }

    /**
     * execute() an httpRequest catching exceptions and returning null instead.
     * 
     * @param httpRequest
     * @return
     * @throws IOException
     */
    private HttpResponse executeHttpRequest(HttpRequestBase httpRequest) throws IOException {
        if (DEBUG) Log.d(TAG, "executing HttpRequest for: " + httpRequest.getURI().toString());
        try {
            mHttpClient.getConnectionManager().closeExpiredConnections();
            return mHttpClient.execute(httpRequest);
        } catch (IOException e) {
            httpRequest.abort();
            throw e;
        }
    }

    private List<NameValuePair> stripNulls(NameValuePair... nameValuePairs) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (int i = 0; i < nameValuePairs.length; i++) {
            NameValuePair param = nameValuePairs[i];
            if (param != null && param.getValue() != null) {
                if (DEBUG) Log.e(TAG, "params : " + param);
                params.add(param);
            }
        }
        return params;
    }

    public static final DefaultHttpClient createHttpClient() {
        final SchemeRegistry supportedSchemes = new SchemeRegistry();
        final SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        final HttpParams httpParams = createHttpParams();
        HttpClientParams.setRedirecting(httpParams, false);
        final ClientConnectionManager ccm = new ThreadSafeClientConnManager(httpParams, supportedSchemes);
        return new DefaultHttpClient(ccm, httpParams);
    }

    private static final HttpParams createHttpParams() {
        final HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        return params;
    }

    private void checkServerStatus() throws IcdException {
        try {
            HttpResponse resp = executeHttpRequest(createHttpGet(Config.getServerStatusUrl()));
            int statusCode = resp.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String content = EntityUtils.toString(resp.getEntity());
                content = new String(content.getBytes("ISO8859-1"), "UTF-8");
                if (DEBUG) Log.d(TAG, content);
                JSONObject json = new JSONObject(content);
                if (json.getInt("serverStatus") != 1) {
                    String subject = json.getString("subject");
                    String message = json.getString("message");
                    throw new IcdException(subject + "\n" + message);
                }
            }
        } catch (ParseException e) {
        } catch (IOException e) {
        } catch (JSONException e) {
        }
    }
}
