package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class FileFactoryUploadPlugin {

    private static URL url;

    private static URLConnection conn;

    private static String cookie;

    private static PrintWriter pw;

    private static HttpURLConnection uc;

    private static BufferedReader br;

    private static URL u;

    private static String content;

    private static String membershipcookie;

    private static String downloadlink;

    private static String uname = "";

    private static String pwd = "";

    public static void main(String[] args) throws Exception {
        if (uname.isEmpty() || pwd.isEmpty()) {
            System.out.println("Please give valid username,pwd");
            return;
        }
        initilize();
        fileUpload();
    }

    public static void initilize() throws Exception {
        url = new URL("http://filefactory.com/");
        conn = url.openConnection();
        cookie = conn.getHeaderField("Set-Cookie");
        System.out.println(cookie);
        content = "redirect=%2F&email=" + URLEncoder.encode(uname, "UTF-8") + "&password=" + pwd;
        postData(content, "http://filefactory.com/member/login.php");
    }

    public static void setHttpHeader(String cookie) {
        try {
            uc = (HttpURLConnection) u.openConnection();
            uc.setDoOutput(true);
            uc.setRequestProperty("Host", "filefactory.com");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setRequestProperty("Referer", "http://filefactory.com/");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
            uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uc.setRequestProperty("Accept-Encoding", "html");
            uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.setRequestProperty("Cookie", cookie);
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
            String httpResp = br.readLine();
            System.out.println("Http Response value :" + httpResp);
            membershipcookie = uc.getHeaderField("Set-Cookie");
            System.out.println("FileFactory cookie : " + membershipcookie);
            if (membershipcookie == null) {
                System.out.println("FileFacotry login failed");
            } else {
                System.out.println("Filefacotry login success");
            }
        } catch (Exception e) {
            System.out.println("ex " + e.toString());
        }
    }

    public static String getData(String url) {
        try {
            u = new URL(url);
            uc = (HttpURLConnection) u.openConnection();
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                k += temp;
            }
            br.close();
            u = null;
            uc = null;
            return k;
        } catch (Exception e) {
            System.out.println("exception : " + e.toString());
            return "";
        }
    }

    public static void postData(String content, String posturl) {
        try {
            u = new URL(posturl);
            setHttpHeader(cookie);
            writeHttpContent(content);
            u = null;
            uc = null;
        } catch (Exception e) {
            System.out.println("exception " + e.toString());
        }
    }

    public static void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://upload.filefactory.com/upload.php");
        httppost.setHeader("Cookie", cookie + ";" + membershipcookie);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        FileBody bin = new FileBody(new File("h:\\Learning Plan 1.0.pdf"));
        reqEntity.addPart("Filedata", bin);
        reqEntity.addPart("upload", new StringBody("Submit Query"));
        httppost.setEntity(reqEntity);
        System.out.println("Now uploading your file into filefactory.com. Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        String page = "";
        if (resEntity != null) {
            page = EntityUtils.toString(resEntity);
            System.out.println("PAGE :" + page);
        }
        downloadlink = getData("http://www.filefactory.com/mupc/" + page);
        downloadlink = downloadlink.substring(downloadlink.indexOf("<div class=\"metadata\">"));
        downloadlink = downloadlink.replace("<div class=\"metadata\">", "");
        downloadlink = downloadlink.substring(0, downloadlink.indexOf("<"));
        downloadlink = downloadlink.trim();
        System.out.println("Download Link : " + downloadlink);
    }
}
