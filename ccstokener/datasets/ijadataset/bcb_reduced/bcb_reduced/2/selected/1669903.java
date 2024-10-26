package neembuuuploader.uploaders;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.ScribdAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NeembuuUploaderProperties;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class Scribd extends AbstractUploader implements UploaderAccountNecessary {

    ScribdAccount scribdAccount = (ScribdAccount) AccountsManager.getAccount("Scribd.com");

    private String doc_id;

    private String SCRIBD_API_KEY = "5t8cd1k0ww3iupw31bb2a";

    private String SCRIBD_API_SIGNATURE = "sec-b27x1xqgbvhrtccudzeh0s709n";

    private String SCRIBD_UPLOAD_URL = "http://api.scribd.com/api?method=docs.upload&api_key=" + SCRIBD_API_KEY + "&api_sig=" + SCRIBD_API_SIGNATURE + "&session_key" + NeembuuUploaderProperties.getEncryptedProperty("scribd_session_key");

    public Scribd(File file) {
        super(file);
        downURL = UploadStatus.NA.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "Scribd.com";
        if (scribdAccount.loginsuccessful) {
            host = scribdAccount.username + " | Scribd.com";
        }
    }

    @Override
    public void run() {
        String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        ext = ext.toLowerCase();
        String[] supported = new String[] { "pdf", "ps", "doc", "docx", "ppt", "pptx", "pps", "ppsx", "xls", "xlsx", "odt", "sxw", "odp", "sxi", "ods", "sxc", "txt", "rtf", "tif", "tiff", "otg", "otf", "sxd" };
        boolean isFileTypeSupported = false;
        for (int i = 0; i < supported.length; i++) {
            if (ext.equals(supported[i])) {
                isFileTypeSupported = true;
                break;
            }
        }
        if (!isFileTypeSupported) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.filetypenotsupported") + ": <b>" + ext + "</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        if (file.length() > 104857600) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>100MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        status = UploadStatus.INITIALISING;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(SCRIBD_UPLOAD_URL);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("file", new MonitoredFileBody(file, uploadProgress));
            httppost.setEntity(reqEntity);
            System.out.println("Now uploading your file into scribd........ Please wait......................");
            status = UploadStatus.UPLOADING;
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                doc_id = EntityUtils.toString(resEntity);
                System.out.println(doc_id);
                if (doc_id.contains("stat=\"ok\"")) {
                    doc_id = CommonUploaderTasks.parseResponse(doc_id, "<doc_id>", "</doc_id>");
                    System.out.println("doc id :" + doc_id);
                    uploadFinished();
                } else {
                    throw new Exception("There might be problem with your internet connection or server error. Please try again some after time :(");
                }
            } else {
                throw new Exception("There might be problem with your internet connection or server error. Please try again some after time :(");
            }
        } catch (Exception e) {
            Logger.getLogger(Scribd.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        } finally {
            doc_id = null;
            SCRIBD_API_KEY = null;
            SCRIBD_API_SIGNATURE = null;
            SCRIBD_UPLOAD_URL = null;
        }
    }
}
