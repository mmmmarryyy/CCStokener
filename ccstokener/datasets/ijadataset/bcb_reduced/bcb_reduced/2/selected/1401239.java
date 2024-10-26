package net.sf.dvstar.transmission.protocol;

import net.sf.dvstar.transmission.utils.LocalSettiingsFactory;
import net.sf.dvstar.transmission.utils.ConfigStorage;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dstarzhynskyi
 */
public class TransmissionWebClient {

    private boolean authenticate;

    private static String x_transmission_session_id = null;

    private DefaultHttpClient httpclient = null;

    private LocalSettiingsFactory localSettiingsFactory;

    private ConfigStorage configStorage;

    private boolean connected = false;

    private HttpHost targetHttpHost = null;

    public HttpHost getProxyHttpHost() {
        return proxyHttpHost;
    }

    public HttpHost getTargetHttpHost() {
        return targetHttpHost;
    }

    private HttpHost proxyHttpHost = null;

    private String responseData;

    private boolean enableTraceOut = false;

    private Logger loggerProvider = null;

    public TransmissionWebClient(boolean authenticate, Logger loggerProvider) {
        this.authenticate = authenticate;
        this.loggerProvider = loggerProvider;
        configStorage = new ConfigStorage();
        configStorage.loadConfig();
        localSettiingsFactory = new LocalSettiingsFactory();
        localSettiingsFactory.setConfigProperties(configStorage);
        targetHttpHost = new HttpHost(localSettiingsFactory.getHost(), localSettiingsFactory.getIPort(), "http");
        SchemeRegistry supportedSchemes = new SchemeRegistry();
        supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);
        int connectionTimeoutMillis = 5 * 1000;
        HttpConnectionParams.setConnectionTimeout(params, connectionTimeoutMillis);
        HttpConnectionParams.setSoTimeout(params, connectionTimeoutMillis);
        httpclient = new DefaultHttpClient(ccm, params);
        if (localSettiingsFactory.isUseProxy()) {
            proxyHttpHost = new HttpHost(localSettiingsFactory.getProxyHost(), Integer.parseInt(localSettiingsFactory.getProxyPort()), "http");
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHttpHost);
        }
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(localSettiingsFactory.getHost(), Integer.parseInt(localSettiingsFactory.getPort())), new UsernamePasswordCredentials(localSettiingsFactory.getUser(), localSettiingsFactory.getPass()));
    }

    long starWebRequest = 0;

    long stopWebRequest = 0;

    long diffWebRequest = 0;

    String prefixTrace = "";

    public JSONObject processWebRequest(JSONObject jobj, String prefix) throws UnknownHostException, IOException, HttpException, InterruptedIOException {
        JSONObject ret = null;
        prefixTrace = prefix;
        String url = localSettiingsFactory.refreshUrlCache();
        HttpPost httprequest = new HttpPost(url);
        if (x_transmission_session_id != null && authenticate) {
            httprequest.setHeader("X-Transmission-Session-Id", x_transmission_session_id);
            httprequest.setEntity(new StringEntity(jobj.toString()));
            tracePrint("Request=" + jobj.toString());
            starWebRequest = System.currentTimeMillis();
            HttpResponse response = httpclient.execute(targetHttpHost, httprequest);
            stopWebRequest = System.currentTimeMillis();
            diffWebRequest = stopWebRequest - starWebRequest;
            tracePrint("[getStatusLine()]----------------------------------------");
            tracePrint(response.getStatusLine().toString());
            tracePrint("[getStatusLine()]----------------------------------------");
            HttpEntity entity = response.getEntity();
            tracePrint("[processWebRequest][consumeContent()]----------------------------------------");
            responseData = null;
            if (entity != null) {
                tracePrint(true, "[processWebRequest] Response content length: [" + entity.getContentLength() + "] time: [" + diffWebRequest + "]");
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT) {
                    String old_x_transmission_session_id = x_transmission_session_id;
                    x_transmission_session_id = getTansmissionSessionId(response);
                    tracePrint(true, "[processWebRequest] Change x_transmission_session_id [" + old_x_transmission_session_id + "]->[" + x_transmission_session_id + "]]");
                }
                responseData = EntityUtils.toString(entity);
                tracePrint(responseData);
                try {
                    ret = JSONObject.fromObject(responseData);
                } catch (net.sf.json.JSONException ex) {
                    ret = null;
                }
            }
            if (entity != null) {
                entity.consumeContent();
            }
            tracePrint("[processWebRequest][consumeContent()]----------------------------------------");
        }
        return ret;
    }

    public boolean prepareWebRequest() throws UnknownHostException, IOException, HttpException, SocketTimeoutException, TorrentsCommonException {
        return prepareWebRequest(true);
    }

    public boolean prepareWebRequest(boolean allowRecursion) throws UnknownHostException, IOException, HttpException, SocketTimeoutException, TorrentsCommonException {
        boolean ret = false;
        String url = localSettiingsFactory.refreshUrlCache();
        HttpPost httprequest = new HttpPost(url);
        if (x_transmission_session_id != null && authenticate) {
            httprequest.setHeader("X-Transmission-Session-Id", x_transmission_session_id);
        }
        tracePrint("[getCredentials()]----------------------------------------");
        tracePrint("Credentials " + httpclient.getCredentialsProvider().toString());
        tracePrint("executing request to " + targetHttpHost + " via " + proxyHttpHost);
        tracePrint("[getCredentials()]----------------------------------------");
        starWebRequest = System.currentTimeMillis();
        HttpResponse response = null;
        try {
            response = httpclient.execute(targetHttpHost, httprequest);
        } catch (SocketTimeoutException ex) {
            throw new TorrentsCommonException("Connection timeout with " + targetHttpHost);
        }
        stopWebRequest = System.currentTimeMillis();
        diffWebRequest = stopWebRequest - starWebRequest;
        tracePrint("[getStatusLine()]----------------------------------------");
        tracePrint(response.getStatusLine().toString());
        tracePrint("[getStatusLine()]----------------------------------------");
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT && allowRecursion) {
            x_transmission_session_id = getTansmissionSessionId(response);
            ret = prepareWebRequest(false);
        } else {
            HttpEntity entity = response.getEntity();
            tracePrint("[consumeContent()]----------------------------------------");
            responseData = null;
            if (entity != null) {
                tracePrint(true, "[prepareWebRequest]Response content length: [" + entity.getContentLength() + "] time: [" + diffWebRequest + "]");
                responseData = EntityUtils.toString(entity);
                tracePrint(responseData);
            }
            if (entity != null) {
                entity.consumeContent();
            }
            tracePrint("[consumeContent()]----------------------------------------");
            connected = ret = true;
        }
        return (ret);
    }

    public void closeWebRequest() {
        if (httpclient != null && connected) {
            httpclient.getConnectionManager().shutdown();
        }
    }

    /**
     * @return the responseData
     */
    public String getResponseData() {
        return responseData;
    }

    /**
     * @param responseData the responseData to set
     */
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    /**
     * @return the enableTraceOut
     */
    public boolean isEnableTraceOut() {
        return enableTraceOut;
    }

    /**
     * @param enableTraceOut the enableTraceOut to set
     */
    public void setEnableTraceOut(boolean enableTraceOut) {
        this.enableTraceOut = enableTraceOut;
    }

    private void tracePrint(String string) {
        tracePrint(false, string);
    }

    private void tracePrint(boolean force, String string) {
        Date curr = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy hh:mm:ss");
        if (enableTraceOut || force) {
            System.out.println(sdf.format(curr) + " [" + prefixTrace + "] " + string);
        }
        loggerProvider.log(Level.INFO, string);
    }

    private String getTansmissionSessionId(HttpResponse response) {
        String sessId = x_transmission_session_id;
        tracePrint("[getAllHeaders()]----------------------------------------");
        Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            tracePrint(true, "Header[" + i + "]" + headers[i].toString());
            if (headers[i].getName().indexOf("X-Transmission-Session-Id") >= 0) {
                sessId = headers[i].getValue();
            }
        }
        tracePrint("[getAllHeaders()]----------------------------------------");
        return sessId;
    }
}
