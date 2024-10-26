package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
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
public class ShareSend extends AbstractUploader {

    private URL u;

    private BufferedReader br;

    private String tmp, sharesendlink = "";

    private String postURL;

    private String uploadresponse = "";

    private String downloadlink = "";

    private HttpURLConnection uc;

    public ShareSend(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "ShareSend.com";
    }

    private void initialize() throws IOException, Exception {
        NULogger.getLogger().info("Getting startup cookie from sharesend.com");
        u = new URL("http://sharesend.com/");
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(u.openStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        NULogger.getLogger().info("Getting sharesend dynamic upload link");
        sharesendlink = CommonUploaderTasks.parseResponse(k, "action=\"", "\"");
        NULogger.getLogger().info(sharesendlink);
    }

    private void fileUpload() throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("Filename", new StringBody(file.getName()));
        mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        NULogger.getLogger().info("Now uploading your file into sharesend.com");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        NULogger.getLogger().log(Level.INFO, "Upload Response : {0}", uploadresponse);
        NULogger.getLogger().log(Level.INFO, "Download Link : http://sharesend.com/{0}", uploadresponse);
        downloadlink = "http://sharesend.com/" + uploadresponse;
        downURL = downloadlink;
        httpclient.getConnectionManager().shutdown();
        uploadFinished();
    }

    public void run() {
        try {
            if (file.length() > 100 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>100MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            initialize();
            postURL = sharesendlink + "?flash=1";
            fileUpload();
        } catch (Exception ex) {
            NULogger.getLogger().severe(ex.toString());
            uploadFailed();
        }
    }
}
