package com.taobao.api;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import com.taobao.api.model.TaobaoResponse;

/**
 * 开发中...
 * 
 * 使用<a
 * href="http://hc.apache.org/httpcomponents-client/">commons-httpclient-4.x</a>
 * 实现的UrlFetch接口。一般情况下默认配置即可。
 * 
 * 因为httpclient4重写了httpclient3,所以使用httpclient3的朋友请使用 HttpClient3UrlFetch
 * 
 * @author <a href="mailto:zixue@taobao.com">zixue</a>
 */
public class HttpClient4UrlFetch extends AbstractUrlFetch {

    public static final String SIP_STATUS_OK = "9999";

    private HttpClient httpClient;

    private HttpParams httpParams;

    private boolean keepAlive = false;

    public HttpClient4UrlFetch() {
        initHttpClient();
    }

    private void initHttpClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        httpParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        this.setMaxTotalConnections(DEFAULT_MAX_TOTAL_CONNECTIONS);
        this.setReadTimeout(DEFAULT_READ_TIMEOUT);
        ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
        httpClient = new DefaultHttpClient(cm, httpParams);
    }

    public TaobaoResponse fetch(URL url, Map<String, CharSequence> payload) throws TaobaoApiException {
        return this.fetchWithFile(url, payload, null);
    }

    public TaobaoResponse fetchWithFile(URL url, Map<String, CharSequence> payload, File file) throws TaobaoApiException {
        TaobaoResponse rsp = null;
        HttpPost postMethod = null;
        try {
            postMethod = new HttpPost(url.toString());
            rsp = _fetch(postMethod, payload, file);
        } catch (Exception e) {
            throw new TaobaoApiException(e);
        } finally {
        }
        return rsp;
    }

    protected TaobaoResponse _fetch(HttpPost post, Map<String, CharSequence> payload, File file) throws IOException {
        Set<Entry<String, CharSequence>> entries = payload.entrySet();
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (Entry<String, CharSequence> entry : entries) {
            NameValuePair nvp = new BasicNameValuePair(entry.getKey(), (String) entry.getValue());
            nvps.add(nvp);
        }
        if (file != null) {
        } else {
            post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        }
        if (this.keepAlive) {
            post.setHeader("Connection", "Keep-Alive");
        }
        Header responseHeader = null;
        HttpResponse response = httpClient.execute(post);
        responseHeader = post.getLastHeader("sip_status");
        String body = EntityUtils.toString(response.getEntity());
        TaobaoResponse urlRsp = new TaobaoResponse();
        if (responseHeader != null) {
            String status = responseHeader.getValue();
            if (!SIP_STATUS_OK.equals(status)) {
                urlRsp.setErrorCode(status);
                urlRsp.setMsg(post.getLastHeader("sip_error_message").getValue());
                if (status.equals("1004")) {
                    urlRsp.setRedirectUrl(post.getLastHeader("sip_isp_loginurl").getValue());
                }
            }
        }
        urlRsp.setBody(body);
        return urlRsp;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setStaleCheckingEnabled(boolean value) {
        throw new UnsupportedOperationException();
    }

    public void setMaxConnectionsPerHost(int maxHostConnections) {
        throw new UnsupportedOperationException();
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        ConnManagerParams.setMaxTotalConnections(this.httpParams, maxTotalConnections);
    }

    public void setConnectTimeout(int milliSecond) {
        throw new UnsupportedOperationException();
    }

    public void setReadTimeout(int milliSecond) {
        ConnManagerParams.setTimeout(this.httpParams, DEFAULT_READ_TIMEOUT);
    }
}
