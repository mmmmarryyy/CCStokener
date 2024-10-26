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
public class ZippyShare extends AbstractUploader {

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL;

    private String downloadlink;

    private String tmp;

    private String uploadresponse;

    private String zippylink;

    public ZippyShare(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "ZippyShare.com";
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting zippyshare dynamic upload link");
        u = new URL("http://www.zippyshare.com/");
        uc = (HttpURLConnection) u.openConnection();
        br = new BufferedReader(new InputStreamReader(u.openStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        zippylink = CommonUploaderTasks.parseResponse(k, "var server = '", "'");
        zippylink = zippylink.toLowerCase();
        NULogger.getLogger().info(zippylink);
    }

    private void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("Filename", new StringBody(file.getName()));
        mpEntity.addPart("notprivate", new StringBody("false"));
        mpEntity.addPart("folder", new StringBody("/"));
        mpEntity.addPart("Filedata", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into zippyshare.com");
        HttpResponse response = httpclient.execute(httppost);
        status = UploadStatus.GETTINGLINK;
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
            downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "value=\"http://", "\"");
            downloadlink = "http://" + downloadlink;
            NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
            downURL = downloadlink;
            httpclient.getConnectionManager().shutdown();
        } else {
            throw new Exception("ZippyShare server problem or Internet connectivity problem");
        }
    }

    public void run() {
        if (file.length() > 200 * 1024 * 1024) {
            JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>200MB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            uploadFailed();
            return;
        }
        try {
            status = UploadStatus.INITIALISING;
            initialize();
            postURL = "http://" + zippylink + ".zippyshare.com/upload";
            fileUpload();
            uploadFinished();
        } catch (Exception e) {
            NULogger.getLogger().severe(e.toString());
            uploadFailed();
        }
    }
}
