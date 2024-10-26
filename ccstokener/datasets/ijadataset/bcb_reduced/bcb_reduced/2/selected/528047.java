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
import neembuuuploader.accounts.SlingFileAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author dinesh
 */
public class SlingFile extends AbstractUploader implements UploaderAccountNecessary {

    SlingFileAccount slingFileAccount = (SlingFileAccount) AccountsManager.getAccount("SlingFile.com");

    private HttpURLConnection uc;

    private BufferedReader br;

    private URL u;

    private String sling_guest_url = "";

    private String ssd = "";

    private String postURL = "";

    private String postuploadpage = "";

    private StringBuilder slingfilecookie;

    private String downloadlink = "";

    private String deletelink = "";

    public SlingFile(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "SlingFile.com";
        if (slingFileAccount.loginsuccessful) {
            host = slingFileAccount.username + " | SlingFile.com";
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("After login,geting the link again :)");
        u = new URL("http://www.slingfile.com/");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", SlingFileAccount.getSlingfilecookie().toString());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        sling_guest_url = CommonUploaderTasks.parseResponse(k, "single-premium?uu=", "'");
        NULogger.getLogger().log(Level.INFO, "sling guest url : {0}", sling_guest_url);
        ssd = CommonUploaderTasks.parseResponse(sling_guest_url, "&ssd=", "&rd");
        NULogger.getLogger().log(Level.INFO, "SSD : {0}", ssd);
        postURL = CommonUploaderTasks.parseResponse(sling_guest_url, "http://", "&ssd");
        postURL = "http://" + postURL;
        NULogger.getLogger().log(Level.INFO, "Post URL : {0}", postURL);
        postuploadpage = sling_guest_url.substring(sling_guest_url.indexOf("&rd=") + 4);
        NULogger.getLogger().log(Level.INFO, "post upload page : {0}", postuploadpage);
        slingfilecookie = SlingFileAccount.getSlingfilecookie();
        NULogger.getLogger().info(slingfilecookie.toString());
    }

    public void run() {
        try {
            if (slingFileAccount.loginsuccessful) {
                host = slingFileAccount.username + " | SlingFile.com";
            } else {
                host = "SlingFile.com";
                uploadFailed();
                return;
            }
            if (file.length() > 2147483648l) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            initialize();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(postURL);
            MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            mpEntity.addPart("Filename", new StringBody(file.getName()));
            mpEntity.addPart("ssd", new StringBody(ssd));
            mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
            httppost.setEntity(mpEntity);
            NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
            NULogger.getLogger().info("Now uploading your file into slingfile.com");
            status = UploadStatus.UPLOADING;
            HttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("There might be a problem with your internet connectivity or server problem. Please try again some after time. :(");
            }
            status = UploadStatus.GETTINGLINK;
            NULogger.getLogger().info("Getting download & delete links......");
            u = new URL(postuploadpage);
            uc = (HttpURLConnection) u.openConnection();
            uc.setRequestProperty("Cookie", slingfilecookie.toString());
            uc.setRequestProperty("Referer", sling_guest_url);
            NULogger.getLogger().info(String.valueOf(uc.getResponseCode()));
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                k += temp;
            }
            downloadlink = CommonUploaderTasks.parseResponse(k, "this.select();", "\" type=\"text\"");
            downloadlink = downloadlink.replace("\" value=\"", "");
            NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
            downURL = downloadlink;
            deletelink = CommonUploaderTasks.parseResponse(k, "Delete Link:", "\" type=\"text");
            deletelink = deletelink.substring(deletelink.indexOf("http://"));
            NULogger.getLogger().log(Level.INFO, "Delete link : {0}", deletelink);
            delURL = deletelink;
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
