package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author Dinesh
 */
public class TwoSharedUploaderPlugin {

    private static URL u;

    private static HttpURLConnection uc;

    private static BufferedReader br;

    private static String postURL;

    private static String uploadID;

    private static String downloadURL;

    private static String adminURL;

    public static void main(String args[]) throws IOException, Exception {
        getPostURL();
        fileUpload();
        System.out.println("Getting download URL....");
        String tmp = getData("http://www.2shared.com/uploadComplete.jsp?" + uploadID);
        downloadURL = tmp;
        adminURL = tmp;
        System.out.println("Upload complete. Please wait......................");
        getDownloadPageURL();
        getAdminPageURL();
    }

    public static void getPostURL() {
        System.out.println("Gettign File upload URL");
        postURL = getData("http://www.2shared.com");
        postURL = postURL.substring(postURL.indexOf("action=\""));
        postURL = postURL.replace("action=\"", "");
        postURL = postURL.substring(0, postURL.indexOf("\""));
        System.out.println(postURL);
        uploadID = postURL.substring(postURL.indexOf("sId="));
        System.out.println(uploadID);
    }

    public static String getData(String url) {
        try {
            u = new URL(url);
            uc = (HttpURLConnection) u.openConnection();
            uc.setRequestMethod("GET");
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

    public static void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        FileBody bin = new FileBody(new File("h:\\OpenSubtitlesHasher.java"));
        reqEntity.addPart("fff", bin);
        httppost.setEntity(reqEntity);
        System.out.println("Now uploading your file into 2shared.com. Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
    }

    public static void getDownloadPageURL() {
        downloadURL = downloadURL.substring(downloadURL.indexOf("action=\""));
        downloadURL = downloadURL.replace("action=\"", "");
        downloadURL = downloadURL.substring(0, downloadURL.indexOf("\""));
        System.out.println("File download  link : " + downloadURL);
    }

    public static void getAdminPageURL() {
        adminURL = adminURL.replace("<form action=\"" + downloadURL, "");
        adminURL = adminURL.substring(adminURL.indexOf("action=\""));
        adminURL = adminURL.replace("action=\"", "");
        adminURL = adminURL.substring(0, adminURL.indexOf("\""));
        System.out.println("File adminstration link : " + adminURL);
    }
}
