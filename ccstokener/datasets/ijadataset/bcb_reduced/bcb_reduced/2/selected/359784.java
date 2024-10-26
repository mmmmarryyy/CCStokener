package org.apache.http.examples.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

/**
 * An example that performs GETs from multiple threads.
 * 
 * @author Michael Becke
 */
public class ClientMultiThreadedExecution {

    public static void main(String[] args) throws Exception {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        HttpClient httpClient = new DefaultHttpClient(cm, params);
        String[] urisToGet = { "http://jakarta.apache.org/", "http://jakarta.apache.org/commons/", "http://jakarta.apache.org/commons/httpclient/", "http://svn.apache.org/viewvc/jakarta/httpcomponents/" };
        GetThread[] threads = new GetThread[urisToGet.length];
        for (int i = 0; i < threads.length; i++) {
            HttpGet httpget = new HttpGet(urisToGet[i]);
            threads[i] = new GetThread(httpClient, httpget, i + 1);
        }
        for (int j = 0; j < threads.length; j++) {
            threads[j].start();
        }
    }

    /**
     * A thread that performs a GET.
     */
    static class GetThread extends Thread {

        private final HttpClient httpClient;

        private final HttpContext context;

        private final HttpGet httpget;

        private final int id;

        public GetThread(HttpClient httpClient, HttpGet httpget, int id) {
            this.httpClient = httpClient;
            this.context = new BasicHttpContext();
            this.httpget = httpget;
            this.id = id;
        }

        /**
         * Executes the GetMethod and prints some status information.
         */
        @Override
        public void run() {
            System.out.println(id + " - about to get something from " + httpget.getURI());
            try {
                HttpResponse response = httpClient.execute(httpget, context);
                System.out.println(id + " - get executed");
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    System.out.println(id + " - " + bytes.length + " bytes read");
                }
            } catch (Exception e) {
                httpget.abort();
                System.out.println(id + " - error: " + e);
            }
        }
    }
}
