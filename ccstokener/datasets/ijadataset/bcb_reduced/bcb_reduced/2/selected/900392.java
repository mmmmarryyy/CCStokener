package com.wendal.java.happydog;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpClientVM {

    private DefaultHttpClient client = new DefaultHttpClient();

    public DefaultHttpClient getClient() {
        return client;
    }

    public String accessURL(String url) throws Throwable {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        String entuity = EntityUtils.toString(response.getEntity());
        handleHeaders(response.getAllHeaders());
        return entuity;
    }

    public void accessURL(String url, File file) throws Throwable {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        byte[] data = EntityUtils.toByteArray(response.getEntity());
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.flush();
        fos.close();
        handleHeaders(response.getAllHeaders());
    }

    public String accessURL_Post(String url, Map<String, String> keyValue, String encoding) throws Throwable {
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (String key : keyValue.keySet()) {
            formparams.add(new BasicNameValuePair(key, keyValue.get(key)));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, encoding);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        String entuity = EntityUtils.toString(response.getEntity());
        handleHeaders(response.getAllHeaders());
        return entuity;
    }

    private Set<String> currentCookie = new HashSet<String>();

    private void handleHeaders(Header[] header) {
        if (header == null) return;
        for (Header head : header) {
            if ("Set-Cookie".equalsIgnoreCase(head.getName())) {
                currentCookie.add(head.getValue());
            }
        }
    }

    public Set<String> getCookies() {
        return currentCookie;
    }

    public String getCookie(String name) {
        for (Cookie s : client.getCookieStore().getCookies()) {
            if (s.getName().equalsIgnoreCase(name)) {
                return s.getValue();
            }
        }
        return null;
    }

    public String accessURL_Post(String url, String postStr, String encoding) throws Throwable {
        StringEntity entity = new StringEntity(postStr, encoding);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        String entuity = EntityUtils.toString(response.getEntity());
        handleHeaders(response.getAllHeaders());
        return entuity;
    }
}
