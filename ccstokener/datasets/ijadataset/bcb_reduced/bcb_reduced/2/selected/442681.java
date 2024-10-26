package com.baozou.framework.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class HttpUtil {

    private static final String D = "HttpUtil";

    HttpClient hc;

    public HttpUtil() {
        hc = new DefaultHttpClient();
    }

    public List<String> getImgsFromUrl(String url) {
        List<String> list = new ArrayList<String>();
        HttpGet get = new HttpGet(url);
        Log.d(D, "��ʼ������ҳ��" + url);
        try {
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String str = EntityUtils.toString(response.getEntity());
                Document doc = Jsoup.parse(str);
                Elements elements = doc.getElementsByClass("txt_img");
                for (Element e : elements) {
                    Elements imgs = e.getElementsByTag("img");
                    for (Element img : imgs) {
                        Log.d(D, img.attr("src"));
                        list.add(img.attr("src"));
                    }
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Bitmap getBitmapFromUrl(String url) {
        Bitmap b = null;
        try {
            Log.d(D, "��ʼ����ͼƬ��" + url);
            URL u = new URL(url);
            b = BitmapFactory.decodeStream(u.openStream());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }
}
