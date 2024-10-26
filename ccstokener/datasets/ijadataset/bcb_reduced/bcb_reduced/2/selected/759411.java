package org.regilo.core.utils;

import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceStore;
import org.regilo.core.RegiloPlugin;
import org.regilo.core.preferences.ProxyPreferenceConstants;

public class HeadlessBrowser {

    private DefaultHttpClient client;

    private static final Logger log = Logger.getLogger(HeadlessBrowser.class);

    public HeadlessBrowser() {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        ConnManagerParams.setTimeout(params, 30000);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, RegiloPlugin.USER_AGENT);
        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        client = new DefaultHttpClient(cm, params);
        IPreferenceStore preferenceStore = RegiloPlugin.getDefault().getPreferenceStore();
        if (preferenceStore.getBoolean(ProxyPreferenceConstants.PROXY_TYPE)) {
            String host = preferenceStore.getString(ProxyPreferenceConstants.PROXY_HOST);
            int port = preferenceStore.getInt(ProxyPreferenceConstants.PROXY_PORT);
            HttpHost proxy = new HttpHost(host, port, "http");
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
    }

    public HttpResponse navigateTo(URI url) {
        log.debug("navigateTo: " + url.toString());
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            log.debug(response.getStatusLine());
            return response;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getDataFrom(URI url) throws Exception {
        HttpGet get = new HttpGet(url);
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(get, responseHandler);
            return responseBody;
        } catch (ClientProtocolException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        }
    }
}
