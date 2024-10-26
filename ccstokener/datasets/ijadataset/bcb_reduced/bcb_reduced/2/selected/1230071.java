package org.chartsy.main.datafeed;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Viorel
 */
public class GetThread extends Thread {

    private final HttpClient httpClient;

    private final HttpContext context;

    private final HttpGet httpGet;

    private final int id;

    public GetThread(HttpClient httpClient, HttpGet httpGet, int id) {
        this.httpClient = httpClient;
        this.context = new BasicHttpContext();
        this.httpGet = httpGet;
        this.id = id;
    }

    public void run() {
        try {
            HttpResponse response = httpClient.execute(httpGet, context);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                byte[] bytes = EntityUtils.toByteArray(entity);
                System.out.println(id + " - " + bytes.length + " bytes read");
            }
        } catch (Exception ex) {
            System.out.println(id + " - error: " + ex);
        } finally {
            httpGet.abort();
        }
    }
}
