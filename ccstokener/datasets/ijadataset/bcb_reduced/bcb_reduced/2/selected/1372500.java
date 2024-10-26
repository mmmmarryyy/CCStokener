package neembuuuploader.uploaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.SugarSyncAccount;
import neembuuuploader.interfaces.UploadStatus;
import neembuuuploader.interfaces.UploaderAccountNecessary;
import neembuuuploader.interfaces.abstractimpl.AbstractUploader;
import neembuuuploader.uploaders.common.CommonUploaderTasks;
import neembuuuploader.uploaders.common.MonitoredFileBody;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author dinesh
 */
public class SugarSync extends AbstractUploader implements UploaderAccountNecessary {

    SugarSyncAccount sugarSyncAccount = (SugarSyncAccount) AccountsManager.getAccount("SugarSync.com");

    private URL u;

    private HttpURLConnection uc;

    private String USER_INFO_API_URL = "https://api.sugarsync.com/user";

    private String CREATE_FILE_REQUEST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<file>" + "<displayName>%s</displayName>" + "<mediaType>%s</mediaType>" + "</file>";

    private BufferedReader br;

    private String upload_folder_url;

    private PrintWriter pw;

    private String SugarSync_File_Upload_URL;

    public SugarSync(File file) {
        super(file);
        host = "SugarSync.com";
        downURL = UploadStatus.NA.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        if (sugarSyncAccount.loginsuccessful) {
            host = sugarSyncAccount.username + " | SugarSync.com";
        }
    }

    private void getUserInfo() throws Exception {
        u = new URL(USER_INFO_API_URL);
        uc = (HttpURLConnection) u.openConnection();
        uc.setRequestMethod("GET");
        uc.setRequestProperty("Authorization", SugarSyncAccount.getAuth_token());
        uc.setRequestProperty("Host", "api.sugarsync.com");
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "", tmp = "";
        while ((tmp = br.readLine()) != null) {
            NULogger.getLogger().info(tmp);
            k += tmp;
        }
        upload_folder_url = CommonUploaderTasks.parseResponse(k, "<magicBriefcase>", "</magicBriefcase>");
        NULogger.getLogger().log(Level.INFO, "Upload_Folder : {0}", upload_folder_url);
    }

    public void setHttpHeader() throws Exception {
        uc = (HttpURLConnection) u.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("Host", "www.sugarsync.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Referer", "https://www.sugarsync.com/");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
        uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        uc.setRequestProperty("Accept-Encoding", "html");
        uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
        uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        uc.setRequestProperty("Content-Type", "application/xml");
        uc.setRequestProperty("Authorization", SugarSyncAccount.getAuth_token());
        uc.setRequestMethod("POST");
        uc.setInstanceFollowRedirects(false);
    }

    public void writeHttpContent(String content) throws Exception {
        pw = new PrintWriter(new OutputStreamWriter(uc.getOutputStream()), true);
        pw.print(content);
        pw.flush();
        pw.close();
        NULogger.getLogger().log(Level.INFO, "res : {0}", uc.getResponseCode());
        if (uc.getResponseCode() == 401) {
            NULogger.getLogger().info("hey SugarSync login failed :(");
            return;
        }
        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String k = "", tmp = "";
        while ((tmp = br.readLine()) != null) {
            NULogger.getLogger().info(tmp);
            k += tmp;
        }
        if (uc.getHeaderFields().containsKey("Location")) {
            SugarSync_File_Upload_URL = uc.getHeaderField("Location");
            SugarSync_File_Upload_URL = SugarSync_File_Upload_URL + "/data";
            NULogger.getLogger().log(Level.INFO, "Post URL : {0}", SugarSync_File_Upload_URL);
        } else {
            NULogger.getLogger().info("There might be problem interface getting Upload URL from SugarSync. Please try after some time :(");
        }
    }

    public void postData(String content, String posturl) throws Exception {
        u = new URL(posturl);
        setHttpHeader();
        writeHttpContent(content);
        u = null;
        uc = null;
    }

    @Override
    public void run() {
        try {
            if (sugarSyncAccount.loginsuccessful) {
                host = sugarSyncAccount.username + " | SugarSync.com";
            } else {
                host = "SugarSync.com";
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            getUserInfo();
            String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String CREATE_FILE_REQUEST = String.format(CREATE_FILE_REQUEST_TEMPLATE, file.getName(), ext + " file");
            NULogger.getLogger().info("now creating file request............");
            postData(CREATE_FILE_REQUEST, upload_folder_url);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPut httpput = new HttpPut(SugarSync_File_Upload_URL);
            httpput.setHeader("Authorization", SugarSyncAccount.getAuth_token());
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("", new MonitoredFileBody(file, uploadProgress));
            httpput.setEntity(reqEntity);
            NULogger.getLogger().info("Now uploading your file into sugarsync........ Please wait......................");
            NULogger.getLogger().log(Level.INFO, "Now executing.......{0}", httpput.getRequestLine());
            HttpResponse response = httpclient.execute(httpput);
            NULogger.getLogger().info(response.getStatusLine().toString());
            if (response.getStatusLine().getStatusCode() == 204) {
                NULogger.getLogger().info("File uploaded successfully :)");
                uploadFinished();
            } else {
                throw new Exception("There might be problem with your internet connection or server error. Please try again some after time :(");
            }
        } catch (Exception e) {
            Logger.getLogger(RapidShare.class.getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }
}
