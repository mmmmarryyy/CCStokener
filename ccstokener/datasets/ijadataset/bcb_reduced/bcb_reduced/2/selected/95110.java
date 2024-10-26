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
import neembuuuploader.accounts.LetitbitAccount;
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
public class Letitbit extends AbstractUploader {

    LetitbitAccount letitbitAccount = (LetitbitAccount) AccountsManager.getAccount("Letitbit.net");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String tmp;

    private String phpsessioncookie, debugcookie = "", downloadlink = "", deletelink = "";

    private String server, postURL = "";

    private String base;

    private String uploadresponse;

    private String uploadpage;

    private String pin = "";

    private String uid;

    public Letitbit(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "Letitbit.net";
        if (letitbitAccount.loginsuccessful) {
            host = letitbitAccount.username + " | Letitbit.net";
        }
    }

    private void initialize() throws Exception {
        NULogger.getLogger().info("Getting startup cookie from letitbit.net");
        u = new URL("http://www.letitbit.net/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                tmp = header.get(i);
                if (tmp.contains("PHPSESSID")) {
                    phpsessioncookie = tmp;
                }
                if (tmp.contains("debug_panel")) {
                    debugcookie = tmp;
                }
            }
        }
        phpsessioncookie = phpsessioncookie.substring(0, phpsessioncookie.indexOf(";"));
        debugcookie = debugcookie.substring(0, debugcookie.indexOf(";"));
        NULogger.getLogger().log(Level.INFO, "phpsessioncookie: {0}", phpsessioncookie);
        NULogger.getLogger().log(Level.INFO, "debugcookie : {0}", debugcookie);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        server = CommonUploaderTasks.parseResponse(k, "ACUPL_UPLOAD_SERVER = '", "'");
        base = CommonUploaderTasks.parseResponse(k, "\"base\" type=\"hidden\" value=\"", "\"");
        NULogger.getLogger().log(Level.INFO, "base : {0}", base);
        generateLetitbitID();
        NULogger.getLogger().log(Level.INFO, "server : {0}", server);
        postURL = "http://" + server + "/marker=" + uid;
        NULogger.getLogger().log(Level.INFO, "Post URL :{0}", postURL);
    }

    private void generateLetitbitID() throws Exception {
        String rand = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        sb.append(Long.toHexString(System.currentTimeMillis()).toUpperCase());
        sb.append("_");
        for (int i = 0; i < 40; i++) {
            sb.append(rand.charAt((int) Math.round(1 + (int) (Math.random() * 60))));
        }
        uid = sb.toString();
    }

    private void getData() throws Exception {
        u = new URL("http://www.letitbit.net/");
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", LetitbitAccount.getPhpsessioncookie() + ";" + LetitbitAccount.getDebugcookie() + ";" + LetitbitAccount.getLogcookie() + ";" + LetitbitAccount.getPascookie() + ";" + LetitbitAccount.getHostcookie());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "";
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        server = CommonUploaderTasks.parseResponse(k, "ACUPL_UPLOAD_SERVER = '", "'");
        base = CommonUploaderTasks.parseResponse(k, "\"base\" type=\"hidden\" value=\"", "\"");
        pin = CommonUploaderTasks.parseResponse(k, "\"pin\" type=\"hidden\" value=\"", "\"");
        NULogger.getLogger().log(Level.INFO, "pin : {0}", pin);
        NULogger.getLogger().log(Level.INFO, "base : {0}", base);
        generateLetitbitID();
        NULogger.getLogger().log(Level.INFO, "server : {0}", server);
        postURL = "http://" + server + "/marker=" + uid;
        NULogger.getLogger().log(Level.INFO, "Post URL :{0}", postURL);
    }

    private String getData(String geturl) throws Exception {
        String k = "";
        u = new URL(geturl);
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("Cookie", LetitbitAccount.getPhpsessioncookie() + ";" + LetitbitAccount.getDebugcookie() + ";" + LetitbitAccount.getLogcookie() + ";" + LetitbitAccount.getPascookie() + ";" + LetitbitAccount.getHostcookie());
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        while ((tmp = br.readLine()) != null) {
            k += tmp;
        }
        return k;
    }

    private void fileUpload() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(postURL);
        MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        mpEntity.addPart("MAX_FILE_SIZE", new StringBody("2147483647"));
        mpEntity.addPart("owner", new StringBody(""));
        mpEntity.addPart("pin", new StringBody(pin));
        mpEntity.addPart("base", new StringBody(base));
        mpEntity.addPart("host", new StringBody("letitbit.net"));
        mpEntity.addPart("file0", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().log(Level.INFO, "executing request {0}", httppost.getRequestLine());
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into letitbit.net");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
        }
        NULogger.getLogger().log(Level.INFO, "Upload response : {0}", uploadresponse);
    }

    public void run() {
        if (letitbitAccount.loginsuccessful) {
            host = letitbitAccount.username + " | Letitbit.net";
        } else {
            host = "Letitbit.net";
        }
        try {
            status = UploadStatus.INITIALISING;
            if (letitbitAccount.loginsuccessful) {
                getData();
            } else {
                initialize();
            }
            fileUpload();
            status = UploadStatus.GETTINGLINK;
            uploadresponse = "http://letitbit.net/acupl_proxy.php?srv=" + server + "&uid=" + uid;
            tmp = getData(uploadresponse);
            tmp = CommonUploaderTasks.parseResponse(tmp, "\"post_result\": \"", "\"");
            NULogger.getLogger().log(Level.INFO, "upload page : {0}", tmp);
            uploadpage = getData(tmp);
            downloadlink = CommonUploaderTasks.parseResponse(uploadpage, "Links to download files:", "</textarea>");
            downloadlink = downloadlink.substring(downloadlink.lastIndexOf(">") + 1);
            deletelink = CommonUploaderTasks.parseResponse(uploadpage, "Links to delete files:", "</div>");
            deletelink = deletelink.replace("<br/>", "");
            deletelink = deletelink.substring(deletelink.lastIndexOf(">") + 1);
            NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
            NULogger.getLogger().log(Level.INFO, "Delete Link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(Letitbit.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
