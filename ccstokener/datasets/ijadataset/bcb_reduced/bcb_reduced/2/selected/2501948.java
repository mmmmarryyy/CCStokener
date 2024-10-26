package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.UploadingDotComAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class UploadingDotCom extends AbstractUploader implements UploaderAccountNecessary {

    UploadingDotComAccount uploadingDotComAccount = (UploadingDotComAccount) AccountsManager.getAccount("Uploading.com");

    private URL u;

    private BufferedReader br;

    private String tmp = "", sidcookie = "", timecookie = "", cachecookie = "", ucookie = "";

    private String uploadresponse = "", uploadinglink = "", postURL = "", sid = "";

    private String afterloginpage = "";

    private String downloadlink = "";

    private HttpURLConnection uc;

    private String fileID;

    public UploadingDotCom(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "Uploading.com";
        if (uploadingDotComAccount.loginsuccessful) {
            host = uploadingDotComAccount.username + " | Uploading.com";
        }
    }

    private String getData() throws Exception {
        u = new URL("http://www.uploading.com");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", UploadingDotComAccount.getSidcookie() + ";" + UploadingDotComAccount.getUcookie() + ";" + UploadingDotComAccount.getCachecookie() + ";" + UploadingDotComAccount.getTimecookie());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        return k;
    }

    private void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", UploadingDotComAccount.getSidcookie() + ";" + UploadingDotComAccount.getUcookie() + ";" + UploadingDotComAccount.getCachecookie() + ";" + UploadingDotComAccount.getTimecookie());
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("Filename", new StringBody(getFileName()));
        reqEntity.addPart("SID", new StringBody(sid));
        reqEntity.addPart("folder_id", new StringBody("0"));
        reqEntity.addPart("file", new StringBody(fileID));
        reqEntity.addPart("file", new MonitoredFileBody(file, uploadProgress));
        reqEntity.addPart("upload", new StringBody("Submit Query"));
        httppost.setEntity(reqEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into uploading.com. Please wait......................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "PAGE :{0}", uploadresponse);
            uploadresponse = CommonUploaderTasks.parseResponse(uploadresponse, "answer\":\"", "\"");
            downURL = downloadlink;
            uploadFinished();
        } else {
            throw new Exception("There might be a problem with your internet connection or server error. Please try after some time :(");
        }
    }

    private void getPreDownloadLink() throws Exception {
        DefaultHttpClient d = new DefaultHttpClient();
        HttpPost h = new HttpPost("http://uploading.com/files/generate/?ajax");
        h.setHeader("Cookie", sidcookie + ";" + ucookie + ";" + timecookie + ";" + cachecookie);
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("name", file.getName()));
        formparams.add(new BasicNameValuePair("size", String.valueOf(file.length())));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
        h.setEntity(entity);
        HttpResponse r = d.execute(h);
        HttpEntity e = r.getEntity();
        downloadlink = EntityUtils.toString(e);
        fileID = CommonUploaderTasks.parseResponse(downloadlink, "file_id\":\"", "\"");
        downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "\"link\":\"", "\"");
        NULogger.getLogger().log(Level.INFO, "File ID : {0}", fileID);
        NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
    }

    public void run() {
        try {
            if (uploadingDotComAccount.loginsuccessful) {
                host = uploadingDotComAccount.username + " | Uploading.com";
            } else {
                host = "Uploading.com";
                uploadFailed();
                return;
            }
            if (file.length() > 2147483648L) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            afterloginpage = getData();
            uploadinglink = CommonUploaderTasks.parseResponse(afterloginpage, "upload_url\":\"", "\"");
            uploadinglink = uploadinglink.replaceAll("\\\\", "");
            NULogger.getLogger().log(Level.INFO, "New Upload link : {0}", uploadinglink);
            postURL = uploadinglink;
            sid = CommonUploaderTasks.parseResponse(afterloginpage, "SID: '", "'");
            NULogger.getLogger().log(Level.INFO, "New sid from site : {0}", sid);
            getPreDownloadLink();
            fileUpload();
        } catch (Exception e) {
            Logger.getLogger(UploadingDotCom.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
