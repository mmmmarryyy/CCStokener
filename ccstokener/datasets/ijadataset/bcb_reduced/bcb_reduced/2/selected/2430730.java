package com.GNG.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import android.content.Context;
import android.util.Log;

public class Http {

    public DefaultHttpClient httpClient = new DefaultHttpClient();

    public String post(String url, List<NameValuePair> _params) {
        String strResult;
        HttpPost httpRequest = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params = _params;
        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                strResult = EntityUtils.toString(httpResponse.getEntity());
            } else {
                strResult = "bengle";
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (IOException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (Exception e) {
            e.printStackTrace();
            strResult = "bengle";
        }
        return strResult;
    }

    public String get(String url) {
        String strResult;
        HttpGet httpRequest = new HttpGet(url);
        try {
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                strResult = EntityUtils.toString(httpResponse.getEntity(), "gbk");
            } else {
                strResult = "bengle";
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (IOException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (Exception e) {
            e.printStackTrace();
            strResult = "bengle";
        }
        return strResult;
    }

    public String getWithCookie(String url) {
        String strResult;
        HttpGet httpRequest = new HttpGet(url);
        BasicHeader mBasicHeader = new BasicHeader("Cookie", Constant.cookie);
        httpRequest.setHeader(mBasicHeader);
        try {
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                strResult = EntityUtils.toString(httpResponse.getEntity());
            } else {
                strResult = "bengle";
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (IOException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (Exception e) {
            e.printStackTrace();
            strResult = "bengle";
        }
        return strResult;
    }

    public String postWithCookie(String url, List<NameValuePair> _params) {
        String strResult;
        HttpPost httpRequest = new HttpPost(url);
        BasicHeader mBasicHeader = new BasicHeader("Cookie", Constant.cookie);
        httpRequest.setHeader(mBasicHeader);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params = _params;
        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, "gb2312"));
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                strResult = EntityUtils.toString(httpResponse.getEntity());
            } else {
                strResult = "bengle";
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (IOException e) {
            e.printStackTrace();
            strResult = "bengle";
        } catch (Exception e) {
            e.printStackTrace();
            strResult = "bengle";
        }
        return strResult;
    }
}
