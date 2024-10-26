package neembuuuploader.test.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class BayFilesUploaderPlugin {

    private static URL u;

    private static HttpURLConnection uc;

    private static BufferedReader br;

    private static String tmp;

    private static String postURL;

    private static File file;

    private static String uploadresponse;

    private static String downloadlink;

    private static String deletelink;

    private static String sessioncookie;

    private static boolean login = false;

    public static void main(String[] args) throws Exception {
        loginBayFiles();
        initialize();
        fileUpload();
    }

    private static void initialize() throws Exception {
        System.out.println("Getting upload url from bayfiles.com");
        u = new URL("http://bayfiles.com/ajax_upload?_=" + new Date().getTime());
        uc = (HttpURLConnection) u.openConnection();
        if (login) {
            uc.setRequestProperty("Cookie", sessioncookie);
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        postURL = parseResponse(k, "\"upload_url\":\"", "\"");
        postURL = postURL.replaceAll("\\\\", "");
        System.out.println("Post URL : " + postURL);
    }

    public static String parseResponse(String response, String stringStart, String stringEnd) {
        response = response.substring(response.indexOf(stringStart));
        response = response.replace(stringStart, "");
        response = response.substring(0, response.indexOf(stringEnd));
        return response;
    }

    private static void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        file = new File("h:\\Rock Lee.jpg");
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        ContentBody cbFile = new FileBody(file);
        mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        System.out.println("executing request " + httppost.getRequestLine());
        System.out.println("Now uploading your file into bayfiles.com");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        System.out.println(response.getStatusLine());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        System.out.println("Upload response : " + uploadresponse);
        downloadlink = parseResponse(uploadresponse, "\"downloadUrl\":\"", "\"");
        downloadlink = downloadlink.replaceAll("\\\\", "");
        deletelink = parseResponse(uploadresponse, "\"deleteUrl\":\"", "\"");
        deletelink = deletelink.replaceAll("\\\\", "");
        System.out.println("Download link : " + downloadlink);
        System.out.println("Delete link : " + deletelink);
    }

    public static void loginBayFiles() throws Exception {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        System.out.println("Trying to log in to bayfiles.com");
        HttpPost httppost = new HttpPost("http://bayfiles.com/ajax_login");
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("action", "login"));
        formparams.add(new BasicNameValuePair("username", ""));
        formparams.add(new BasicNameValuePair("password", ""));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        httppost.setEntity(entity);
        HttpResponse httpresponse = httpclient.execute(httppost);
        System.out.println("Getting cookies........");
        Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
        Cookie escookie = null;
        while (it.hasNext()) {
            escookie = it.next();
            if (escookie.getName().equalsIgnoreCase("SESSID")) {
                sessioncookie = "SESSID=" + escookie.getValue();
                System.out.println(sessioncookie);
                login = true;
                System.out.println("BayFiles.com Login success :)");
            }
        }
        if (!login) {
            System.out.println("BayFiles.com Login failed :(");
        }
    }
}
