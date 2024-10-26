package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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
import neembuuuploader.accounts.WuploadAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

/**
 *
 * @author dinesh
 */
public class Wupload extends AbstractUploader {

    WuploadAccount wuploadAccount = (WuploadAccount) AccountsManager.getAccount("Wupload.com");

    private boolean login = false;

    private String uploadID = "";

    private URL u;

    private HttpURLConnection uc;

    private String rolecookie = "";

    private String langcookie = "";

    private String wuploadlink = "";

    private BufferedReader br;

    private String postURL = "";

    private String linkID = "";

    private String downloadlink = "";

    private String tmp;

    private String wudomain;

    public Wupload(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "Wupload.com";
        if (wuploadAccount.loginsuccessful) {
            login = true;
            host = wuploadAccount.username + " | Wupload.com";
        }
    }

    public void run() {
        try {
            if (file.length() > 2147483648l) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            if (wuploadAccount.loginsuccessful) {
                login = true;
                host = wuploadAccount.username + " | Wupload.com";
            } else {
                login = false;
                host = "Wupload.com";
            }
            if (login) {
                uploadID = "upload_" + new Date().getTime() + "_" + WuploadAccount.getSessioncookie().replace("PHPSESSID", "") + "_" + Math.round(Math.random() * 90000);
                wudomain = WuploadAccount.getWudomain();
            } else {
                uploadID = "upload_" + new Date().getTime() + "_" + Math.round(Math.random() * 90000);
                initialize();
            }
            status = UploadStatus.INITIALISING;
            NULogger.getLogger().info("Getting dynamic wupload upload link value ........");
            if (login) {
                wuploadlink = getData("http://www" + wudomain + "/file-manager/list");
            } else {
                wuploadlink = getData("http://www" + wudomain);
            }
            wuploadlink = CommonUploaderTasks.parseResponse(wuploadlink, "uploadServerHostname = '", "'");
            wuploadlink = "http://" + wuploadlink + "/";
            NULogger.getLogger().info(wuploadlink);
            postURL = wuploadlink + "?callbackUrl=http://www." + wudomain + "/upload/done/:uploadProgressId&X-Progress-ID=" + uploadID;
            NULogger.getLogger().log(Level.INFO, "post URL : {0}", postURL);
            fileUpload();
            getDownloadLink();
            uploadFinished();
        } catch (Exception ex) {
            Logger.getLogger(Wupload.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            uploadFailed();
        }
    }

    private void getDownloadLink() throws Exception {
        status = UploadStatus.GETTINGLINK;
        linkID = getData("http://www" + wudomain + "/upload/done/" + uploadID);
        NULogger.getLogger().log(Level.INFO, "****************************************\n{0}", linkID);
        linkID = CommonUploaderTasks.parseResponse(linkID, "\"linkId\":\"", "\"");
        NULogger.getLogger().log(Level.INFO, "Link ID: {0}", linkID);
        downloadlink = getData("http://www" + wudomain + "/file-manager/share/urls/" + linkID);
        downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "value=\"http://", "\"");
        NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
        downURL = "http://" + downloadlink;
    }

    public void fileUpload() throws Exception {
        status = UploadStatus.UPLOADING;
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(postURL);
        if (login) {
            httppost.setHeader("Cookie", WuploadAccount.getLangcookie() + ";" + WuploadAccount.getSessioncookie() + ";" + WuploadAccount.getMailcookie() + ";" + WuploadAccount.getNamecookie() + ";" + WuploadAccount.getRolecookie() + ";" + WuploadAccount.getOrderbycookie() + ";" + WuploadAccount.getDirectioncookie() + ";");
        } else {
            httppost.setHeader("Cookie", langcookie + ";" + rolecookie + ";");
        }
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("files[]", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().info("Now uploading your file into wupload...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
    }

    private void initialize() throws Exception {
        status = UploadStatus.INITIALISING;
        u = new URL("http://api.wupload.com/utility?method=getWuploadDomainForCurrentIp");
        br = new BufferedReader(new InputStreamReader(u.openStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        wudomain = CommonUploaderTasks.parseResponse(k, "\"response\":\"", "\"");
        NULogger.getLogger().log(Level.INFO, "WUpload Domain: {0}", wudomain);
        NULogger.getLogger().info("Getting startup cookies from wupload.com");
        u = new URL("http://www" + wudomain + "/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = "";
                tmp = header.get(i);
                if (tmp.contains("role")) {
                    rolecookie = tmp;
                    rolecookie = rolecookie.substring(0, rolecookie.indexOf(";"));
                }
                if (tmp.contains("lang")) {
                    langcookie = tmp;
                    langcookie = langcookie.substring(0, langcookie.indexOf(";"));
                }
            }
            NULogger.getLogger().log(Level.INFO, "role cookie : {0}", rolecookie);
            NULogger.getLogger().log(Level.INFO, "lang cookie : {0}", langcookie);
        }
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www" + wudomain);
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://wupload.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (login) {
            uc.setRequestProperty("Cookie", WuploadAccount.getLangcookie() + ";" + WuploadAccount.getSessioncookie() + ";" + WuploadAccount.getMailcookie() + ";" + WuploadAccount.getNamecookie() + ";" + WuploadAccount.getRolecookie() + ";" + WuploadAccount.getOrderbycookie() + ";" + WuploadAccount.getDirectioncookie() + ";");
        } else {
            uc.setRequestProperty("Cookie", langcookie + ";" + rolecookie + ";");
        }
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
}
