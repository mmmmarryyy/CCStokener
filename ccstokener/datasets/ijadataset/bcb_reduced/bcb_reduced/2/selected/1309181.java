package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.ZohoDocsAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class ZohoDocs extends AbstractUploader implements UploaderAccountNecessary {

    ZohoDocsAccount zohoDocsAccount = (ZohoDocsAccount) AccountsManager.getAccount("ZohoDocs.com");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    public ZohoDocs(File file) {
        super(file);
        downURL = UploadStatus.NA.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "ZohoDocs.com";
        if (zohoDocsAccount.loginsuccessful) {
            host = zohoDocsAccount.username + " | ZohoDocs.com";
        }
    }

    public void run() {
        try {
            if (zohoDocsAccount.loginsuccessful) {
                host = zohoDocsAccount.username + " | ZohoDocs.com";
            } else {
                host = "ZohoDocs.com";
                uploadFailed();
                return;
            }
            if (file.length() > 52428800) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>50MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpPost httppost = new HttpPost("https://docs.zoho.com/uploadsingle.do?isUploadStatus=false&folderId=0&refFileElementId=refFileElement0");
            httppost.setHeader("Cookie", ZohoDocsAccount.getZohodocscookies().toString());
            MultipartEntity mpEntity = new MultipartEntity();
            mpEntity.addPart("multiupload_file", new MonitoredFileBody(file, uploadProgress));
            httppost.setEntity(mpEntity);
            NULogger.getLogger().info("Now uploading your file into zoho docs...........................");
            status = UploadStatus.UPLOADING;
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            NULogger.getLogger().info(response.getStatusLine().toString());
            if (resEntity != null) {
                if (EntityUtils.toString(resEntity).contains("File Uploaded Successfully")) {
                    NULogger.getLogger().info("File Uploaded Successfully");
                    uploadFinished();
                } else {
                    throw new Exception("There might be a problem with your internet connection or server error. Please try after some time. :(");
                }
            }
        } catch (Exception e) {
            Logger.getLogger(RapidShare.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
