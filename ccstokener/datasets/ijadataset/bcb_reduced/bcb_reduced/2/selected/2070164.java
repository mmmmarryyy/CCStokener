package org.apache.http.examples.client;

import java.util.concurrent.TimeUnit;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 * Example demonstrating how to evict expired and idle connections
 * from the connection pool.
 */
public class ClientEvictExpiredConnections {

    public static void main(String[] args) throws Exception {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        HttpClient httpclient = new DefaultHttpClient(cm, params);
        String[] urisToGet = { "http://jakarta.apache.org/", "http://jakarta.apache.org/commons/", "http://jakarta.apache.org/commons/httpclient/", "http://svn.apache.org/viewvc/jakarta/httpcomponents/" };
        IdleConnectionEvictor connEvictor = new IdleConnectionEvictor(cm);
        connEvictor.start();
        for (int i = 0; i < urisToGet.length; i++) {
            String requestURI = urisToGet[i];
            HttpGet req = new HttpGet(requestURI);
            System.out.println("executing request " + requestURI);
            HttpResponse rsp = httpclient.execute(req);
            HttpEntity entity = rsp.getEntity();
            System.out.println("----------------------------------------");
            System.out.println(rsp.getStatusLine());
            if (entity != null) {
                System.out.println("Response content length: " + entity.getContentLength());
            }
            System.out.println("----------------------------------------");
            if (entity != null) {
                entity.consumeContent();
            }
        }
        Thread.sleep(20000);
        connEvictor.shutdown();
        connEvictor.join();
        httpclient.getConnectionManager().shutdown();
    }

    public static class IdleConnectionEvictor extends Thread {

        private final ClientConnectionManager connMgr;

        private volatile boolean shutdown;

        public IdleConnectionEvictor(ClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        connMgr.closeExpiredConnections();
                        connMgr.closeIdleConnections(5, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
