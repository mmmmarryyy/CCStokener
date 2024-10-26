package com.trinea.sns.serviceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import weibo4android.Status;
import weibo4android.Weibo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.trinea.sns.service.UpdateStatusService;

public class UpdateStatusServiceImpl implements UpdateStatusService {

    private static CountDownLatch finishLatch = new CountDownLatch(5);

    private Weibo getSinaWeibo(boolean isOauth, String... args) {
        Weibo weibo = new Weibo();
        if (isOauth) {
            weibo.setToken(args[0], args[1]);
        } else {
            weibo.setUserId(args[0]);
            weibo.setPassword(args[1]);
            weibo.setSource(args[2]);
        }
        return weibo;
    }

    public String updateSina() {
        String statusStr = "";
        System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
        System.setProperty("weibo4j.oauth.consumerSecret", Weibo.CONSUMER_SECRET);
        try {
            Weibo weibo = getSinaWeibo(false, "xingw.1989@163.com", "gengxinwuxl");
            Status status = weibo.updateStatus("Android Test la");
            statusStr = status.getId() + " : " + status.getText() + "  " + status.getCreatedAt();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return statusStr;
    }

    @Override
    public String baiDuHotNews() {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://news.baidu.com/z/wise_topic_processor/wise_hotwords_list.php?bd_page_type=1&tn=wapnews_hotwords_list&type=1&index=1&pfr=3-11-bdindex-top-3--");
        String hostNews = "";
        try {
            HttpResponse response = client.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
            String line = "";
            boolean todayNewsExist = false, firstNewExist = false;
            int newsCount = -1;
            while ((line = buffer.readLine()) != null) {
                if (todayNewsExist || line.contains("<div class=\"news_title\">")) todayNewsExist = true; else continue;
                if (firstNewExist || line.contains("<div class=\"list-item\">")) {
                    firstNewExist = true;
                    newsCount++;
                } else continue;
                if (todayNewsExist && firstNewExist && (newsCount == 1)) {
                    Pattern hrefPattern = Pattern.compile("<a.*>(.+?)</a>.*");
                    Matcher matcher = hrefPattern.matcher(line);
                    if (matcher.find()) {
                        hostNews = matcher.group(1);
                        break;
                    } else newsCount--;
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostNews;
    }

    @Override
    public void sinaT(WebView webView, final String statusContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://3g.sina.com.cn/prog/wapsite/sso/login.php?ns=1&revalid=2&backURL=http%3A%2F%2Ft.sina.cn%2Fdpool%2Fttt%2Fhome.php&backTitle=%D0%C2%C0%CB%CE%A2%B2%A9");
        webView.requestFocus();
        MyJavaScript javaScript = new MyJavaScript();
        webView.addJavascriptInterface(javaScript, "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {

            public int count = 0;

            @Override
            public void onPageFinished(WebView webView, String url) {
                count++;
                if (count == 1) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('mobile')[0].value='xingw.1989@163.com');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('password')[0].value='gengxinwuxl');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('submit')[0].click());");
                }
                if (count == 3) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('content')[0].value='" + statusContent + "');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('content')[0].nextSibling.nextSibling.click());");
                    finishLatch.countDown();
                }
            }
        });
    }

    @Override
    public void renRenT(WebView webView, final String statusContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://m.renren.com");
        webView.requestFocus();
        MyJavaScript javaScript = new MyJavaScript();
        webView.addJavascriptInterface(javaScript, "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {

            public int count = 0;

            @Override
            public void onPageFinished(WebView webView, String url) {
                count++;
                if (count == 1) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('email')[0].value='xingw.1989@163.com');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('password')[0].value='gengxinwuxn');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('login')[0].click());");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                }
                if (count == 2) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('status')[0].value='" + statusContent + "');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('update')[0].click());");
                    finishLatch.countDown();
                }
            }
        });
    }

    @Override
    public void kaiXinT(WebView webView, final String statusContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://wap.kaixin001.com/");
        webView.requestFocus();
        MyJavaScript javaScript = new MyJavaScript();
        webView.addJavascriptInterface(javaScript, "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {

            public int count = 0;

            @Override
            public void onPageFinished(WebView webView, String url) {
                count++;
                if (count == 1) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('email')[0].value='xingw.1989@163.com');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('password')[0].value='gengxinwukx');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('login')[0].click());");
                }
                if (count == 2) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('content')[0].value='" + statusContent + "');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('action')[1].click());");
                    finishLatch.countDown();
                }
            }
        });
    }

    @Override
    public void qqT(WebView webView, final String statusContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://t.qq.com");
        webView.requestFocus();
        MyJavaScript javaScript = new MyJavaScript();
        webView.addJavascriptInterface(javaScript, "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {

            public int count = 0;

            @Override
            public void onPageFinished(WebView webView, String url) {
                count++;
                if (count == 1) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('u')[0].value='1206774631');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('p')[0].value='gengxinwu');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementById('login_btn').click());");
                }
                webView.loadUrl("javascript:window.HTMLOUT.showHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
            }
        });
    }

    @Override
    public void sohuT(WebView webView, final String statusContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://w.sohu.com/t2/login.jsp");
        webView.requestFocus();
        MyJavaScript javaScript = new MyJavaScript();
        webView.addJavascriptInterface(javaScript, "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {

            public int count = 0;

            @Override
            public void onPageFinished(WebView webView, String url) {
                count++;
                if (count == 1) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('u')[0].value='xingw.1989@163.com');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('p')[0].value='gengxinwush');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('m')[0].nextSibling.click());");
                }
                if (count == 2) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('content')[0].value='" + statusContent + "');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('send')[0].click());");
                    finishLatch.countDown();
                }
            }
        });
    }

    @Override
    public void netEasyT(WebView webView, final String statusContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://t.163.com");
        webView.requestFocus();
        MyJavaScript javaScript = new MyJavaScript();
        webView.addJavascriptInterface(javaScript, "HTMLOUT");
        webView.setWebViewClient(new WebViewClient() {

            public int count = 0;

            @Override
            public void onPageFinished(WebView webView, String url) {
                count++;
                if (count == 1) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('username')[0].value='xingw.1989@163.com');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('password')[0].value='xingeng.1989');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('sub')[0].click());");
                }
                if (count == 2) {
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('status')[0].value='" + statusContent + "');");
                    webView.loadUrl("javascript:window.HTMLOUT.showHTML(document.getElementsByName('sub')[0].click());");
                    finishLatch.countDown();
                }
            }
        });
    }

    private class MyJavaScript {

        public void showHTML(String html) {
        }
    }

    ;

    public boolean isFinished() throws InterruptedException {
        finishLatch.await();
        return true;
    }
}
