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
import neembuuuploader.accounts.GRuploadAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class GRupload extends AbstractUploader {

    GRuploadAccount gruploadAccount = (GRuploadAccount) AccountsManager.getAccount("GRupload.com");

    private HttpURLConnection uc;

    private BufferedReader br;

    private String uploadresponse;

    private String downloadlink;

    private URL u;

    private String deletelink;

    private static String gruploadlink;

    private long UploadID;

    private String tmpserver;

    private String fnvalue;

    public GRupload(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "GRupload.com";
        if (gruploadAccount.loginsuccessful) {
            host = gruploadAccount.username + " | GRupload.com";
        }
    }

    public void run() {
        try {
            if (gruploadAccount.loginsuccessful) {
                host = gruploadAccount.username + " | GRupload.com";
            } else {
                host = "GRupload.com";
            }
            if (file.length() > 1073741824l) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            initialize();
            fileUpload();
            status = UploadStatus.GETTINGLINK;
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            HttpPost httppost = new HttpPost("http://grupload.com/");
            if (gruploadAccount.loginsuccessful) {
                httppost.setHeader("Cookie", GRuploadAccount.getLogincookie() + ";" + GRuploadAccount.getXfsscookie());
            }
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("op", "upload_result"));
            formparams.add(new BasicNameValuePair("fn", fnvalue));
            formparams.add(new BasicNameValuePair("st", "OK"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            uploadresponse = EntityUtils.toString(httpresponse.getEntity());
            downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "Download Link", "</textarea>");
            downloadlink = downloadlink.substring(downloadlink.indexOf("http://"));
            NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
            deletelink = CommonUploaderTasks.parseResponse(uploadresponse, "Delete Link", "</textarea>");
            deletelink = deletelink.substring(deletelink.indexOf("http://"));
            NULogger.getLogger().log(Level.INFO, "Delete Link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
            httpclient.getConnectionManager().shutdown();
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }

    private void initialize() throws Exception {
        u = new URL("http://www.grupload.com");
        uc = (HttpURLConnection) u.openConnection();
        if (gruploadAccount.loginsuccessful) {
            uc.setRequestProperty("Cookie", GRuploadAccount.getLogincookie() + ";" + GRuploadAccount.getXfsscookie());
        }
        NULogger.getLogger().info("Getting dynamic grupload upload link value ........");
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        gruploadlink = CommonUploaderTasks.parseResponse(k, "action=\"", "\"");
        tmpserver = gruploadlink.substring(0, gruploadlink.indexOf("/cgi-bin"));
        NULogger.getLogger().log(Level.INFO, "Temp Server : {0}", tmpserver);
        UploadID = (long) (Math.floor(Math.random() * 900000000000L) + 100000000000L);
        gruploadlink += UploadID + "&js_on=1";
        NULogger.getLogger().info(gruploadlink);
    }

    public void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(gruploadlink);
        if (gruploadAccount.loginsuccessful) {
            httppost.setHeader("Cookie", GRuploadAccount.getLogincookie() + ";" + GRuploadAccount.getXfsscookie());
        }
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("upload_type", new StringBody("file"));
        if (gruploadAccount.loginsuccessful) {
            mpEntity.addPart("sess_id", new StringBody(GRuploadAccount.getXfsscookie().substring(5)));
        }
        mpEntity.addPart("srv_tmp_url", new StringBody(tmpserver + "/tmp"));
        mpEntity.addPart("file_0", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().info("Now uploading your file into grupload...........................");
        status = UploadStatus.UPLOADING;
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            String tmp = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "Upload response : {0}", tmp);
            fnvalue = CommonUploaderTasks.parseResponse(tmp, "name='fn'>", "<");
            NULogger.getLogger().log(Level.INFO, "fn value : {0}", fnvalue);
        } else {
            throw new Exception("There might be a problem with your internet connection or server error. Please try again some after time. :(");
        }
    }
}
