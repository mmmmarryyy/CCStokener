package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.NetLoadAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author dinesh
 */
public class NetLoad extends AbstractUploader {

    NetLoadAccount netLoadAccount = (NetLoadAccount) AccountsManager.getAccount("Netload.in");

    private HttpURLConnection uc;

    private BufferedReader br;

    private String uploadresponse;

    private String downloadlink;

    private URL u;

    private String deletelink;

    private String uid;

    private String phpsessioncookie;

    private String usercookie;

    private String postURL;

    private String upload_hash;

    public NetLoad(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "Netload.in";
        if (netLoadAccount.loginsuccessful) {
            host = netLoadAccount.username + " | Netload.in";
        }
    }

    private String getData(String myurl) throws Exception {
        URL url = new URL(myurl);
        uc = (HttpURLConnection) url.openConnection();
        uc.setRequestProperty("Cookie", NetLoadAccount.getPhpsessioncookie());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        return k;
    }

    private void initialize() throws Exception {
        if (netLoadAccount.loginsuccessful) {
            u = new URL("http://netload.in/index.php");
        } else {
            u = new URL("http://netload.in/");
        }
        uc = (HttpURLConnection) u.openConnection();
        if (netLoadAccount.loginsuccessful) {
            uc.setRequestProperty("Cookie", NetLoadAccount.getPhpsessioncookie() + ";" + NetLoadAccount.getUsercookie());
            NULogger.getLogger().info("After login success, getting netload page again for post url & upload hash value......");
        } else {
            NULogger.getLogger().info("gettig netload post url.......");
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        if (!netLoadAccount.loginsuccessful) {
            Map<String, List<String>> headerFields = uc.getHeaderFields();
            if (headerFields.containsKey("Set-Cookie")) {
                List<String> header = headerFields.get("Set-Cookie");
                for (int i = 0; i < header.size(); i++) {
                    String tmp = header.get(i);
                    if (tmp.contains("PHPSESSID")) {
                        phpsessioncookie = tmp;
                        phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
                    }
                }
            }
            NULogger.getLogger().log(Level.INFO, "PHP session cookie : {0}", phpsessioncookie);
        }
        postURL = CommonUploaderTasks.parseResponse(k, "action=\"http://", "\"");
        postURL = "http://" + postURL;
        NULogger.getLogger().log(Level.INFO, "postURL : {0}", postURL);
        if (netLoadAccount.loginsuccessful) {
            upload_hash = CommonUploaderTasks.parseResponse(k, "\"upload_hash\" value=\"", "\"");
            NULogger.getLogger().log(Level.INFO, "Upload hash : {0}", upload_hash);
        }
    }

    public void run() {
        try {
            if (netLoadAccount.loginsuccessful) {
                host = netLoadAccount.username + " | Netload.in";
            } else {
                host = "Netload.in";
            }
            status = UploadStatus.INITIALISING;
            initialize();
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(postURL);
            if (netLoadAccount.loginsuccessful) {
                httppost.setHeader("Cookie", usercookie);
            }
            MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            if (netLoadAccount.loginsuccessful) {
                mpEntity.addPart("upload_hash", new StringBody(upload_hash));
            }
            mpEntity.addPart("file", new MonitoredFileBody(file, uploadProgress));
            httppost.setEntity(mpEntity);
            NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
            NULogger.getLogger().info("Now uploading your file into netload");
            status = UploadStatus.UPLOADING;
            HttpResponse response = httpclient.execute(httppost);
            status = UploadStatus.GETTINGLINK;
            HttpEntity resEntity = response.getEntity();
            NULogger.getLogger().info(response.getStatusLine().toString());
            httpclient.getConnectionManager().shutdown();
            if (response.containsHeader("Location")) {
                Header firstHeader = response.getFirstHeader("Location");
                NULogger.getLogger().info(firstHeader.getValue());
                uploadresponse = getData(firstHeader.getValue());
                downloadlink = CommonUploaderTasks.parseResponse(uploadresponse, "The download link is: <br/>", "\" target=\"_blank\">");
                downloadlink = downloadlink.substring(downloadlink.indexOf("href=\""));
                downloadlink = downloadlink.replace("href=\"", "");
                NULogger.getLogger().log(Level.INFO, "download link : {0}", downloadlink);
                deletelink = CommonUploaderTasks.parseResponse(uploadresponse, "The deletion link is: <br/>", "\" target=\"_blank\">");
                deletelink = deletelink.substring(deletelink.indexOf("href=\""));
                deletelink = deletelink.replace("href=\"", "");
                NULogger.getLogger().log(Level.INFO, "delete link : {0}", deletelink);
                downURL = downloadlink;
                delURL = deletelink;
                uploadFinished();
            } else {
                throw new Exception("There might be a problem with your internet connection or server error. Please try after some time :(");
            }
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
