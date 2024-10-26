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
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.SendSpaceAccount;
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
public class SendSpace extends AbstractUploader {

    SendSpaceAccount sendSpaceAccount = (SendSpaceAccount) AccountsManager.getAccount("SendSpace.com");

    private URL u;

    private BufferedReader br;

    private HttpURLConnection uc;

    private String tmp;

    private String sidcookie = "", ssuicookie = "";

    private String sendspacelink;

    private String postURL;

    private String uploadid;

    private String destinationdir;

    private String signature;

    private String userid;

    private String uploadresponse;

    private String downloadlink;

    private String deletelink;

    public SendSpace(File file) {
        super(file);
        host = "SendSpace.com";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        if (sendSpaceAccount.loginsuccessful) {
            host = sendSpaceAccount.username + " | SendSpace.com";
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from sendspace.com");
        u = new URL("http://www.sendspace.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("SID")) {
                    sidcookie = tmp;
                }
                if (tmp.contains("ssui")) {
                    ssuicookie = tmp;
                }
            }
        }
        sidcookie = sidcookie.substring(0, sidcookie.indexOf(";"));
        ssuicookie = ssuicookie.substring(0, ssuicookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "sidcookie: {0}", sidcookie);
        NULogger.getLogger().log(Level.INFO, "ssuicookie: {0}", ssuicookie);
    }

    public void getDynamicSendSpaceValues() throws Exception {
        String k = "";
        if (sendSpaceAccount.loginsuccessful) {
            NULogger.getLogger().info("Getting sendspace page after login success");
            u = new URL("http://www.sendspace.com/");
            uc = (HttpURLConnection) u.openConnection();
            uc.setRequestProperty("Cookie", SendSpaceAccount.getSidcookie() + ";" + SendSpaceAccount.getSsuicookie() + ";" + SendSpaceAccount.getSsalcookie());
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            while ((tmp = br.readLine()) != null) {
                k += tmp;
            }
        } else {
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            while ((tmp = br.readLine()) != null) {
                k += tmp;
            }
        }
        NULogger.getLogger().info("Getting zshare dynamic upload link");
        sendspacelink = CommonUploaderTasks.parseResponse(k, "action=\"", "\"", true);
        NULogger.getLogger().log(Level.INFO, "sendspacelink : {0}", sendspacelink);
        postURL = sendspacelink;
        uploadid = CommonUploaderTasks.parseResponse(k, "\"UPLOAD_IDENTIFIER\" value=\"", "\"", false);
        NULogger.getLogger().log(Level.INFO, "uploadid : {0}", uploadid);
        destinationdir = CommonUploaderTasks.parseResponse(k, "\"DESTINATION_DIR\"	value=\"", "\"", false);
        NULogger.getLogger().log(Level.INFO, "destinationdir : {0}", destinationdir);
        signature = CommonUploaderTasks.parseResponse(k, "\"signature\" value=\"", "\"", false);
        NULogger.getLogger().log(Level.INFO, "signature : {0}", signature);
        if (sendSpaceAccount.loginsuccessful) {
            userid = CommonUploaderTasks.parseResponse(k, "\"userid\" value=\"", "\"", false);
        }
    }

    private void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", sidcookie + ";" + ssuicookie);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("MAX_FILE_SIZE", new StringBody("314572800"));
        mpEntity.addPart("UPLOAD_IDENTIFIER", new StringBody(uploadid));
        mpEntity.addPart("DESTINATION_DIR", new StringBody(destinationdir));
        mpEntity.addPart("js_enabled", new StringBody("1"));
        mpEntity.addPart("signature", new StringBody(signature));
        mpEntity.addPart("upload_files", new StringBody(""));
        if (sendSpaceAccount.loginsuccessful) {
            mpEntity.addPart("userid", new StringBody(userid));
        }
        mpEntity.addPart("terms", new StringBody("1"));
        mpEntity.addPart("file[]", new StringBody(""));
        mpEntity.addPart("description[]", new StringBody(""));
        mpEntity.addPart("upload_file[]", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into sendspace.com");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            status = UploadStatus.GETTINGLINK;
            uploadresponse = EntityUtils.toString(resEntity);
        }
        downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "Download Link", "target", false);
        deletelink = CommonUploaderTasks.parseResponse(uploadresponse, "Delete File Link", "target", false);
        downloadlink = downloadlink.replaceAll("\\s+", " ");
        deletelink = deletelink.replaceAll("\\s+", " ");
        downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "<a href=\"", "\"", false);
        deletelink = CommonUploaderTasks.parseResponse(deletelink, "href=\"", "\"", false);
        NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
        NULogger.getLogger().log(Level.INFO, "Delete link : {0}", deletelink);
        downURL = downloadlink;
        delURL = deletelink;
        httpclient.getConnectionManager().shutdown();
        uploadFinished();
    }

    public void run() {
        try {
            if (sendSpaceAccount.loginsuccessful) {
                host = sendSpaceAccount.username + " | SendSpace.com";
            } else {
                host = "SendSpace.com";
            }
            if (file.length() > 300 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>300MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            if (!sendSpaceAccount.loginsuccessful) {
                initialize();
            }
            getDynamicSendSpaceValues();
            fileUpload();
        } catch (Exception e) {
            Logger.getLogger(SendSpace.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
