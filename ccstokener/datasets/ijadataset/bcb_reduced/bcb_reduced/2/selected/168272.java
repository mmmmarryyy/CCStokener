package com._5i56.mailtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import com.xdatasystem.contactsimporter.C;
import com.xdatasystem.contactsimporter.MemorizingRedirectHandler;
import com.xdatasystem.contactsimporter.UpdateableCookieStore;

public class MailTest {

    public static void main(String[] args) throws Exception {
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        DefaultHttpClient client = new DefaultHttpClient(cm, params);
        HttpContext context = new BasicHttpContext();
        client.setCookieStore(new UpdateableCookieStore());
        client.setRedirectHandler(new MemorizingRedirectHandler());
        client.getParams().setParameter("http.protocol.cookie-policy", "compatibility");
        List<BasicHeader> headers = new ArrayList<BasicHeader>();
        headers.add(new BasicHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 598; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; .NET CLR 2.0.50727)"));
        client.getParams().setParameter("http.default-headers", headers);
        String account = "pengq1986";
        String mail163com = "http://mail.163.com/";
        HttpGet mail163comGet = new HttpGet(mail163com);
        setMail163comHeaders(mail163comGet, "");
        doGet(mail163comGet, client, context);
        String adgeo = "http://adgeo.163.com/ad_cookies";
        HttpGet adgeoGet = new HttpGet(adgeo);
        setAdgeoHeaders(adgeoGet, mail163com);
        doGet(adgeoGet, client, context);
        List<NameValuePair> data = new ArrayList<NameValuePair>(10);
        data.add(new BasicNameValuePair("verifycookie", "1"));
        data.add(new BasicNameValuePair("style", "-1"));
        data.add(new BasicNameValuePair("product", "mail163"));
        data.add(new BasicNameValuePair("username", account));
        data.add(new BasicNameValuePair("password", "ilike8ds"));
        data.add(new BasicNameValuePair("savelogin", ""));
        data.add(new BasicNameValuePair("selType", "-1"));
        data.add(new BasicNameValuePair("secure", "on"));
        long time = System.currentTimeMillis();
        String loginPageUrl = "http://reg.163.com/login.jsp?type=1&product=mail163&url=http://entry.mail.163.com/coremail/fcg/ntesdoor2?lightweight%3D1%26verifycookie%3D1%26language%3D-1%26style%3D-1";
        HttpPost post = new HttpPost(loginPageUrl);
        post.addHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));
        setLoginHeaders(post, "");
        String ntes_sess = "";
        String p_info = "";
        String s_info = "";
        HttpResponse resp = doPost(post, client, context);
        InputStream is = resp.getEntity().getContent();
        String content = readInputStream(is, C._163_ENCODE);
        System.out.println(content);
        String responseUrl = "";
        if (content.contains("window.location.replace(\"")) {
            responseUrl = content.substring(content.indexOf("window.location.replace(\"") + "window.location.replace(\"".length());
            responseUrl = responseUrl.substring(0, responseUrl.indexOf("\""));
        } else {
            throw new Exception("Login failed");
        }
        BasicClientCookie logTypeCookie = new BasicClientCookie("logType", "-1");
        BasicClientCookie ntes_mail_firstpageCookie = new BasicClientCookie("ntes_mail_firstpage", "normal");
        BasicClientCookie nts_mail_userCookie = new BasicClientCookie("nts_mail_user", account + ":-1:0");
        logTypeCookie.setDomain("mail.163.com");
        logTypeCookie.setPath("/");
        ntes_mail_firstpageCookie.setDomain("mail.163.com");
        ntes_mail_firstpageCookie.setPath("/");
        nts_mail_userCookie.setDomain("mail.163.com");
        nts_mail_userCookie.setPath("/");
        client.getCookieStore().addCookie(logTypeCookie);
        client.getCookieStore().addCookie(ntes_mail_firstpageCookie);
        client.getCookieStore().addCookie(nts_mail_userCookie);
        HttpGet entryGet = new HttpGet(responseUrl);
        client.setRedirectHandler(new RedirectHandler() {

            public boolean isRedirectRequested(HttpResponse arg0, HttpContext arg1) {
                return false;
            }

            public URI getLocationURI(HttpResponse arg0, HttpContext arg1) throws ProtocolException {
                return null;
            }
        });
        setEntryHeaders(entryGet, "");
        HttpResponse entryResp = doGet(entryGet, client, context);
        String location = "";
        for (Header header : entryResp.getHeaders("Location")) {
            location = header.getValue();
        }
        InputStream entryIs = entryResp.getEntity().getContent();
        content = readInputStream(entryIs, C._163_ENCODE);
        System.out.println(content);
        System.out.println(location);
        String indexUrl = location.replace("main.jsp", "index.jsp");
        HttpGet rediGet = new HttpGet(location);
        HttpResponse rediResp = doGet(rediGet, client, context);
        InputStream rediIs = rediResp.getEntity().getContent();
        content = readInputStream(rediIs, C._163_ENCODE);
        System.out.println(content);
        String sid = null;
        if (content.contains("sid=")) {
            sid = content.substring(content.indexOf("sid=") + 4);
            sid = sid.substring(0, sid.indexOf("\""));
        }
        System.out.println("sid:" + sid);
        HttpGet indexGet = new HttpGet(indexUrl);
        HttpResponse indexResp = doGet(indexGet, client, context);
        InputStream indexIs = indexResp.getEntity().getContent();
        content = readInputStream(indexIs, C._163_ENCODE);
        String setcookieUrl = "http://reg.youdao.com/setcookie.jsp";
        String setcookieUrlEndTag = "&username=" + account + "@163.com";
        if (content.contains(setcookieUrl)) {
            setcookieUrl = content.substring(content.indexOf(setcookieUrl), content.indexOf(setcookieUrlEndTag) + setcookieUrlEndTag.length());
        }
        System.out.println("setcookieUrl:" + setcookieUrl);
        String mailHost = location.substring(location.indexOf("http://") + "http://".length(), location.indexOf("/a/j"));
        String sequentialUrl = "http://" + mailHost + "/a/s?sid=" + sid + "&func=global:sequential";
        String sequentialPostDate = "<?xml version=\"1.0\"?><object><array name=\"items\"><object><string name=\"func\">pab:searchContacts</string><object name=\"var\"><array name=\"order\"><object><string name=\"field\">FN</string><boolean name=\"ignoreCase\">true</boolean></object></array></object></object><object><string name=\"func\">user:getSignatures</string></object><object><string name=\"func\">pab:getAllGroups</string></object></array></object>";
        BasicClientCookie mail163ssnCookie = new BasicClientCookie("MAIL163_SSN", account);
        BasicClientCookie mailHostCookie = new BasicClientCookie("mail_host", mailHost);
        BasicClientCookie mailStyleCookie = new BasicClientCookie("mail_style", "js3");
        BasicClientCookie mailUidCookie = new BasicClientCookie("mail_uid", account + "@163.com");
        BasicClientCookie wmsvrDomainCookie = new BasicClientCookie("wmsvr_domain", mailHost);
        mail163ssnCookie.setDomain("mail.163.com");
        mail163ssnCookie.setPath("/");
        mailHostCookie.setDomain(".163.com");
        mailHostCookie.setPath("/");
        mailStyleCookie.setDomain("mail.163.com");
        mailStyleCookie.setPath("/");
        mailUidCookie.setDomain("mail.163.com");
        mailUidCookie.setPath("/");
        wmsvrDomainCookie.setDomain("mail.163.com");
        wmsvrDomainCookie.setPath("/");
        client.getCookieStore().addCookie(mail163ssnCookie);
        client.getCookieStore().addCookie(mailHostCookie);
        client.getCookieStore().addCookie(mailStyleCookie);
        client.getCookieStore().addCookie(mailUidCookie);
        client.getCookieStore().addCookie(wmsvrDomainCookie);
        System.out.println("setcookieContent:" + content);
        String mailAction = "http://" + mailHost + "/a/s?sid=" + sid + "&func=mbox:compose";
        HttpPost mailPost = new HttpPost(mailAction);
        HttpEntity mailEntity = new FileEntity(new File("c:/2.xml"), "application/xml");
        mailPost.setEntity(mailEntity);
        String cookieStr = "";
        for (Cookie cookie : client.getCookieStore().getCookies()) {
            String name = cookie.getName();
            String value = cookie.getValue();
            cookieStr += name + "=" + value + ";";
            System.out.printf("%-20s %-100s %-2s %-20s\n", cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain());
        }
        String refer = indexUrl;
        setMailHeaders(mailPost, refer, mailHost, cookieStr);
        Thread.sleep(5000);
        HttpResponse mailResp = doPost(mailPost, client, context);
        content = readInputStream(mailResp.getEntity().getContent(), "GB2312");
        System.out.println(content);
    }

    protected static HttpResponse doPost(HttpPost post, HttpClient client, HttpContext context) throws Exception {
        HttpProtocolParams.setUseExpectContinue(client.getParams(), false);
        HttpProtocolParams.setUseExpectContinue(post.getParams(), false);
        HttpResponse resp = client.execute(post, context);
        return resp;
    }

    protected static HttpResponse doGet(HttpGet get, HttpClient client, HttpContext context) throws Exception {
        client.getConnectionManager().closeIdleConnections(0L, TimeUnit.MILLISECONDS);
        HttpResponse resp = client.execute(get, context);
        return resp;
    }

    protected static void setMail163comHeaders(HttpRequest req, String referer) {
        req.addHeader("Accept", "*/*");
        req.addHeader("Accept-Language", "zh-cn");
        req.addHeader("Accept-Encoding", "gzip, deflate");
        req.addHeader("Connection", "Keep-Alive");
        req.addHeader("Host", "mail.163.com");
    }

    protected static void setAdgeoHeaders(HttpRequest req, String referer) {
        req.addHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, */*");
        req.addHeader("Accept-Language", "zh-cn");
        req.addHeader("Accept-Encoding", "gzip, deflate");
        req.addHeader("Connection", "Keep-Alive");
        req.addHeader("Host", "adgeo.163.com");
        req.addHeader("Referer", referer);
    }

    protected static void setEntryHeaders(HttpRequest req, String referer) {
        req.addHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, */*");
        req.addHeader("Accept-Language", "zh-cn");
        req.addHeader("Accept-Encoding", "gzip, deflate");
        req.addHeader("Connection", "Keep-Alive");
        req.addHeader("Host", "entry.mail.163.com");
    }

    protected static void setLoginHeaders(HttpRequest req, String referer) {
        req.addHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, */*");
        req.addHeader("Referer", "http://mail.163.com");
        req.addHeader("Accept-Language", "zh-cn");
        req.addHeader("Connection", "Keep-Alive");
        req.addHeader("Cache-Control", "no-cache");
        req.addHeader("Host", "reg.163.com");
    }

    protected static void setMailHeaders(HttpRequest req, String referer, String mailHost, String cookieStr) {
        req.addHeader("Accept", " text/javascript");
        req.addHeader("Referer", "http://mail.163.com");
        req.addHeader("Accept-Language", "zh-cn");
        req.addHeader("Accept-Encoding", "gzip, deflate");
        req.addHeader("Connection", "Keep-Alive");
        req.addHeader("Cache-Control", "no-cache");
        req.addHeader("Cookie", cookieStr);
        req.addHeader("Content-Type", "application/xml");
        req.addHeader("Host", mailHost);
        System.out.println(referer);
        req.addHeader("Referer", referer);
    }

    protected static String readInputStream(InputStream is, String encode) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is, encode));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) buffer.append(line);
        is.close();
        return buffer.toString();
    }
}
