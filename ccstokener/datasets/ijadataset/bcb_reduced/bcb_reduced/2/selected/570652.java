package com.iximo.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

/**
 * 网络工具类
 *
 */
public class InternetService {

    /**
	 * get
	 */
    public static InputStream get(String uri) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet get = new HttpGet(uri);
        HttpResponse response = httpClient.execute(get);
        return response.getEntity().getContent();
    }

    /**
	 * insert
	 */
    public static InputStream post(String uri, Map params) throws Exception {
        List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
        for (Object key : params.entrySet()) {
            Map.Entry<String, String> key1 = (Map.Entry<String, String>) key;
            data.add(new BasicNameValuePair(key1.getKey(), key1.getValue()));
        }
        return post4(uri, data);
    }

    /**
	 * delete 
	 */
    public static void delete(String uri, Map params) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        HttpDelete del = new HttpDelete(uri);
        HttpResponse response = httpClient.execute(del);
        System.out.println(response.getStatusLine());
    }

    /**
	 * put
	 */
    public static void update(String uri, Map params) throws Exception {
        List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
        for (Object key : params.keySet()) {
            String keyString = (String) key;
            data.add(new BasicNameValuePair(keyString, (String) params.get(key)));
        }
        update4(uri, data);
    }

    private static void update4(String uri, List<BasicNameValuePair> params) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPut put = new HttpPut(uri);
        put.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        try {
            HttpResponse response = httpClient.execute(put);
            System.out.println("response statusLine: " + response.getStatusLine());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public static InputStream post4(String uri, List<? extends NameValuePair> params) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost post = new HttpPost(uri);
        post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        try {
            HttpResponse response = new DefaultHttpClient().execute(post);
            return response.getEntity().getContent();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
	 * 读取输入流
	 * @return
	 */
    public static String readStream(InputStream inStream) {
        if (inStream != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = -1;
            try {
                while ((len = inStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return new String(out.toByteArray(), "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
}
