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
import neembuuuploader.accounts.DepositFilesAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class DepositFiles extends AbstractUploader {

    DepositFilesAccount depositFilesAccount = (DepositFilesAccount) AccountsManager.getAccount("DepositFiles.com");

    private String UPLOAD_ID_CHARS = "1234567890qwertyuiopasdfghjklzxcvbnm";

    private boolean login;

    private String postURL = "";

    private String uid = "";

    private URL u;

    private HttpURLConnection uc;

    private String uploadresponse = "";

    private String downloadlink = "";

    private String deletelink = "";

    private String uprandcookie = "";

    private BufferedReader br;

    public DepositFiles(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "DepositFiles.com";
        if (depositFilesAccount.loginsuccessful) {
            login = true;
            host = depositFilesAccount.username + " | DepositFiles.com";
        }
    }

    public void run() {
        try {
            if (depositFilesAccount.loginsuccessful) {
                login = true;
                host = depositFilesAccount.username + " | DepositFiles.com";
            } else {
                login = false;
                host = "DepositFiles.com";
            }
            if (!depositFilesAccount.loginsuccessful) {
                if (file.length() > 314572800) {
                    JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>300MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                    uploadFailed();
                    return;
                }
            } else {
                if (file.length() > 2147483648l) {
                    JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                    uploadFailed();
                    return;
                }
            }
            status = UploadStatus.INITIALISING;
            NULogger.getLogger().info("Now getting deposifiles page post action value........");
            postURL = getData("http://www.depositfiles.com/en/");
            postURL = CommonUploaderTasks.parseResponse(postURL, "file_upload_action = '", "'");
            postURL = postURL.substring(0, postURL.indexOf("=") + 1);
            generateDepositFilesID();
            NULogger.getLogger().log(Level.INFO, "Post URL  : {0}", postURL);
            fileUpload();
            uploadFinished();
        } catch (Exception ex) {
            NULogger.getLogger().severe(ex.toString());
            uploadFailed();
        } finally {
            UPLOAD_ID_CHARS = null;
            postURL = null;
            uid = null;
            u = null;
            uc = null;
            uploadresponse = null;
            downloadlink = null;
            deletelink = null;
            uprandcookie = null;
            br = null;
        }
    }

    public void fileUpload() throws Exception {
        status = UploadStatus.UPLOADING;
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", DepositFilesAccount.getUprandcookie() + ";" + DepositFilesAccount.getAutologincookie());
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new MonitoredFileBody(file, uploadProgress);
        mpEntity.addPart("MAX_FILE_SIZE", new StringBody("2097152000"));
        mpEntity.addPart("UPLOAD_IDENTIFIER", new StringBody(uid));
        mpEntity.addPart("go", new StringBody("1"));
        mpEntity.addPart("files", cbFile);
        httppost.setEntity(mpEntity);
        NULogger.getLogger().info("Now uploading your file into depositfiles...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            status = UploadStatus.GETTINGLINK;
            uploadresponse = EntityUtils.toString(resEntity);
            downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "ud_download_url = '", "'");
            deletelink = CommonUploaderTasks.parseResponse(uploadresponse, "ud_delete_url = '", "'");
            NULogger.getLogger().log(Level.INFO, "download link : {0}", downloadlink);
            NULogger.getLogger().log(Level.INFO, "delete link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
        } else {
            throw new Exception();
        }
    }

    public void generateDepositFilesID() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date().getTime() / 1000);
        for (int i = 0; i < 32; i++) {
            int idx = 1 + (int) (Math.random() * 35);
            sb.append(UPLOAD_ID_CHARS.charAt(idx));
        }
        uid = sb.toString();
        postURL += sb.toString();
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www.depositfiles.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://depositfiles.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Cookie", uprandcookie);
        uc.setRequestMethod("GET");
        uc.setInstanceFollowRedirects(false);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        u = null;
        uc = null;
        return k;
    }
}
