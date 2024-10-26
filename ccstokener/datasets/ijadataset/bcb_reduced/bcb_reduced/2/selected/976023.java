package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.BayFilesAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class BayFiles extends AbstractUploader {

    BayFilesAccount bayFilesAccount = (BayFilesAccount) AccountsManager.getAccount("BayFiles.com");

    private URL u;

    private HttpURLConnection uc;

    private String tmp;

    private BufferedReader br;

    private String postURL;

    private String uploadresponse;

    private String downloadlink;

    private String deletelink;

    public BayFiles(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "BayFiles.com";
        if (bayFilesAccount.loginsuccessful) {
            host = bayFilesAccount.username + " | BayFiles.com";
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting upload url from bayfiles.com");
        u = new URL("http://bayfiles.com/ajax_upload?_=" + new Date().getTime());
        uc = (HttpURLConnection) u.openConnection();
        if (bayFilesAccount.loginsuccessful) {
            uc.setRequestProperty("Cookie", BayFilesAccount.getSessioncookie());
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        postURL = CommonUploaderTasks.parseResponse(k, "\"upload_url\":\"", "\"");
        postURL = postURL.replaceAll("\\\\", "");
        NULogger.getLogger().log(Level.INFO, "Post URL : {0}", postURL);
    }

    private void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("file", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into bayfiles.com");
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
        downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "\"downloadUrl\":\"", "\"");
        downloadlink = downloadlink.replaceAll("\\\\", "");
        deletelink = CommonUploaderTasks.parseResponse(uploadresponse, "\"deleteUrl\":\"", "\"");
        deletelink = deletelink.replaceAll("\\\\", "");
        NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
        NULogger.getLogger().log(Level.INFO, "Delete link : {0}", deletelink);
        downURL = downloadlink;
        delURL = deletelink;
        uploadFinished();
    }

    public void run() {
        if (bayFilesAccount.loginsuccessful) {
            host = bayFilesAccount.username + " | BayFiles.com";
        } else {
            host = "Bayfiles.com";
        }
        long uploadlimit;
        if (bayFilesAccount.loginsuccessful) {
            uploadlimit = 524288000;
        } else {
            uploadlimit = 262144000;
        }
        if (file.length() > uploadlimit) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>" + uploadlimit + "</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        try {
            status = UploadStatus.INITIALISING;
            initialize();
            fileUpload();
        } catch (Exception e) {
            NULogger.getLogger().severe(e.toString());
            uploadFailed();
        } finally {
            u = null;
            uc = null;
            tmp = null;
            br = null;
            postURL = null;
            uploadresponse = null;
            downloadlink = null;
            deletelink = null;
        }
    }
}
