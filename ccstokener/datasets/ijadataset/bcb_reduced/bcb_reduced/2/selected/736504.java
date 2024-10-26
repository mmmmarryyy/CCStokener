package com.baozou.app.robot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.baozou.app.activity.LatestActivity.MyHandler;

public class LatestRobot {

    private static final String D = "LatestRobot";

    public String startUrl = "";

    public String endUrl = "";

    public int currentPage = 1;

    public int totalPage = 0;

    public Document currentPageDoc = null;

    public boolean docIsOk = false;

    public HttpClient hc = null;

    public MyHandler handler = null;

    public LatestRobot(String startUrl, String endUrl, int totalPage, MyHandler handler) {
        this.startUrl = startUrl;
        this.endUrl = endUrl;
        this.totalPage = totalPage;
        this.handler = handler;
        hc = new DefaultHttpClient();
        HttpParams params = hc.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 3000);
        HttpConnectionParams.setSoTimeout(params, 16000);
        currentPageDoc = getDocument(this.startUrl + 1 + this.endUrl, handler);
    }

    public Document getDocument(String url, MyHandler handler) {
        Document doc = null;
        docIsOk = false;
        HttpGet get = new HttpGet(url);
        Log.d(D, "��ʼ��ȡ��ҳ��" + url);
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            HttpResponse response = hc.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Log.d(D, "��ȡ��ҳ�ɹ�����ʼ������ҳ��" + url);
                if (handler != null) handler.updateProgressBar(20);
                HttpEntity en = response.getEntity();
                out = new ByteArrayOutputStream();
                in = en.getContent();
                long total = en.getContentLength();
                byte[] buffer = new byte[1024];
                int count = 0;
                int length = -1;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                    count += length;
                    int complete = (int) (count / total) * 60;
                    if (handler != null) handler.updateProgressBar(complete + 20);
                }
                String str = new String(out.toByteArray(), "gb2312");
                doc = Jsoup.parse(str);
                docIsOk = true;
            }
        } catch (Exception e) {
            if (e instanceof org.apache.http.conn.ConnectTimeoutException) Log.d(D, "connect to " + url + " timed out.");
            if (e instanceof java.net.SocketTimeoutException) Log.d(D, "�ȴ�ͻ����ӳ�ʱ��");
            e.printStackTrace();
        }
        return doc;
    }

    public List<String> getImgsFromDoc() {
        List<String> list = new ArrayList<String>();
        int i = 0;
        while (true) {
            if (this.docIsOk) {
                Log.d(D, "robot.docIsOk: OK.");
                if (this.currentPageDoc != null) {
                    Elements elements = this.currentPageDoc.getElementsByClass("txt_img");
                    for (Element e : elements) {
                        Elements imgs = e.getElementsByTag("img");
                        for (Element img : imgs) {
                            Log.d(D, img.attr("src"));
                            list.add(img.attr("src"));
                        }
                    }
                }
                break;
            } else {
                if (i > 2) {
                    currentPageDoc = getDocument(this.startUrl + currentPage + this.endUrl, handler);
                }
                try {
                    Thread.sleep(2000);
                    Log.d(D, "Thread.sleep(2000)");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ++i;
        }
        return list;
    }

    public List<Map<String, Object>> getImgs() {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        List<String> list = getImgsFromDoc();
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("url", list.get(i));
            map.put("bitmap", getBitmapFromUrl(list.get(i)));
            ret.add(map);
        }
        return ret;
    }

    public int getTotalPageFromDoc(Document doc) {
        Log.d(D, "getTotalPageFromDocc");
        int totalPage = 0;
        int i = 0;
        while (true) {
            if (this.docIsOk) {
                Log.d(D, "robot.docIsOk: OK.");
                if (doc != null) {
                    Elements elements = doc.getElementsByAttributeValue("class", "page_mmhz");
                    Log.d(D, "elements.size():" + elements.size());
                    for (Element e : elements) {
                        String str = e.attr("onclick");
                        int sIndex = str.indexOf("(");
                        int eIndex = str.indexOf(",");
                        String t = str.substring(sIndex, eIndex + 1);
                        Log.d(D, "getTotalPageFromDoc:" + t);
                        totalPage = Integer.parseInt(t);
                    }
                }
                break;
            } else {
                if (i > 2) {
                    currentPageDoc = getDocument(this.startUrl + currentPage + this.endUrl, handler);
                }
                try {
                    Thread.sleep(2000);
                    Log.d(D, "Thread.sleep(2000)");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ++i;
        }
        return totalPage;
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

    public void nextPage() {
        Log.d(D, "nextPage()");
        Log.d(D, "currentPage:" + currentPage + ",totalPage:" + totalPage);
        if (currentPage + 1 <= totalPage) {
            ++currentPage;
            currentPageDoc = getDocument(this.startUrl + currentPage + this.endUrl, handler);
        }
    }
}
