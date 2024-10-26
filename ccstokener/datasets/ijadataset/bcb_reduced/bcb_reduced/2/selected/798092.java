package com.coyousoft.adsys.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class HttpUtil {

    private static ClientConnectionManager cm = null;

    public static String readResponse(HttpResponse resp, String encode) throws IOException {
        InputStreamReader is = new InputStreamReader(resp.getEntity().getContent(), encode);
        BufferedReader in = new BufferedReader(is);
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) buffer.append(line);
        is.close();
        return buffer.toString();
    }

    public static HttpResponse doPost(HttpPost post, HttpClient client) throws Exception {
        HttpProtocolParams.setUseExpectContinue(client.getParams(), false);
        HttpProtocolParams.setUseExpectContinue(post.getParams(), false);
        HttpResponse resp = client.execute(post);
        return resp;
    }

    public static HttpResponse doGet(HttpGet get, HttpClient client) throws Exception {
        client.getConnectionManager().closeIdleConnections(0L, TimeUnit.MILLISECONDS);
        HttpResponse resp = client.execute(get);
        return resp;
    }

    public static String getRandomIp() {
        Random r = new Random();
        return (r.nextInt(200) + 1) + "." + (r.nextInt(253) + 1) + "." + (r.nextInt(253) + 1) + "." + (r.nextInt(253) + 1);
    }

    public static void main(String[] args) {
        System.out.println(getRandomIp());
    }

    public static ClientConnectionManager getConnectionManager(HttpParams params) {
        if (cm != null) {
            return cm;
        }
        ConnManagerParams.setMaxTotalConnections(params, 100);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        new ThreadSafeClientConnManager(params, schemeRegistry);
        return cm;
    }
}
