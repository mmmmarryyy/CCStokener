package neembuuuploader.uploaders;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.FourSharedAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author dinesh
 */
public class FourShared extends AbstractUploader implements UploaderAccountNecessary {

    FourSharedAccount fourSharedAccount = (FourSharedAccount) AccountsManager.getAccount("4Shared.com");

    private String downloadlink;

    public FourShared(File file) {
        super(file);
        host = "4Shared.com";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        if (fourSharedAccount.loginsuccessful) {
            host = fourSharedAccount.username + " | 4Shared.com";
        }
    }

    @Override
    public void run() {
        try {
            if (fourSharedAccount.loginsuccessful) {
                host = fourSharedAccount.username + " | 4Shared.com";
            } else {
                host = "4shared.com";
                uploadFailed();
                return;
            }
            if (file.length() > 2147483648L) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2048MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            if (!fourSharedAccount.da.hasRightUpload()) {
                throw new Exception("Uploading is temporarily disabled for your account :(. Conact 4shared support.");
            }
            long newFileId = fourSharedAccount.da.uploadStartFile(fourSharedAccount.getUsername(), fourSharedAccount.getPassword(), -1, file.getName(), file.length());
            NULogger.getLogger().log(Level.INFO, "File id : {0}", newFileId);
            String sessionKey = fourSharedAccount.da.createUploadSessionKey(fourSharedAccount.getUsername(), fourSharedAccount.getPassword(), -1);
            long dcId = fourSharedAccount.da.getNewFileDataCenter(fourSharedAccount.getUsername(), fourSharedAccount.getPassword());
            String url = fourSharedAccount.da.getUploadFormUrl((int) dcId, sessionKey);
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);
            MultipartEntity me = new MultipartEntity();
            StringBody rfid = new StringBody("" + newFileId);
            StringBody rfb = new StringBody("" + 0);
            me.addPart("resumableFileId", rfid);
            me.addPart("resumableFirstByte", rfb);
            me.addPart("FilePart", new MonitoredFileBody(file, uploadProgress));
            post.setEntity(me);
            status = UploadStatus.UPLOADING;
            NULogger.getLogger().info("Now uploading your file into 4Shared............");
            HttpResponse resp = client.execute(post);
            HttpEntity resEnt = resp.getEntity();
            String res = fourSharedAccount.da.uploadFinishFile(fourSharedAccount.getUsername(), fourSharedAccount.getPassword(), newFileId, DigestUtils.md5Hex(new FileInputStream(file)));
            if (res.isEmpty()) {
                NULogger.getLogger().info("File uploaded successfully :)");
                NULogger.getLogger().info("Now getting download link............");
                downloadlink = fourSharedAccount.da.getFileDownloadLink(fourSharedAccount.getUsername(), fourSharedAccount.getPassword(), newFileId);
                NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
                downURL = downloadlink;
                uploadFinished();
            } else {
                NULogger.getLogger().log(Level.INFO, "Upload failed: {0}", res);
                uploadFailed();
            }
            client.getConnectionManager().shutdown();
        } catch (Exception e) {
            Logger.getLogger(RapidShare.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
