package com.flsoft.sspider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

public class Run_URLTest {

    @Test
    public void urlTest() throws ClientProtocolException, IOException {
        String url = "http://www.pharmnet.com.cn/sms/index1.html";
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String urlContent = IOUtils.toString(entity.getContent());
        System.out.println(urlContent);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void urlDecode() throws UnsupportedEncodingException {
        System.out.println(URLDecoder.decode("http://www.pharmnet.com.cn/product/pclist-%B8%BE%BF%C6%C7%A7%BD%F0%C6%AC-1.html"));
        System.out.println(URLEncoder.encode("����ǧ��Ƭ", "gb2312"));
    }
}
