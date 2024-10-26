package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class TwoShared extends AbstractUploader {

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL;

    private String uploadID;

    private String downloadlink;

    private String adminURL;

    public TwoShared(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "2Shared.com";
    }

    public void getPostURL() throws Exception {
        NULogger.getLogger().info("Gettign File upload URL");
        postURL = getData("http://www.2shared.com");
        postURL = postURL.substring(postURL.indexOf("action=\""));
        postURL = postURL.replace("action=\"", "");
        postURL = postURL.substring(0, postURL.indexOf("\""));
        NULogger.getLogger().info(postURL);
        uploadID = postURL.substring(postURL.indexOf("sId="));
        NULogger.getLogger().info(uploadID);
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestMethod("GET");
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

    public void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("fff", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(reqEntity);
        NULogger.getLogger().info("Now uploading your file into 2shared.com. Please wait......................");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            String page = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "PAGE :{0}", page);
        }
    }

    public void run() {
        try {
            if (file.length() > 100 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>100MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            getPostURL();
            fileUpload();
            NULogger.getLogger().info("Getting download URL....");
            String tmp = getData("http://www.2shared.com/uploadComplete.jsp?" + uploadID);
            downloadlink = tmp;
            adminURL = tmp;
            NULogger.getLogger().info("Upload complete. Please wait......................");
            downloadlink = downloadlink.substring(downloadlink.indexOf("action=\""));
            downloadlink = downloadlink.replace("action=\"", "");
            downloadlink = downloadlink.substring(0, downloadlink.indexOf("\""));
            NULogger.getLogger().log(Level.INFO, "File download  link : {0}", downloadlink);
            adminURL = adminURL.replace("<form action=\"" + downloadlink, "");
            adminURL = adminURL.substring(adminURL.indexOf("action=\""));
            adminURL = adminURL.replace("action=\"", "");
            adminURL = adminURL.substring(0, adminURL.indexOf("\""));
            NULogger.getLogger().log(Level.INFO, "File adminstration link : {0}", adminURL);
            downURL = downloadlink;
            delURL = adminURL;
            uploadFinished();
        } catch (Exception ex) {
            NULogger.getLogger().severe(ex.toString());
            uploadFailed();
        }
    }
}
