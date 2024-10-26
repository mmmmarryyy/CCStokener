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
import neembuuuploader.accounts.FileServeAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
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
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class FileServe extends AbstractUploader implements UploaderAccountNecessary {

    FileServeAccount FileServeAccount = (FileServeAccount) AccountsManager.getAccount("FileServe.com");

    private URL u;

    private HttpURLConnection uc;

    private BufferedReader br;

    private String postURL = "";

    private String uploadresponse = "";

    private String downloadlink = "", deletelink = "";

    private String sessioncookie = "";

    private PrintWriter pw;

    private String tmpURL = "";

    public FileServe(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        host = "FileServe.com";
        if (FileServeAccount.loginsuccessful) {
            host = FileServeAccount.username + " | FileServe.com";
        }
    }

    public void run() {
        try {
            if (FileServeAccount.loginsuccessful) {
                host = FileServeAccount.username + " | FileServe.com";
            } else {
                host = "FileServe.com";
                uploadFailed();
                return;
            }
            String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            if (ext.equals("html") || ext.equals("js") || ext.equals("css") || ext.equals("jsp") || ext.equals("asp") || ext.equals("aspx") || ext.equals("php")) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.filetypenotsupported") + ": <b>html</b>, <b>js</b>, <b>css</b>, <b>asp</b>, <b>aspx</b>, <b>jsp</b>, <b>php</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            NULogger.getLogger().log(Level.INFO, "File Length : {0}", file.length());
            if (file.length() > 1024 * 1024 * 1024) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>1GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            tmpURL = CommonUploaderTasks.parseResponse(getData("http://fileserve.com/upload-file.php"), "http://upload.fileserve.com/upload/", "\"");
            postURL = "http://upload.fileserve.com/upload/" + tmpURL;
            long uploadtime = new Date().getTime();
            tmpURL = "http://upload.fileserve.com/upload/" + tmpURL + "?callback=jQuery" + Math.round((Math.random() * 1000000000000000000L) + 1000000000000000000L) + "_" + uploadtime + "&_=" + (uploadtime + Math.round(Math.random() * 100000));
            System.out.println("tmp URL : " + tmpURL);
            String sessionid = getData(tmpURL);
            sessionid = CommonUploaderTasks.parseResponse(sessionid, "sessionId:'", "'");
            System.out.println("Session ID  : " + sessionid);
            postURL += sessionid;
            System.out.println("post URL : " + postURL);
            fileUpload();
            uploadFinished();
        } catch (Exception ex) {
            Logger.getLogger(FileServe.class.getName()).log(Level.SEVERE, null, ex);
            uploadFailed();
        }
    }

    public void initialize() throws IOException {
        status = UploadStatus.INITIALISING;
        NULogger.getLogger().info("Getting start up cookie from FileServe.com");
        u = new URL("http://www.fileserve.com/");
        uc = (HttpURLConnection) u.openConnection();
        Map<String, List<String>> headerFields = uc.getHeaderFields();
        if (headerFields.containsKey("Set-Cookie")) {
            List<String> header = headerFields.get("Set-Cookie");
            for (int i = 0; i < header.size(); i++) {
                String tmp = header.get(i);
                if (tmp.contains("PHPSESSID")) {
                    sessioncookie = tmp;
                    sessioncookie = sessioncookie.substring(0, sessioncookie.indexOf(";"));
                }
            }
            NULogger.getLogger().log(Level.INFO, "session cookie : {0}", sessioncookie);
        }
    }

    public String getData(String url) throws Exception {
        u = new URL(url);
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www.fileserve.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "http://fileserve.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (FileServeAccount.loginsuccessful) {
            uc.setRequestProperty("Cookie", FileServeAccount.getDashboardcookie());
        } else {
            uc.setRequestProperty("Cookie", sessioncookie);
        }
        uc.setRequestMethod("GET");
        uc.setInstanceFollowRedirects(false);
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String temp = "", k = "";
        while ((temp = br.readLine()) != null) {
            k += temp;
        }
        br.close();
        return k;
    }

    public void fileUpload() throws Exception {
        status = UploadStatus.UPLOADING;
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(postURL);
        httppost.setHeader("Cookie", FileServeAccount.getDashboardcookie());
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("files", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        NULogger.getLogger().info("Now uploading your file into fileserve...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            uploadresponse = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "upload response : {0}", uploadresponse);
            status = UploadStatus.GETTINGLINK;
            String shortencode = CommonUploaderTasks.parseResponse(uploadresponse, "\"shortenCode\":\"", "\"");
            String fileName = CommonUploaderTasks.parseResponse(uploadresponse, "\"fileName\":\"", "\"");
            String deleteCode = CommonUploaderTasks.parseResponse(uploadresponse, "\"deleteCode\":\"", "\"");
            downloadlink = "http://www.fileserve.com/file/" + shortencode + "/" + fileName;
            deletelink = "http://www.fileserve.com/file/" + shortencode + "/delete/" + deleteCode;
            NULogger.getLogger().log(Level.INFO, "Download Link : {0}", downloadlink);
            NULogger.getLogger().log(Level.INFO, "Delete Link : {0}", deletelink);
            downURL = downloadlink;
            delURL = deletelink;
            httpclient.getConnectionManager().shutdown();
        } else {
            throw new Exception("There might be a problem with your internet connection or server error. Please try again later :(");
        }
    }
}
