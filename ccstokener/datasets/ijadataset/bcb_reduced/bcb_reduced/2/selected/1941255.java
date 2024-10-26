package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.CrockoAccount;
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
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class Crocko extends AbstractUploader {

    CrockoAccount crockoAccount = (CrockoAccount) AccountsManager.getAccount("Crocko.com");

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL;

    private String uploadresponse;

    private String downloadlink;

    private URL u;

    private String deletelink;

    public Crocko(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "Crocko.com";
        if (crockoAccount.loginsuccessful) {
            host = crockoAccount.username + " | Crocko.com";
        }
    }

    public void run() {
        try {
            if (crockoAccount.loginsuccessful) {
                host = crockoAccount.username + " | Crocko.com";
            } else {
                host = "Crocko.com";
            }
            if (file.length() > 2147483648l) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            if (crockoAccount.loginsuccessful) {
                getData();
            } else {
                initialize();
            }
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(postURL);
            MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            mpEntity.addPart("Filename", new StringBody(file.getName()));
            if (crockoAccount.loginsuccessful) {
                NULogger.getLogger().info("adding php sess .............");
                mpEntity.addPart("PHPSESSID", new StringBody(CrockoAccount.getSessionid()));
            }
            mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
            httppost.setEntity(mpEntity);
            NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
            NULogger.getLogger().info("Now uploading your file into crocko");
            status = UploadStatus.UPLOADING;
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            NULogger.getLogger().info(response.getStatusLine().toString());
            if (resEntity != null) {
                uploadresponse = EntityUtils.toString(resEntity);
            } else {
                throw new Exception("There might be a problem with your internet connectivity or server error. Please try again later :(");
            }
            status = UploadStatus.GETTINGLINK;
            NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
            downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "Download link:", "</dd>");
            downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "value=\"", "\"");
            deletelink = CommonUploaderTasks.parseResponse(uploadresponse, "Delete link:", "</a></dd>");
            deletelink = deletelink.substring(deletelink.indexOf("http://"));
            NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
            NULogger.getLogger().log(Level.INFO, "Delete link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
            httpclient.getConnectionManager().shutdown();
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        } finally {
            uc = null;
            br = null;
            postURL = null;
            uploadresponse = null;
            downloadlink = null;
            u = null;
            deletelink = null;
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting upload url of crocko........");
        u = new URL("http://crocko.com/");
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        postURL = CommonUploaderTasks.parseResponse(k, "upload_url : \"", "\"");
        NULogger.getLogger().log(Level.INFO, "Post URL : {0}", postURL);
        uc.disconnect();
    }

    private void getData() throws Exception {
        u = new URL("http://www.crocko.com/accounts/upload");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", CrockoAccount.getCookies().toString());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        postURL = CommonUploaderTasks.parseResponse(k, "upload_url : \"", "\"");
        NULogger.getLogger().log(Level.INFO, "Post URL : {0}", postURL);
        uc.disconnect();
    }
}
