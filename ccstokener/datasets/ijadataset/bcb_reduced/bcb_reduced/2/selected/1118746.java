package com.kenstevens.stratdom.site;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import com.kenstevens.stratdom.main.Constants;

/**
 * A example that demonstrates how HttpClient APIs can be used to perform
 * form-based logon. based on 2008-2009 version 4 API http://hc.apache.org/
 */
public class TestHttpClient {

    public void testLogin() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        getHomePage(httpclient);
        login(httpclient);
        yourGames(httpclient);
        chooseGame(httpclient);
    }

    private void chooseGame(DefaultHttpClient httpclient) throws IOException, ClientProtocolException {
        HttpGet httpget = new HttpGet(Constants.STRATEGICDOMINATION_URL + "/gameboard.cgi?gameid=" + 1687);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        System.out.println("cg form get: " + response.getStatusLine());
        if (entity != null) {
            InputStream inStream = entity.getContent();
            IOUtils.copy(inStream, System.out);
        }
        System.out.println("cg set of cookies:");
        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        if (cookies.isEmpty()) {
            System.out.println("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
            }
        }
    }

    private void yourGames(DefaultHttpClient httpclient) throws IOException, ClientProtocolException {
        HttpGet httpget = new HttpGet(Constants.YOUR_GAMES_URL);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        System.out.println("yg form get: " + response.getStatusLine());
        if (entity != null) {
            entity.consumeContent();
        }
        System.out.println("yg set of cookies:");
        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        if (cookies.isEmpty()) {
            System.out.println("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
            }
        }
    }

    private void login(DefaultHttpClient httpclient) throws UnsupportedEncodingException, IOException, ClientProtocolException {
        HttpResponse response;
        HttpEntity entity;
        List<Cookie> cookies;
        HttpPost httpost = new HttpPost(Constants.STRATEGICDOMINATION_URL + "/login.html");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("username", "hydrogen"));
        nvps.add(new BasicNameValuePair("password", "crimson7"));
        nvps.add(new BasicNameValuePair("command", "login"));
        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        response = httpclient.execute(httpost);
        entity = response.getEntity();
        System.out.println("Login form get: " + response.getStatusLine());
        if (entity != null) {
            entity.consumeContent();
        }
        System.out.println("Post logon cookies:");
        cookies = httpclient.getCookieStore().getCookies();
        if (cookies.isEmpty()) {
            System.out.println("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
            }
        }
    }

    private void getHomePage(DefaultHttpClient httpclient) throws IOException, ClientProtocolException {
        HttpGet httpget = new HttpGet(Constants.STRATEGICDOMINATION_URL);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        System.out.println("Login form get: " + response.getStatusLine());
        if (entity != null) {
            entity.consumeContent();
        }
        System.out.println("Initial set of cookies:");
        List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        if (cookies.isEmpty()) {
            System.out.println("None");
        } else {
            for (int i = 0; i < cookies.size(); i++) {
                System.out.println("- " + cookies.get(i).toString());
            }
        }
    }
}
