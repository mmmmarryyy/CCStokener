package com.wenda.java.common.net;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * a simple way to use HttpClient
 * @author Wendal
 *
 */
public class HttpClientVM {

    private DefaultHttpClient client;

    public HttpClientVM() {
        client = new DefaultHttpClient();
    }

    public String get(String url) throws Throwable {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        String entuity = EntityUtils.toString(response.getEntity());
        return entuity;
    }

    public void get2file(String url, File file) throws Throwable {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        byte[] data = EntityUtils.toByteArray(response.getEntity());
        if (file.exists()) file.delete();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.flush();
        fos.close();
    }

    public String post(String url, Map<String, String> keyValue, String encoding) throws Throwable {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (String key : keyValue.keySet()) {
            formparams.add(new BasicNameValuePair(key, keyValue.get(key)));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, encoding);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        String entuity = EntityUtils.toString(response.getEntity());
        return entuity;
    }

    public String post(String url, String postStr, String encoding) throws Throwable {
        StringEntity entity = new StringEntity(postStr, encoding);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        String entuity = EntityUtils.toString(response.getEntity());
        return entuity;
    }

    public void reset() {
        client = new DefaultHttpClient();
    }
}
