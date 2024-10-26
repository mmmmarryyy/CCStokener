package com.oolive.tools.games.gallendor.web.consummer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class UnitCrawler {

    public static void main(String[] args) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://www.google.com");
        httpget.setHeader("User-Agent", "MySuperUserAgent");
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = entity.getContent();
            int l;
            byte[] tmp = new byte[2048];
            while ((l = instream.read()) != -1) {
                StringBuilder builder = new StringBuilder(l);
                builder.append((char) l);
                System.out.print(builder);
            }
        }
    }
}
