package neembuuuploader.uploaders;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.LocalhostrAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class Localhostr extends AbstractUploader {

    LocalhostrAccount localhostrAccount = (LocalhostrAccount) AccountsManager.getAccount("Localhostr.com");

    private String downloadlink;

    private String localhostrurl;

    public Localhostr(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "Localhostr.com";
        if (localhostrAccount.loginsuccessful) {
            host = localhostrAccount.username + " | Localhostr.com";
        }
    }

    public void run() {
        try {
            if (localhostrAccount.loginsuccessful) {
                host = localhostrAccount.username + " | Localhostr.com";
            } else {
                host = "Localhostr.com";
            }
            long length_limit;
            if (localhostrAccount.loginsuccessful) {
                length_limit = 1073741824;
            } else {
                length_limit = 52428800;
            }
            if (file.length() > length_limit) {
                if (localhostrAccount.loginsuccessful) {
                    JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>50MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                }
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            initialize();
            HttpOptions httpoptions = new HttpOptions(localhostrurl);
            DefaultHttpClient httpclient;
            httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpResponse myresponse = httpclient.execute(httpoptions);
            HttpEntity myresEntity = myresponse.getEntity();
            NULogger.getLogger().info(EntityUtils.toString(myresEntity));
            httpclient.getConnectionManager().shutdown();
            fileUpload();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookies from localhostr.com");
        HttpGet httpget = new HttpGet("http://localhostr.com/");
        if (localhostrAccount.loginsuccessful) {
            httpget.setHeader("Cookie", LocalhostrAccount.getSessioncookie());
        }
        DefaultHttpClient httpclient;
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpResponse myresponse = httpclient.execute(httpget);
        HttpEntity myresEntity = myresponse.getEntity();
        localhostrurl = EntityUtils.toString(myresEntity);
        localhostrurl = CommonUploaderTasks.parseResponse(localhostrurl, "url : '", "'");
        NULogger.getLogger().log(Level.INFO, "Localhost url : {0}", localhostrurl);
        InputStream is = myresponse.getEntity().getContent();
        is.close();
    }

    public void fileUpload() throws Exception {
        DefaultHttpClient httpclient;
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(localhostrurl);
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("name", new StringBody(file.getName()));
        if (localhostrAccount.loginsuccessful) {
            mpEntity.addPart("session", new StringBody(LocalhostrAccount.getSessioncookie().substring(LocalhostrAccount.getSessioncookie().indexOf("=") + 2)));
        }
        mpEntity.addPart("file", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into localhostr...........................");
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        httpclient.getConnectionManager().shutdown();
        if (resEntity != null) {
            String tmp = EntityUtils.toString(resEntity);
            downloadlink = CommonUploaderTasks.parseResponse(tmp, "\"url\":\"", "\"");
            NULogger.getLogger().log(Level.INFO, "download link : {0}", downloadlink);
            downURL = downloadlink;
            uploadFinished();
        } else {
            throw new Exception("There might be a problem with your internet connection. Please try after some time. :(");
        }
    }
}
