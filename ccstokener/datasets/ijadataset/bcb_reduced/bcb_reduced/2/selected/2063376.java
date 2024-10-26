package neembuuuploader.uploaders;

import java.util.logging.Level;
import neembuuuploader.accounts.MediaFireAccount;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
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
public class MediaFire extends AbstractUploader {

    MediaFireAccount mediaFireAccount = (MediaFireAccount) AccountsManager.getAccount("MediaFire.com");

    private URL url;

    private URLConnection conn;

    private String ukeycookie;

    private String content;

    private PrintWriter pw;

    private HttpURLConnection uc;

    private BufferedReader br;

    private URL u;

    private String skeycookie = "";

    private String usercookie;

    private String myfiles;

    private String uploadkey;

    private String mfulconfig;

    private String postURL;

    private String uploadresponsekey;

    private String downloadlink;

    private boolean login = false;

    public MediaFire(File file) {
        super(file);
        host = "MediaFire.com";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        if (mediaFireAccount.loginsuccessful) {
            login = true;
            host = mediaFireAccount.username + " | MediaFire.com";
        }
    }

    public void run() {
        if (mediaFireAccount.loginsuccessful) {
            login = true;
            host = mediaFireAccount.username + " | MediaFire.com";
        } else {
            login = false;
            host = "MediaFire.com";
        }
        if (login) {
            uploadLogin();
        } else {
            uploadWithoutLogin();
        }
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www.mediafire.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://mediafire.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Cookie", ukeycookie + ";" + skeycookie + ";" + usercookie);
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

    public void initWithoutLogin() throws Exception {
        NULogger.getLogger().info("Getting uploader configuration details.............");
        String s = getData("http://www.mediafire.com/basicapi/uploaderconfiguration.php?45144");
        ukeycookie = CommonUploaderTasks.parseResponse(s, "<ukey>", "<");
        usercookie = CommonUploaderTasks.parseResponse(s, "<user>", "<");
        uploadkey = CommonUploaderTasks.parseResponse(s, "<folderkey>", "<");
        mfulconfig = CommonUploaderTasks.parseResponse(s, "<MFULConfig>", "<");
        postURL = "http://www.mediafire.com/douploadtoapi/?type=basic&ukey=" + ukeycookie + "&user" + usercookie + "&uploadkey=" + uploadkey + "&filenum=0&uploader=0&MFULConfig=" + mfulconfig;
    }

    public void getDownloadLink() throws Exception {
        status = UploadStatus.GETTINGLINK;
        do {
            downloadlink = getData("http://www.mediafire.com/basicapi/pollupload.php?key=" + uploadresponsekey + "&MFULConfig=" + mfulconfig);
        } while (!downloadlink.contains("No more"));
        downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "<quickkey>", "<");
        downloadlink = "http://www.mediafire.com/?" + downloadlink;
        NULogger.getLogger().log(Level.INFO, "download link is {0}", downloadlink);
        downURL = downloadlink;
    }

    public void getMyFilesLinks() throws Exception {
        myfiles = getData("http://www.mediafire.com/myfiles.php");
        myfiles = CommonUploaderTasks.parseResponse(myfiles, "LoadJS(\"", "\"");
    }

    public void getUploadKey() throws Exception {
        uploadkey = getData("http://www.mediafire.com" + myfiles);
        uploadkey = CommonUploaderTasks.parseResponse(uploadkey, "var zb='", "'");
    }

    public void getMFULConfig() throws Exception {
        mfulconfig = getData("http://www.mediafire.com/basicapi/uploaderconfiguration.php?45144");
        mfulconfig = CommonUploaderTasks.parseResponse(mfulconfig, "<MFULConfig>", "<");
    }

    public void getUploadResponseKey() {
        uploadresponsekey = CommonUploaderTasks.parseResponse(uploadresponsekey, "<key>", "<");
    }

    private void uploadLogin() {
        try {
            status = UploadStatus.INITIALISING;
            ukeycookie = MediaFireAccount.getUKeyCookie();
            skeycookie = MediaFireAccount.getSKeyCookie();
            usercookie = MediaFireAccount.getUserCookie();
            status = UploadStatus.GETTINGCOOKIE;
            NULogger.getLogger().log(Level.INFO, "uploadkey {0}", uploadkey);
            NULogger.getLogger().info("Getting MFULConfig value........");
            getMFULConfig();
            postURL = "http://www.mediafire.com/douploadtoapi/?type=basic&" + ukeycookie + "&" + usercookie + "&uploadkey=myfiles&filenum=0&uploader=0&MFULConfig=" + mfulconfig;
            status = UploadStatus.UPLOADING;
            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpPost httppost = new HttpPost(postURL);
            NULogger.getLogger().info(ukeycookie);
            httppost.setHeader("Cookie", ukeycookie + ";" + skeycookie + ";" + usercookie);
            MultipartEntity mpEntity = new MultipartEntity();
            MonitoredFileBody cbFile = new MonitoredFileBody(file, uploadProgress);
            mpEntity.addPart("", cbFile);
            httppost.setEntity(mpEntity);
            NULogger.getLogger().info("Now uploading your file into mediafire...........................");
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            NULogger.getLogger().info(response.getStatusLine().toString());
            if (resEntity != null) {
                NULogger.getLogger().info("Getting upload response key value..........");
                uploadresponsekey = EntityUtils.toString(resEntity);
                getUploadResponseKey();
                NULogger.getLogger().log(Level.INFO, "upload response key {0}", uploadresponsekey);
            }
            getDownloadLink();
            uploadFinished();
        } catch (Exception ex) {
            ex.printStackTrace();
            NULogger.getLogger().severe(ex.toString());
            uploadFailed();
        }
    }

    private void uploadWithoutLogin() {
        try {
            if (file.length() > 209715200) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>200MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            initWithoutLogin();
            status = UploadStatus.UPLOADING;
            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpPost httppost = new HttpPost(postURL);
            NULogger.getLogger().info(ukeycookie);
            httppost.setHeader("Cookie", ukeycookie + ";" + skeycookie + ";" + usercookie);
            MultipartEntity mpEntity = new MultipartEntity();
            MonitoredFileBody cbFile = new MonitoredFileBody(file, uploadProgress);
            mpEntity.addPart("", cbFile);
            httppost.setEntity(mpEntity);
            NULogger.getLogger().info("Now uploading your file into mediafire...........................");
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            NULogger.getLogger().info(response.getStatusLine().toString());
            if (resEntity != null) {
                NULogger.getLogger().info("Getting upload response key value..........");
                uploadresponsekey = EntityUtils.toString(resEntity);
                getUploadResponseKey();
                NULogger.getLogger().log(Level.INFO, "upload resoponse key {0}", uploadresponsekey);
            }
            getDownloadLink();
            uploadFinished();
        } catch (Exception ex) {
            ex.printStackTrace();
            uploadFailed();
        }
    }
}
