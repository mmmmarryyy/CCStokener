package com.coyousoft.wangyu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class HttpUtil {

    private static ClientConnectionManager cm = null;

    /**
     * 读取HttpResponse但未调用resp.getEntity().consumeContent(),因此调用该方法之后需要手动关闭连接.
     * @param resp
     * @param encode
     * @return
     * @throws IOException
     * @deprecated readResp方法取代之.
     */
    public static String readResponse(HttpResponse resp, String encode) throws IOException {
        String contentEncoding = "";
        for (Header header : resp.getHeaders("Content-Encoding")) {
            contentEncoding = header.getValue();
        }
        InputStream is = null;
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            is = new GZIPInputStream(resp.getEntity().getContent());
        } else {
            is = resp.getEntity().getContent();
        }
        InputStreamReader isr = new InputStreamReader(is, encode);
        BufferedReader in = new BufferedReader(isr);
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) {
            buffer.append(line);
        }
        in.close();
        isr.close();
        return buffer.toString();
    }

    /***
     * 读取HttpResponse并且自动调用resp.getEntity().consumeContent();关闭连接.
     * @param resp
     * @param encode
     * @return
     * @throws Exception 
     */
    public static String getContent(HttpClient client, HttpGet get, String encode) throws Exception {
        HttpResponse resp = doGet(get, client);
        String returnValue = readResponse(resp, encode);
        resp.getEntity().consumeContent();
        return returnValue;
    }

    /***
     * 读取HttpResponse并且自动调用resp.getEntity().consumeContent();关闭连接.
     * @param resp
     * @param encode
     * @return
     * @throws Exception 
     */
    public static String getContent(HttpClient client, HttpPost post, String encode) throws Exception {
        HttpResponse resp = doPost(post, client);
        String returnValue = readResponse(resp, encode);
        resp.getEntity().consumeContent();
        return returnValue;
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

    public static void printCookie(DefaultHttpClient client) {
        CookieStore cs = client.getCookieStore();
        for (Cookie c : cs.getCookies()) {
            System.out.println("Comment:" + c.getComment());
            System.out.println("CommentURL:" + c.getCommentURL());
            System.out.println("Domain:" + c.getDomain());
            System.out.println("Name:" + c.getName());
            System.out.println("Path:" + c.getPath());
            System.out.println("Value:" + c.getValue());
            System.out.println("Version:" + c.getVersion());
            System.out.println("ExpiryDate:" + c.getExpiryDate());
            System.out.print("Port:");
            if (c.getPorts() != null) {
                for (int port : c.getPorts()) {
                    System.out.print(port + ",");
                }
            }
            System.out.println("**********************");
        }
    }
}
