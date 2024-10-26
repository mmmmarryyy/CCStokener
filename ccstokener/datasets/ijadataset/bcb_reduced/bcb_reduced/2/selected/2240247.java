package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.ZShareAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
public class ZShare extends AbstractUploader {

    ZShareAccount zShareAccount = (ZShareAccount) AccountsManager.getAccount("ZShare.net");

    static final String UPLOAD_ID_CHARS = "1234567890qwertyuiopasdfghjklzxcvbnm";

    private URL u;

    private BufferedReader br;

    private HttpURLConnection uc;

    private PrintWriter pw;

    private String sidcookie = "";

    private String tmp = "";

    private String phpcookie = "";

    private String mysessioncookie = "";

    private String zsharelink = "";

    private String uplodid = "";

    private String postURL = "";

    private String uploadresponse;

    private String linkpage;

    private String downloadlink = "";

    private String deletelink;

    public ZShare(File file) {
        super(file);
        host = "ZShare.net";
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        if (zShareAccount.loginsuccessful) {
            host = zShareAccount.username + " | ZShare.net";
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from zshare.net");
        u = new URL("http://www.zshare.net/");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                sidcookie = tmp;
            }
        }
        sidcookie = sidcookie.substring(0, sidcookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "Cookie : {0}", sidcookie);
        br = new BufferedReader(new InputStreamReader(u.openStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        NULogger.getLogger().info("Getting zshare dynamic upload link");
        zsharelink = CommonUploaderTasks.parseResponse(k, "action=\"", "\"");
        zsharelink = zsharelink.toLowerCase();
        NULogger.getLogger().info(zsharelink);
    }

    private String getData(String myurl) throws Exception {
        URL url = new URL(myurl);
        uc = (HttpURLConnection) url.openConnection();
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        return k;
    }

    public void generateZShareID() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int idx = 1 + (int) (Math.random() * 35);
            sb.append(UPLOAD_ID_CHARS.charAt(idx));
        }
        uplodid = sb.toString();
        NULogger.getLogger().log(Level.INFO, "Upload id : {0}", uplodid);
    }

    private void fileUpload() throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        if (zShareAccount.loginsuccessful) {
            httppost.setHeader("Cookie", zShareAccount.getSidcookie() + ";" + zShareAccount.getMysessioncookie());
        } else {
            httppost.setHeader("Cookie", sidcookie + ";" + mysessioncookie);
        }
        generateZShareID();
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("", new MonitoredFileBody(file, uploadProgress));
        mpEntity.addPart("TOS", new StringBody("1"));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into zshare.net");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        uploadresponse = uploadresponse.replaceAll("\n", "");
        uploadresponse = uploadresponse.substring(uploadresponse.indexOf("index2.php"));
        uploadresponse = uploadresponse.substring(0, uploadresponse.indexOf("\">here"));
        uploadresponse = uploadresponse.replaceAll("amp;", "");
        if (zShareAccount.loginsuccessful) {
            uploadresponse = zShareAccount.getZsharelink() + uploadresponse;
        } else {
            uploadresponse = zsharelink + uploadresponse;
        }
        uploadresponse = uploadresponse.replaceAll(" ", "%20");
        NULogger.getLogger().log(Level.INFO, "resp : {0}", uploadresponse);
        httpclient.getConnectionManager().shutdown();
    }

    private void getDownloadLink() throws Exception {
        status = UploadStatus.GETTINGLINK;
        NULogger.getLogger().info("Now Getting Download link...");
        HttpClient client = new DefaultHttpClient();
        HttpGet h = new HttpGet(uploadresponse);
        h.setHeader("Referer", postURL);
        if (zShareAccount.loginsuccessful) {
            h.setHeader("Cookie", zShareAccount.getSidcookie() + ";" + zShareAccount.getMysessioncookie());
        } else {
            h.setHeader("Cookie", sidcookie + ";" + mysessioncookie);
        }
        HttpResponse res = client.execute(h);
        HttpEntity entity = res.getEntity();
        linkpage = EntityUtils.toString(entity);
        linkpage = linkpage.replaceAll("\n", "");
        downloadlink = CommonUploaderTasks.parseResponse(linkpage, "value=\"", "\"");
        deletelink = CommonUploaderTasks.parseResponse(linkpage, "delete.html?", "\"");
        deletelink = "http://www.zshare.net/delete.html?" + deletelink;
        downURL = downloadlink;
        delURL = deletelink;
        NULogger.getLogger().log(Level.INFO, "Download link : {0}", downloadlink);
        NULogger.getLogger().log(Level.INFO, "Delete Link : {0}", deletelink);
        uploadFinished();
    }

    public void run() {
        try {
            if (zShareAccount.loginsuccessful) {
                host = zShareAccount.username + " | ZShare.com";
            } else {
                host = "ZShare.com";
            }
            if (file.length() > 1024 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            if (zShareAccount.loginsuccessful) {
                NULogger.getLogger().info("Getting upload id ......");
                uplodid = getData(zShareAccount.getZsharelink() + "uberupload/ubr_link_upload.php?rnd_id=" + new Date().getTime());
                uplodid = CommonUploaderTasks.parseResponse(uplodid, "startUpload(\"", "\"");
                NULogger.getLogger().log(Level.INFO, "Upload id : {0}", uplodid);
                postURL = zShareAccount.getZsharelink() + "cgi-bin/ubr_upload.pl?upload_id=" + uplodid + "&multiple=0&is_private=0&is_eighteen=0&pass=&descr=";
            } else {
                initialize();
                NULogger.getLogger().info("Getting upload id ......");
                uplodid = getData(zsharelink + "uberupload/ubr_link_upload.php?rnd_id=" + new Date().getTime());
                uplodid = CommonUploaderTasks.parseResponse(uplodid, "startUpload(\"", "\"");
                NULogger.getLogger().log(Level.INFO, "Upload id : {0}", uplodid);
                postURL = zsharelink + "cgi-bin/ubr_upload.pl?upload_id=" + uplodid + "&multiple=0&is_private=0&is_eighteen=0&pass=&descr=";
            }
            fileUpload();
            getDownloadLink();
        } catch (Exception e) {
            Logger.getLogger(ZShare.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
