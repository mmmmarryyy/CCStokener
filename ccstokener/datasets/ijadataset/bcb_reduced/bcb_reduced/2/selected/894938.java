package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.BadongoAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
public class Badongo extends AbstractUploader {

    BadongoAccount badongoAccount = (BadongoAccount) AccountsManager.getAccount("Badongo.com");

    private String UPLOAD_ID_CHARS = "1234567890qwertyuiopasdfghjklzxcvbnm";

    private HttpURLConnection uc;

    private BufferedReader br;

    private String uid;

    private String postURL;

    private String dataid;

    private String uploadresponse;

    private String downloadlink;

    public Badongo(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "Badongo.com";
        if (badongoAccount.loginsuccessful) {
            host = badongoAccount.username + " | Badongo.com";
        }
    }

    private String getData(String myurl) throws Exception {
        URL url = new URL(myurl);
        uc = (HttpURLConnection) url.openConnection();
        if (badongoAccount.loginsuccessful) {
            uc.setRequestProperty("Cookie", BadongoAccount.getUsercookie() + ";" + BadongoAccount.getPwdcookie());
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        return k;
    }

    public void run() {
        try {
            if (badongoAccount.loginsuccessful) {
                host = badongoAccount.username + " | Badongo.com";
            } else {
                host = "Badongo.com";
            }
            if (file.length() > 1024 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            generateBadongoID();
            postURL = "http://upload.badongo.com/mpu_upload_single.php?UL_ID=undefined&UPLOAD_IDENTIFIER=undefined&page=upload_s&s=&cou=en&PHPSESSID=" + uid + "&desc=";
            NULogger.getLogger().log(Level.INFO, "post : {0}", postURL);
            if (badongoAccount.loginsuccessful) {
                dataid = getData("http://upload.badongo.com/mpu.php?cou=en&k=member");
                dataid = CommonUploaderTasks.parseResponse(dataid, "\"PHPSESSID\" : \"", "\"");
                NULogger.getLogger().log(Level.INFO, "Data : {0}", dataid);
            }
            fileUpload();
            status = UploadStatus.GETTINGLINK;
            if (badongoAccount.loginsuccessful) {
                downloadlink = getData("http://upload.badongo.com/upload_complete.php?session=" + dataid);
                downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "msg_u=", "&");
            } else {
                downloadlink = getData("http://upload.badongo.com/upload_complete.php?page=upload_s_f&PHPSESSID=" + uid + "&url=undefined&url_kill=undefined&affliate=");
                downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "url=", "&");
            }
            downloadlink = URLDecoder.decode(downloadlink, "UTF-8");
            NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
            downURL = downloadlink;
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        } finally {
            UPLOAD_ID_CHARS = null;
            uc = null;
            br = null;
            uid = null;
            postURL = null;
            dataid = null;
            uploadresponse = null;
            downloadlink = null;
        }
    }

    private void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        if (badongoAccount.loginsuccessful) {
            postURL = "http://upload.badongo.com/mpu_upload.php";
        }
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("Filename", new StringBody(file.getName()));
        if (badongoAccount.loginsuccessful) {
            mpEntity.addPart("PHPSESSID", new StringBody(dataid));
        }
        mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        NULogger.getLogger().info("Now uploading your file into badongo.com");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        NULogger.getLogger().log(Level.INFO, "res {0}", uploadresponse);
        httpclient.getConnectionManager().shutdown();
    }

    public void generateBadongoID() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int idx = 1 + (int) (Math.random() * 35);
            sb.append(UPLOAD_ID_CHARS.charAt(idx));
        }
        uid = sb.toString();
    }
}
