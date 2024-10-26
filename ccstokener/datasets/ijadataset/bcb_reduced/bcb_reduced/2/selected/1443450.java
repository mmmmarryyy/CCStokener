package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class ZShareUploaderPlugin {

    private static HttpURLConnection uc = null;

    private static URL u = null;

    private static BufferedReader br;

    private static String zsharelink;

    static final String UPLOAD_ID_CHARS = "1234567890qwertyuiopasdfghjklzxcvbnm";

    private static String uplodid = "";

    private static File file;

    private static String postURL = "";

    private static String uploadresponse = "";

    private static String sidcookie = "";

    private static String linkpage = "";

    private static String downloadlink = "";

    private static String deletelink = "";

    private static String phpcookie = "";

    private static PrintWriter pw;

    private static String mysessioncookie = "";

    private static String tmp = "";

    private static String uname = "";

    private static String pwd = "";

    public static void main(String[] args) throws IOException, Exception {
        if (uname.isEmpty() || pwd.isEmpty()) {
            System.out.println("Please give valid username,pwd");
            return;
        }
        initialize();
        loginZShare();
        System.out.println("Getting upload id ......");
        uplodid = getData(zsharelink + "uberupload/ubr_link_upload.php?rnd_id=" + new Date().getTime());
        uplodid = parseResponse(uplodid, "startUpload(\"", "\"");
        System.out.println("Upload id : " + uplodid);
        postURL = zsharelink + "cgi-bin/ubr_upload.pl?upload_id=" + uplodid + "&multiple=0&is_private=0&is_eighteen=0&pass=&descr=";
        fileUpload();
        getDownloadLink();
    }

    private static void getDownloadLink() throws Exception {
        System.out.println("Now Getting Download link...");
        HttpClient client = new DefaultHttpClient();
        HttpGet h = new HttpGet(uploadresponse);
        h.setHeader("Referer", postURL);
        h.setHeader("Cookie", sidcookie + ";" + mysessioncookie);
        HttpResponse res = client.execute(h);
        HttpEntity entity = res.getEntity();
        linkpage = EntityUtils.toString(entity);
        linkpage = linkpage.replaceAll("\n", "");
        downloadlink = parseResponse(linkpage, "value=\"", "\"");
        deletelink = parseResponse(linkpage, "delete.html?", "\"");
        deletelink = "http://www.zshare.net/delete.html?" + deletelink;
        System.out.println("Download link : " + downloadlink);
        System.out.println("Delete Link : " + deletelink);
    }

    private static void fileUpload() throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", sidcookie + ";" + mysessioncookie);
        generateZShareID();
        file = new File("g:/Way2SMSClient.7z");
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        ContentBody cbFile = new FileBody(file);
        mpEntity.addPart("", cbFile);
        mpEntity.addPart("TOS", new StringBody("1"));
        httppost.setEntity(mpEntity);
        System.out.println("executing request " + httppost.getRequestLine());
        System.out.println("Now uploading your file into zshare.net");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        System.out.println(response.getStatusLine());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        uploadresponse = uploadresponse.replaceAll("\n", "");
        uploadresponse = uploadresponse.substring(uploadresponse.indexOf("index2.php"));
        uploadresponse = uploadresponse.substring(0, uploadresponse.indexOf("\">here"));
        uploadresponse = uploadresponse.replaceAll("amp;", "");
        uploadresponse = zsharelink + uploadresponse;
        uploadresponse = uploadresponse.replaceAll(" ", "%20");
        System.out.println("shew : " + uploadresponse);
        httpclient.getConnectionManager().shutdown();
    }

    private static void initialize() throws IOException, Exception {
        System.out.println("Getting startup cookie from zshare.net");
        u = new URL("http://www.zshare.net/");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                sidcookie = tmp;
            }
        }
        sidcookie = sidcookie.substring(0, sidcookie.indexOf(";"));
        System.out.println("Cookie : " + sidcookie);
        br = new BufferedReader(new InputStreamReader(u.openStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        System.out.println("Getting zshare dynamic upload link");
        zsharelink = parseResponse(k, "action=\"", "\"");
        zsharelink = zsharelink.toLowerCase();
        System.out.println(zsharelink);
    }

    private static String getData(String myurl) throws Exception {
        URL url = new URL(myurl);
        uc = (HttpURLConnection) url.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        return k;
    }

    public static void generateZShareID() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int idx = 1 + (int) (Math.random() * 35);
            sb.append(UPLOAD_ID_CHARS.charAt(idx));
        }
        uplodid = sb.toString();
        System.out.println("Upload id : " + uplodid);
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    public static void loginZShare() throws Exception {
        u = new URL("http://www.zshare.net/myzshare/login.php");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
        uc.setRequestProperty("Cookie", sidcookie);
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                phpcookie = tmp;
            }
        }
        phpcookie = phpcookie.substring(0, phpcookie.indexOf(";"));
        System.out.println("PHP Session Coookie : " + phpcookie);
        postData("username=" + uname + "&password=" + pwd + "&submit=+Login+to+your+account+", "http://www.zshare.net/myzshare/process.php?loc=http://www.zshare.net/myzshare/login.php");
    }

    public static void setHttpHeader() {
        try {
            uc = (HttpURLConnection) u.openConnection();
            uc.setDoOutput(true);
            uc.setRequestProperty("Host", "www.zshare.net");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setRequestProperty("Referer", "http://www.zshare.net/");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
            uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uc.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
            uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.setRequestProperty("Cookie", sidcookie + ";" + phpcookie);
            uc.setRequestMethod("POST");
            uc.setInstanceFollowRedirects(false);
        } catch (Exception e) {
            System.out.println("ex" + e.toString());
        }
    }

    public static void writeHttpContent(String content) {
        try {
            System.out.println(content);
            pw = new PrintWriter(new OutputStreamWriter(uc.getOutputStream()), true);
            pw.print(content);
            pw.flush();
            pw.close();
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            Map<String, List<String>> headerFields = uc.getHeaderFields();
            if (headerFields.containsKey("Set-Cookie")) {
                List<String> header = headerFields.get("Set-Cookie");
                for (int i = 0; i < header.size(); i++) {
                    tmp = header.get(i);
                    mysessioncookie = tmp;
                }
                System.out.println("ZShare login success :)");
            } else {
                System.out.println("ZShare login failed :(");
            }
            mysessioncookie = mysessioncookie.substring(0, mysessioncookie.indexOf(";"));
        } catch (Exception e) {
            System.out.println("ex " + e.toString());
        }
    }

    public static void postData(String content, String posturl) {
        try {
            u = new URL(posturl);
            setHttpHeader();
            writeHttpContent(content);
            u = null;
            uc = null;
        } catch (Exception e) {
            System.out.println("exception " + e.toString());
        }
    }
}
