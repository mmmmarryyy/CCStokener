package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.UploadedDotToAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class UploadedDotTo extends AbstractUploader implements UploaderAccountNecessary {

    UploadedDotToAccount uploadedDotToAccount = (UploadedDotToAccount) AccountsManager.getAccount("Uploaded.to");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String tmp;

    private String phpsessioncookie, downloadlink = "";

    private String admincode;

    private String userid;

    private String userpwd;

    private String postURL;

    private String uploadresponse;

    public UploadedDotTo(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "Uploaded.to";
        if (uploadedDotToAccount.loginsuccessful) {
            host = uploadedDotToAccount.username + " | Uploaded.to";
        }
    }

    public void generateUploadedValue() {
        char[] nonvowel = new char[] { 'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z' };
        char[] vowel = new char[] { 'a', 'e', 'i', 'o', 'u' };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(nonvowel[(int) Math.round(Math.random() * 1000) % 20]).append("").append(vowel[(int) Math.round(Math.random() * 1000) % 5]);
        }
        admincode = sb.toString();
        NULogger.getLogger().log(Level.INFO, "Admin Code : {0}", admincode);
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from uploaded.to");
        u = new URL("http://uploaded.to/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("PHPSESSID")) {
                    phpsessioncookie = tmp;
                }
            }
        }
        phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "phpsessioncookie: {0}", phpsessioncookie);
    }

    private void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("Filename", new StringBody(file.getName()));
        mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        NULogger.getLogger().info("Now uploading your file into uploaded.to");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
        uploadresponse = uploadresponse.substring(0, uploadresponse.indexOf(","));
        downloadlink = "http://ul.to/" + uploadresponse;
        NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
        downURL = downloadlink;
        uploadFinished();
    }

    private String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", UploadedDotToAccount.getPhpsessioncookie() + ";" + UploadedDotToAccount.getLogincookie() + ";" + UploadedDotToAccount.getAuthcookie());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        return k;
    }

    public void run() {
        if (uploadedDotToAccount.loginsuccessful) {
            host = uploadedDotToAccount.username + " | Uploaded.to";
        } else {
            host = "Uploaded.to";
        }
        try {
            status = UploadStatus.INITIALISING;
            if (!uploadedDotToAccount.loginsuccessful) {
                initialize();
            } else {
                tmp = getData("http://uploaded.to/");
                userid = CommonUploaderTasks.parseResponse(tmp, "id=\"user_id\" value=\"", "\"");
                userpwd = CommonUploaderTasks.parseResponse(tmp, "id=\"user_pw\" value=\"", "\"");
            }
            u = new URL("http://uploaded.to/js/script.js");
            uc = (HttpURLConnection) u.openConnection();
            if (uploadedDotToAccount.loginsuccessful) {
                uc.setRequestProperty("Cookie", phpsessioncookie + ";" + uploadedDotToAccount.getLogincookie() + ";" + uploadedDotToAccount.getAuthcookie());
            } else {
                uc.setRequestProperty("Cookie", phpsessioncookie);
            }
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String k = "";
            while ((tmp = br.readLine()) != null) {
                k += tmp;
            }
            generateUploadedValue();
            if (uploadedDotToAccount.loginsuccessful) {
                postURL = CommonUploaderTasks.parseResponse(k, "uploadServer = '", "'") + "upload?admincode=" + admincode + "&id=" + userid + "&pw=" + userpwd;
            } else {
                postURL = CommonUploaderTasks.parseResponse(k, "uploadServer = '", "'") + "upload?admincode=" + admincode;
            }
            NULogger.getLogger().log(Level.INFO, "postURL : {0}", postURL);
            fileUpload();
        } catch (Exception e) {
            Logger.getLogger(UploadedDotTo.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
