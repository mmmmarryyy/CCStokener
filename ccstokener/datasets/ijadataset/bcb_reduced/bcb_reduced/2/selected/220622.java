package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.RapidShareAccount;
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
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Dinesh
 */
public class RapidShare extends AbstractUploader implements UploaderAccountNecessary {

    RapidShareAccount rapidShareAccount = (RapidShareAccount) AccountsManager.getAccount("RapidShare.com");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL;

    private String uploadresponse;

    private String downloadid;

    private String filename;

    private String downloadlink;

    public RapidShare(File file) {
        super(file);
        host = "RapidShare.com";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        if (rapidShareAccount.loginsuccessful) {
            host = rapidShareAccount.username + " | RapidShare.com";
        }
    }

    public void run() {
        try {
            if (rapidShareAccount.loginsuccessful) {
                host = rapidShareAccount.username + " | RapidShare.com";
            } else {
                host = "RapidShare.com";
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            NULogger.getLogger().info("Now getting dynamic rs link");
            String link = getData("http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=nextuploadserver&cbid=3&cbf=rs.jsonp.callback");
            link = link.substring(link.indexOf("\"") + 1);
            link = link.substring(0, link.indexOf("\""));
            NULogger.getLogger().log(Level.INFO, "rs link : {0}", link);
            long uploadID = (long) Math.floor(Math.random() * 90000000000L) + 10000000000L;
            postURL = "http://rs" + link + "l3.rapidshare.com/cgi-bin/rsapi.cgi?uploadid=" + uploadID;
            NULogger.getLogger().log(Level.INFO, "rapidshare : {0}", postURL);
            fileUpload();
        } catch (Exception e) {
            Logger.getLogger(RapidShare.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www.rapidshare.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://rapidshare.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
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

    public void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("sub", new StringBody("upload"));
        mpEntity.addPart("cookie", new StringBody(RapidShareAccount.getRscookie()));
        mpEntity.addPart("folder", new StringBody("0"));
        mpEntity.addPart("filecontent", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into rs...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            status = UploadStatus.GETTINGLINK;
            uploadresponse = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "Actual response : {0}", uploadresponse);
            uploadresponse = uploadresponse.replace("COMPLETE\n", "");
            downloadid = uploadresponse.substring(0, uploadresponse.indexOf(","));
            uploadresponse = uploadresponse.replace(downloadid + ",", "");
            filename = uploadresponse.substring(0, uploadresponse.indexOf(","));
            NULogger.getLogger().log(Level.INFO, "download id : {0}", downloadid);
            NULogger.getLogger().log(Level.INFO, "File name : {0}", filename);
            downloadlink = "http://rapidshare.com/files/" + downloadid + "/" + filename;
            NULogger.getLogger().log(Level.INFO, "Download Link :{0}", downloadlink);
            downURL = downloadlink;
            uploadFinished();
        }
    }
}
