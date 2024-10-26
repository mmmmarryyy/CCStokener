package howbuy.android.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 * http������
 * 
 * @author yescpu
 * 
 */
public class UrlConnectionUtil {

    private static final DefaultHttpClient sClient;

    static {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpProtocolParams.setUserAgent(params, "xxxx");
        ConnPerRoute connPerRoute = new ConnPerRouteBean(12);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
        ConnManagerParams.setMaxTotalConnections(params, 20);
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);
        HttpConnectionParams.setSoTimeout(params, 60 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpClientParams.setRedirecting(params, false);
        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
        sClient = new DefaultHttpClient(conMgr, params);
    }

    /**
	 * Get����
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
    public static InputStream getRequest(String path) throws Exception {
        HttpGet httpGet = new HttpGet(path);
        HttpResponse httpResponse = sClient.execute(httpGet);
        Header[] hs = httpResponse.getAllHeaders();
        boolean a = false;
        for (Header h : hs) {
            if (h.getValue().contains("gzip")) {
                a = true;
            }
        }
        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(httpResponse.getEntity());
            InputStream is = bufHttpEntity.getContent();
            GZIPInputStream iss = new GZIPInputStream(is);
            return iss;
        } else {
            return null;
        }
    }

    /**
	 * post����
	 * 
	 * @param path
	 * @param params
	 * @return
	 * @throws Exception
	 */
    public static InputStream executePost(String path, Map<String, String> params) throws Exception {
        HttpPost httpPost = new HttpPost(path);
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            postParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        HttpEntity entity = new UrlEncodedFormEntity(postParams, "UTF-8");
        httpPost.setEntity(entity);
        HttpResponse httpResponse = sClient.execute(httpPost);
        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return httpResponse.getEntity().getContent();
        } else {
            return null;
        }
    }
}
