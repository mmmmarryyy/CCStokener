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
import neembuuuploader.accounts.ImageShackAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
public class ImageShack extends AbstractUploader {

    ImageShackAccount imageShackAccount = (ImageShackAccount) AccountsManager.getAccount("ImageShack.us");

    private String downloadlink = "";

    private String ext = "";

    private boolean support = false;

    private URL u;

    private HttpURLConnection uc;

    private String upload_key;

    private BufferedReader br;

    public ImageShack(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "ImageShack.us";
        if (imageShackAccount.loginsuccessful) {
            host = imageShackAccount.username + " | ImageShack.us";
        }
    }

    private void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        ext = ext.toLowerCase();
        HttpPost httppost = null;
        if (ext.equals("jpeg") || ext.equals("jpg") || ext.equals("bmp") || ext.equals("gif") || ext.equals("png") || ext.equals("tiff")) {
            httppost = new HttpPost("http://www.imageshack.us/upload_api.php");
        }
        if (ext.equals("avi") || ext.equals("mkv") || ext.equals("mpeg") || ext.equals("mp4") || ext.equals("mov") || ext.equals("3gp") || ext.equals("flv") || ext.equals("3gpp")) {
            httppost = new HttpPost("http://render.imageshack.us/upload_api.php");
        }
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("Filename", new StringBody(file.getName()));
        reqEntity.addPart("optimage", new StringBody("1"));
        reqEntity.addPart("new_flash_uploader", new StringBody("y"));
        reqEntity.addPart("rembar", new StringBody("0"));
        reqEntity.addPart("myimages", new StringBody("null"));
        reqEntity.addPart("optsize", new StringBody("optimize"));
        reqEntity.addPart("rem_bar", new StringBody("0"));
        if (imageShackAccount.loginsuccessful) {
            reqEntity.addPart("isUSER", new StringBody(ImageShackAccount.getUsercookie()));
            reqEntity.addPart("myimages", new StringBody(ImageShackAccount.getMyimagescookie()));
        } else {
            reqEntity.addPart("isUSER", new StringBody("null"));
        }
        reqEntity.addPart("swfupload", new StringBody("1"));
        reqEntity.addPart("ulevel", new StringBody("null"));
        reqEntity.addPart("always_opt", new StringBody("null"));
        reqEntity.addPart("key", new StringBody(upload_key));
        reqEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
        reqEntity.addPart("upload", new StringBody("Submit Query"));
        httppost.setEntity(reqEntity);
        NULogger.getLogger().info("Now uploading your file into imageshack.us Please wait......................");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            downloadlink = EntityUtils.toString(resEntity);
            downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "<image_link>", "<");
            NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
            downURL = downloadlink;
            uploadFinished();
        } else {
            throw new Exception("Temporary ImageShack server problem or network problem");
        }
    }

    public void run() {
        if (imageShackAccount.loginsuccessful) {
            host = imageShackAccount.username + " | ImageShack.us";
        } else {
            host = "ImageShack.us";
        }
        ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        ext = ext.toLowerCase();
        String[] supported = new String[] { "jpeg", "jpg", "bmp", "gif", "png", "tiff", "avi", "mkv", "mpeg", "mp4", "mov", "3gp", "flv", "3gpp" };
        for (int i = 0; i < supported.length; i++) {
            if (ext.equals(supported[i])) {
                support = true;
                break;
            }
        }
        if (!support) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.filetypenotsupported") + ": <b>" + ext + "</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        if (ext.equals("jpeg") || ext.equals("jpg") || ext.equals("bmp") || ext.equals("gif") || ext.equals("png") || ext.equals("tiff")) {
            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>5MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
        }
        try {
            status = UploadStatus.INITIALISING;
            if (!imageShackAccount.loginsuccessful) {
                u = new URL("http://imageshack.us/");
                uc = (HttpURLConnection) u.openConnection();
                String k = "", tmp = "";
                br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                while ((tmp = br.readLine()) != null) {
                    k += tmp;
                }
                upload_key = CommonUploaderTasks.parseResponse(k, "name=\"key\" value=\"", "\"");
                NULogger.getLogger().log(Level.INFO, "upload_key : {0}", upload_key);
            } else {
                upload_key = imageShackAccount.getUpload_key();
            }
            fileUpload();
        } catch (Exception ex) {
            Logger.getLogger(ImageShack.class.getName()).log(Level.SEVERE, null, ex);
            uploadFailed();
        }
    }
}
