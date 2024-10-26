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
import neembuuuploader.accounts.OneFichierAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

/**
 *
 * @author dinesh
 */
public class OneFichier extends AbstractUploader {

    OneFichierAccount oneFichierAccount = (OneFichierAccount) AccountsManager.getAccount("1fichier.com");

    final String UPLOAD_ID_CHARS = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

    private HttpURLConnection uc;

    private BufferedReader br;

    private String uploadresponse;

    private String downloadlink;

    private URL u;

    private String deletelink;

    private String uid;

    public OneFichier(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "1fichier.com";
        if (oneFichierAccount.loginsuccessful) {
            host = oneFichierAccount.username + " | 1fichier.com";
        }
    }

    public void generateOneFichierID() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int idx = 1 + (int) (Math.random() * 51);
            sb.append(UPLOAD_ID_CHARS.charAt(idx));
        }
        uid = sb.toString();
        NULogger.getLogger().log(Level.INFO, "uid : {0}", uid);
    }

    public void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost("http://upload.1fichier.com/en/upload.cgi?id=" + uid);
        if (oneFichierAccount.loginsuccessful) {
            httppost.setHeader("Cookie", OneFichierAccount.getSidcookie());
        }
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("file[]", new MonitoredFileBody(file, uploadProgress));
        mpEntity.addPart("domain", new StringBody("0"));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().info("Now uploading your file into 1fichier...........................");
        NULogger.getLogger().log(Level.INFO, "Now executing.......{0}", httppost.getRequestLine());
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        httpclient.getConnectionManager().shutdown();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (response.containsHeader("Location")) {
            uploadresponse = response.getFirstHeader("Location").getValue();
            NULogger.getLogger().log(Level.INFO, "Upload location link : {0}", uploadresponse);
        } else {
            throw new Exception("There might be a problem with your internet connection or server error. Please try again");
        }
    }

    public void run() {
        try {
            if (oneFichierAccount.loginsuccessful) {
                host = oneFichierAccount.username + " | 1fichier.com";
            } else {
                host = "1fichier.com";
            }
            if (file.length() > 10995116277760L) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>10GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            generateOneFichierID();
            fileUpload();
            NULogger.getLogger().info("Getting file links.............");
            u = new URL("http://upload.1fichier.com" + uploadresponse);
            uc = (HttpURLConnection) u.openConnection();
            if (oneFichierAccount.loginsuccessful) {
                uc.setRequestProperty("Cookie", OneFichierAccount.getSidcookie());
            }
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                k += temp;
            }
            downloadlink = CommonUploaderTasks.parseResponse(k, "<td><a href=\"", "\"");
            deletelink = CommonUploaderTasks.parseResponse(k, "http://www.1fichier.com/en/remove/", "<");
            deletelink = "http://www.1fichier.com/en/remove/" + deletelink;
            NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
            NULogger.getLogger().log(Level.INFO, "Delete link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
