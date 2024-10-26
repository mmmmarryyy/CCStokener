package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.FileSonicAccount;
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
public class FileSonic extends AbstractUploader implements UploaderAccountNecessary {

    FileSonicAccount fileSonicAccount = (FileSonicAccount) AccountsManager.getAccount("FileSonic.com");

    private boolean login = false;

    private String uploadID = "";

    private String filesoniclink = "";

    private String postURL = "";

    private String linkID = "";

    private String downloadlink = "";

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String rolecookie = "", langcookie = "";

    private String uploadresponse;

    private String tmp;

    private String fsdomain;

    public FileSonic(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "FileSonic.com";
        if (fileSonicAccount.loginsuccessful) {
            login = true;
            host = fileSonicAccount.username + " | FileSonic.com";
        }
    }

    public void run() {
        try {
            if (file.length() > 1073741824) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            if (fileSonicAccount.loginsuccessful) {
                login = true;
                host = fileSonicAccount.username + " | FileSonic.com";
            }
            uploadID = "upload_" + new Date().getTime() + "_" + FileSonicAccount.getSessioncookie().replace("PHPSESSID", "") + "_" + Math.round(Math.random() * 90000);
            filesoniclink = FileSonicAccount.getFsdomain();
            status = UploadStatus.INITIALISING;
            NULogger.getLogger().info("Getting dynamic filesonic upload link value ........");
            postURL = filesoniclink.replace("www", "web.eu") + "?callbackUrl=" + filesoniclink + "/upload-completed/:uploadProgressId&X-Progress-ID=" + uploadID;
            NULogger.getLogger().log(Level.INFO, "post URL : {0}", postURL);
            fileUpload();
        } catch (Exception ex) {
            Logger.getLogger(FileSonic.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            uploadFailed();
        }
    }

    public void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", FileSonicAccount.getLangcookie() + ";" + FileSonicAccount.getSessioncookie() + ";" + FileSonicAccount.getMailcookie() + ";" + FileSonicAccount.getNamecookie() + ";" + FileSonicAccount.getRolecookie() + ";");
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("files[]", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into filesonic...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            tmp = EntityUtils.toString(resEntity);
        }
        uploadresponse = response.getLastHeader("Location").getValue();
        NULogger.getLogger().log(Level.INFO, "Upload response URL : {0}", uploadresponse);
        uploadresponse = getData(uploadresponse);
        if (uploadresponse.contains("File was successfully uploaded")) {
            NULogger.getLogger().info("File was successfully uploaded :)");
            uploadFinished();
        } else {
            throw new Exception("There might be a problem with your internet connecivity or server error. Please try after some time. :(");
        }
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", filesoniclink);
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://filesonic.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        uc.setRequestProperty("Cookie", FileSonicAccount.getLangcookie() + ";" + FileSonicAccount.getSessioncookie() + ";" + FileSonicAccount.getMailcookie() + ";" + FileSonicAccount.getNamecookie() + ";" + FileSonicAccount.getRolecookie());
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
