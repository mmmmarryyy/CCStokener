package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class DropBoxUploaderPlugin {

    private static URL u;

    private static HttpURLConnection uc;

    private static String tmp;

    private static String gvccookie;

    private static BufferedReader br;

    private static String tvalue;

    private static String forumjarcookie = "", touchcookie = "";

    private static String forumlidcookie = "", lidcookie = "", jarcookie = "";

    private static File file;

    private static String uploadresponse;

    private static String puccookie = "";

    private static String uid;

    public static void main(String[] args) throws Exception {
        initialize();
        loginDropBox();
        getData();
        fileUpload();
    }

    private static void initialize() throws Exception {
        System.out.println("Getting startup cookie from dropbox.com");
        u = new URL("http://www.dropbox.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("set-cookie")) {
            List<String> header = headerFields.get("set-cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("gvc")) {
                    gvccookie = tmp;
                }
            }
        }
        gvccookie = gvccookie.substring(0, gvccookie.indexOf(";"));
        System.out.println("gvccookie: " + gvccookie);
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    public static void loginDropBox() throws Exception {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        System.out.println("Trying to log in to dropbox.com");
        HttpPost httppost = new HttpPost("https://www.dropbox.com/login");
        httppost.setHeader("Cookie", gvccookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("login_email", "007007dinesh@gmail.com"));
        formparams.add(new BasicNameValuePair("login_password", ""));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpresponse = httpclient.execute(httppost);
        System.out.println("Getting cookies........");
        Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
        Cookie escookie = null;
        while (it.hasNext()) {
            escookie = it.next();
            if (escookie.getName().equalsIgnoreCase("forumjar")) {
                forumjarcookie = "forumjar=" + escookie.getValue();
                System.out.println(forumjarcookie);
            }
            if (escookie.getName().equalsIgnoreCase("touch")) {
                touchcookie = "touch=" + escookie.getValue();
                System.out.println(touchcookie);
            }
            if (escookie.getName().equalsIgnoreCase("forumlid")) {
                forumlidcookie = "forumlid=" + escookie.getValue();
                System.out.println(forumlidcookie);
            }
            if (escookie.getName().equalsIgnoreCase("lid")) {
                lidcookie = "lid=" + escookie.getValue();
                System.out.println(lidcookie);
            }
            if (escookie.getName().equalsIgnoreCase("jar")) {
                jarcookie = "jar=" + escookie.getValue();
                System.out.println(jarcookie);
            }
        }
    }

    private static void getData() throws Exception {
        System.out.println("Getting token,user id value from Dropbox ...");
        String k = "";
        u = new URL("https://www.dropbox.com/home");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", gvccookie + ";" + lidcookie + ";" + forumjarcookie + ";" + jarcookie + ";" + touchcookie + ";" + forumlidcookie);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        tvalue = parseResponse(k, "TOKEN: '", "'");
        uid = parseResponse(k, "uid: '", "'");
        System.out.println("tvalue : " + tvalue);
        System.out.println("uid : " + uid);
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("set-cookie")) {
            List<String> header = headerFields.get("set-cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("puc")) {
                    puccookie = tmp;
                }
            }
        }
        puccookie = puccookie.substring(0, puccookie.indexOf(";"));
        System.out.println("puccookie : " + puccookie);
    }

    private static void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("https://dl-web.dropbox.com/upload");
        httppost.setHeader("Referer", "https://www.dropbox.com/home/Public");
        httppost.setHeader("Cookie", forumjarcookie + ";" + forumlidcookie + ";" + touchcookie);
        file = new File("C:\\Documents and Settings\\dinesh\\Desktop\\GigaSizeUploaderPlugin.java");
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        ContentBody cbFile = new FileBody(file);
        mpEntity.addPart("t", new StringBody(tvalue));
        mpEntity.addPart("plain", new StringBody("yes"));
        mpEntity.addPart("dest", new StringBody("/Public"));
        mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        System.out.println("executing request " + httppost.getRequestLine());
        System.out.println("Now uploading your file into dropbox.com");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        System.out.println(response.getStatusLine());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        System.out.println("Upload response : " + uploadresponse);
        if (uploadresponse.contains("The resource was found at https://www.dropbox.com/home/Public")) {
            System.out.println("Downloadlink : http://dl.dropbox.com/u/" + uid + "/" + (URLEncoder.encode(file.getName(), "UTF-8").replace("+", "%20")));
        } else {
            System.out.println("Upload failed");
        }
    }
}
