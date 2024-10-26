package net.sf.dvstar.transmission.protocol;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dstarzhynskyi
 */
public class TestConnection {

    public static void testConnection() throws Exception {
        HttpHost target = new HttpHost("195.74.67.237", 80, "http");
        HttpHost proxy = new HttpHost("192.168.4.7", 3128, "http");
        SchemeRegistry supportedSchemes = new SchemeRegistry();
        supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);
        DefaultHttpClient httpclient = new DefaultHttpClient(ccm, params);
        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        HttpGet req = new HttpGet("/");
        System.out.println("executing request to " + target + " via " + proxy);
        HttpResponse rsp = httpclient.execute(target, req);
        HttpEntity entity = rsp.getEntity();
        System.out.println("----------------------------------------");
        System.out.println(rsp.getStatusLine());
        Header[] headers = rsp.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
        }
        System.out.println("----------------------------------------");
        if (entity != null) {
            System.out.println(EntityUtils.toString(entity));
        }
        httpclient.getConnectionManager().shutdown();
    }

    public static void main(String[] args) throws Exception {
        testConnection();
    }
}
