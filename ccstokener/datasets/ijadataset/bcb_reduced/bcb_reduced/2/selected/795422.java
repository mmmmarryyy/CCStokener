package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class LocalhostrUploaderPlugin {

    private static String cfduidcookie;

    private static URL u;

    private static HttpURLConnection uc;

    private static boolean login = false;

    private static String cookies = "";

    private static BufferedReader br;

    private static File file;

    private static String localhostrurl;

    private static DefaultHttpClient httpclient;

    private static String downloadlink;

    private static String sessioncookie;

    public static void main(String[] args) throws IOException {
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        loginLocalhostr();
        initialize();
        HttpOptions httpoptions = new HttpOptions(localhostrurl);
        HttpResponse myresponse = httpclient.execute(httpoptions);
        HttpEntity myresEntity = myresponse.getEntity();
        System.out.println(EntityUtils.toString(myresEntity));
        fileUpload();
    }

    public static String getData(String url) {
        try {
            u = new URL(url);
            uc = (HttpURLConnection) u.openConnection();
            uc.setDoOutput(true);
            uc.setRequestProperty("Host", "www.esnips.com");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setRequestProperty("Referer", "http://www.esnips.com/upload.php");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
            uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uc.setRequestProperty("Accept-Encoding", "html");
            uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.setRequestProperty("Cookie", cookies + cfduidcookie + ";");
            uc.setRequestMethod("GET");
            uc.setInstanceFollowRedirects(false);
            System.out.println(uc.getResponseCode());
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                System.out.println(temp);
                k += temp;
            }
            return k;
        } catch (Exception e) {
            System.out.println("exception : " + e.toString());
            return "";
        }
    }

    private static void initialize() throws IOException {
        System.out.println("Getting startup cookies from localhostr.com");
        HttpGet httpget = new HttpGet("http://localhostr.com/");
        if (login) {
            httpget.setHeader("Cookie", sessioncookie);
        }
        HttpResponse myresponse = httpclient.execute(httpget);
        HttpEntity myresEntity = myresponse.getEntity();
        localhostrurl = EntityUtils.toString(myresEntity);
        localhostrurl = parseResponse(localhostrurl, "url : '", "'");
        System.out.println("Localhost url : " + localhostrurl);
        InputStream is = myresponse.getEntity().getContent();
        is.close();
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    public static void loginLocalhostr() throws IOException {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        httpclient = new DefaultHttpClient(params);
        System.out.println("Trying to log in to localhostr");
        HttpPost httppost = new HttpPost("http://localhostr.com/signin");
        httppost.setHeader("Referer", "http://www.localhostr.com/");
        httppost.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("username", "007007dinesh@gmail.com"));
        formparams.add(new BasicNameValuePair("password", ""));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpresponse = httpclient.execute(httppost);
        System.out.println("Getting cookies........");
        System.out.println(httpresponse.getStatusLine());
        Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
        Cookie escookie = null;
        while (it.hasNext()) {
            escookie = it.next();
            if (escookie.getName().contains("session")) {
                sessioncookie = escookie.getName() + " = " + escookie.getValue();
                System.out.println("session cookie : " + sessioncookie);
            }
        }
        if (httpresponse.getStatusLine().getStatusCode() == 302) {
            login = true;
            System.out.println("localhostr Login Success");
        } else {
            System.out.println("localhostr Login failed");
        }
        System.out.println(EntityUtils.toString(httpresponse.getEntity()));
        InputStream is = httpresponse.getEntity().getContent();
        is.close();
    }

    public static void fileUpload() throws IOException {
        file = new File("C:\\Documents and Settings\\dinesh\\Desktop\\ImageShackUploaderPlugin.java");
        HttpPost httppost = new HttpPost(localhostrurl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new FileBody(file);
        mpEntity.addPart("name", new StringBody(file.getName()));
        if (login) {
            mpEntity.addPart("session", new StringBody(sessioncookie.substring(sessioncookie.indexOf("=") + 2)));
        }
        mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        System.out.println("Now uploading your file into localhost...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        System.out.println(response.getStatusLine());
        if (resEntity != null) {
            String tmp = EntityUtils.toString(resEntity);
            downloadlink = parseResponse(tmp, "\"url\":\"", "\"");
            System.out.println("download link : " + downloadlink);
        }
    }
}
