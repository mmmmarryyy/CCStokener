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
import javax.swing.JOptionPane;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.accounts.GigaSizeAccount;
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
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author dinesh
 */
public class GigaSize extends AbstractUploader {

    GigaSizeAccount GigaSizeAccount = (GigaSizeAccount) AccountsManager.getAccount("GigaSize.com");

    private HttpURLConnection uc;

    private BufferedReader br;

    private URL u;

    private StringBuilder gigasizecookies = null;

    private String downloadlink;

    private String uploadid;

    private String sid;

    static String referer = "";

    static final String UPLOAD_ID_CHARS = "1234567890abcdef";

    public GigaSize(File file) {
        super(file);
        downURL = UploadStatus.PLEASEWAIT.getLocaleSpecificString();
        delURL = UploadStatus.NA.getLocaleSpecificString();
        host = "GigaSize.com";
        if (GigaSizeAccount.loginsuccessful) {
            host = GigaSizeAccount.username + " | GigaSize.com";
        }
    }

    private void initialize() throws Exception {
        gigasizecookies = new StringBuilder();
        NULogger.getLogger().info("Getting startup cookies from gigasize.com");
        u = new URL("http://www.gigasize.com/");
        uc = (HttpURLConnection) u.openConnection();
        if (GigaSizeAccount.loginsuccessful) {
            gigasizecookies = GigaSizeAccount.getGigasizecookies();
            uc.setRequestProperty("Cookie", gigasizecookies.toString());
        } else {
            Map<String, List<String>> headerFields = uc.getHeaderFields();
            if (headerFields.containsKey("Set-Cookie")) {
                List<String> header = headerFields.get("Set-Cookie");
                for (int i = 0; i < header.size(); i++) {
                    String tmp = header.get(i);
                    gigasizecookies.append(tmp);
                }
                NULogger.getLogger().info(gigasizecookies.toString());
            }
        }
        generateGigasizeID();
    }

    public void generateGigasizeID() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int idx = 1 + (int) (Math.random() * 15);
            sb.append(UPLOAD_ID_CHARS.charAt(idx));
        }
        sid = sb.toString();
        NULogger.getLogger().log(Level.INFO, "sid : {0} - {1}", new Object[] { sid, sid.length() });
        uploadid = sid;
    }

    public String getData(String url) {
        try {
            u = new URL(url);
            uc = (HttpURLConnection) u.openConnection();
            uc.setDoOutput(true);
            uc.setRequestProperty("Host", "http://www.gigasize.com");
            uc.setRequestProperty("Connection", "keep-alive");
            uc.setRequestProperty("Referer", "http://gigasize.com/");
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1");
            uc.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uc.setRequestProperty("Accept-Encoding", "html");
            uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
            uc.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.setRequestProperty("Cookie", gigasizecookies.toString());
            uc.setRequestMethod("GET");
            uc.setInstanceFollowRedirects(false);
            br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String temp = "", k = "";
            while ((temp = br.readLine()) != null) {
                k += temp;
            }
            return k;
        } catch (Exception e) {
            NULogger.getLogger().log(Level.INFO, "exception : {0}", e.toString());
            return "";
        }
    }

    public void run() {
        try {
            if (GigaSizeAccount.loginsuccessful) {
                host = GigaSizeAccount.username + " | GigaSize.com";
            } else {
                host = "GigaSize.com";
            }
            if (file.length() > 2147483648l) {
                JOptionPane.showMessageDialog(neembuuuploader.NeembuuUploader.getInstance(), "<html><b>" + getClass().getSimpleName() + "</b> " + TranslationProvider.get("neembuuuploader.uploaders.maxfilesize") + ": <b>2GB</b></html>", getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                uploadFailed();
                return;
            }
            status = UploadStatus.INITIALISING;
            initialize();
            fileUpload();
            status = UploadStatus.GETTINGLINK;
            String uploadCompleteID;
            long randID;
            do {
                randID = (long) Math.floor(Math.random() * 90000000000000000L) + 10000000000L;
                uploadCompleteID = getData("http://www.gigasize.com/status.php?sid=" + sid + "&rnd=" + randID);
            } while (!uploadCompleteID.contains("done"));
            uploadCompleteID = CommonUploaderTasks.parseResponse(uploadCompleteID, "\\/uploadcompleteie\\/", "\"");
            NULogger.getLogger().log(Level.INFO, "Upload Complete ID : {0}", uploadCompleteID);
            referer = "http://www.gigasize.com/uploadcompleteie/" + uploadCompleteID;
            NULogger.getLogger().log(Level.INFO, "referer : {0}", referer);
            downloadlink = getData("http://www.gigasize.com/uploadcompleteie/" + uploadCompleteID);
            downloadlink = CommonUploaderTasks.parseResponse(downloadlink, "Download URL:</span> <a href=\"", "\"");
            NULogger.getLogger().log(Level.INFO, "Download URL : {0}", downloadlink);
            downURL = downloadlink;
            uploadFinished();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            uploadFailed();
        }
    }

    public void fileUpload() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost("http://www.gigasize.com/uploadie");
        httppost.setHeader("Cookie", gigasizecookies.toString());
        MultipartEntity mpEntity = new MultipartEntity();
        mpEntity.addPart("UPLOAD_IDENTIFIER", new StringBody(uploadid));
        mpEntity.addPart("sid", new StringBody(sid));
        mpEntity.addPart("fileUpload1", new MonitoredFileBody(file, uploadProgress));
        httppost.setEntity(mpEntity);
        status = UploadStatus.UPLOADING;
        NULogger.getLogger().info("Now uploading your file into Gigasize...........................");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        NULogger.getLogger().info(response.getStatusLine().toString());
        if (resEntity != null) {
            sid = "";
            sid = EntityUtils.toString(resEntity);
            NULogger.getLogger().log(Level.INFO, "After upload sid value : {0}", sid);
        } else {
            throw new Exception("There might be a problem with your internet connection or GigaSize server problem. Please try after some time :(");
        }
    }
}
