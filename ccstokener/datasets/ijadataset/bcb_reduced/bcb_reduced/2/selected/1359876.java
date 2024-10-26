package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.DropBoxAccount;
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
public class DropBox extends AbstractUploader implements UploaderAccountNecessary {

    DropBoxAccount dropBoxAccount = (DropBoxAccount) AccountsManager.getAccount("DropBox.com");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String uploadresponse;

    private String downloadlink;

    private String tmp;

    private String token = "", uid = "";

    private String puccookie;

    public DropBox(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "DropBox.com";
        if (dropBoxAccount.loginsuccessful) {
            host = dropBoxAccount.username + " | DropBox.com";
        }
    }

    private void getData() throws Exception {
        NULogger.getLogger().info("Getting token,user id value from Dropbox ...");
        String k = "";
        u = new URL("https://www.dropbox.com/home");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", DropBoxAccount.getGvccookie() + ";" + DropBoxAccount.getLidcookie() + ";" + DropBoxAccount.getForumjarcookie() + ";" + DropBoxAccount.getJarcookie() + ";" + DropBoxAccount.getTouchcookie() + ";" + DropBoxAccount.getForumlidcookie());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        token = CommonUploaderTasks.parseResponse(k, "TOKEN: '", "'");
        uid = CommonUploaderTasks.parseResponse(k, "uid: '", "'");
        NULogger.getLogger().log(Level.INFO, "token : {0}", token);
        NULogger.getLogger().log(Level.INFO, "uid : {0}", uid);
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
        NULogger.getLogger().log(Level.INFO, "puccookie : {0}", puccookie);
    }

    private void fileUpload() throws Exception {
        NULogger.getLogger().info("now file upload code");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("https://dl-web.dropbox.com/upload");
        httppost.setHeader("Referer", "https://www.dropbox.com/home/Public");
        httppost.setHeader("Cookie", DropBoxAccount.getForumjarcookie() + ";" + DropBoxAccount.getForumlidcookie() + ";" + DropBoxAccount.getTouchcookie());
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("t", new StringBody(token));
        mpEntity.addPart("plain", new StringBody("yes"));
        mpEntity.addPart("dest", new StringBody("/Public"));
        mpEntity.addPart("file", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        NULogger.getLogger().info("Now uploading your file into dropbox.com");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
        if (uploadresponse.contains("The resource was found at https://www.dropbox.com/home/Public")) {
            downloadlink = "http://dl.dropbox.com/u/" + uid + "/" + (URLEncoder.encode(file.getName(), "UTF-8").replace("+", "%20"));
            NULogger.getLogger().log(Level.INFO, "Downloadlink : {0}", downloadlink);
            downURL = downloadlink;
        } else {
            throw new Exception("Dropbox server problem or network problem.. Couldn't get proper response.");
        }
    }

    public void run() {
        if (dropBoxAccount.loginsuccessful) {
            host = dropBoxAccount.username + " | DropBox.com";
        } else {
            host = "DropBox.com";
            uploadFailed();
            return;
        }
        if (file.length() > 300 * 1024 * 1024) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>300MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        try {
            status = UploadStatus.INITIALISING;
            getData();
            fileUpload();
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(DropBox.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
